package com.openlauncher.app.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

enum class ClockStyle { DIGITAL, ANALOG }
enum class UnitSystem { METRIC, IMPERIAL }
enum class DayNightMode { DARK, LIGHT, AUTO, SYSTEM }
enum class SidebarPosition { LEFT, RIGHT, BOTTOM }
enum class GradientDirection { TOP_TO_BOTTOM, LEFT_TO_RIGHT, DIAGONAL, RADIAL }
enum class MapProvider { OSM, GOOGLE }
enum class MapType { ROADMAP, SATELLITE, HYBRID, TERRAIN }

enum class DefaultShortcutIcon {
    NONE,
    // Navigation & vehicle
    RADIO, CAMERA, PHONE, MAP, NAVIGATION, CAR, GAS_STATION, DASHBOARD,
    // Audio & media
    MUSIC, SPEAKER, HEADSET, EQUALIZER, VOLUME_UP,
    // Connectivity
    BLUETOOTH, WIFI,
    // Lighting & climate
    LIGHTBULB, BRIGHTNESS, AC, THERMOSTAT,
    // General utility
    TV, VIDEOCAM, STAR, MESSAGE, TIMER, LOCK, SETTINGS, FAVORITE,
    // Web / location
    GLOBE,
    // Nuevos
    CUSTOM_MUSIC, CUSTOM_RADIO, CUSTOM_AAUTO, CUSTOM_PHONE, CUSTOM_VIDEO, CUSTOM_MAPS,
    // Nuevos originales
    CUSTOM_MUSIC_ORI, CUSTOM_RADIO_ORI, CUSTOM_AAUTO_ORI, CUSTOM_PHONE_ORI, CUSTOM_VIDEO_ORI, CUSTOM_MAPS_ORI
}

data class SoundPadConfig(
    val label: String,
    val audioUri: String = "",
    val synthType: String = "BEEP"
)

fun defaultSoundboardPads() = listOf(
    SoundPadConfig("mario_jump",   synthType = "mario_jump"),
    SoundPadConfig("mario_coin",   synthType = "mario_coin"),
    SoundPadConfig("boom",         synthType = "boom"),
    SoundPadConfig("loud_fart",    synthType = "loud_fart"),
    SoundPadConfig("+",            synthType = ""),
    SoundPadConfig("+",            synthType = "")
)

data class ShortcutConfig(
    val packageName: String = "",
    val label: String = "",
    val isDefault: Boolean = false,
    val defaultIcon: DefaultShortcutIcon = DefaultShortcutIcon.NONE,
    // null = native app icon; non-null = override with this vector icon
    val customIconOverride: DefaultShortcutIcon? = null
)

const val GRID_COLS = 6
const val GRID_ROWS = 4

data class WidgetConfig(
    val id: String,          // "CLOCK" | "WEATHER" | "TELEMETRY" | "NOW_PLAYING"
    val gridX: Int,          // column 0..(GRID_COLS-1)
    val gridY: Int,          // row    0..(GRID_ROWS-1)
    val spanX: Int = 1,
    val spanY: Int = 1,
    val enabled: Boolean = true
)

data class AppSettings(
    val vehicleName: String = "MY CAR",
    val accentColor: Int = Color(0xFFFF7600).toArgb(),
    val backgroundColor: Int = Color.Black.toArgb(),
    val fontColor: Int = Color.White.toArgb(),
    val wallpaperUri: String = "",
    val fontBold: Boolean = false,
    val textScale: Float = 1.2f,
    val uiScale: Float = 1.0f,
    val clockStyle: ClockStyle = ClockStyle.DIGITAL,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val showWeather: Boolean = false,
    val showClock: Boolean = false,
    val showTelemetry: Boolean = true,
    val showNowPlaying: Boolean = true,
    val shortcuts: List<ShortcutConfig> = defaultShortcuts(),
    val widgetLayout: List<WidgetConfig> = defaultWidgetLayout(),
    val carPlayPackage: String = "",
    val androidAutoPackage: String = "",
    val useGradient: Boolean = false,
    val gradientEndColor: Int = Color.Black.toArgb(),
    val wallpaperDim: Float = 0.55f,
    val sidebarPosition: SidebarPosition = SidebarPosition.LEFT,
    val bottomBarShortcutsRight: Boolean = false,
    val showAltimeter: Boolean = false,
    val showSpeedometer: Boolean = false,
    val dayNightMode: DayNightMode = DayNightMode.DARK,
    val showPip: Boolean = false,
    val pipAppPackage: String = "",
    // Head unit's radio app — mirrored & controlled via its MediaSession
    val radioPackage: String = "",
    val onboardingCompleted: Boolean = false,
    val showVitals: Boolean = false,
    val showTripTracker: Boolean = false,
    val compassOffset: Float = 0f,
    val showSoundboard: Boolean = false,
    val soundboardPads: List<SoundPadConfig> = defaultSoundboardPads(),
    val vitalsAsBars: Boolean = false,
    val speedometerDigitalOnly: Boolean = false,
    val gradientDirection: GradientDirection = GradientDirection.DIAGONAL,
    val useCustomBackgroundColor: Boolean = false,
    val showMap: Boolean = true,
    val mapProvider: MapProvider = MapProvider.OSM,
    val mapType: MapType = MapType.ROADMAP,
    val showTraffic: Boolean = false,
    val autostartPackages: List<String> = emptyList(),
    val autostartDelay: Int = 2
)

