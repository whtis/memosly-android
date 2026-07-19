# Memos Android - Claude 开发指南

## 项目概况
- Memos Android 客户端，对接 Memos 开源服务端
- Kotlin + Jetpack Compose + MVVM + Clean Architecture
- 16 模块：1 app + 7 core + 8 feature
- minSdk 26, targetSdk 35, compileSdk 35

## 多版本兼容（最重要）
App 必须同时兼容 Memos server v0.24、v0.25、v0.26。
- **任何 API 修改必须先确认目标服务器版本**
- **修一个版本不能破坏其他版本** — 始终用版本分支逻辑
- 版本检测存储在 `SessionPreferences.serverVersion`

### 已知版本差异
| 功能 | v0.24/v0.25 | v0.26 |
|------|-------------|-------|
| 资源端点 | `api/v1/resources` | `api/v1/attachments` |
| 用户统计 | `users/{id}/stats` | `users/{id}:getStats` |
| 访问令牌 | `access_tokens` | `personalAccessTokens` |
| MemoRelation | string/numeric | object/string enum |
| 枚举序列化 | 数字 (1,2,3) | 字符串 ("NORMAL","ARCHIVED") |
| 关联附件 | v0.24: `PATCH memos/{id}/resources`<br>v0.25: `PATCH memos/{id}/attachments` | `PATCH memos/{id}/attachments` |

**关联附件一律是 PATCH，不是 POST** —— proto 里绑的是 `patch:`，发 POST 会得到
`501 Method Not Allowed`。这个错在 v0.24 上藏了很久（详见下方 2026-07-19 修复记录）。

## 架构与技术栈
- **构建**: build-logic convention plugins (Application, Library, Compose, Hilt, Feature)
- **Feature 插件**: 自动依赖 core:common, core:model, core:data, core:ui + lifecycle/navigation/hilt
- **网络**: Retrofit + OkHttp + Moshi (KSP codegen)
- **DI**: Hilt
- **分页**: Paging 3 + `_refreshTrigger` StateFlow 强制刷新
- **图片**: Coil 3
- **Markdown**: CommonMark + GFM 扩展
- **导航**: Compose Navigation + savedStateHandle 跨屏通信

## 关键开发模式

### 资源上传
- 服务端期望 JSON `{filename, type, content}`，content 是 base64
- 不是 multipart form data（gRPC-gateway 把 protobuf bytes 映射为 base64 JSON）
- v0.24 和 v0.26 格式相同，仅端点不同
- **全内存编码**：bytes → base64 字符串 → JSON，峰值堆约为文件大小的 3-4 倍，且未开 `largeHeap`。
  上传前必须按 `MAX_UPLOAD_BYTES`（32MB，对齐服务端 `MEMOS_MAX_UPLOAD_SIZE_MIB` 默认值）拦截，
  且要在读字节**之前**用 `OpenableColumns.SIZE` 判断 —— 读进来再判断就已经 OOM 了
- 多文件必须**串行**上传（`MemoEditorViewModel.uploadMutex`），并发会同时爆内存和状态

### 系统分享接入
- Manifest 注册两组 filter：`SEND` + `text/plain`（文本），`SEND`/`SEND_MULTIPLE` + `image/*`/`video/*`（媒体）
- 文本走导航参数；媒体的 Uri 走 `SharedMediaBuffer`（Hilt singleton）—— 可能多个、可能很长，
  且读权限属于接收 Activity，塞不进路由字符串
- **分享是文本或媒体，二选一**：媒体分享忽略发送方附带的 EXTRA_TEXT
- **待处理的分享必须无条件消费**：编辑器已开着时会跳过导航保住草稿，但如果不消费 flag，
  它会在路由一变时重新触发，把早就过期的分享塞进用户刚打开的东西里
- 未登录时分享会挂起，登录后自动续上（`LaunchedEffect` 在 `AUTH_ROUTE` 上不消费）

### 发文/编辑后刷新
- 编辑器保存时：`previousBackStackEntry.savedStateHandle.set("memo_saved", true)`
- 列表页用 `LaunchedEffect(refreshOnReturn)` 监听，触发 `viewModel.refresh()`
- Tab 模式下同时刷新 Home 和 Explore 的 ViewModel

### 评论即时反馈（两阶段更新）
1. 立即把新评论追加到本地 `_commentPreviews`
2. 后台从服务器重新加载完整评论列表覆盖
3. 输入框立即清空，用户无感等待

### 分页数据刷新
- `_refreshTrigger` MutableStateFlow(0) 参与 `combine` 进 paging flow
- `refresh()` 中 `_refreshTrigger.value++` 触发 `flatMapLatest` 重新发起请求

### 视频检测
- 通过文件扩展名判断：mp4, webm, mov, avi, mkv, 3gp, m4v
- Android ContentResolver 可能丢失扩展名 → 上传时根据 MIME type 补回

### MIME 类型检测链
```
ContentResolver.getType(uri)
  → MimeTypeMap.getMimeTypeFromExtension(扩展名)
    → guessMimeTypeFromBytes(魔数: JPEG/PNG/GIF/WEBP/MP4)
      → rawMimeType
        → "application/octet-stream"
```

### OkHttp 日志
- 用 `Level.HEADERS` 不用 `Level.BODY`，避免上传时 base64 刷屏

