# Gecko Browser

A lightweight, privacy-focused Android web browser powered by **Mozilla GeckoView 118**, featuring a modern **iOS Liquid Glass** mobile interface, **Android TV** D-pad support, and compatibility with Android 4.1+ (`minSdkVersion 16`).

---

## Features

### Interface & Navigation
- **Phone Mode (iOS-style Liquid Glass UI):**
  - Floating bottom navigation capsule with Back, Forward, `+` New Tab (long-press for Homepage), Tab Counter, and `•••` More menu.
  - Bottom Omnibox with centered domain display when unfocused, full URL editing with autocomplete suggestions, voice search, security shield indicator, bookmark star, share button, and reload/stop control.
  - **New Tab / Start Page (`about:home`):** Quick Incognito/Settings top controls, search bar pill, quick-action shortcuts (Bookmarks, History, Downloads, Private), and customizable Speed Dial grid.
  - **Tab Grid Switcher:** 2-column (3-column on wide screens) tab cards with Normal and Incognito styling, `PC` desktop badges, and quick controls to close all tabs or open normal/private tabs.
  - **Bottom Sheet More Menu:** Fast access to tabs, reopen closed tab, duplicate tab, Find in Page, Desktop Site toggle, Share, Copy Link, Open in External App, Add to Home Screen, and Quick Wipe.
- **Android TV Mode:**
  - Switchable in **Settings → Interface Layout (Phone / TV)**.
  - Top toolbar layout with D-pad focus highlights (`bg_tv_focus`) across navigation, omnibox, and Start Page controls.
- **Themes & Localization:**
  - System Default, Light, and Dark themes (with automatic dark styling for Private/Incognito tabs).
  - Built-in support for **English**, **Tiếng Việt**, **Español**, **Français**, and **日本語**.

### Browsing, Privacy & Productivity
- **GeckoView 118 Engine:** Full modern web standards support with per-session Desktop/Mobile viewport and user-agent toggling.
- **Privacy & Security:**
  - Mozilla Content Blocking (anti-tracking & safe browsing) with a live per-tab blocked tracker counter and HTTPS/certificate inspection dialog.
  - Private / Incognito tabs that do not record browsing history and isolate session state.
  - Quick Wipe & granular clearing for browsing history, bookmarks, and cookies/site cache.
- **Search & Discovery:**
  - Presets for DuckDuckGo, Google, Bing, Brave Search, Ecosia, or custom search URLs.
  - Local bookmark/history matching plus optional DuckDuckGo autocomplete suggestions (disabled automatically in Private tabs).
- **Bookmarks, History, Downloads & File Uploads:**
  - Searchable, editable bookmarks and browsing history persisted via `BrowserDataStore`.
  - Download confirmation prompts integrated with Android `DownloadManager`, `data:` URI saving, and `FileProvider` file opening.
  - Web file picker uploads (`ACTION_GET_CONTENT`), HTTP authentication prompts, JavaScript dialogs, and full-screen video playback.

---

## Tech Stack & Configuration

| Component | Version / Value |
| --- | --- |
| **Engine** | `org.mozilla.geckoview:geckoview:118.0.20231009140911` |
| **Language** | Java (`JavaVersion.VERSION_1_8` source/target) |
| **Build JDK** | JDK 17+ |
| **Android Gradle Plugin** | `9.1.1` |
| **Gradle Wrapper** | `9.3.1` |
| **compileSdkVersion** | `33` |
| **targetSdkVersion** | `30` |
| **minSdkVersion** | `16` (Android 4.1+) |

---

## Project Structure

```text
.
├── .github/workflows/android.yml      # GitHub Actions CI & APK Release workflow
├── app/
│   ├── build.gradle                   # Dependencies, GeckoView version, SDK & signing config
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/geckobrowser/
│       │   │   ├── MainActivity.java          # Tabs, GeckoRuntime/GeckoSession lifecycle, UI & delegates
│       │   │   ├── BrowserDialogs.java        # Tab grid, Chrome More sheet, bookmarks/history, security info
│       │   │   ├── BrowserPromptDelegate.java # JS alert/confirm/prompt, auth, choice, and file pickers
│       │   │   ├── BrowserHelper.java         # Downloads, FileProvider opening, home screen shortcuts
│       │   │   ├── BrowserDataStore.java      # SharedPreferences JSON storage for tabs, data & settings
│       │   │   ├── BrowserTab.java            # Per-tab state model
│       │   │   ├── LocaleHelper.java          # Language, theme, and Phone/TV layout persistence
│       │   │   ├── SettingsActivity.java      # Settings screen and preferences management
│       │   │   └── Tls12SocketFactory.java    # TLS 1.2 support on pre-Lollipop devices
│       │   └── res/                           # Layouts, Liquid Glass drawables, localized strings
│       └── test/java/com/example/geckobrowser/
│           └── ExampleUnitTest.java           # Pure-JVM unit tests
└── build.gradle
```

---

## Building & Testing

### Prerequisites
- **JDK 17** or higher
- **Android SDK Platform 33** and Build Tools

### Local Gradle Commands
```bash
# Run unit tests and Android Lint
./gradlew check

# Build the debug APK
./gradlew assembleDebug

# Full build
./gradlew build
```
The generated debug APK is written to:
```text
app/build/outputs/apk/debug/app-debug.apk
```

### Continuous Integration
Commits and pull requests targeting `master` trigger `.github/workflows/android.yml`, which sets up JDK 17, restores `debug.keystore`, runs `./gradlew build`, and publishes `app-debug.apk` to GitHub Releases on pushes.

---

## License

This project is licensed under the **GNU General Public License v3.0** — see [`LICENSE`](LICENSE) for details.
