# Changelog

All notable changes to Memosly will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.1] - 2026-05-31

### Fixed
- Share intent could leave the app stuck on the auth route after a
  cold-start from the system share sheet. `AuthViewModel` now buffers a
  pending login-success signal until `AuthScreen` registers its callback,
  and `MemosNavHost` observes the back stack via
  `currentBackStackEntryAsState()` so the editor is reliably pushed on
  top of home/tabs once authenticated.
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

[1.1.1]: https://github.com/whtis/memosly-android/releases/tag/v1.1.1
[1.1.0]: https://github.com/whtis/memosly-android/releases/tag/v1.1.0
[1.0.1]: https://github.com/whtis/memosly-android/releases/tag/v1.0.1
[1.0.0]: https://github.com/whtis/memosly-android/releases/tag/v1.0.0
[#2]: https://github.com/whtis/memosly-android/issues/2
