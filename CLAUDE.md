# Open Launcher — Claude Code Context

## Project
Android car launcher app (`com.openlauncher.app`). Kotlin + Jetpack Compose. Single-activity architecture.

## Build
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$ANDROID_HOME/platform-tools:$JAVA_HOME/bin:$PATH"
cd /Users/davidlam/Developer/OpenLauncher
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

> Always rebuild and reinstall after any code change.

## Stack
| Layer | Tech |
|---|---|
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose + Material3 (BOM 2024.09.00) |
| State | DataStore Preferences + ViewModel + StateFlow |
| JSON | Gson (for complex DataStore types: widget layout, shortcuts) |
| Network | Retrofit2 + Gson → Open-Meteo weather API (no API key) |
| Images | Coil |
| Sensors | LocationManager (GPS) + SensorManager (accelerometer + magnetometer) |
| Media | NotificationListenerService → MediaSessionManager → MediaController |
| Build | AGP 9.1.1, Gradle 9.4.1, compileSdk/targetSdk 36 |

## Package structure
```
com.openlauncher.app/
├── MainActivity.kt          — single activity, root Compose tree
├── data/
│   ├── AppSettings.kt       — data models + defaults (ShortcutConfig, WidgetConfig, enums)
│   ├── SettingsRepository.kt— DataStore read/write, Gson serialization
│   └── WeatherApi.kt        — Retrofit service + Open-Meteo DTOs
├── model/                   — pure data classes (AppInfo, NowPlayingState, WeatherState, NavDestination)
├── service/
│   └── MediaListenerService.kt — NotificationListenerService; exposes SharedFlow<NowPlayingState?>
├── util/
│   └── LocationCompassManager.kt — GPS + compass sensor wrapper, exposes StateFlows
├── viewmodel/
│   └── LauncherViewModel.kt — central state hub, all side effects
└── ui/
    ├── theme/               — Color, Theme, Type (dynamic Material3 dark theme)
    ├── components/          — Sidebar, ColorPickerDialog, ConfirmDialog
    ├── screen/              — HomeScreen, AppLibraryScreen, SettingsScreen
    └── widget/              — ClockWidget, WeatherWidget, TelemetryWidget, NowPlayingWidget
```

## Key design decisions
- **No navigation library** — `NavDestination` enum + `AnimatedContent` is sufficient for 3 panes
- **DataStore over Room** — settings are flat key/value; complex types (lists) serialized to JSON strings via Gson
- **MediaSession via NotificationListenerService** — requires user to grant Notification Access in Android settings; `MediaListenerService` companion object exposes a `StateFlow` so the ViewModel can collect without binding
- **Open-Meteo** — free weather API, no key required; location comes from GPS StateFlow
- **Sidebar nav buttons are pinned at bottom** — top section is scrollable to handle small phone screens; nav buttons use fixed layout so they are always reachable
- **Widget grid uses `BoxWithConstraints` + `absoluteOffset`** — avoids LazyGrid complexity, supports arbitrary spanX/spanY
- **`<queries>` manifest element** — required on API 30+ for `queryIntentActivities` to return all launcher apps

## Permissions required at runtime
| Permission | Purpose |
|---|---|
| `ACCESS_FINE_LOCATION` | GPS telemetry widget + weather location |
| Notification Access (Settings) | Now Playing / MediaSession |

## Known issues / TODO
- Analog clock `Condition is always true` warning at HomeScreen.kt:280 — benign, selectedId null-check in rearrange mode
- `Icons.Default.Radio` from material-icons-extended — verify icon name if build fails
- Weather widget requires location permission to be granted first
