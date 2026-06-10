<div align="center">
  <img width="256" height="256" alt="logoo" src="https://github.com/user-attachments/assets/4c5c4ddb-836d-4c59-8325-76b8c8d78bb3" />
  <h1>Open Launcher</h1>
  <p><strong>An open-source, offline-first Android launcher built specifically for aftermarket car head units.</strong></p>

  [![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
  [![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](http://makeapullrequest.com)
</div>

---

## 📖 Table of Contents
- [Why Build Another Car Launcher?](#-why-build-another-car-launcher)
- [The Philosophy](#-the-philosophy-offline-first--oem-aesthetics)
- [Current Features](#-current-features)
- [Roadmap & Future Plans](#️-roadmap--future-plans)
- [Contributing](#-contributing-open-source-first)
- [License](#-license)

---

## 🛑 Why build another car launcher?

I built this project for three simple reasons:

1. **No Premium Paywalls:** I didn't want to pay someone for basic dashboard functionality.
2. **Community-Driven:** I wanted it to be open-source. The car modding community is incredible, and I wanted to create a foundation that others could actually build upon, fork, and improve.
3. **Clean Aesthetics:** Let's be honest—most Android launchers look like cheap video games, have zero functionality, or half the features are broken. This is built to look clean, professional, and integrated.

## 🧠 The Philosophy: Offline-First & OEM+ Aesthetics

Most modern head unit setups rely on wireless CarPlay or Android Auto for navigation and media. That means the head unit itself is offline 90% of the time.

I designed this launcher around that reality. It is built to be functional without a Wi-Fi connection while remaining highly customizable. Whether you are installing this in a 2020 Corolla Hybrid or a custom project car, the goal is for the UI to look like it actually belongs in your car's interior—a true OEM+ aesthetic.

---

## ✨ Current Features

This project is currently in active development, but the core foundation is highly customizable:

<img width="2400" height="896" alt="Screenshot_20260527-025436" src="https://github.com/user-attachments/assets/a1bc63f3-2d4e-4ac0-bd56-b5d181681658" />
<img width="2400" height="896" alt="Screenshot_20260527-212602" src="https://github.com/user-attachments/assets/cf319144-0a06-4bc6-ab83-855ef8514a9c" />
<img width="2400" height="896" alt="Screenshot_20260527-025446" src="https://github.com/user-attachments/assets/cc038c53-dcf5-4b4b-bd73-bfcd18bd82d2" />
<img width="2400" height="896" alt="Screenshot_20260527-025451" src="https://github.com/user-attachments/assets/cc038c53-dcf5-4b4b-bd73-bfcd18bd82d2" />

### 🧩 Modular Widget Grid
The home screen is a fully drag-and-drop, resize-capable grid. Every widget pane can be moved, scaled, and stacked however you want. Nothing is locked to a fixed position. Add and remove widgets from the built-in library at any time.

### 🎛️ Instrument Panel Widgets
A growing library of purpose-built car widgets designed to look like they belong on a dash — not a phone:

* **Smart Music Player** — pulls track metadata, album art, and playback controls from any local or streaming source. Detects when CarPlay or Android Auto is in use and switches into shortcut mode automatically.
* **AM/FM Radio** — instrument panel-style digital display that mirrors your head unit's **real tuner**. On szchoiceway-based units it talks to the MCU directly (seek, FM1/FM2/FM3/AM band switching, direct-tune frequency presets with memory). On every other unit it mirrors and controls your vendor radio app through its media session — assign the app once from the widget and get live frequency/station readout plus seek controls. No simulated stations or fake static.
* **Speedometer** — standalone GPS-based digital speed readout. Independent from the trip tracker so it can live anywhere on the grid.
* **Altimeter** — live elevation tracking pulled from the device GPS.
* **Trip Meter** — taxi-style rolling odometer display with trip distance and elapsed time. Includes a hidden **0–100 km/h timer** (tap the meter label to reveal it) that auto-starts from standstill and locks in your time at 100.
* **Head Unit Vitals** — real-time CPU load, memory pressure, and temperature readouts for monitoring your head unit's thermals on long drives.
* **Soundboard** — 6 fully assignable sound pads. Each pad can be set to a built-in synth type (HORN, BEEP, ALERT, KICK, SNARE, BASS, FART) or loaded with any custom audio file from device storage. Assignments persist across restarts.
* **Dynamic Weather** — auto-detects connectivity. Shows live weather when online, hides cleanly when offline. No broken blank widgets.
* **GPS Compass** — live bearing with calibrated heading display.

### 🗂️ App Library
Pulls every installed app, including system-level apps that most launchers miss — such as buried CarPlay and Android Auto receiver apps on head units that don't surface them normally.

### 📌 Sidebar Shortcuts
The sidebar holds your most-used app shortcuts. Drag to reorder, long-press to remap, and position the entire bar on the Left, Right, or Bottom of the screen to suit your driving hand or interior layout.

### 🌗 Smart Day/Night Theme Engine
Four distinct modes for head units that can't always pass the car's headlight signal to Android:
* **Forced Dark / Forced Light** — static overrides.
* **System Sync** — follows the head unit's native light/dark setting.
* **Sunset Mode** — automatically switches at local sunrise/sunset using offline location calculations, no internet required.

### 🛰️ GPS with Offline Calibration
Speed and distance calculations use a rewritten GPS math layer with improved filtering. A calibration offset option is available for devices whose GPS chips report inaccurate baselines — accessible from the trip meter settings for reliable offline use.

### 🎨 Deep Personalization
Accent color, background color, gradient, wallpaper with adjustable dim, font weight, text scale, UI scale, and app font — all tunable from the settings menu, which is organized into logical sections (Appearance, Layout, Widgets, System).

### 📱 Picture-in-Picture (PiP) Overlay ⚠️ *Beta*
Launch any app as a floating freeform window layered over the launcher.

> **This feature is currently in beta and requires the special `openlauncher-test-pip` build.** It relies on AOSP platform-level signing to access the window embedding APIs. The standard APK does not include PiP — use the `test-pip` release asset if you want to try it. Expect rough edges: not all apps behave correctly in a freeform window, and compatibility varies heavily by head unit ROM. This is a work in progress.

### 🔔 First-Run Onboarding
A clean onboarding flow on first launch explains key permissions (location, notification listener, draw-over-apps) before requesting them, with direct links to the relevant system settings screens.

---

## 🗺️ Roadmap & Future Plans

The current priority is **stability and universal compatibility** — ensuring the launcher scales correctly across the wide range of aftermarket head unit resolutions and hardware specs.

Remaining targets:

- [ ] **Advanced Color Engine:** Per-element hex control for every surface in the UI — accent, text, borders, backgrounds — to precisely match a car's specific dashboard ambient lighting.
- [ ] **Offline Weather via FM/RDS:** A highly experimental goal to pull local weather data directly from FM radio bands (RDS/TMC) using the car's physical antenna — bypassing Wi-Fi entirely.
- [ ] **Universal Theming Engine:** A standardized platform for the community to build, share, and install full visual themes.

---

## 🤝 Contributing (Open Source First)

This project is open-source because it takes a community to build something that works across hundreds of different head unit models. Whether you are a developer, a designer, or just someone testing it in your car, your help is welcome!

### How you can help:
1. **Test on your hardware:** Install the APK on your specific head unit, break things, and submit Bug Reports in the [Issues tab](../../issues).
2. **Feature Requests:** Have a cool idea? Open a discussion.
3. **Pull Requests:** See a bug you can fix or a feature you want to add? Fork the repo and submit a PR. *(Please check the issues tab first to see what is currently being worked on!)*

---

## Donate

This app is fully free and open-source, a donation isn't required but would be greatly appreciated to help support the constant updates and fixes planned based on your suggestions!

[![Donate with PayPal](https://raw.githubusercontent.com/stefan-niedermann/paypal-donate-button/master/paypal-donate-button.png)](https://paypal.me/dw2lam)
