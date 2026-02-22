<p align="center">
  <img src="logo.png" width="120" alt="Memosly Logo" />
</p>

<h1 align="center">Memosly</h1>

<p align="center">
  A modern, beautiful Android client for <a href="https://github.com/usememos/memos">Memos</a>
</p>

<p align="center">
  <a href="https://play.google.com/store/apps/details?id=com.whtis.memosly">
    <img src="https://img.shields.io/badge/Google_Play-Download-green?style=flat-square&logo=google-play" alt="Google Play" />
  </a>
</p>

---

## Features

### Memo Management
- Create, edit, archive, and delete memos
- Set memo visibility (Public / Protected / Private)
- Pin important memos to the top
- Tag support with `#hashtag` syntax and quick-filter chips
- Full-text search across all memos

### Rich Content
- Markdown editor with live preview toggle
- Formatting toolbar (bold, italic, code, headings, lists, etc.)
- Image, video, and file attachments with inline preview
- Attachment preview strip in editor (thumbnails, play icons, file chips)
- Full-screen media viewer for images and videos
- File download with progress notification

### Social & Interaction
- Explore public memos from all users
- Emoji reactions with inline picker
- Comment threads on memo detail page
- Quick actions via three-dot overflow menu (edit / archive / delete)

### Navigation
- Switchable layout: Bottom Tabs or Navigation Drawer
- 4 main sections: Home, Explore, Archive, Profile
- Center FAB for quick memo creation

### Server Compatibility
- Compatible with Memos server **v0.24**, **v0.25**, and **v0.26**
- Version-aware API handling — select your server version at login

### Profile & Settings
- User stats (memo count, tag count)
- Access token management (view / delete)
- Webhook management (view / delete)
- Admin info display (server version, mode, identity providers)
- Language switching: English / 中文 / System Default
- Sign out

### Design & Experience
- Material You (Material 3) with Jetpack Compose
- Dynamic color theming
- Light / Dark mode support
- Pull-to-refresh and paginated loading
- Relative timestamps ("2 hours ago")
- Persistent scroll position per tab

## Screenshots

<!-- TODO: Add screenshots -->

## Download

<a href="https://play.google.com/store/apps/details?id=com.whtis.memosly">
  <img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" width="200" alt="Get it on Google Play" />
</a>

> Requires Android 8.0 (API 26) or above.

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM + Clean Architecture (16 modules)
- **DI**: Hilt
- **Network**: Retrofit + OkHttp + Moshi
- **Pagination**: Paging 3
- **Image Loading**: Coil 3
- **Markdown**: CommonMark + GFM extensions

## Support

If you find this app useful, consider supporting the development:

- [GitHub Sponsors](https://github.com/sponsors/whtis)

| WeChat | Alipay |
|--------|--------|
| <img src="donation_wechat.png" width="200" /> | <img src="donation_alipay.jpeg" width="200" /> |

## Privacy

- [Privacy Policy](PRIVACY_POLICY.md)
- [Data Deletion Instructions](DATA_DELETION.md)

## License

All rights reserved.
