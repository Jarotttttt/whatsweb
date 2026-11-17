# Biometric App Lock Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement **Biometric App Lock** in WhatsWeb using Android's native `BiometricPrompt` (Fingerprint/Face/PIN fallback), securing the app behind a privacy overlay when cold launched or backgrounded for more than 1 minute.

**Architecture:** A standalone domain helper `BiometricLockHelper` evaluates timeout and capability rules. `MainActivity` implements a full-screen Material 3 `lockOverlay` and delegates authentication to `androidx.biometric.BiometricPrompt`. Settings are persisted in `SharedPreferences`.

**Tech Stack:** Kotlin, `androidx.biometric:biometric:1.2.0-alpha05`, Material Design 3.

**Spec:** [`D:/whatsdirect_web/docs/superpowers/specs/2026-11-17-biometric-lock-design.md`](file:///D:/whatsdirect_web/docs/superpowers/specs/2026-11-17-biometric-lock-design.md)

---

## Global Constraints

- **Timeout Threshold:** Exactly 60,000 milliseconds (1 minute). If background duration $\le 60,000\text{ms}$, remain unlocked. If $> 60,000\text{ms}$, prompt lock overlay.
- **Fallback Authentication:** Always allow `BIOMETRIC_STRONG or DEVICE_CREDENTIAL` so users without fingerprint enrolled can still unlock using their device PIN/pattern/password.
- **Privacy Barrier:** When locked, the lock overlay must completely cover the WebView to prevent chat previews.

---

### Task 1: Biometric Lock Domain Helper & Unit Tests (TDD)

**Files:**
- Create: `app/src/main/java/com/whatsweb/security/BiometricLockHelper.kt`
- Test: `app/src/test/java/com/whatsweb/security/BiometricLockHelperTest.kt`

**Interfaces:**
- Produces:
  - `BiometricLockHelper.shouldLock(enabled: Boolean, lastBackgroundTime: Long, currentTime: Long, timeoutMs: Long = 60_000L): Boolean`
  - `BiometricLockHelper.DEFAULT_TIMEOUT_MS = 60_000L`

- [ ] **Step 1: Write failing test for `BiometricLockHelper`**
  In `app/src/test/java/com/whatsweb/security/BiometricLockHelperTest.kt`:
  ```kotlin
  package com.whatsweb.security
  import org.junit.Assert.*
  import org.junit.Test

  class BiometricLockHelperTest {
      @Test
      fun shouldNotLockWhenDisabled() {
          assertFalse(BiometricLockHelper.shouldLock(enabled = false, lastBackgroundTime = 1000L, currentTime = 70_000L))
      }

      @Test
      fun shouldLockOnColdStartWhenEnabled() {
          assertTrue(BiometricLockHelper.shouldLock(enabled = true, lastBackgroundTime = 0L, currentTime = 5000L))
      }

      @Test
      fun shouldNotLockWithinOneMinuteTimeout() {
          val lastBg = 100_000L
          val current = 145_000L // 45 seconds later
          assertFalse(BiometricLockHelper.shouldLock(enabled = true, lastBackgroundTime = lastBg, currentTime = current))
      }

      @Test
      fun shouldLockAfterOneMinuteTimeoutExceeded() {
          val lastBg = 100_000L
          val current = 161_000L // 61 seconds later
          assertTrue(BiometricLockHelper.shouldLock(enabled = true, lastBackgroundTime = lastBg, currentTime = current))
      }
  }
  ```

- [ ] **Step 2: Run test to verify it fails**
  Run: `./gradlew.bat testDebugUnitTest --tests com.whatsweb.security.BiometricLockHelperTest`
  Expected: FAIL

- [ ] **Step 3: Implement `BiometricLockHelper.kt`**
  ```kotlin
  package com.whatsweb.security

  object BiometricLockHelper {
      const val DEFAULT_TIMEOUT_MS: Long = 60_000L
      const val PREF_BIOMETRIC_ENABLED = "biometric_lock_enabled"
      const val PREFS_NAME = "whatsweb_security_prefs"

      fun shouldLock(
          enabled: Boolean,
          lastBackgroundTime: Long,
          currentTime: Long,
          timeoutMs: Long = DEFAULT_TIMEOUT_MS
      ): Boolean {
          if (!enabled) return false
          if (lastBackgroundTime <= 0L) return true
          val elapsed = currentTime - lastBackgroundTime
          return elapsed > timeoutMs
      }
  }
  ```

- [ ] **Step 4: Run test to verify it passes**
  Run: `./gradlew.bat testDebugUnitTest --tests com.whatsweb.security.BiometricLockHelperTest`
  Expected: PASS

- [ ] **Step 5: Commit**
  ```bash
  git add app/src/main/java/com/whatsweb/security/BiometricLockHelper.kt app/src/test/java/com/whatsweb/security/BiometricLockHelperTest.kt
  git commit -m "feat: add BiometricLockHelper with 1-minute timeout evaluation and unit tests"
  ```

---

### Task 2: Biometric Dependency, Lock Overlay Layout & MainActivity Integration

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/res/menu/menu_toolbar.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/drawable/ic_lock.xml`
- Modify: `app/src/main/java/com/whatsweb/MainActivity.kt`

**Interfaces:**
- Consumes: `BiometricLockHelper`, `androidx.biometric.BiometricPrompt`.
- Produces: Complete biometric lock experience with 1-minute background timeout.

- [ ] **Step 1: Add `androidx.biometric:biometric:1.2.0-alpha05` to `app/build.gradle.kts`**
- [ ] **Step 2: Add lock icon `ic_lock.xml`, strings, and overlay in `activity_main.xml`**
  - Add `lockOverlay` (FrameLayout or LinearLayout, background surface color, lock icon, title, subtitle, MaterialButton `btnUnlockBiometric`).
  - Add `action_biometric_lock` in `menu_toolbar.xml` with checkable attribute.
- [ ] **Step 3: Wire up `BiometricPrompt` and lifecycle in `MainActivity.kt`**
  - In `onStop()`, save `lastBackgroundTime = System.currentTimeMillis()`.
  - In `onStart()`, evaluate `BiometricLockHelper.shouldLock(...)`:
    - If true, display `lockOverlay` and automatically invoke `showBiometricPrompt()`.
  - Tapping `btnUnlockBiometric` re-triggers `showBiometricPrompt()`.
  - On auth success: hide `lockOverlay` and reset `lastBackgroundTime = 0L`.
  - In toolbar menu: toggle `action_biometric_lock`, saving preference to `SharedPreferences`.
- [ ] **Step 4: Run tests and assemble APK**
  Run: `./gradlew.bat testDebugUnitTest && ./gradlew.bat assembleDebug`
  Expected: ALL PASS, `app-debug.apk` built.
- [ ] **Step 5: Commit and tag v1.1.0**
  ```bash
  git add .
  git commit -m "feat: implement biometric app lock with 1-minute timeout and device credential fallback"
  git tag -a v1.1.0 -m "Release v1.1.0 - WhatsWeb with Biometric Lock"
  ```
