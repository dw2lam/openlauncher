# Release v0.0.3: The Instrument Panel Update

This release transforms OpenLauncher from a capable launcher into a proper in-car instrument panel. Everything that was rough around the edges has been refined, and a batch of new purpose-built widgets make this feel like software designed for the car — not adapted for it.

---

## ✨ What's New

### 🔊 Soundboard Widget
A new standalone widget with 6 fully assignable pads. Each pad can be set to a built-in synth type (HORN, BEEP, ALERT, KICK, SNARE, BASS, or FART) or loaded with any custom audio file from your device storage. Long-press any pad in edit mode to reassign it. Audio file access persists across restarts.

### 🚕 Taxi-Style Trip Meter
The trip tracker has been redesigned with a proper taxi/fare meter aesthetic. The odometer-style rolling display shows trip distance and elapsed time in a format that feels native to vehicle dashboards.

### ⚡ 0–100 Speed Tester (Hidden)
Inside the trip meter widget, there's a hidden 0–100 km/h timer. Tap the trip meter label area to reveal it. It auto-starts when motion is detected from standstill and locks in your time when you hit 100. For track day use — don't be an idiot on public roads.

### 🎙️ Redesigned Radio Widget
The AM/FM radio widget has been completely rebuilt to match the instrument panel design language: monospace font, minimal hairline borders, uppercase labels, preset slots with frequency memory. Gone is the skeuomorphic Pioneer deck — this fits the rest of the UI.

### 📡 New Onboarding Flow
First-launch onboarding has been overhauled. The flow is cleaner, faster, and actually explains the key permissions required (location, notification listener) before asking for them.

### 🏎️ GPS Math Overhaul + Calibration
The GPS speed and distance calculations have been rewritten with better filtering logic. There's now an offline calibration option — useful for devices whose GPS chips report inaccurate baselines. Accessible from the trip meter settings.

### 🖥️ Car Head Unit Stats
A new system stats widget shows CPU load, memory pressure, and temperature readouts in the instrument-panel style. Useful for monitoring head unit thermals during long drives.

### 💨 New Speedometer Widget
Standalone speedometer widget with a large, clean digital readout. Independent from the trip tracker so you can place it anywhere on the grid.

---

## 🛠️ Fixes & Tweaks

- **Day mode bug fixed** — theme was not correctly applying in certain widget states; all widgets now properly respond to day/night transitions
- **Widget library layout fixed** — now 4 equal square columns instead of the previous cramped 3-column list; cards are square and uniformly sized
- **Onboarding flash fixed** — launcher no longer briefly shows the onboarding screen on every boot after setup is complete
- **Faster launch on head unit** — app is now marked persistent to stay resident in memory; noticeable improvement on cold start
- **Sidebar tweaks** — spacing and interaction areas adjusted for glove-friendly tapping
- **Settings menu reorganized** — grouped into logical sections (Appearance, Layout, Widgets, System) instead of one long scroll
- **Settings additions** — new entries for GPS calibration offset, soundboard toggle, and widget-specific display options
- **GitHub update link** — a "Check for Updates" row at the bottom of Settings links directly to the GitHub releases page

---

## 📦 Release Files

| File | Description |
|------|-------------|
| `openlauncher-0.0.3.apk` | Standard build — installs on any Android head unit, no special signing needed |
| `openlauncher-0.0.3-test-pip.apk` | AOSP platform-signed build — for testing PiP app embedding on AOSP-based devices only. **Will not install on Xiaomi/MIUI devices.** |
| `openlauncher-0.0.3-source.zip` | Full source code snapshot |

---

## ⚠️ Notes

The `test-pip` build is signed with the AOSP test platform key (`android.uid.system` shared UID). It requires an AOSP-derived ROM where the platform key matches. On Xiaomi/MIUI and most OEM devices this build **will fail to install** — use the standard APK instead.

---

<div align="center">
  <i>Open source and built for the drive. Check the Issues tab to report bugs or submit feature requests.</i>
</div>
