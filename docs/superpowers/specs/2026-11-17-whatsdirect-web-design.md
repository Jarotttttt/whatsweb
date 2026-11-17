# Design Specification: WhatsDirect Web

**Document ID:** `2026-11-17-whatsdirect-web-design`  
**Date:** 2026-11-17  
**Status:** Approved Specification  
**Author:** Antigravity Team & User Collaboration  

---

## 1. Intent & Problem Statement

### 1.1 Intent
**WhatsDirect Web** is a fast, lightweight, and reliable native Android application that bridges two essential WhatsApp utilities into a single app:
1. **WhatsApp Web on Mobile:** Opens the full desktop version of WhatsApp Web directly on an Android smartphone with session persistence, pinch-to-zoom, file uploads, and camera/mic support.
2. **Direct Chat:** Allows users to initiate a WhatsApp chat or send messages to any phone number without needing to save it to their phone's contact list first.

### 1.2 The Problem Solved
- **Mobile Compression & Size Bypass:** By running WhatsApp Web via a desktop user agent, users can upload videos, documents, and media status without the aggressive ~10–16 MB downscaling enforced by the native WhatsApp mobile app.
- **Dual Account / Tablet Experience:** Enables users to access a second WhatsApp account or link their primary WhatsApp account on a single phone or tablet.
- **Frictionless Messaging:** Eliminates contact list bloat for one-time transactions (delivery couriers, marketplace sellers, customer inquiries) via Direct Chat.

---

## 2. Goals, Non-Goals, and Anti-Goals

### 2.1 Goals
- **Seamless WhatsApp Web Experience:**
  - Spoofed Desktop Chrome User-Agent to prevent mobile redirection.
  - Persistent login session across app restarts via `CookieManager` and HTML5 `localStorage` / `indexedDB`.
  - Full media upload support (`WebChromeClient.onShowFileChooser`) for photos, videos, and documents.
  - Camera and Microphone access (`onPermissionRequest`) for WebRTC voice notes and direct photos.
  - Smooth pinch-to-zoom and pan support for desktop viewport navigation on mobile screens.
- **Direct Chat Utility:**
  - Phone number input with country code formatting (default `+62` for Indonesia).
  - Optional pre-filled message text.
  - One-tap launch into official WhatsApp using universal deep links (`https://wa.me/`).
- **Clean Native UI & Performance:**
  - Native Android (Kotlin + XML) with Google Material 3 components.
  - Standard **Bottom Navigation Bar** with 2 tabs: **WA Web** and **Direct Chat**.
  - Top Toolbar with quick **Refresh** and **Clear Cache / Logout** options.
  - Fast startup, minimal APK size (< 8 MB), and low memory footprint.

### 2.2 Non-Goals
- No custom messaging bot, headless scraper, or unofficial API engine (no Node.js/Baileys). Everything operates safely through official Chromium WebView and official WhatsApp deep links.
- No third-party analytics or invasive tracking.

### 2.3 Anti-Goals
- Never store or intercept user messages, QR data, or login tokens. All authentication is strictly handled directly between the WebView and `web.whatsapp.com`.
- No background battery drain: the app does not run background daemons when closed.

---

## 3. Technology Stack & Architecture

- **Platform:** Android (Min SDK 24 / Android 7.0, Target SDK 34 / Android 14).
- **Language:** Kotlin.
- **UI Framework:** Native Android XML with Material Design 3 (`com.google.android.material`).
- **Navigation:** `BottomNavigationView` hosting two Fragment destinations managed by `FragmentManager`:
  1. `WaWebFragment` (WebView lifecycle with state preservation).
  2. `DirectChatFragment` (Phone number & message form with `Intent.ACTION_VIEW`).
- **Core Dependencies:**
  - `androidx.core:core-ktx:1.13.1`
  - `androidx.appcompat:appcompat:1.7.0`
  - `com.google.android.material:material:1.12.0`
  - `androidx.constraintlayout:constraintlayout:2.1.4`
  - `androidx.webkit:webkit:1.11.0`
  - `androidx.fragment:fragment-ktx:1.8.0`

---

## 4. Component & UI Specifications

### 4.1 Screen & Navigation Layout

```
+------------------------------------------+
| WhatsDirect Web               [⟳] [⋮]    | <- MaterialToolbar (Refresh / Menu)
+------------------------------------------+
|                                          |
|                                          |
|                                          |
|          [ Active Fragment ]             |
|                                          |
|    - Tab 1: WhatsApp Web (WebView)       |
|    - Tab 2: Direct Chat Form             |
|                                          |
|                                          |
+------------------------------------------+
|   [ 💬 WA Web ]      [ ⚡ Direct Chat ]  | <- BottomNavigationView
+------------------------------------------+
```

### 4.2 `WaWebFragment` (WebView Engine)
- **URL:** `https://web.whatsapp.com`
- **User Agent:**
  `Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36`
- **WebSettings Configuration:**
  - `javaScriptEnabled = true`
  - `domStorageEnabled = true`
  - `databaseEnabled = true`
  - `useWideViewPort = true`
  - `loadWithOverviewMode = true`
  - `setSupportZoom(true)`
  - `builtInZoomControls = true`
  - `displayZoomControls = false`
  - `cacheMode = WebSettings.LOAD_DEFAULT`
- **`CookieManager`:**
  - `setAcceptCookie(true)`
  - `setAcceptThirdPartyCookies(webView, true)`
- **File Chooser (`WebChromeClient`):**
  - Implements `onShowFileChooser` using `ActivityResultContracts.StartActivityForResult()` or `GetMultipleContents()` to support camera and gallery photo/video/document selection.
- **Runtime Permissions:**
  - `CAMERA`, `RECORD_AUDIO`, `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`.
  - `WebChromeClient.onPermissionRequest` grants WebRTC audio/video permissions dynamically when user records voice notes or captures media.
- **Back Navigation Handling:**
  - If `webView.canGoBack()`, navigate backward inside the web history before exiting the app.

### 4.3 `DirectChatFragment` (Direct Messaging Form)
- **Components:**
  - Country Code Input (`TextInputLayout` with default `+62`).
  - Phone Number Input (`TextInputEditText` with phone keyboard type).
  - Message Input (`TextInputEditText` with multiline support).
  - Action Button ("Kirim Pesan / Buka di WhatsApp"):
    - Normalizes phone number: strips spaces, dashes, leading zeroes, and prepends country code.
    - Validates minimum length.
    - Launches `Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$cleanNumber?text=$encodedMessage"))`.
    - Handles case where official WhatsApp app is installed or falls back to browser.

---

## 5. Security & Privacy
- **Direct-to-WhatsApp:** All traffic in `WaWebFragment` connects directly via HTTPS/WSS to official WhatsApp domains (`*.whatsapp.com`, `*.whatsapp.net`).
- **No Third-Party Interception:** No cookies, session tokens, or messages are logged, forwarded, or stored outside Android's standard sandbox.
- **Clear Session:** Toolbar menu provides a "Logout / Bersihkan Sesi" option that invokes `CookieManager.getInstance().removeAllCookies()` and `webView.clearCache(true)`.

---

## 6. What Is Left to the Builder
1. Implementation of the runtime permission request dialog for Camera & Microphone with graceful fallbacks.
2. Styling the country code selector (simple text field with standard prefix vs full searchable spinner).
3. Swipe-to-refresh integration (`SwipeRefreshLayout`) on top of the WebView.

---

## 7. Approval & Implementation Plan
This specification defines the complete scope for **WhatsDirect Web**. Next step is generating the implementation plan.