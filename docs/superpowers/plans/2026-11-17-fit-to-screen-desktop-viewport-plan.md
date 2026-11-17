# Fit-to-Screen Desktop Viewport Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Revert the manual desktop toggle from the toolbar menu and implement fit-to-screen viewport auto-scaling for WhatsApp Web in the WebView.

**Architecture:** Remove `DesktopModeHelper` and clean up `menu_toolbar.xml` and `MainActivity`. In `WaWebViewClient`, inject `VIEWPORT_FIT_SCRIPT` targeting `width=1024, user-scalable=yes` to force WebKit/Blink to zoom-out and fit the two-column desktop WhatsApp Web interface to the device display without horizontal scrolling.

**Tech Stack:** Kotlin, Android SDK 34, AndroidX WebKit, JUnit 4.

**Spec:** docs/superpowers/specs/2026-11-17-fit-to-screen-desktop-viewport-design.md

## Global Constraints

- Android minSdk 24, targetSdk 34.
- English-only user-facing strings in `strings.xml`.
- Full desktop WhatsApp Web interface (1024px) must fit the screen without requiring horizontal scrolling or panning.
- Maintain pinch-to-zoom support (`setSupportZoom(true)`, `builtInZoomControls(true)`).

## Review Focus

1. **Clean Menu:** Menu toolbar contains only Refresh, Log out / Clear Session, App Lock, and About.
2. **Session Safety:** Removing toggle logic must not touch cookies, localStorage, or IndexedDB.
3. **Viewport Scale Script:** `VIEWPORT_FIT_SCRIPT` must dynamically ensure `<meta name="viewport" content="width=1024, user-scalable=yes">` is present in the DOM.
4. **Desktop UA:** `MainActivity` unconditionally uses `WaWebConfig.DESKTOP_USER_AGENT`.
5. **No Broken References:** Clean deletion of `DesktopModeHelper` without lingering imports or broken tests.

---

### Task 1: Revert Desktop Mode Toggle from Menu & Clean Up Code

**Files:**
- Delete: `app/src/main/java/com/whatsweb/engine/DesktopModeHelper.kt`
- Delete: `app/src/test/java/com/whatsweb/engine/DesktopModeHelperTest.kt`
- Modify: `app/src/main/res/menu/menu_toolbar.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/java/com/whatsweb/MainActivity.kt`

- [ ] **Step 1: Delete `DesktopModeHelper.kt` and `DesktopModeHelperTest.kt`**
- [ ] **Step 2: Remove `action_desktop_mode` from `menu_toolbar.xml`**
- [ ] **Step 3: Remove desktop mode strings from `strings.xml`**
- [ ] **Step 4: Clean up `MainActivity.kt`**
  - Remove `defaultMobileUserAgent` and `applyDesktopModeSettings`.
  - Set `userAgentString = WaWebConfig.DESKTOP_USER_AGENT`, `useWideViewPort = true`, `loadWithOverviewMode = true` in `configureWebView()`.
  - Remove `action_desktop_mode` from `onPrepareOptionsMenu` and `onOptionsItemSelected`.
- [ ] **Step 5: Run unit tests**
  - Run: `.\gradlew.bat testDebugUnitTest`
  - Expected: BUILD SUCCESSFUL (all 23 tests pass).
- [ ] **Step 6: Commit**
  ```bash
  git commit -am "refactor: revert manual desktop site menu toggle"
  ```

---

### Task 2: Implement Fit-to-Screen Desktop Viewport Scaling

**Files:**
- Modify: `app/src/main/java/com/whatsweb/engine/WaWebViewClient.kt`
- Modify: `app/src/test/java/com/whatsweb/engine/WaWebViewClientTest.kt`

- [ ] **Step 1: Write failing test in `WaWebViewClientTest.kt`**
  ```kotlin
  @Test
  fun testViewportFitScriptConfiguredForDesktop1024() {
      val script = WaWebViewClient.VIEWPORT_FIT_SCRIPT
      assertTrue(script.contains("width=1024"))
      assertTrue(script.contains("user-scalable=yes"))
  }
  ```
- [ ] **Step 2: Run test to verify it fails**
  - Run: `.\gradlew.bat testDebugUnitTest --tests com.whatsweb.engine.WaWebViewClientTest.testViewportFitScriptConfiguredForDesktop1024`
  - Expected: FAIL (Unresolved reference: `VIEWPORT_FIT_SCRIPT`).
- [ ] **Step 3: Implement `VIEWPORT_FIT_SCRIPT` in `WaWebViewClient.kt`**
  - Add constant:
    ```kotlin
    const val VIEWPORT_FIT_SCRIPT = """
        (function() {
            var meta = document.querySelector('meta[name="viewport"]');
            if (!meta) {
                meta = document.createElement('meta');
                meta.name = 'viewport';
                document.head.appendChild(meta);
            }
            meta.setAttribute('content', 'width=1024, user-scalable=yes');
        })();
    """
    ```
  - In `onPageCommitVisible` and `onPageFinished`, evaluate `VIEWPORT_FIT_SCRIPT` when URL is an allowed WhatsApp URL.
- [ ] **Step 4: Run unit tests and assembleRelease**
  - Run: `.\gradlew.bat testDebugUnitTest`
  - Run: `.\gradlew.bat assembleRelease`
  - Expected: BUILD SUCCESSFUL.
- [ ] **Step 5: Commit**
  ```bash
  git commit -am "feat: add fit-to-screen desktop viewport scaling script"
  ```
