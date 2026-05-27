<div align="center">
  <img width="256" height="256" alt="logoo" src="https://github.com/user-attachments/assets/4c5c4ddb-836d-4c59-8325-76b8c8d78bb3" />
  <h1>Open Launcher</h1>
  <p><strong>An open-source, offline-first Android launcher built specifically for aftermarket car head units.</strong></p>

  <!-- Optional Badges: Replace links with your actual repo data once live -->
  [![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
  [![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](http://makeapullrequest.com)
</div>

---

## 📖 Table of Contents
- [Why Build Another Car Launcher?](#-why-build-another-car-launcher)
- [The Philosophy](#-the-philosophy)
- [Current Features](#-current-features)
- [Roadmap & Future Plans](#-roadmap--future-plans)
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
<img width="2400" height="896" alt="Screenshot_20260527-025446" src="https://github.com/user-attachments/assets/b079208f-acac-4d3f-80c5-a7ea59f3e6bf" />
<img width="2400" height="896" alt="Screenshot_20260527-025451" src="https://github.com/user-attachments/assets/cc038c53-dcf5-4b4b-bd73-bfcd18bd82d2" />

* **Draggable, Customizable Shortcuts:** The left sidebar holds your main app shortcuts. You can easily remap them and drag them around to rearrange the order on the fly.
* **Deep System App Access:** Pulls *every* Android app on the device. Many head units hide their built-in CarPlay/Android Auto receiver apps down at the system level (which a lot of alternative launchers fail to find). This launcher pulls them directly so you can actually link to them.
* **Context-Aware Widgets:**
  * **Smart Music Player:** If you are playing music directly on the head unit (local files or native Spotify), it pulls track metadata and album art. If your CarPlay/AA app is open, the widget smartly recognizes that (if you've set your Carplay/AA app) and switches into a shortcut mode instead.
  * **Dynamic Weather:** Auto-detects your internet connection. If data/Wi-Fi is on, it pulls and displays the weather. If you are offline, it auto-hides so you aren't staring at a blank or broken widget.
* **Modular Home Grid:** Resize, rearrange, and toggle the core dashboard widgets (Clock, Telemetry, and GPS/Compass) to fit your layout preference.
* **Deep Personalization:** Change UI accent colors, swap background canvases, and tweak the aesthetic to match your car's specific dashboard lighting.

---

## 🗺️ Roadmap & Future Plans

The current priority is **stability and universal compatibility**—fixing bugs and ensuring the launcher scales perfectly across the wild west of different aftermarket head unit resolutions and hardware specs. 

Future targets include:

- [ ] **Advanced Color Engine:** More granular control over every hex code in the UI to perfectly match your car's dashboard lighting.
- [ ] **Picture-in-Picture (PiP):** Support for floating maps or media over the launcher.
- [ ] **Offline Weather via FM/RDS:** A highly experimental goal to pull local weather data directly from FM radio bands (RDS/TMC) utilizing the car's physical antenna—bypassing the need for Wi-Fi entirely.
- [ ] **Universal Theming Engine:** A standardized platform allowing the community to build, share, and install entirely new visual themes.

---

## 🤝 Contributing (Open Source First)

This project is open-source because it takes a community to build something that works across hundreds of different head unit models. Whether you are a developer, a designer, or just someone testing it in your car, your help is welcome!

### How you can help:
1. **Test on your hardware:** Install the APK on your specific head unit, break things, and submit Bug Reports in the [Issues tab](../../issues). 
2. **Feature Requests:** Have a cool idea? Open a discussion.
3. **Pull Requests:** See a bug you can fix or a feature you want to add? Fork the repo and submit a PR. *(Please check the issues tab first to see what is currently being worked on!)*

### Quick Start for Developers:
1. Clone the repository: (not yet)
