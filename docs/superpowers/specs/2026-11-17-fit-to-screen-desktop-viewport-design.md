# Fit-to-Screen Desktop Viewport Design Spec

**Date:** 2026-11-17  
**Author:** Xyuraa  
**Feature:** Fit-to-Screen Desktop Viewport & Menu Clean-up  

---

## 1. Intent & Motivation
When viewing WhatsApp Web on mobile browsers with "Desktop site" enabled (such as in Chrome for Android), the browser scales the entire desktop layout (typically 1024px width) down so that both the chat list sidebar and the active conversation pane fit onto the screen without requiring horizontal scrolling or panning.

Currently, WhatsApp Web does not include a `<meta name="viewport">` tag, causing the WebView's overview mode to render at an unscaled zoom level that cuts off the right side and requires constant horizontal dragging. Furthermore, the user requested reverting the manual "Desktop Site" menu toggle so the app stays clean and dedicated to the desktop experience.

---

## 2. Goals & Anti-Goals
- **Goals:**
  - Revert the "Desktop Site" checkbox from the toolbar overflow menu.
  - Remove `DesktopModeHelper` and restore a clean, streamlined menu (`Refresh`, `Log out / Clear Session`, `App Lock`, `About`).
  - Automatically scale the desktop viewport to fit the device screen width on initial load (no horizontal scrolling required to see the chat UI).
  - Retain pinch-to-zoom capability so users can zoom in on text or media when desired.
  - Maintain English-only UI and author information (`Xyuraa`).
- **Anti-Goals:**
  - Do not alter user authentication or session persistence.
  - Do not introduce distortion or crop message balloons.

---

## 3. Technical Implementation
1. **Menu Reversion:**
   - Remove `action_desktop_mode` from `app/src/main/res/menu/menu_toolbar.xml`.
   - Remove desktop mode strings from `app/src/main/res/values/strings.xml`.
   - Remove `DesktopModeHelper.kt` and `DesktopModeHelperTest.kt`.
   - Clean up `MainActivity.kt` to always use `WaWebConfig.DESKTOP_USER_AGENT` with `useWideViewPort = true` and `loadWithOverviewMode = true`.

2. **Fit-to-Screen Viewport Script:**
   - In `WaWebViewClient.kt`, introduce `VIEWPORT_FIT_SCRIPT`:
     ```javascript
     (function() {
         var meta = document.querySelector('meta[name="viewport"]');
         if (!meta) {
             meta = document.createElement('meta');
             meta.name = 'viewport';
             document.head.appendChild(meta);
         }
         meta.setAttribute('content', 'width=1024, user-scalable=yes');
     })();
     ```
   - Evaluate `VIEWPORT_FIT_SCRIPT` in `onPageCommitVisible` (or `onPageFinished`) so the layout engine automatically adjusts the initial overview scale to 1024px, fitting the entire desktop WhatsApp interface perfectly to the device screen.
