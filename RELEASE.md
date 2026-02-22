# Release Guide

## 发布新版本

### 1. 更新版本号

编辑 `app/build.gradle.kts`：

```kotlin
defaultConfig {
    applicationId = "com.whtis.memosly"
    versionCode = 2          // 每次发布 +1
    versionName = "1.1.0"    // 语义化版本号
}
```

版本号规则：
- **主版本号** (1.x.x)：大功能改版、不兼容变更
- **次版本号** (x.1.x)：新功能
- **修订号** (x.x.1)：Bug 修复

### 2. 构建 Release APK

```bash
./gradlew assembleRelease
```

产物路径：`app/build/outputs/apk/release/app-release.apk`

> 签名配置读取自 `keystore.properties`（已 gitignore），keystore 文件在 `keystore/release.keystore`。

### 3. 发布到 GitHub

```bash
gh release create v1.1.0 \
  app/build/outputs/apk/release/app-release.apk#Memosly-v1.1.0.apk \
  --repo whtis/memosly-android \
  --title "v1.1.0" \
  --notes "更新内容..."
```

或使用详细的 release notes：

```bash
gh release create v1.1.0 \
  app/build/outputs/apk/release/app-release.apk#Memosly-v1.1.0.apk \
  --repo whtis/memosly-android \
  --title "v1.1.0" \
  --notes "$(cat <<'EOF'
## What's New
- 新功能描述
- Bug 修复描述

## Requirements
- Android 8.0+
- Memos server v0.24+
EOF
)"
```

### 4. 验证

发布后检查：
- https://github.com/whtis/memosly-android/releases/latest
- 确认 APK 能正常下载安装