### Token 持久化（关键模式）
- `TokenManager` 是内存单例（`MutableStateFlow`），进程被杀后会丢失
- Android 进程死亡 + Navigation 恢复回退栈 → 跳过 AuthScreen → TokenManager 为空
- **修复**：`TokenManager` 构造函数中从 `SessionPreferences` 立即恢复 token/serverUrl/version
- **规则**：任何持有认证状态的内存单例，必须在构造函数中从持久化存储恢复

### restoreSession 错误处理
- `AuthRepositoryImpl.restoreSession()` 必须区分认证失败和临时错误
- HTTP 401/403 → 清除 token，返回 false（需要重新登录）
- 网络错误/服务器 500 → 保持 token，返回 true（让用户继续使用）
- **绝对不能**在网络超时时清除有效的 token

### 编辑器附件发现
- 编辑已有 memo 时，**必须用 `memo.resources` 列表**显示已有附件
- **绝对不能**从 markdown 内容中正则解析附件（视频从不嵌入 markdown）
- UiState 需要 `existingResources` 跟踪现有资源，保存时合并 existing + pending
- 删除附件时同时从 `existingResources` 和 `pendingResources` 中移除

### Markdown 语法区分
- **图片**：**不嵌入 markdown** — 仅通过 SetMemoResources API 关联，附件系统显示
- **视频**：不嵌入 markdown — 仅通过 SetMemoResources API 关联，附件系统显示
- **文件**：`[filename](url)` — 可点击链接

**图片曾经嵌入 markdown，2026-05-07 (c463cd2) 起不再嵌入**：同时嵌入又关联会让 web 端显示两次（issue #5）。
现在附件列表是图片显示的唯一来源 —— 上传的图片**必须**出现在 setMemoResources 调用里，漏掉就会变成孤儿资源，
传上去了但任何端都看不到。

### MemoCard 溢出菜单
- 三点 MoreVert 图标 + DropdownMenu，包含编辑/归档/删除操作
- 可选回调 `onEdit`/`onArchive`/`onDelete`，至少一个非 null 时才显示菜单
- 归档/恢复根据 `memo.state == MemoState.ARCHIVED` 切换
- 回调传递链：MemoCard → MemoList → Screen → ViewModel → NavHost

## API 层要点
- Memos 用 gRPC-gateway REST API，proto 定义是真实源头
- ID 是 nanoid 字符串（如 `8KEgSYrZ2Phz3osYEwXbi6`），嵌在 `name` 字段里（如 `memos/xxx`）
- DTO 必须给默认值，Moshi 严格解析遇到缺失字段会崩
- `SignInRequest` 是嵌套结构：`{passwordCredentials: {username, password}}`
- Auth 路径是 `api/v1/auth/me`（不是 `users/me`）

## 修复记录

### v0.24 完整修复 (2026-02-08 ~ 2026-02-20)
详见 memory 目录下 `v024-fixes.md`，涵盖：
1. API 层修复（Auth/Memo/Resource/User 端点和请求体）
2. 列表渲染修复（DTO 结构/uid 派生/Int→String ID 迁移）
3. 媒体上传与播放（视频检测/扩展名注入/MIME 类型/URL 解析）
4. 文件类型支持（图片视频 vs 文档的 markdown 语法）
5. 编辑器附件预览条（缩略图/播放图标/文件芯片）
6. 发文后刷新（savedStateHandle 跨屏通信）
7. 评论即时刷新（两阶段更新模式）

### 2026-02-21 修复
详见 memory 目录下 `session-fixes-0221.md`，涵盖：
1. Token 持久化修复（TokenManager 构造函数从 SessionPreferences 恢复）
2. restoreSession 错误处理（仅 401/403 清除 token，网络错误保持）
3. 编辑器附件预览修复（用 memo.resources 替代 markdown 正则解析）
4. MemoCard 三点溢出菜单（编辑/归档/删除，9 个文件联动）

### 2026-07-19 修复（v1.2.1，PR #13）
**v0.24 附件从来没有被关联上**：`setMemoResources` 用了 `@POST`，v0.24 的 proto 绑的是
`patch:`，服务端一律回 `501 Method Not Allowed`。上传本身是成功的，所以文件躺在服务端
成了 unused resource，任何客户端都看不到。0.24.0 / 0.24.2 / 0.24.3 实测行为一致。

- **图片是从 v1.1.1 开始坏的**，不是一开始就坏：在 c463cd2 去掉 markdown 嵌入之前，
  图片同时被嵌进正文，即使关联失败也能显示 —— 嵌入是遮羞布。视频和文件从不嵌入，
  所以它们在 v0.24 上**从来没工作过**。
- **两个"正确的修复"互相抵消的第二例**（第一例见 issue #5 的孤儿图片）。判断某个
  功能"以前能用"时，先确认它是靠什么机制显示的。
- 关联失败原来是 `catch { Log.e }` 吞掉的，保存看起来完全成功 —— 这才是它能藏这么久的
  根本原因。现在会报错并留在编辑器；memo 此时已创建，所以 `memoId` 会被记下，重试走更新
  而不是再发一条。
- 回归测试：`MemoApiAttachmentEndpointTest`（MockWebServer 钉住 method + path + body）。

### 已知未解决问题
- **v0.26 归档内容不可见**：归档标签页在 v0.24 正常，v0.26 看不到。可能与 v0.26 ListMemos API 的 state 过滤参数变化有关，需进一步调查。