fun defaultShortcuts() = listOf(
    ShortcutConfig(label = "Radio", isDefault = true, defaultIcon = DefaultShortcutIcon.RADIO),
    ShortcutConfig(label = "Camera", isDefault = true, defaultIcon = DefaultShortcutIcon.CAMERA),
    ShortcutConfig(label = "Music", isDefault = true, defaultIcon = DefaultShortcutIcon.MUSIC),
    ShortcutConfig(label = "Phone", isDefault = true, defaultIcon = DefaultShortcutIcon.PHONE)
)

fun defaultWidgetLayout() = listOf(
    WidgetConfig("MAP",       gridX = 0, gridY = 0, spanX = 4, spanY = 4),
    WidgetConfig("NOW_PLAYING",     gridX = 4, gridY = 0, spanX = 2, spanY = 3),
    WidgetConfig("TELEMETRY", gridX = 4, gridY = 3, spanX = 2, spanY = 1)
)

fun AppSettings.activeWidgetIds(): Set<String> = buildSet {
    if (showClock) add("CLOCK")
    if (showWeather) add("WEATHER")
    if (showNowPlaying) add("NOW_PLAYING")
    if (showTelemetry) add("TELEMETRY")
    if (showAltimeter) add("ALTIMETER")
    if (showSpeedometer) add("SPEEDOMETER")
    if (showVitals) add("VITALS")
    if (showTripTracker) add("TRIP_TRACKER")
    if (showSoundboard) add("SOUNDBOARD")
    if (showMap) add("MAP")
}

/**
 * Moves [movingId] to ([targetX], [targetY]) and pushes any displaced widgets to the
 * first available free cell, cascading until all conflicts are resolved.
 * Operates only on the supplied [layout] list — callers should pass only active/enabled widgets.
 */
fun computeWidgetMove(
    layout: List<WidgetConfig>,
    movingId: String,
    targetX: Int,
    targetY: Int
): List<WidgetConfig> {
    val moving = layout.find { it.id == movingId } ?: return layout
    val placed = moving.copy(
        gridX = targetX.coerceIn(0, GRID_COLS - moving.spanX),
        gridY = targetY.coerceIn(0, GRID_ROWS - moving.spanY)
    )

    val others  = layout.filter { it.id != movingId }
    val result  = mutableListOf(placed)
    val occupied = buildOccupied(result).toMutableSet()

    // Stable widgets that don't conflict go first; displaced ones are pushed afterwards
    val (stable, displaced) = others.partition { w -> result.none { widgetsOverlap(it, w) } }

    for (w in stable) {
        result.add(w)
        for (dx in 0 until w.spanX) for (dy in 0 until w.spanY) occupied.add(w.gridX + dx to w.gridY + dy)
    }

    for (w in displaced) {
        val pos = firstFreeGridPos(w.spanX, w.spanY, occupied)
        val resolved = if (pos != null) w.copy(gridX = pos.first, gridY = pos.second) else w
        result.add(resolved)
        for (dx in 0 until resolved.spanX) for (dy in 0 until resolved.spanY) occupied.add(resolved.gridX + dx to resolved.gridY + dy)
    }

    return result
}

private fun buildOccupied(widgets: List<WidgetConfig>) = buildSet<Pair<Int, Int>> {
    widgets.forEach { w -> for (dx in 0 until w.spanX) for (dy in 0 until w.spanY) add(w.gridX + dx to w.gridY + dy) }
}

private fun widgetsOverlap(a: WidgetConfig, b: WidgetConfig): Boolean =
    a.gridX < b.gridX + b.spanX && a.gridX + a.spanX > b.gridX &&
    a.gridY < b.gridY + b.spanY && a.gridY + a.spanY > b.gridY

private fun firstFreeGridPos(spanX: Int, spanY: Int, occupied: Set<Pair<Int, Int>>): Pair<Int, Int>? {
    for (row in 0 until GRID_ROWS) for (col in 0 until GRID_COLS) {
        if (col + spanX > GRID_COLS || row + spanY > GRID_ROWS) continue
        if ((0 until spanX).all { dx -> (0 until spanY).all { dy -> (col + dx to row + dy) !in occupied } })
            return col to row
    }
    return null
}
