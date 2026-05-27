# Open Launcher — Design Reference

## Visual Language
High-contrast, flat, minimal. Designed for legibility at arm's length (driver seating position).

| Token | Default | "Sporty" preset |
|---|---|---|
| Background | `#000000` | `#000000` |
| Accent | `#FFFFFF` | `#2979FF` |
| Surface (cards) | `#111111` | `#111111` |
| Dim surface | `#0D0D0D` | `#0D0D0D` |
| Muted text | `#888888` | `#888888` |
| Dividers | `#2A2A2A` | `#2A2A2A` |

Accent colour is user-configurable via a colour picker in Settings → Appearance.

## Layout

### Global
```
┌─────────────────────────────────────────────────────────────┐
│  Sidebar (72dp)  │           Main content pane              │
│                  │  (AnimatedContent — 3 panes)             │
└─────────────────────────────────────────────────────────────┘
```
Always landscape orientation (`android:screenOrientation="landscape"`).

### Sidebar (72dp wide)
```
┌────────┐
│  [S1]  │  ← shortcut slot (scrollable region)
│  [S2]  │
│  [S3]  │
│  [S4]  │
├────────┤
│  Apps  │  ← pinned nav (always visible)
│  ⚙     │  ← pinned nav (always visible)
└────────┘
```
- Shortcut slots: 42×42dp rounded icon + 8px label. Scrollable vertically.
- Nav buttons: always fixed at bottom, pinned outside scroll area.
- Active nav state: accent-colour tint + accent background at 18% alpha.
- Long-press on shortcut → app picker mode (App Library pane).

### Home Dashboard
```
┌─────────────────────────────────────────────────────────┐
│  VEHICLE NAME                      [WiFi] [Data]  [⊞]  │
├─────────────────────────────────────────────────────────┤
│  ┌─────────────────┐   ┌─────────────────────────────┐  │
│  │   CLOCK         │   │   WEATHER                   │  │
│  │  (digital/      │   │  emoji + temp + condition   │  │
│  │   analog)       │   │                             │  │
│  └─────────────────┘   └─────────────────────────────┘  │
│  ┌─────────────────┐   ┌─────────────────────────────┐  │
│  │  TELEMETRY      │   │  NOW PLAYING                │  │
│  │  lat/lon/alt    │   │  (album art background)     │  │
│  │  compass rose   │   │  title · artist · controls  │  │
│  └─────────────────┘   └─────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```
- Widget grid: `BoxWithConstraints` + `absoluteOffset` 2-column × 2-row.
- Each widget: `16dp` rounded corners, `#111111` background.
- Rearrange mode: long-press a widget to select → tap another to swap.

### Widget Details

#### Clock
- **Digital**: `52sp` time, muted date below. Accent colour for digits.
- **Analog**: Canvas-drawn. Accent hour hand, white minute, red second. Tick marks at 12 positions (bold at 3/6/9/12).

#### Weather
- Source: Open-Meteo API (`api.open-meteo.com`) — no API key needed.
- Shows: WMO condition emoji, temperature (°C or °F), condition label, wind speed.
- Auto-fetched when GPS location becomes available.

#### Telemetry / Compass
- Left: GPS lat / lon / altitude in monospace-style small text.
- Right: Canvas compass rose. Arrow rotates with sensor bearing. North = accent colour, South = muted.

#### Now Playing
- Background: album art bitmap cropped full-widget + vertical dark gradient scrim.
- Controls: Prev · Play/Pause (accent circle) · Next.
- Data source: `NotificationListenerService` → `MediaSessionManager` → `MediaController`.
- Falls back to "No media playing" placeholder if no active session.

### App Library
- `LazyVerticalGrid` with `GridCells.Adaptive(96dp)`.
- Search field filters by app name in real-time.
- In picker mode (shortcut assignment): header changes, tapping selects and returns.

### Settings
- Scrollable `Column` on opaque black background.
- Grouped into sections: System, Vehicle, Appearance, Typography, Home Widgets, Units, Maintenance.
- Colour picker: HSV sliders + 5 preset swatches.
- Reset confirmation: `AlertDialog` before wiping DataStore.

## Typography
Scales dynamically via `textScale` setting (0.8× → 1.4×). Font weight toggle (Regular / Bold). Base sizes:

| Role | Size |
|---|---|
| Vehicle name header | 18sp |
| Digital clock | 52sp |
| Widget temperature | 28sp |
| Section labels | letterspaced uppercase |
| App tile label | 10sp |
| Sidebar labels | 8sp |

## Colour Presets
| Name | Accent |
|---|---|
| Default | `#FFFFFF` |
| Sporty | `#2979FF` |
| Racing | `#00E676` |
| Amber | `#FFAB00` |
| Red | `#FF1744` |
