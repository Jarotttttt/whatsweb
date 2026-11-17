# Design Specification: WhatsWeb — Native Android WhatsApp Web Client

**Document ID:** `2026-11-17-whatsweb-design`  
**Date:** 2026-11-17  
**Status:** Approved Specification  
**Author:** Antigravity Team & User Collaboration  

---

## 1. Intent & Problem Statement

### 1.1 Intent
**WhatsWeb** is a fast, minimal, and lightweight native Android application designed for a single dedicated purpose: **running the full desktop version of WhatsApp Web directly on an Android smartphone**.

### 1.2 The Problem Solved
- **Mobile Size & Compression Bypass:** By running WhatsApp Web via a desktop browser environment, users can upload videos, documents, and media status without the aggressive ~10–16 MB downscaling enforced by the native WhatsApp mobile app.
- **Dual Account / Second WhatsApp:** Allows opening a second WhatsApp account on the same phone without needing dual-app cloning or third-party web browsers.
- **Zero Distraction / Frictionless:** Opens straight into WhatsApp Web immediately on launch with zero secondary menus or unnecessary tabs.

---

## 2. Goals, Non-Goals, and Anti-Goals

### 2.1 Goals
- **Instant Fullscreen WhatsApp Web:**
  - Spoofed Desktop Chrome User-Agent (`Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36`) to prevent mobile site redirection.
  - Persistent login session across app restarts via `CookieManager` and HTML5 `localStorage` / `indexedDB`.
  - Full media upload support (`WebChromeClient.onShowFileChooser`) for photos, videos, and documents.
  - Camera and Microphone access (`onPermissionRequest`) for WebRTC voice notes and camera captures.
  - Smooth pinch-to-zoom and pan support for desktop viewport navigation on mobile screens.
  - Pull-to-refresh (`SwipeRefreshLayout`) and horizontal loading progress bar.
  - Proper back-button navigation: if the webview history can go back (e.g. inside a chat or viewing media), navigate back instead of immediately closing the app.
  - Top Toolbar with quick **Reload** button and **Logout / Bersihkan Sesi** option.
- **Performance & Footprint:**
  - Native Kotlin + Android XML.
  - Extremely small APK size (< 5 MB).
  - Minimal RAM and zero background battery drain when closed.

### 2.2 Non-Goals
- No Direct Chat tab, contact scrapers, or third-party utilities.
- No third-party analytics or background daemons.

### 2.3 Anti-Goals
- Never intercept, read, or forward user messages, QR data, or authentication credentials. All communication is 100% direct and encrypted between the WebView and official WhatsApp servers (`web.whatsapp.com`).

---

## 3. Technology Stack & Architecture

- **Platform:** Android (Min SDK 24 / Android 7.0, Target SDK 34 / Android 14).
- **Language:** Kotlin.
- **UI Architecture:** Single Activity (`MainActivity`) hosting a `SwipeRefreshLayout` and full-screen `WebView`.
- **Dependencies:**
  - `androidx.core:core-ktx:1.13.1`
  - `androidx.appcompat:appcompat:1.7.0`
  - `com.google.android.material:material:1.12.0`
  - `androidx.swiperefreshlayout:swiperefreshlayout:1.1.0`
  - `androidx.webkit:webkit:1.11.0`

---

## 4. UI & Layout Specification

```
+------------------------------------------+
| WhatsWeb                      [⟳] [⋮]    | <- MaterialToolbar (Refresh / Logout)
+------------------------------------------+
| [========== Loading Bar ==========]      | <- ProgressBar (visible during load)
|                                          |
|                                          |
|                                          |
|           WhatsApp Web Desktop           |
|                                          |
|       - QR Code / Chat List / Chat       |
|       - Swipe down to Refresh            |
|       - Pinch to Zoom & Pan              |
|                                          |
|                                          |
+------------------------------------------+
```

### 4.1 WebView Configuration
- **Target URL:** `https://web.whatsapp.com`
- **User Agent:** `Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36`
- **Settings:**
  - `javaScriptEnabled = true`
  - `domStorageEnabled = true`
  - `databaseEnabled = true`
  - `useWideViewPort = true`
  - `loadWithOverviewMode = true`
  - `setSupportZoom(true)`
  - `builtInZoomControls = true`
  - `displayZoomControls = false`
- **Cookie & Storage Persistence:**
  - `CookieManager.getInstance().setAcceptCookie(true)`
  - `CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)`
- **Permissions & Media:**
  - Android Manifest: `INTERNET`, `ACCESS_NETWORK_STATE`, `CAMERA`, `RECORD_AUDIO`, `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`.
  - `WebChromeClient.onShowFileChooser`: handles gallery and camera file selection.
  - `WebChromeClient.onPermissionRequest`: handles WebRTC audio/video capture permissions.

---

## 5. What Is Left to the Builder
1. Implementation of runtime permission prompt handling for Camera & Microphone with user-friendly explanations.
2. Handling offline/network loss screen with a retry button.