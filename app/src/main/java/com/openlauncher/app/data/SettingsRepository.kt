package com.openlauncher.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_settings")

class SettingsRepository(private val context: Context) {

    private val gson = Gson()

    private object Keys {
        val VEHICLE_NAME       = stringPreferencesKey("vehicle_name")
        val ACCENT_COLOR       = intPreferencesKey("accent_color")
        val BG_COLOR           = intPreferencesKey("bg_color")
        val WALLPAPER_URI      = stringPreferencesKey("wallpaper_uri")
        val FONT_BOLD          = booleanPreferencesKey("font_bold")
        val TEXT_SCALE         = floatPreferencesKey("text_scale")
        val UI_SCALE           = floatPreferencesKey("ui_scale")
        val CLOCK_STYLE        = stringPreferencesKey("clock_style")
        val UNIT_SYSTEM        = stringPreferencesKey("unit_system")
        val APP_FONT           = stringPreferencesKey("app_font")
        val SHOW_WEATHER       = booleanPreferencesKey("show_weather")
        val SHOW_CLOCK         = booleanPreferencesKey("show_clock")
        val SHOW_TELEMETRY     = booleanPreferencesKey("show_telemetry")
        val SHOW_NOW_PLAYING   = booleanPreferencesKey("show_now_playing")
        val SHORTCUTS_JSON     = stringPreferencesKey("shortcuts_json")
        val WIDGET_LAYOUT_JSON = stringPreferencesKey("widget_layout_json")
        val CAR_PLAY_PACKAGE      = stringPreferencesKey("car_play_package")
        val ANDROID_AUTO_PACKAGE  = stringPreferencesKey("android_auto_package")
        val USE_GRADIENT          = booleanPreferencesKey("use_gradient")
        val GRADIENT_END_COLOR    = intPreferencesKey("gradient_end_color")
        val WALLPAPER_DIM         = floatPreferencesKey("wallpaper_dim")
        val RIGHT_HAND_DRIVE      = booleanPreferencesKey("right_hand_drive")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            val defaults = AppSettings()
            val shortcutsJson = prefs[Keys.SHORTCUTS_JSON]
            val shortcuts = if (shortcutsJson != null) {
                gson.fromJson<List<ShortcutConfig>>(
                    shortcutsJson,
                    object : TypeToken<List<ShortcutConfig>>() {}.type
                ) ?: defaults.shortcuts
            } else defaults.shortcuts

            val widgetJson = prefs[Keys.WIDGET_LAYOUT_JSON]
            val widgets = if (widgetJson != null) {
                val loaded = gson.fromJson<List<WidgetConfig>>(
                    widgetJson,
                    object : TypeToken<List<WidgetConfig>>() {}.type
                ) ?: defaults.widgetLayout
                // Migrate: old 2×2 layout has no widget with gridX≥2 — replace with new 3×2 default
                if (loaded.none { it.gridX >= 2 }) defaults.widgetLayout else loaded
            } else defaults.widgetLayout

            AppSettings(
                vehicleName    = prefs[Keys.VEHICLE_NAME]     ?: defaults.vehicleName,
                accentColor    = prefs[Keys.ACCENT_COLOR]     ?: defaults.accentColor,
                backgroundColor = prefs[Keys.BG_COLOR]        ?: defaults.backgroundColor,
                wallpaperUri   = prefs[Keys.WALLPAPER_URI]    ?: defaults.wallpaperUri,
                fontBold       = prefs[Keys.FONT_BOLD]        ?: defaults.fontBold,
                textScale      = prefs[Keys.TEXT_SCALE]       ?: defaults.textScale,
                uiScale        = prefs[Keys.UI_SCALE]         ?: defaults.uiScale,
                clockStyle     = prefs[Keys.CLOCK_STYLE]?.let { ClockStyle.valueOf(it) } ?: defaults.clockStyle,
                unitSystem     = prefs[Keys.UNIT_SYSTEM]?.let { UnitSystem.valueOf(it) } ?: defaults.unitSystem,
                appFont        = prefs[Keys.APP_FONT]?.let { runCatching { AppFont.valueOf(it) }.getOrNull() } ?: defaults.appFont,
                showWeather    = prefs[Keys.SHOW_WEATHER]     ?: defaults.showWeather,
                showClock      = prefs[Keys.SHOW_CLOCK]       ?: defaults.showClock,
                showTelemetry  = prefs[Keys.SHOW_TELEMETRY]   ?: defaults.showTelemetry,
                showNowPlaying = prefs[Keys.SHOW_NOW_PLAYING] ?: defaults.showNowPlaying,
                shortcuts      = shortcuts,
                widgetLayout   = widgets,
                carPlayPackage      = prefs[Keys.CAR_PLAY_PACKAGE]      ?: defaults.carPlayPackage,
                androidAutoPackage  = prefs[Keys.ANDROID_AUTO_PACKAGE]  ?: defaults.androidAutoPackage,
                useGradient      = prefs[Keys.USE_GRADIENT]        ?: defaults.useGradient,
                gradientEndColor = prefs[Keys.GRADIENT_END_COLOR]  ?: defaults.gradientEndColor,
                wallpaperDim     = prefs[Keys.WALLPAPER_DIM]       ?: defaults.wallpaperDim,
                rightHandDrive   = prefs[Keys.RIGHT_HAND_DRIVE]    ?: defaults.rightHandDrive
            )
        }

    suspend fun saveSettings(s: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.VEHICLE_NAME]       = s.vehicleName
            prefs[Keys.ACCENT_COLOR]       = s.accentColor
            prefs[Keys.BG_COLOR]           = s.backgroundColor
            prefs[Keys.WALLPAPER_URI]      = s.wallpaperUri
            prefs[Keys.FONT_BOLD]          = s.fontBold
            prefs[Keys.TEXT_SCALE]         = s.textScale
            prefs[Keys.UI_SCALE]           = s.uiScale
            prefs[Keys.CLOCK_STYLE]        = s.clockStyle.name
            prefs[Keys.UNIT_SYSTEM]        = s.unitSystem.name
            prefs[Keys.APP_FONT]           = s.appFont.name
            prefs[Keys.SHOW_WEATHER]       = s.showWeather
            prefs[Keys.SHOW_CLOCK]         = s.showClock
            prefs[Keys.SHOW_TELEMETRY]     = s.showTelemetry
            prefs[Keys.SHOW_NOW_PLAYING]   = s.showNowPlaying
            prefs[Keys.SHORTCUTS_JSON]     = gson.toJson(s.shortcuts)
            prefs[Keys.WIDGET_LAYOUT_JSON] = gson.toJson(s.widgetLayout)
            prefs[Keys.CAR_PLAY_PACKAGE]      = s.carPlayPackage
            prefs[Keys.ANDROID_AUTO_PACKAGE]  = s.androidAutoPackage
            prefs[Keys.USE_GRADIENT]       = s.useGradient
            prefs[Keys.GRADIENT_END_COLOR] = s.gradientEndColor
            prefs[Keys.WALLPAPER_DIM]      = s.wallpaperDim
            prefs[Keys.RIGHT_HAND_DRIVE]   = s.rightHandDrive
        }
    }

    suspend fun resetToDefaults() {
        context.dataStore.edit { it.clear() }
    }
}
