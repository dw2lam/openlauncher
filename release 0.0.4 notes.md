# Release v0.0.4: The Customizer & Vitals Update

This release brings ultimate personalization and robust diagnostic telemetry to your dashboard. Version 0.0.4 resolves critical styling and day/night mode conflicts, overhauls the soundboard with intuitive management options, adds new visualization views for your device vitals, introduces a minimalist digital-only speedometer, and stabilizes the launcher across a wider array of OEM head units.

---

## ✨ What's New

### 🎨 Fixed & Enhanced Custom Appearance System
- **Custom Background & Font Colors**: Fully resolved issues with custom colors not applying or resetting. Font colors and background colors are now correctly persisted.
- **Gradient Background Directions**: Added support for gradient backgrounds, allowing you to select and rotate the direction of color gradients on your home screen.
- **Day/Night Theme Persistence**: Handled color overrides so that your chosen custom background colors persist in light (day) mode. Added a default "clear" option to instantly restore standard high-contrast day/night system defaults.

### 🏎️ Standalone Speedometer Digital-Only View
- Added a new toggle to display the speedometer as **fully digital**. Toggling this mode hides the tachometer ring entirely, leaving a large, clean, high-contrast digital number and unit for a ultra-minimalist, distraction-free drive.

### 🖥️ Upgraded Device Vitals Widget
- **Multiple Telemetry Views**: The diagnostics widget (monitoring CPU, RAM, and temperature) can now be toggled to render either as circular **radial dials** (`DialGauge`) or as horizontal **progress bars** (`BarGauge`).
- **High-Contrast Day Mode Outlines**: Integrated crisp, flat vector outlines (`0.16f` and `0.22f` opacities) around the tracks and active gauge arcs in Day Mode to guarantee excellent visibility and skeleton definition under direct sunlight or bright custom backdrops.

### 🔊 Upgraded Soundboard Widget
- **Intuitive Sound Management**: Cleared pads now render as a simple `+` button indicating you can add sound effects.
- **Clear Sound Action**: Added a contextual menu option to clear custom audio from pads.
- **Dynamic Picker Triggering**: Tapping an empty `+` pad now automatically triggers the media picker flow to load audio files instantly.

### 📻 Radio Widget (WIP)
- Further developmental updates to the built-in AM/FM radio screen preset management, memory slots, and frequency dials to pave the way for full headunit hardware tuner integrations.

### 🚜 Speed-Based Trip Tracking
- Fully resolved odometer calculations inside the **Trip Tracker** widget, ensuring accurate distance and fare calculations strictly correlated to vehicle GPS speed.

### 📡 Updated Onboarding Flow
- Updated the wizard steps for version 0.0.4, removing references to the unstable experimental Picture-in-Picture window and highlighting customizable gradients, colors, custom fonts, soundboard, and shortcut widgets.

---

## 🛠️ Stability & Bug Fixes

- **OEM Head Unit Crash Fixes** — Resolved application lifecycle exceptions and startup crashes on specific landscape car head units.
- **Removed Unstable PiP Features** — Removed experimental Picture-in-Picture (PiP) overlays to ensure premium system stability while the background overlay engine is redesigned.
- **Accent Color Compatibility** — Addressed styling bugs where accent colors did not synchronize correctly across sub-widgets on theme changes.

---

## 📦 Release Files

| File | Description |
|------|-------------|
| `openlauncher-0.0.4.apk` | Standard production build — installs on all Android head units and tablets |
| `openlauncher-0.0.4-source.zip` | Full source code snapshot |

---

<div align="center">
  <i>Open source and built for the drive. Check the Issues tab to report bugs or submit feature requests.</i>
</div>
