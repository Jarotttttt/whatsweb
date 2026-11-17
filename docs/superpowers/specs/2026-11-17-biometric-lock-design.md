# Design Specification: Biometric App Lock for WhatsWeb

**Document ID:** `2026-11-17-biometric-lock-design`  
**Date:** 2026-11-17  
**Status:** Approved  
**Author:** Antigravity Team & User Collaboration  

---

## 1. Intent & Problem Statement
Users accessing WhatsApp Web on their phone store sensitive personal and business conversations. If someone else borrows the device, they can open the app and view active chats. **Biometric App Lock** protects user privacy by securing WhatsWeb behind Android's native fingerprint/biometric authentication with a device credential fallback (PIN/pattern) and a convenient 1-minute background timeout.

---

## 2. Requirements & Behavior

### 2.1 Activation & Persistence
- A toggle switch item in the top toolbar menu: **"Kunci Aplikasi (Biometrik)"** (ON/OFF).
- Saved in `SharedPreferences` (`biometric_lock_enabled: Boolean`).
- Default: `false` (disabled until the user turns it on).

### 2.2 Lock Timing (Option B: 1-Minute Timeout)
- When enabled:
  - When the app is backgrounded (`onStop()`), record the timestamp `lastBackgroundTime = System.currentTimeMillis()`.
  - When the app is foregrounded (`onStart()` / `onResume()`):
    - If `System.currentTimeMillis() - lastBackgroundTime > 60_000L` (1 minute), trigger the lock screen.
    - If within 60 seconds, keep unlocked.
  - On fresh cold launch (`savedInstanceState == null`), immediately lock.

### 2.3 Lock Screen Overlay & Authentication
- Full-screen `lockOverlayLayout` (covering the WebView):
  - Lock icon (`ic_lock.xml`).
  - Title: "WhatsWeb Terkunci".
  - Subtitle: "Gunakan sidik jari atau PIN untuk membuka".
  - Button: "Buka Kunci" (`btnUnlock`).
- Trigger Android's native `BiometricPrompt`:
  - Authenticators: `BIOMETRIC_STRONG or DEVICE_CREDENTIAL` (ensuring devices with fingerprint, face, or standard PIN/pattern can all unlock seamlessly).
  - Title: "Verifikasi WhatsWeb".
  - Subtitle: "Pindai sidik jari atau masukkan PIN perangkat".
- On success: hide `lockOverlayLayout`, reveal WhatsApp Web.
- On failure/cancel: keep `lockOverlayLayout` visible and allow tapping "Buka Kunci" to re-prompt.

---

## 3. Technology & Dependencies
- `androidx.biometric:biometric:1.2.0-alpha05` (or `1.1.0`)
- Android `BiometricManager`, `BiometricPrompt`
- Pure native Android integration with zero external cloud dependencies.
