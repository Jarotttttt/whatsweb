# WhatsWeb: WhatsApp Web Desktop Client for Android

**WhatsWeb** is a fast, lightweight, and dedicated native Android application designed to run the full desktop version of WhatsApp Web directly on your Android device.

---

## Overview & Motivation

When accessing WhatsApp on mobile devices, official clients and standard mobile browsers enforce limitations:
1. **Aggressive Media Compression:** WhatsApp's native mobile app aggressively compresses and downscales uploaded videos and media (often limiting status uploads to ~10–16 MB and heavily reducing resolution).
2. **Dual-Account Friction:** Running a second WhatsApp account on the same phone often requires heavy dual-app clones or clunky third-party browser setups.
3. **Desktop Interface on Mobile:** Mobile browsers frequently redirect `web.whatsapp.com` to mobile landing pages (`whatsapp.com/download`) rather than loading the QR code login and full web chat UI.

**WhatsWeb** solves these problems by spoofing a verified desktop browser environment, persisting authentication, and enabling native hardware access—providing a smooth, native-like WhatsApp Web experience in an ultra-compact package (< 5 MB).

---

## Key Features

- **Desktop User-Agent Spoofing:** Employs a tested desktop Chrome environment string (`Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36`) to bypass mobile download landing pages and open WhatsApp Web directly.
- **Persistent Session Storage:** Preserves user login state across app restarts and background kills using Android's `CookieManager`, HTML5 `localStorage`, and `IndexedDB`.
- **Full Media Upload Support (No Mobile Compression Limits):** Implements `WebChromeClient.onShowFileChooser` connected to Android's activity result contracts, enabling seamless document, image, and video uploads at full desktop quality without the native app's 10–16 MB downscaling.
- **Hardware Camera & Microphone Permissions:** Built-in Android runtime permission handling (`CAMERA`, `RECORD_AUDIO`) and WebRTC permission bridges (`WebChromeClient.onPermissionRequest`) for in-browser voice notes and webcam captures.
- **Desktop Viewport Navigation:** Configured with wide viewport (`useWideViewPort`), overview mode (`loadWithOverviewMode`), and smooth pinch-to-zoom controls (`setSupportZoom`, `builtInZoomControls`) for easy pan-and-scan navigation on small screens.
- **Swipe-to-Refresh & Loading Indicator:** Integrated `SwipeRefreshLayout` and a sleek Material horizontal `LinearProgressIndicator` matching WhatsApp brand green.
- **Smart Back-Button Navigation:** Utilizes Android's `OnBackPressedDispatcher`—navigates back through WhatsApp Web chat history if available before closing the app.
- **Brand Theming (Day / Night Support):** WhatsApp signature teal (`#128C7E`), deep green (`#075E54`), and accent green (`#25D366`) palette with custom toolbar styling and automatic Dark Mode support (`#121B22`, `#1F2C34`).
- **Offline Error Overlay:** Friendly error screen with a "Retry" button when network connectivity is lost.
- **Clean Session Management:** Quick toolbar menu option to log out, purge cookies and cached data, and reset session safely.

---

## Technology Stack & Architecture

- **Operating System:** Android (Minimum SDK: 24 / Android 7.0 Nougat; Target SDK: 34 / Android 14)
- **Programming Language:** Kotlin 1.9.22
- **Build System:** Gradle (Kotlin DSL `build.gradle.kts`)
- **UI Framework:** Android Material 3 Design Components (`com.google.android.material:material:1.12.0`)
- **Key Libraries:**
  - `androidx.core:core-ktx:1.13.1`
  - `androidx.appcompat:appcompat:1.7.0`
  - `androidx.swiperefreshlayout:swiperefreshlayout:1.1.0`
  - `androidx.webkit:webkit:1.11.0`
- **Testing:** JUnit 4 unit tests (`testDebugUnitTest`)

### Architecture Overview

```
com.whatsweb
├── MainActivity.kt               # Core Activity: hosts WebView, Toolbar, lifecycle & permissions
└── engine/
    ├── WaWebConfig.kt            # Constants: URL, Desktop User-Agent & UA validator
    ├── WaWebChromeClient.kt      # Progress tracking, file chooser & WebRTC permissions
    └── WaWebViewClient.kt       # Loading states, error handling & internal URL routing
```

---

## Build & Installation Guide

### Prerequisites

- Java Development Kit (JDK) 17 or higher
- Android SDK (API 34 and Build Tools installed)

### 1. Run Unit Tests

Execute unit tests to verify configuration and client behavior:

- **Linux / macOS:**
  ```bash
  ./gradlew testDebugUnitTest
  ```
- **Windows:**
  ```cmd
  gradlew.bat testDebugUnitTest
  ```

### 2. Build Debug APK

Compile and generate the debug APK:

- **Linux / macOS:**
  ```bash
  ./gradlew assembleDebug
  ```
- **Windows:**
  ```cmd
  gradlew.bat assembleDebug
  ```

The built APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### 3. Install to Device / Emulator

With USB debugging enabled or an active emulator:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Privacy & Security

WhatsWeb functions solely as a dedicated browser window for `https://web.whatsapp.com`:
- **No Middlemen:** All communications, messages, media, and credentials go directly and exclusively between your device and WhatsApp's official servers via end-to-end encrypted HTTPS/WSS.
- **No Analytics / Telemetry:** Zero tracking scripts, zero background daemons, and zero third-party telemetry.
- **Sandboxed Storage:** All cookies and Web storage reside in Android's private app sandbox.

---

## License

This project is licensed under the Apache License, Version 2.0.
