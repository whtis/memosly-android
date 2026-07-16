# Changelog

All notable changes to Memosly will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.0] - 2026-07-16

### Added
- Share images and videos to Memosly from any app. Memosly now appears in
  the system share sheet for pictures and video, alongside the text
  sharing added in v1.1.1. Multiple files can be shared at once (up to 9
  per share), and they upload into a new memo. ([#10])
- Files larger than **32 MB** are refused up front with a message naming
  the file, rather than being read into memory and failing. The limit
  matches the Memos server default (`MEMOS_MAX_UPLOAD_SIZE_MIB`), so
  larger files would be rejected server-side anyway.

### Fixed
- **Images uploaded from the editor never appeared on Memos server v0.25
  and v0.26.** They were uploaded successfully but linked to no memo, so
  neither the web client nor the app ever showed them — the file was on
  the server, invisible. This affected every image added through the
  editor, not just shared ones, and has been broken since v1.1.1. Videos
  and other file types were unaffected. ([#5])
- A share that arrived while the memo editor was already open was left
  pending instead of being discarded, and would re-fire later — dropping
  a long-abandoned share into whatever the user opened next. Affected
  text shares since v1.1.1.
- Sharing several files at once could silently drop some of them: uploads
  ran concurrently and raced each other while updating the attachment
  list. Uploads now run one at a time.

## [1.1.2] - 2026-06-01

### Changed
- README explicitly states that Memos server v0.27 and newer are not
  supported. Upstream v0.27 introduced breaking API changes (e.g. user
  resource names switched from `users/{id}` to `users/{username}`,
  CEL filter semantics tightened, attachment ownership enforcement)
  that this client does not adapt to. Pin your Memos server to
  **v0.26.2** for the best experience. ([#5])
- Auth screen version picker no longer shows the misleading `v0.26+`
  label — it now shows `v0.26` to match the actual compatibility
  ceiling.
- Bump `versionCode` to 5 for the documentation release.

## [1.1.1] - 2026-05-31

### Fixed
- Share intent could leave the app stuck on the auth route after a
  cold-start from the system share sheet. `AuthViewModel` now buffers a
  pending login-success signal until `AuthScreen` registers its callback,
  and `MemosNavHost` observes the back stack via
  `currentBackStackEntryAsState()` so the editor is reliably pushed on
  top of home/tabs once authenticated. ([#6])
- Resolved leftover merge-conflict markers in six files (`MainActivity`,
  `MemosNavHost`, `AuthInterceptor`, `ServerUrlInterceptor`,
  `MemoEditorViewModel`, `MemoNavigation`) that had been committed as-is
  from prior night-shift merges, restoring a clean Kotlin compile.

### Changed
- Bump `versionCode` to 4 for the hotfix release.

## [1.1.0] - 2026-02-27

### Added
- Sign in with a Personal Access Token (PAT) as an alternative to username
  and password. Lets OIDC/SSO users generate a PAT from the Memos web UI and
  authenticate in the Android app. ([#2])
- Login screen toggle between Password and Access Token modes (segmented
  buttons).

### Changed
- Pasted access tokens have any leading `Bearer ` prefix stripped
  automatically.

## [1.0.1] - 2026-02-23

### Fixed
- GitHub Release workflow: add `contents: write` permission so the workflow
  can publish APKs.
- Keystore properties heredoc now uses single quotes to prevent shell
  variable expansion when writing the signing config in CI.

### Changed
- Bump `versionCode` to 2 (Google Play requirement for subsequent uploads).

## [1.0.0] - 2026-02-22

Initial open source release.

### Added
- Memo CRUD with Markdown editor and live preview toggle.
- Formatting toolbar (bold, italic, code, headings, lists, etc.).
- Image, video, and file attachments with inline preview.
- Full-screen media viewer for images and videos; file download with
  progress notification.
- `#hashtag` parsing and quick-filter chips.
- Full-text search across memos and an Explore feed for public memos.
- Emoji reactions with inline picker; comment threads on memo detail.
- Three-dot overflow menu on memo cards (edit / archive / delete).
- Switchable navigation: Bottom Tabs or Navigation Drawer.
- Pin, archive, restore, and delete memos; visibility selector
  (Public / Protected / Private).
- Profile with user stats, access token management, webhook management,
  and admin info (server version, mode, identity providers).
- Language switching: English / 中文 / System Default.
- Material 3 with dynamic color and Light / Dark mode.
- Compatible with Memos server **v0.24**, **v0.25**, and **v0.26**
  (version-aware API handling, selected at login).

[1.2.0]: https://github.com/whtis/memosly-android/releases/tag/v1.2.0
[1.1.2]: https://github.com/whtis/memosly-android/releases/tag/v1.1.2
[1.1.1]: https://github.com/whtis/memosly-android/releases/tag/v1.1.1
[1.1.0]: https://github.com/whtis/memosly-android/releases/tag/v1.1.0
[1.0.1]: https://github.com/whtis/memosly-android/releases/tag/v1.0.1
[1.0.0]: https://github.com/whtis/memosly-android/releases/tag/v1.0.0
[#2]: https://github.com/whtis/memosly-android/issues/2
[#5]: https://github.com/whtis/memosly-android/issues/5
[#6]: https://github.com/whtis/memosly-android/issues/6
[#10]: https://github.com/whtis/memosly-android/issues/10
