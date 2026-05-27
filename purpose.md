# Open Launcher — Purpose & Product Vision

## What it is
Open Launcher is a custom Android home screen replacement (launcher) built specifically for **aftermarket Android head units** installed in vehicles. It replaces the stock launcher that ships with cheap Android car stereos, providing a clean, high-contrast, driver-optimised interface.

## What it is NOT
- It does not connect to the vehicle's CAN-bus or OBD port
- It does not control any vehicle systems (engine, lights, HVAC, etc.)
- It is not a navigation or mapping app
- It is not a CarPlay or Android Auto replacement

It is purely an Android **home screen** — the thing that appears when you press "Home" — redesigned for automotive context.

## Target hardware
Aftermarket Android head units sold under brands like Joying, Eonon, Atoto, Pumpkin, etc. These run standard Android (typically Android 10–13) on MediaTek or Rockchip SoCs. They have:
- 7–12" capacitive touchscreens
- Landscape orientation, often 1280×480 or 1024×600 resolution
- No physical keyboard — all touch
- Standard Android GPS, WiFi, and Bluetooth hardware
- A SIM slot on some units (for cellular data / LTE navigation)

The app also runs on phones for development and testing (e.g. Redmi Note 15 Pro via sideload).

## Core problems solved

| Problem | Solution |
|---|---|
| Stock launchers are ugly, cluttered, hard to tap while driving | Clean minimal grid, large tap targets, high contrast |
| No at-a-glance driving info | Dashboard widgets: clock, weather, GPS coords, compass |
| Music apps have no quick controls on the home screen | Now Playing widget with album art + transport controls |
| Small, hard-to-tap app icons | Full-screen app library with large grid tiles |
| Hard to customise without developer knowledge | Settings pane: colours, fonts, widget toggles, wallpaper |

## Feature overview

### Persistent sidebar
Always-visible left rail (72dp) with:
- 4 customisable shortcut slots (long-press to reassign)
- App Library nav
- Settings nav
- Active state indication for current pane

### Home Dashboard
Modular 2×2 widget grid:
- **Clock** — digital or analog, live tick
- **Weather** — Open-Meteo API, metric or imperial
- **Telemetry / Compass** — live GPS coordinates + bearing from device sensors
- **Now Playing** — MediaSession-based, album art background, transport controls

Widgets can be rearranged (long-press to select, tap another to swap).

### App Library
Full app drawer showing every installed app with a launcher intent. Live search. Doubles as the shortcut picker when reassigning sidebar slots.

### Settings
- Set as default launcher (opens Android system settings)
- Vehicle name label
- Accent colour (colour wheel + presets) and background colour
- Custom wallpaper
- Font weight and global text scale
- Widget visibility toggles
- Clock style (analog / digital)
- Unit system (metric / imperial)
- Reset to defaults

## Technical approach
- **Single Activity** — all navigation is in-process via `AnimatedContent`, no fragment transactions
- **Jetpack Compose** — declarative UI, easy to theme dynamically
- **DataStore Preferences** — persists all settings instantly, survives process kill
- **No third-party design system** — Material3 themed to near-black with accent overrides
- **Open-Meteo** — free weather API, no key, no rate limits for reasonable usage
- **NotificationListenerService** — only way to get MediaSession data from arbitrary music apps without binding to each one individually

## Roadmap (not yet implemented)
- Speed display using GPS velocity (mph / kmh)
- Integration with Android's built-in day/night mode (auto dark/light theme switch at sunset/sunrise)
- Widget for fuel reminders / maintenance intervals (manual input)
- Gesture shortcuts (swipe patterns on home screen)
- Per-app volume shortcuts in sidebar
- OTA update mechanism for head units without Play Store
