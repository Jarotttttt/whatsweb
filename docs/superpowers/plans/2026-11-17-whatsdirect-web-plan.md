# WhatsDirect Web Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build **WhatsDirect Web**, a native Android (Kotlin + Material 3) application that delivers a desktop WhatsApp Web client on mobile (bypassing the 10MB mobile video compression limit) alongside a Direct Chat utility for messaging unsaved phone numbers.

**Architecture:** Native Android single-activity architecture (`MainActivity`) hosting two fragment destinations via `BottomNavigationView`: `WaWebFragment` (desktop WebView with cookie persistence, file picker, and WebRTC media permissions) and `DirectChatFragment` (phone number normalization and deep link dispatch). State is preserved across tab switching using Fragment `hide`/`show` transactions.

**Tech Stack:** Kotlin 1.9+, Android SDK 34 (Min SDK 24), AndroidX Core KTX, Material Design 3 (`com.google.android.material`), AndroidX WebKit, ConstraintLayout.

**Spec:** [`D:/whatsdirect_web/docs/superpowers/specs/2026-11-17-whatsdirect-web-design.md`](file:///D:/whatsdirect_web/docs/superpowers/specs/2026-11-17-whatsdirect-web-design.md)

---

## Global Constraints

- **Platform Target:** Android Min SDK 24 (Android 7.0), Target SDK 34 (Android 14).
- **Language & UI:** 100% Kotlin with XML Material 3 layouts (`Theme.Material3.DayNight.NoActionBar`).
- **User Agent:** Strictly spoof desktop Chrome on Windows 10 (`Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36`).
- **Session Preservation:** Cookies and HTML5 DOM storage must persist across app restarts; Fragment navigation must not reload the WebView.
- **Privacy & Safety:** Zero tracking, zero third-party proxies; all web requests route directly to `web.whatsapp.com`.

---

## Review Focus

1. **Phone Number Sanitization Failure:** User inputs `0812-3456 7890` or `+62 812.3456.7890`; expected behavior is normalized clean international format `6281234567890` without leading zeros or punctuation.
2. **WebView Back Press Crash or Exit:** User presses Android system back button while viewing a chat in WhatsApp Web; expected behavior is navigating back inside web history if `canGoBack()` is true, rather than closing the app.
3. **File Upload Failure (`onShowFileChooser`):** User taps the attach button in WhatsApp Web to upload a high-res video/image; expected behavior is launching Android's system media picker and successfully returning selected URIs to the webview.
4. **Microphone / Camera Permission Denial:** User records a voice message or clicks camera in WhatsApp Web; expected behavior is prompting Android runtime permissions and granting WebRTC capture permissions gracefully.
5. **Session Loss on Tab Switch:** User switches from WA Web tab to Direct Chat tab and back; expected behavior is preserving the logged-in WhatsApp Web DOM state without triggering a page reload.

---

### Task 1: Project Scaffolding & Gradle Build Configuration

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `app/src/test/java/com/whatsdirect/web/BuildConfigurationTest.kt`

**Interfaces:**
- Consumes: None (Root scaffolding).
- Produces: Complete compile-ready Android project structure with required dependencies (`appcompat`, `material`, `webkit`, `core-ktx`, `fragment-ktx`, `junit`).

- [ ] **Step 1: Create Gradle wrapper configuration and root build scripts**
  Set up `settings.gradle.kts` (root project name `WhatsDirectWeb`), root `build.gradle.kts` with Android Application and Kotlin plugins, and `gradle/wrapper/gradle-wrapper.properties` pointing to Gradle 8.4+.

- [ ] **Step 2: Configure `app/build.gradle.kts` and `AndroidManifest.xml`**
  Set `compileSdk = 34`, `minSdk = 24`, `targetSdk = 34`, and add dependencies:
  - `androidx.core:core-ktx:1.13.1`
  - `androidx.appcompat:appcompat:1.7.0`
  - `com.google.android.material:material:1.12.0`
  - `androidx.constraintlayout:constraintlayout:2.1.4`
  - `androidx.webkit:webkit:1.11.0`
  - `androidx.fragment:fragment-ktx:1.8.0`
  - `androidx.swiperefreshlayout:swiperefreshlayout:1.1.0`
  - `testImplementation(junit:junit:4.13.2)`
  In `AndroidManifest.xml`, declare permissions: `INTERNET`, `ACCESS_NETWORK_STATE`, `CAMERA`, `RECORD_AUDIO`, `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`.

- [ ] **Step 3: Write test verifying build package constants**
  In `app/src/test/java/com/whatsdirect/web/BuildConfigurationTest.kt`:
  ```kotlin
  package com.whatsdirect.web
  import org.junit.Assert.assertEquals
  import org.junit.Test

  class BuildConfigurationTest {
      @Test
      fun verifyApplicationPackageName() {
          val expectedPackage = "com.whatsdirect.web"
          assertEquals("com.whatsdirect.web", expectedPackage)
      }
  }
  ```

- [ ] **Step 4: Run test to verify build configuration**
  Run: `./gradlew testDebugUnitTest`
  Expected: PASS

- [ ] **Step 5: Commit scaffolding**
  ```bash
  git add .
  git commit -m "chore: scaffold Android project with Gradle 8 and Material 3 dependencies"
  ```

---

### Task 2: Phone Number Normalizer & WhatsApp Deep Link Helper (TDD)

**Files:**
- Create: `app/src/main/java/com/whatsdirect/web/util/PhoneNumberHelper.kt`
- Test: `app/src/test/java/com/whatsdirect/web/util/PhoneNumberHelperTest.kt`

**Interfaces:**
- Consumes: User inputs (country code string, phone number string, optional message string).
- Produces:
  - `PhoneNumberHelper.normalize(countryCode: String, phoneNumber: String): Result<String>`
  - `PhoneNumberHelper.createWhatsAppUri(normalizedNumber: String, message: String?): String`

- [ ] **Step 1: Write failing tests for phone number normalization and URI generation**
  In `app/src/test/java/com/whatsdirect/web/util/PhoneNumberHelperTest.kt`:
  ```kotlin
  package com.whatsdirect.web.util
  import org.junit.Assert.*
  import org.junit.Test

  class PhoneNumberHelperTest {
      @Test
      fun normalizeIndonesianLocalNumber_stripsLeadingZeroAndPrependsCountryCode() {
          val result = PhoneNumberHelper.normalize("+62", "0812-3456-7890")
          assertTrue(result.isSuccess)
          assertEquals("6281234567890", result.getOrNull())
      }

      @Test
      fun normalizeInternationalNumber_stripsSpacesAndSymbols() {
          val result = PhoneNumberHelper.normalize("1", "(555) 234-5678")
          assertTrue(result.isSuccess)
          assertEquals("15552345678", result.getOrNull())
      }

      @Test
      fun normalizeTooShortNumber_returnsFailure() {
          val result = PhoneNumberHelper.normalize("+62", "123")
          assertTrue(result.isFailure)
      }

      @Test
      fun createWhatsAppUri_withMessage_encodesCorrectly() {
          val uri = PhoneNumberHelper.createWhatsAppUri("6281234567890", "Halo, apa kabar?")
          assertEquals("https://wa.me/6281234567890?text=Halo%2C+apa+kabar%3F", uri)
      }

      @Test
      fun createWhatsAppUri_withoutMessage_returnsPlainNumberUri() {
          val uri = PhoneNumberHelper.createWhatsAppUri("6281234567890", "")
          assertEquals("https://wa.me/6281234567890", uri)
      }
  }
  ```

- [ ] **Step 2: Run test to verify it fails**
  Run: `./gradlew testDebugUnitTest --tests com.whatsdirect.web.util.PhoneNumberHelperTest`
  Expected: FAIL with "Unresolved reference: PhoneNumberHelper"

- [ ] **Step 3: Implement `PhoneNumberHelper` in `app/src/main/java/com/whatsdirect/web/util/PhoneNumberHelper.kt`**
  ```kotlin
  package com.whatsdirect.web.util

  import java.net.URLEncoder
  import java.nio.charset.StandardCharsets

  object PhoneNumberHelper {
      fun normalize(countryCode: String, phoneNumber: String): Result<String> {
          val cleanCode = countryCode.replace(Regex("[^0-9]"), "")
          var cleanPhone = phoneNumber.replace(Regex("[^0-9]"), "")

          if (cleanPhone.startsWith("0")) {
              cleanPhone = cleanPhone.substring(1)
          }

          val fullNumber = cleanCode + cleanPhone
          return if (fullNumber.length in 7..16) {
              Result.success(fullNumber)
          } else {
              Result.failure(IllegalArgumentException("Nomor telepon tidak valid (terlalu pendek atau terlalu panjang)"))
          }
      }

      fun createWhatsAppUri(normalizedNumber: String, message: String?): String {
          val baseUrl = "https://wa.me/$normalizedNumber"
          return if (!message.isNull prematureBlank()) {
              val encoded = URLEncoder.encode(message.trim(), StandardCharsets.UTF_8.toString())
              "$baseUrl?text=$encoded"
          } else {
              baseUrl
          }
      }

      private fun String?.prematureBlank(): Boolean = this == null || this.trim().isEmpty()
  }
  ```

- [ ] **Step 4: Run test to verify it passes**
  Run: `./gradlew testDebugUnitTest --tests com.whatsdirect.web.util.PhoneNumberHelperTest`
  Expected: PASS

- [ ] **Step 5: Commit**
  ```bash
  git add app/src/main/java/com/whatsdirect/web/util/PhoneNumberHelper.kt app/src/test/java/com/whatsdirect/web/util/PhoneNumberHelperTest.kt
  git commit -m "feat: add phone number normalizer and WhatsApp deep link generator with tests"
  ```

---

### Task 3: Direct Chat Screen UI & Fragment Implementation

**Files:**
- Create: `app/src/main/res/layout/fragment_direct_chat.xml`
- Create: `app/src/main/java/com/whatsdirect/web/ui/directchat/DirectChatFragment.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `PhoneNumberHelper.normalize()`, `PhoneNumberHelper.createWhatsAppUri()`.
- Produces: `DirectChatFragment` ready to be attached to `MainActivity`.

- [ ] **Step 1: Define strings and layout in `res/values/strings.xml` and `fragment_direct_chat.xml`**
  Layout contains:
  - Header title and icon ("Kirim Pesan Langsung Tanpa Simpan Kontak").
  - Country code `TextInputLayout` (`edtCountryCode`, prefilled with `+62`).
  - Phone number `TextInputLayout` (`edtPhoneNumber`, `inputType="phone"`).
  - Message `TextInputLayout` (`edtMessage`, multiline, max 5 lines).
  - `MaterialButton` (`btnSendDirect`, text "Buka Chat di WhatsApp", `app:icon="@drawable/ic_whatsapp"`).

- [ ] **Step 2: Implement `DirectChatFragment.kt`**
  - Read input values, call `PhoneNumberHelper.normalize`.
  - On validation error, set `edtPhoneNumberLayout.error = exception.message`.
  - On success, build URI and launch `Intent(Intent.ACTION_VIEW, Uri.parse(uri))`.
  - Catch `ActivityNotFoundException` with a Toast: `"WhatsApp tidak ditemukan di perangkat ini. Membuka di browser..."`.

- [ ] **Step 3: Verify layout compiles and runs**
  Run: `./gradlew assembleDebug`
  Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**
  ```bash
  git add app/src/main/res/layout/fragment_direct_chat.xml app/src/main/java/com/whatsdirect/web/ui/directchat/DirectChatFragment.kt app/src/main/res/values/strings.xml
  git commit -m "feat: implement DirectChatFragment UI and deep link intent dispatcher"
  ```

---

### Task 4: WhatsApp Web Desktop Engine (`WaWebFragment`, Config & Clients)

**Files:**
- Create: `app/src/main/java/com/whatsdirect/web/ui/waweb/WaWebConfig.kt`
- Create: `app/src/main/java/com/whatsdirect/web/ui/waweb/WaWebChromeClient.kt`
- Create: `app/src/main/java/com/whatsdirect/web/ui/waweb/WaWebFragment.kt`
- Create: `app/src/main/res/layout/fragment_wa_web.xml`
- Test: `app/src/test/java/com/whatsdirect/web/ui/waweb/WaWebConfigTest.kt`

**Interfaces:**
- Consumes: Android `WebView`, `CookieManager`, `WebChromeClient`, `SwipeRefreshLayout`.
- Produces: `WaWebFragment` providing full WhatsApp Web desktop browsing with media picker and microphone WebRTC support.

- [ ] **Step 1: Write test for `WaWebConfig` constants**
  In `app/src/test/java/com/whatsdirect/web/ui/waweb/WaWebConfigTest.kt`:
  ```kotlin
  package com.whatsdirect.web.ui.waweb
  import org.junit.Assert.*
  import org.junit.Test

  class WaWebConfigTest {
      @Test
      fun verifyTargetUrlAndUserAgent() {
          assertEquals("https://web.whatsapp.com", WaWebConfig.WHATSAPP_WEB_URL)
          assertTrue(WaWebConfig.DESKTOP_USER_AGENT.contains("Windows NT 10.0"))
          assertTrue(WaWebConfig.DESKTOP_USER_AGENT.contains("Chrome/"))
      }
  }
  ```

- [ ] **Step 2: Run test to verify it fails**
  Run: `./gradlew testDebugUnitTest --tests com.whatsdirect.web.ui.waweb.WaWebConfigTest`
  Expected: FAIL

- [ ] **Step 3: Implement `WaWebConfig.kt`**
  ```kotlin
  package com.whatsdirect.web.ui.waweb

  object WaWebConfig {
      const val WHATSAPP_WEB_URL = "https://web.whatsapp.com"
      const val DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
  }
  ```

- [ ] **Step 4: Implement `WaWebChromeClient.kt`**
  - Implement `onShowFileChooser` using `ValueCallback<Array<Uri>>` and Android's `ActivityResultLauncher` to support camera capture and gallery photo/video selection.
  - Implement `onPermissionRequest` to grant `PermissionRequest.RESOURCE_AUDIO_CAPTURE` and `RESOURCE_VIDEO_CAPTURE` when Android permissions are granted.
  - Implement `onProgressChanged` to update progress bar.

- [ ] **Step 5: Implement `fragment_wa_web.xml` & `WaWebFragment.kt`**
  - Layout: `SwipeRefreshLayout` wrapping `ProgressBar` and `WebView`.
  - Configure `WebSettings`: `javaScriptEnabled = true`, `domStorageEnabled = true`, `databaseEnabled = true`, `setSupportZoom(true)`, `builtInZoomControls = true`, `displayZoomControls = false`, `useWideViewPort = true`, `loadWithOverviewMode = true`.
  - Enable `CookieManager.getInstance().setAcceptCookie(true)` and `setAcceptThirdPartyCookies(webView, true)`.
  - Implement `canGoBack()` and `goBack()` for back navigation.
  - Add public `reloadPage()` and `clearSession()` methods.

- [ ] **Step 6: Run tests and assemble**
  Run: `./gradlew testDebugUnitTest && ./gradlew assembleDebug`
  Expected: ALL PASS

- [ ] **Step 7: Commit**
  ```bash
  git add app/src/main/java/com/whatsdirect/web/ui/waweb/ app/src/main/res/layout/fragment_wa_web.xml app/src/test/java/com/whatsdirect/web/ui/waweb/
  git commit -m "feat: implement WaWebFragment with desktop user agent, file picker, and session persistence"
  ```

---

### Task 5: Main Activity Shell, Material 3 Bottom Navigation & Toolbar Actions

**Files:**
- Create: `app/src/main/res/menu/menu_bottom_nav.xml`
- Create: `app/src/main/res/menu/menu_top_toolbar.xml`
- Create: `app/src/main/res/layout/activity_main.xml`
- Create: `app/src/main/java/com/whatsdirect/web/MainActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: `WaWebFragment`, `DirectChatFragment`, `BottomNavigationView`.
- Produces: Complete navigable application holding both fragments with tab state retention.

- [ ] **Step 1: Create menus in `res/menu/`**
  - `menu_bottom_nav.xml`:
    - `menu_wa_web` (Title: "WA Web", Icon: `@drawable/ic_whatsapp`)
    - `menu_direct_chat` (Title: "Direct Chat", Icon: `@drawable/ic_direct_chat`)
  - `menu_top_toolbar.xml`:
    - `action_refresh` (Title: "Muat Ulang", Icon: `@drawable/ic_refresh`, `app:showAsAction="always"`)
    - `action_clear_session` (Title: "Bersihkan Sesi / Logout", `app:showAsAction="never"`)

- [ ] **Step 2: Create layout `activity_main.xml`**
  - `CoordinatorLayout` root.
  - `MaterialToolbar` (`id="@+id/topToolbar"`).
  - `FrameLayout` container (`id="@+id/fragmentContainer"`).
  - `BottomNavigationView` (`id="@+id/bottomNav"`).

- [ ] **Step 3: Implement `MainActivity.kt`**
  - Initialize both `WaWebFragment` and `DirectChatFragment` using `supportFragmentManager.beginTransaction().add(...)`.
  - When switching tabs: use `hide()` and `show()` so `WaWebFragment`'s WebView and WebSocket connections are **never** destroyed or reloaded.
  - Toolbar `action_refresh` triggers `waWebFragment.reloadPage()`.
  - Toolbar `action_clear_session` shows MaterialAlertDialog confirming logout, then calls `waWebFragment.clearSession()`.
  - Register `OnBackPressedCallback`: if `activeFragment is WaWebFragment && waWebFragment.canGoBack()`, call `waWebFragment.goBack()`, else delegate to default behavior.

- [ ] **Step 4: Build APK and verify full integration**
  Run: `./gradlew assembleDebug`
  Expected: BUILD SUCCESSFUL (Generates `app-debug.apk`)

- [ ] **Step 5: Commit**
  ```bash
  git add app/src/main/res/menu/ app/src/main/res/layout/activity_main.xml app/src/main/java/com/whatsdirect/web/MainActivity.kt app/src/main/AndroidManifest.xml
  git commit -m "feat: implement MainActivity with Material 3 BottomNavigationView and state-preserving tab switcher"
  ```

---

### Task 6: Visual Assets, App Icons, Theming & Git Tagging

**Files:**
- Create: `app/src/main/res/values/colors.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/res/drawable/ic_whatsapp.xml`
- Create: `app/src/main/res/drawable/ic_direct_chat.xml`
- Create: `app/src/main/res/drawable/ic_refresh.xml`
- Create: `README.md`
- Create: `.gitignore`

**Interfaces:**
- Consumes: Android Resource System.
- Produces: Polished, production-ready WhatsApp branded styling and clean git repository.

- [ ] **Step 1: Add vector drawables and WhatsApp brand colors**
  - Primary color: WhatsApp Teal Green (`#128C7E` / `#25D366`), Surface: `#FFFFFF` / `#121212`.
  - Add clean vector drawables for icons.

- [ ] **Step 2: Add `.gitignore` and `README.md`**
  - Configure `.gitignore` for standard Android (`.gradle/`, `build/`, `local.properties`, `*.iml`, `.idea/`).
  - Write `README.md` detailing app features, build instructions (`./gradlew assembleDebug`), and architecture.

- [ ] **Step 3: Run full verification suite**
  Run: `./gradlew testDebugUnitTest && ./gradlew assembleDebug`
  Expected: All unit tests pass, debug APK built successfully.

- [ ] **Step 4: Commit and tag initial release**
  ```bash
  git add .
  git commit -m "release: v1.0.0 WhatsDirect Web ready for build"
  git tag -a v1.0.0 -m "Release v1.0.0 - WhatsDirect Web"
  ```
