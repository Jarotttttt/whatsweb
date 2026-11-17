# Desktop Mode Toggle Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Provide an optional "Desktop Site" toggle in the overflow menu that dynamically switches between desktop and mobile user agents/viewports, persists the choice, and reloads the page.

**Architecture:** Add `DesktopModeHelper` to encapsulate preference storage and User-Agent selection. Wire it into `MainActivity` with an overflow menu checkbox (`action_desktop_mode`), dynamically applying UA and viewport settings (`useWideViewPort`, `loadWithOverviewMode`) followed by `webView.reload()`.

**Tech Stack:** Kotlin, Android SDK 34, AndroidX WebKit, JUnit 4.

**Spec:** docs/superpowers/specs/2026-11-17-desktop-mode-toggle-design.md

## Global Constraints

- Android minSdk 24, targetSdk 34.
- English-only user-facing strings in `strings.xml`.
- Default state must be `true` (Desktop Mode enabled) to preserve seamless WhatsApp Web experience.
- No session clearing or cookie deletion when switching modes.

## Review Focus

1. **Default State Persistence:** Fresh install must return `true` for desktop mode.
2. **User-Agent Switching:** When desktop mode is true, returns `WaWebConfig.DESKTOP_USER_AGENT`; when false, returns the mobile UA.
3. **Viewport Alignment:** When switching to mobile, `useWideViewPort` and `loadWithOverviewMode` must be disabled.
4. **Live Reload:** Switching toggles must immediately reload the WebView without losing user session cookies.
5. **Menu Checkbox Sync:** `onPrepareOptionsMenu` must accurately reflect the stored preference state.

---

### Task 1: DesktopModeHelper & Unit Tests (TDD)

**Files:**
- Create: `app/src/main/java/com/whatsweb/engine/DesktopModeHelper.kt`
- Create: `app/src/test/java/com/whatsweb/engine/DesktopModeHelperTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  object DesktopModeHelper {
      const val PREFS_NAME = "whatsweb_prefs"
      const val PREF_KEY_DESKTOP_MODE = "pref_desktop_mode"
      const val DEFAULT_DESKTOP_MODE = true

      fun resolveUserAgent(isDesktop: Boolean, defaultMobileUa: String): String
      fun isDesktopModeEnabled(context: Context): Boolean
      fun setDesktopModeEnabled(context: Context, enabled: Boolean)
  }
  ```

- [ ] **Step 1: Write the failing unit tests**

```kotlin
package com.whatsweb.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class DesktopModeHelperTest {

    @Test
    fun resolveUserAgent_whenDesktopTrue_returnsDesktopUa() {
        val result = DesktopModeHelper.resolveUserAgent(true, "MobileUA")
        assertEquals(WaWebConfig.DESKTOP_USER_AGENT, result)
    }

    @Test
    fun resolveUserAgent_whenDesktopFalse_returnsDefaultMobileUa() {
        val mobileUa = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36"
        val result = DesktopModeHelper.resolveUserAgent(false, mobileUa)
        assertEquals(mobileUa, result)
    }

    @Test
    fun resolveUserAgent_whenDesktopFalseAndMobileBlank_returnsDesktopUaFallback() {
        val result = DesktopModeHelper.resolveUserAgent(false, "")
        assertEquals(WaWebConfig.DESKTOP_USER_AGENT, result)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests com.whatsweb.engine.DesktopModeHelperTest`  
Expected: FAIL (Unresolved reference: `DesktopModeHelper`).

- [ ] **Step 3: Implement `DesktopModeHelper`**

Implement `resolveUserAgent`, `isDesktopModeEnabled`, and `setDesktopModeEnabled` in `app/src/main/java/com/whatsweb/engine/DesktopModeHelper.kt`.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests com.whatsweb.engine.DesktopModeHelperTest`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/whatsweb/engine/DesktopModeHelper.kt app/src/test/java/com/whatsweb/engine/DesktopModeHelperTest.kt
git commit -m "feat: add DesktopModeHelper with unit tests"
```

---

### Task 2: UI Integration (Menu, Strings, and MainActivity)

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/menu/menu_toolbar.xml`
- Modify: `app/src/main/java/com/whatsweb/MainActivity.kt`

**Interfaces:**
- Consumes: `DesktopModeHelper.isDesktopModeEnabled(context)`, `DesktopModeHelper.setDesktopModeEnabled(context, enabled)`, `DesktopModeHelper.resolveUserAgent(isDesktop, defaultMobileUa)`.
- Produces: Checkable menu item `R.id.action_desktop_mode` in toolbar, applying settings dynamically and reloading the WebView.

- [ ] **Step 1: Add string resources**

Add to `strings.xml`:
```xml
    <string name="menu_desktop_mode">Desktop Site</string>
    <string name="toast_desktop_mode_enabled">Desktop mode enabled</string>
    <string name="toast_desktop_mode_disabled">Mobile mode enabled</string>
```

- [ ] **Step 2: Add menu item to `menu_toolbar.xml`**

```xml
    <item
        android:id="@+id/action_desktop_mode"
        android:title="@string/menu_desktop_mode"
        android:checkable="true"
        app:showAsAction="never" />
```

- [ ] **Step 3: Update `MainActivity.kt`**

- In `configureWebView()`: Save initial `defaultMobileUserAgent = webView.settings.userAgentString`. Apply `applyDesktopModeSettings(isDesktop: Boolean)`.
- In `onPrepareOptionsMenu()`: Set `menu.findItem(R.id.action_desktop_mode)?.isChecked = isDesktopModeEnabled()`.
- In `onOptionsItemSelected()`: For `R.id.action_desktop_mode`:
  ```kotlin
  val newMode = !isDesktopModeEnabled()
  setDesktopModeEnabled(newMode)
  applyDesktopModeSettings(newMode)
  webView.reload()
  val toastRes = if (newMode) R.string.toast_desktop_mode_enabled else R.string.toast_desktop_mode_disabled
  Toast.makeText(this, toastRes, Toast.LENGTH_SHORT).show()
  invalidateOptionsMenu()
  true
  ```

- [ ] **Step 4: Run unit tests & build release APK**

Run: `.\gradlew.bat testDebugUnitTest`  
Run: `.\gradlew.bat assembleRelease`  
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/menu/menu_toolbar.xml app/src/main/java/com/whatsweb/MainActivity.kt
git commit -m "feat: add optional Desktop Site toggle with live reload and persistence"
```
