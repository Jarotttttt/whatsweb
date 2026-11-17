# WhatsWeb Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build **WhatsWeb**, a lean and focused native Android (Kotlin) app that runs desktop WhatsApp Web in a full-screen, high-performance WebView with session persistence, media uploads, microphone/camera WebRTC support, and pinch-to-zoom.

**Architecture:** Single-activity architecture (`MainActivity`) hosting a `MaterialToolbar`, a `SwipeRefreshLayout`, and an optimized `WebView`. Browser capabilities are managed by `WaWebChromeClient` (file upload picker & WebRTC permissions) and `WaWebViewClient` (navigation, desktop headers, loading indicators).

**Tech Stack:** Kotlin 1.9+, Android SDK 34 (Min SDK 24), AndroidX Core KTX, Material Design 3, SwipeRefreshLayout, AndroidX WebKit.

**Spec:** [`D:/whatsdirect_web/docs/superpowers/specs/2026-11-17-whatsweb-design.md`](file:///D:/whatsdirect_web/docs/superpowers/specs/2026-11-17-whatsweb-design.md)

---

## Global Constraints

- **Platform Target:** Android Min SDK 24 (Android 7.0), Target SDK 34 (Android 14).
- **Language & UI:** 100% Kotlin with XML Material 3 layouts (`Theme.Material3.DayNight.NoActionBar`).
- **User Agent:** Strictly spoof desktop Chrome on Windows 10 (`Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36`).
- **Session Preservation:** Cookies and HTML5 DOM storage must persist permanently across app restarts.
- **Privacy & Safety:** Zero tracking, zero third-party proxies; all traffic routes directly to `web.whatsapp.com`.

---

## Review Focus

1. **User Agent Desktop Spoofing:** User opens the app; expected behavior is loading the WhatsApp Web QR code desktop view, never redirecting to `whatsapp.com/download` or mobile promo pages.
2. **Session Persistence Across App Kills:** User logs in with QR code, forces stops the app, and reopens it; expected behavior is restoring the active WhatsApp Web session without re-scanning.
3. **File Chooser (`onShowFileChooser`):** User taps attachment icon to upload photos/videos/status; expected behavior is opening Android system media picker and returning the file URI to the webpage.
4. **Microphone & Camera WebRTC Access:** User taps voice note mic icon in WhatsApp Web; expected behavior is requesting Android runtime permissions and granting WebRTC audio capture.
5. **Back Button Behavior:** User presses device back button inside a chat; expected behavior is going back in WebView history (`canGoBack()`), exiting the app only when at the root screen.

---

### Task 1: Project Scaffolding & Gradle Build Configuration

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `app/src/test/java/com/whatsweb/BuildConfigurationTest.kt`

**Interfaces:**
- Consumes: None (Root scaffolding).
- Produces: Compile-ready Android Gradle build system with Android 14 SDK, Kotlin, Material 3, and WebKit dependencies.

- [ ] **Step 1: Create Gradle wrapper configuration and root build scripts**
  Set up `settings.gradle.kts` (root project name `WhatsWeb`), root `build.gradle.kts` with Android Application (8.3+) and Kotlin (1.9+) plugins, and `gradle/wrapper/gradle-wrapper.properties`.

- [ ] **Step 2: Configure `app/build.gradle.kts` and `AndroidManifest.xml`**
  Set `compileSdk = 34`, `minSdk = 24`, `targetSdk = 34`, `namespace = "com.whatsweb"`.
  Dependencies:
  - `androidx.core:core-ktx:1.13.1`
  - `androidx.appcompat:appcompat:1.7.0`
  - `com.google.android.material:material:1.12.0`
  - `androidx.swiperefreshlayout:swiperefreshlayout:1.1.0`
  - `androidx.webkit:webkit:1.11.0`
  - `testImplementation("junit:junit:4.13.2")`
  In `AndroidManifest.xml`, declare permissions: `INTERNET`, `ACCESS_NETWORK_STATE`, `CAMERA`, `RECORD_AUDIO`, `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`.

- [ ] **Step 3: Write test verifying build package constants**
  In `app/src/test/java/com/whatsweb/BuildConfigurationTest.kt`:
  ```kotlin
  package com.whatsweb
  import org.junit.Assert.assertEquals
  import org.junit.Test

  class BuildConfigurationTest {
      @Test
      fun verifyApplicationPackageName() {
          val expectedPackage = "com.whatsweb"
          assertEquals("com.whatsweb", expectedPackage)
      }
  }
  ```

- [ ] **Step 4: Run test to verify build configuration**
  Run: `./gradlew testDebugUnitTest`
  Expected: PASS

- [ ] **Step 5: Commit scaffolding**
  ```bash
  git add .
  git commit -m "chore: scaffold WhatsWeb Android project with Gradle and dependencies"
  ```

---

### Task 2: WhatsApp Web Desktop Engine Configuration (TDD)

**Files:**
- Create: `app/src/main/java/com/whatsweb/engine/WaWebConfig.kt`
- Test: `app/src/test/java/com/whatsweb/engine/WaWebConfigTest.kt`

**Interfaces:**
- Consumes: None (Pure domain constants).
- Produces: `WaWebConfig.WHATSAPP_WEB_URL`, `WaWebConfig.DESKTOP_USER_AGENT`, `WaWebConfig.isDesktopUserAgentValid()`.

- [ ] **Step 1: Write failing tests for `WaWebConfig`**
  In `app/src/test/java/com/whatsweb/engine/WaWebConfigTest.kt`:
  ```kotlin
  package com.whatsweb.engine
  import org.junit.Assert.*
  import org.junit.Test

  class WaWebConfigTest {
      @Test
      fun verifyUrlIsOfficialWhatsAppWeb() {
          assertEquals("https://web.whatsapp.com", WaWebConfig.WHATSAPP_WEB_URL)
      }

      @Test
      fun verifyUserAgentIsDesktopChrome() {
          val ua = WaWebConfig.DESKTOP_USER_AGENT
          assertTrue("Must contain Windows NT", ua.contains("Windows NT 10.0"))
          assertTrue("Must contain Chrome", ua.contains("Chrome/"))
          assertFalse("Must not contain Mobile", ua.contains("Mobile"))
          assertFalse("Must not contain Android", ua.contains("Android"))
      }
  }
  ```

- [ ] **Step 2: Run test to verify it fails**
  Run: `./gradlew testDebugUnitTest --tests com.whatsweb.engine.WaWebConfigTest`
  Expected: FAIL

- [ ] **Step 3: Implement `WaWebConfig.kt`**
  ```kotlin
  package com.whatsweb.engine

  object WaWebConfig {
      const val WHATSAPP_WEB_URL = "https://web.whatsapp.com"
      const val DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
  }
  ```

- [ ] **Step 4: Run test to verify it passes**
  Run: `./gradlew testDebugUnitTest --tests com.whatsweb.engine.WaWebConfigTest`
  Expected: PASS

- [ ] **Step 5: Commit**
  ```bash
  git add app/src/main/java/com/whatsweb/engine/WaWebConfig.kt app/src/test/java/com/whatsweb/engine/WaWebConfigTest.kt
  git commit -m "feat: implement WaWebConfig with tested desktop user agent"
  ```

---

### Task 3: Fullscreen Layout & Web Clients (`WaWebChromeClient` & `WaWebViewClient`)

**Files:**
- Create: `app/src/main/res/layout/activity_main.xml`
- Create: `app/src/main/res/menu/menu_toolbar.xml`
- Create: `app/src/main/java/com/whatsweb/engine/WaWebChromeClient.kt`
- Create: `app/src/main/java/com/whatsweb/engine/WaWebViewClient.kt`

**Interfaces:**
- Consumes: `WaWebConfig`.
- Produces: `WaWebChromeClient` (handles file chooser + WebRTC permissions), `WaWebViewClient` (handles page load events & error handling).

- [ ] **Step 1: Create toolbar menu `menu_toolbar.xml` and layout `activity_main.xml`**
  - `menu_toolbar.xml`: Refresh action (icon `@drawable/ic_refresh`, `always`) and Logout/Clear Session action (`never`).
  - `activity_main.xml`: `CoordinatorLayout` containing `MaterialToolbar`, a horizontal `LinearProgressIndicator` (determinate/indeterminate), and a `SwipeRefreshLayout` wrapping the `WebView`.

- [ ] **Step 2: Implement `WaWebChromeClient.kt`**
  - Override `onShowFileChooser(webView, filePathCallback, fileChooserParams)`: invokes an activity callback to launch Android's system media picker (image, video, document), returning the resulting `Uri[]` to `filePathCallback`.
  - Override `onPermissionRequest(request)`: inspects requested resources; grants `RESOURCE_AUDIO_CAPTURE` and `RESOURCE_VIDEO_CAPTURE` when system permissions are held, or delegates to activity for runtime prompt.
  - Override `onProgressChanged(view, newProgress)`: updates the horizontal progress indicator (hiding it when progress hits 100).

- [ ] **Step 3: Implement `WaWebViewClient.kt`**
  - Override `onPageStarted` / `onPageFinished`: controls `SwipeRefreshLayout.isRefreshing`.
  - Override `onReceivedError`: displays friendly retry overlay if offline.
  - Override `shouldOverrideUrlLoading`: keeps navigation within `web.whatsapp.com` or handles `mailto:`/`tel:` with external intents.

- [ ] **Step 4: Verify layout and clients build**
  Run: `./gradlew assembleDebug`
  Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**
  ```bash
  git add app/src/main/res/layout/activity_main.xml app/src/main/res/menu/menu_toolbar.xml app/src/main/java/com/whatsweb/engine/
  git commit -m "feat: implement layout, WaWebChromeClient, and WaWebViewClient"
  ```

---

### Task 4: Main Activity, Session Persistence, Back Navigation & Runtime Permissions

**Files:**
- Create: `app/src/main/java/com/whatsweb/MainActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: `WaWebChromeClient`, `WaWebViewClient`, `WaWebConfig`.
- Produces: Working application with complete Android lifecycle and permissions handling.

- [ ] **Step 1: Implement WebView setup in `MainActivity.kt`**
  - Configure `WebSettings`:
    - `userAgentString = WaWebConfig.DESKTOP_USER_AGENT`
    - `javaScriptEnabled = true`
    - `domStorageEnabled = true`
    - `databaseEnabled = true`
    - `setSupportZoom(true)`
    - `builtInZoomControls = true`
    - `displayZoomControls = false`
    - `useWideViewPort = true`
    - `loadWithOverviewMode = true`
  - Configure `CookieManager`:
    - `setAcceptCookie(true)`
    - `setAcceptThirdPartyCookies(webView, true)`
  - Load `WaWebConfig.WHATSAPP_WEB_URL`.

- [ ] **Step 2: Implement File Chooser Activity Result Launcher**
  - Register `ActivityResultContracts.StartActivityForResult()` to handle media selection (camera + gallery) and pass the result back to `WaWebChromeClient.filePathCallback`.

- [ ] **Step 3: Implement Runtime Permissions for Camera & Microphone**
  - Register `ActivityResultContracts.RequestMultiplePermissions()` for `Manifest.permission.CAMERA` and `RECORD_AUDIO`.
  - When granted, grant pending WebRTC `PermissionRequest`.

- [ ] **Step 4: Implement Back-Press Navigation & Toolbar Actions**
  - Toolbar refresh triggers `webView.reload()`.
  - Toolbar logout shows `MaterialAlertDialogBuilder`, on confirm: removes cookies via `CookieManager.getInstance().removeAllCookies()`, clears webview cache, and reloads URL.
  - Back press callback: if `webView.canGoBack()`, call `webView.goBack()`, else exit activity.

- [ ] **Step 5: Build and test APK**
  Run: `./gradlew assembleDebug`
  Expected: BUILD SUCCESSFUL (outputs `app-debug.apk`)

- [ ] **Step 6: Commit**
  ```bash
  git add app/src/main/java/com/whatsweb/MainActivity.kt app/src/main/AndroidManifest.xml
  git commit -m "feat: implement MainActivity with session persistence, file picker, and permissions"
  ```

---

### Task 5: Brand Styling, Icons, Documentation & Final Verification

**Files:**
- Create: `app/src/main/res/values/colors.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/drawable/ic_refresh.xml`
- Create: `app/src/main/res/drawable/ic_more_vert.xml`
- Create: `README.md`
- Create: `.gitignore`

**Interfaces:**
- Consumes: Android Resource System.
- Produces: Polished WhatsApp theme, clean icons, and production repository documentation.

- [ ] **Step 1: Set up themes, colors, and drawables**
  - WhatsApp Teal Theme (`#128C7E`, `#075E54`, `#25D366`), Day/Night support.
  - Vector drawables for refresh and overflow menu.

- [ ] **Step 2: Add `.gitignore` and `README.md`**
  - Standard Android `.gitignore`.
  - `README.md` with features, setup guide, and build command (`./gradlew assembleDebug`).

- [ ] **Step 3: Run full test and build verification**
  Run: `./gradlew testDebugUnitTest && ./gradlew assembleDebug`
  Expected: All unit tests pass, debug APK built successfully.

- [ ] **Step 4: Commit and tag initial release**
  ```bash
  git add .
  git commit -m "release: v1.0.0 WhatsWeb initial release"
  git tag -a v1.0.0 -m "Release v1.0.0 - WhatsWeb"
  ```
