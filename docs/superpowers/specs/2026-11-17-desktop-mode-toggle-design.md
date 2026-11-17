# Desktop Mode Toggle Design Spec

**Date:** 2026-11-17  
**Author:** Xyuraa  
**Feature:** Optional Desktop Mode Toggle for WhatsWeb  

---

## 1. Intent & The Why
WhatsWeb by default loads `https://web.whatsapp.com` with a desktop user agent. However, users may want to toggle between Desktop View (wide viewport, pinch-to-zoom, desktop UA) and Mobile View (standard mobile UA, normal viewport) on demand, similar to the "Desktop site" feature found in Google Chrome.

---

## 2. Goals & Anti-Goals
- **Goals:**
  - Provide a checkable menu item "Desktop Site" in the toolbar overflow menu.
  - Default state: Enabled (true), maintaining full WhatsApp Web compatibility out-of-the-box.
  - Persist user preference in `SharedPreferences` (`pref_desktop_mode`).
  - Switching immediately updates WebView User-Agent and viewport settings, and reloads the active webpage.
  - Provide feedback toasts in English ("Desktop mode enabled" / "Mobile mode enabled").
- **Anti-Goals:**
  - No intrusive floating action buttons (FAB) or dedicated header buttons that crowd the mobile UI.
  - No session clearance or cookie wiping when switching modes.

---

## 3. Approaches Considered
1. **Approach 1 (Chosen): Overflow Menu Checkbox Item**
   - Clean, standard Android UX matching Chrome and Firefox "Desktop site" toggles.
   - Synchronized dynamically in `onPrepareOptionsMenu`.
2. **Approach 2: Dedicated Toolbar Action Icon**
   - Trades off toolbar space for 1-click access; rejected because it clutters the minimalist WhatsApp theme header.

---

## 4. Technical Specifications
- **Shared Preferences:**
  - Name: `"whatsweb_prefs"`
  - Key: `KEY_DESKTOP_MODE = "pref_desktop_mode"`
  - Default: `true`
- **Desktop Mode Enabled (`true`):**
  - `userAgentString`: `WaWebConfig.DESKTOP_USER_AGENT`
  - `useWideViewPort = true`
  - `loadWithOverviewMode = true`
- **Desktop Mode Disabled (`false`):**
  - `userAgentString`: Mobile User Agent (via `WebSettings.getDefaultUserAgent(context)`)
  - `useWideViewPort = false`
  - `loadWithOverviewMode = false`
- **UI Resources:**
  - Menu ID: `R.id.action_desktop_mode` in `menu_toolbar.xml`
  - String: `menu_desktop_mode` ("Desktop Site")
  - String: `toast_desktop_mode_enabled` ("Desktop mode enabled")
  - String: `toast_desktop_mode_disabled` ("Mobile mode enabled")
