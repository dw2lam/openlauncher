# Release v0.0.5: The Stability & Real Radio Update

> ⚠️ **UNTESTED RELEASE** — This version contains a large stability overhaul and a full radio rewrite that has **not yet been verified on physical head units**. Install it if you want the latest fixes and are willing to report issues — otherwise stay on v0.0.4 until a tested follow-up lands. Bug reports on the [Issues tab](../../issues) are hugely appreciated.

This release is a ground-up correctness pass over the entire launcher. Version 0.0.5 fixes every known crash on legacy Android 5.x head units, repairs the trip tracker's core distance recording, replaces the simulated radio with a real-tuner mirroring system that works beyond a single vendor, sharpens album art, makes day mode legible everywhere, and adds a proper "set as default launcher" flow.

---

## ✨ What's New

### 📻 Real Radio — No More Simulations
- **Removed all fake radio audio and demo stations.** The synthesized static generator, fake seek scanning, and placeholder stations ("NIGHTDRIVE FM" and friends) are gone entirely.
- **Two real tuner backends**:
  - **szchoiceway MCU units** — direct canbus control as before: seek, FM1/FM2/FM3/AM band switching, and direct-tune frequency presets with memory.
  - **All other units** — the launcher now mirrors and controls your head unit's **own radio app through its media session** (the same channel steering-wheel keys use). Live frequency and station/RDS readout, seek, open, and power controls.
- **Assign Radio App** — when no tuner source is detected, the radio deck now says so honestly and lets you assign your unit's radio app right from the widget.
- **Capability-aware deck UI** — presets and band chips only appear when the backend can actually tune; mirrored sources show a clean read-only band and live station line.

### 🧭 Trip Tracker Actually Tracks
- Fixed the core recording loop reading a frozen GPS snapshot — distance, drive time, and average speed previously recorded nothing if you started tracking while stationary.
- Distance now accumulates against real elapsed time, and the 0–100 timer uses the monotonic clock so NTP time jumps can't corrupt a run.

### 🎨 Day/Night & Theming Consistency
- Reset and color-picker dialogs are no longer near-invisible in day mode.
- The color picker's **Apply** button is now filled with your chosen color with auto-contrast text — picking a dark color no longer hides the button. Sliders also start on the correct values instead of flashing red.
- Accent-tinted system components now auto-contrast against light/dark accents (no more white-on-white chips in day mode).
- Status icons, edit controls, and widget labels now have proper day-mode variants.

### 🖼️ Album Art Quality
- Now Playing prefers the **full-resolution artwork URI** when the media app provides one, picks the largest available bitmap otherwise, and renders with high filter quality — no more soft, stretched notification thumbnails.

### 🏠 Set as Default Launcher
- New status row in Settings shows whether Open Launcher currently holds the home role, and requests it through the proper system dialog (Android 10+) with automatic fallbacks for vendor ROMs that hide the standard screens.

### 🛰️ Calibration Honesty & GPS
- A-GPS reset now sends the command Android's GPS driver actually recognizes and reports honestly when a device doesn't support it.
- The magnetometer sweep now correctly describes what it does (guides Android's continuous self-calibration) and the placebo "mounting level" button has been removed.
- Location permission can now be granted directly from Settings — skipping onboarding no longer permanently disables GPS features.

---

## 🛠️ Stability & Bug Fixes

- **Fixed all known Android 5.x head unit crashes** — launch crash from a connectivity API guard, Settings screen crash, and a crash when the GPS provider toggles on Android 5–10.
- **Fixed a potential permanent crash-loop** from corrupted or legacy saved settings — all stored values now fail safe to defaults.
- **Fixed rapid settings changes silently reverting each other** (e.g. dragging a widget then flipping a toggle).
- **Fixed widget resize stacking widgets on top of each other** — enlarging now pushes neighbors aside like dragging does; re-adding a large widget can no longer overlap.
- **Fixed wallpaper disappearing after reboot** — the picker now takes a persistable permission that actually survives restarts.
- **Faster wake from sleep** — the media listener no longer rebuilds its state and re-renders the Now Playing widget for every notification on the system.
- **Day/night Sunset mode now flips while parked** — sunrise/sunset and weather refresh no longer require the vehicle to move.
- **Soundboard** — fixed audio player leaks on failed taps, a potential crash assigning pads, and playback errors leaving pads stuck highlighted.
- **Bottom bar** — shortcuts can no longer overflow off-screen or under the nav buttons; the row now scrolls.
- **Misc** — permission statuses refresh when returning from system settings; weather temperature now rounds instead of truncating; widget long-press menus open reliably in edit mode; removed vendor payload spam from logs.

---

## 📦 Release Files

| File | Description |
|------|-------------|
| `openlauncher-0.0.5.apk` | Standard production build — installs on all Android head units and tablets |
| `openlauncher-0.0.5-source.zip` | Full source code snapshot |

---

<div align="center">
  <i>Open source and built for the drive. Check the Issues tab to report bugs or submit feature requests.</i>
</div>
