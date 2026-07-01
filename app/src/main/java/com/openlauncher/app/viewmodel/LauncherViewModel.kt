package com.openlauncher.app.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings as AndroidSettings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.openlauncher.app.data.AppSettings
import com.openlauncher.app.data.DayNightMode
import com.openlauncher.app.data.DefaultShortcutIcon
import com.openlauncher.app.data.GRID_COLS
import com.openlauncher.app.data.GRID_ROWS
import com.openlauncher.app.data.SettingsRepository
import com.openlauncher.app.data.ShortcutConfig
import com.openlauncher.app.data.SoundPadConfig
import com.openlauncher.app.data.WeatherApi
import com.openlauncher.app.data.activeWidgetIds
import com.openlauncher.app.data.computeWidgetMove
import com.openlauncher.app.data.defaultShortcuts
import com.openlauncher.app.util.SunriseSunset
import com.openlauncher.app.model.AppInfo
import com.openlauncher.app.model.NavDestination
import com.openlauncher.app.model.NowPlayingState
import com.openlauncher.app.model.WeatherState
import com.openlauncher.app.service.MediaListenerService
import com.openlauncher.app.util.LocationCompassManager
import com.openlauncher.app.util.LocationData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.openlauncher.app.data.MapProvider
import com.openlauncher.app.data.DailyData
import com.google.gson.Gson
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import org.osmdroid.util.GeoPoint
import com.openlauncher.app.data.ValhallaApi
import com.openlauncher.app.data.ValhallaRequest
import com.openlauncher.app.data.ValhallaLocation
import com.openlauncher.app.data.ValhallaManeuver
import com.openlauncher.app.data.ValhallaSummary
import com.openlauncher.app.data.ValhallaPolylineDecoder
import com.openlauncher.app.data.RouteTracker

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val locationMgr  = LocationCompassManager(application)

    // ── Settings ──────────────────────────────────────────────────────────────
    private val _settingsLoaded = MutableStateFlow(false)
    val settingsLoaded: StateFlow<Boolean> = _settingsLoaded

    val settings: StateFlow<AppSettings> = settingsRepo.settingsFlow
        .onEach { _settingsLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun updateSettings(block: AppSettings.() -> AppSettings) {
        viewModelScope.launch { settingsRepo.updateSettings { it.block() } }
    }

    fun resetSettings() {
        viewModelScope.launch { settingsRepo.resetToDefaults() }
    }

    // ── Valhalla Navigation State ──────────────────────────────────────────────
    private val _activeRoutePoints = MutableStateFlow<List<GeoPoint>?>(null)
    val activeRoutePoints: StateFlow<List<GeoPoint>?> = _activeRoutePoints

    private val _routeManeuvers = MutableStateFlow<List<ValhallaManeuver>?>(null)
    val routeManeuvers: StateFlow<List<ValhallaManeuver>?> = _routeManeuvers

    private val _routeSummary = MutableStateFlow<ValhallaSummary?>(null)
    val routeSummary: StateFlow<ValhallaSummary?> = _routeSummary

    private val _isNavigating = MutableStateFlow(false)
    val isNavigating: StateFlow<Boolean> = _isNavigating

    private val _destinationName = MutableStateFlow<String?>(null)
    val destinationName: StateFlow<String?> = _destinationName

    private val _currentManeuver = MutableStateFlow<ValhallaManeuver?>(null)
    val currentManeuver: StateFlow<ValhallaManeuver?> = _currentManeuver

    private val _distanceToManeuver = MutableStateFlow<Double?>(null)
    val distanceToManeuver: StateFlow<Double?> = _distanceToManeuver

    private val _routeBearing = MutableStateFlow<Float?>(null)
    val routeBearing: StateFlow<Float?> = _routeBearing

    private var destinationCoords: Pair<Double, Double>? = null

    // ── Navigation ────────────────────────────────────────────────────────────
    private val _nav = MutableStateFlow(NavDestination.HOME)
    val nav: StateFlow<NavDestination> = _nav

    fun navigate(dest: NavDestination) { _nav.value = dest }

    // ── Shortcut picker ───────────────────────────────────────────────────────
    private val _shortcutPickerSlot = MutableStateFlow<Int?>(null)
    val shortcutPickerSlot: StateFlow<Int?> = _shortcutPickerSlot

    fun startShortcutPicker(slot: Int) {
        _shortcutPickerSlot.value = slot
        _nav.value = NavDestination.APP_LIBRARY
    }

    fun assignShortcut(slot: Int, app: AppInfo) {
        updateSettings {
            copy(shortcuts = shortcuts.toMutableList().also { list ->
                while (list.size <= slot) list.add(ShortcutConfig())
                list[slot] = ShortcutConfig(packageName = app.packageName, label = app.appName)
            })
        }
        _shortcutPickerSlot.value = null
        _nav.value = NavDestination.HOME
    }

    fun removeShortcut(slot: Int) {
        updateSettings {
            copy(shortcuts = shortcuts.toMutableList().also { list ->
                if (slot in list.indices) {
                    list[slot] = defaultShortcuts().getOrNull(slot) ?: ShortcutConfig()
                }
            })
        }
    }

    fun reorderShortcut(from: Int, to: Int) {
        updateSettings {
            copy(shortcuts = shortcuts.toMutableList().also { list ->
                val item = list.removeAt(from)
                list.add(to.coerceIn(0, list.size), item)
            })
        }
    }

    fun setShortcutIcon(slot: Int, icon: DefaultShortcutIcon?) {
        updateSettings {
            copy(shortcuts = shortcuts.toMutableList().also { list ->
                if (slot in list.indices) list[slot] = list[slot].copy(customIconOverride = icon)
            })
        }
    }

    fun cancelShortcutPicker() {
        _shortcutPickerSlot.value = null
    }

    // ── CarPlay / Android Auto / Autostart picker ─────────────────────────────
    enum class AppPickerTarget { CARPLAY, ANDROID_AUTO, PIP, RADIO, AUTOSTART_1, AUTOSTART_2, AUTOSTART_3, AUTOSTART_4 }

    private val _appPickerTarget = MutableStateFlow<AppPickerTarget?>(null)
    val carPlayPickerActive: StateFlow<Boolean> = MutableStateFlow(false) // kept for compat
    val appPickerTarget: StateFlow<AppPickerTarget?> = _appPickerTarget

    fun startCarPlayPicker() {
        _appPickerTarget.value = AppPickerTarget.CARPLAY
        _nav.value = NavDestination.APP_LIBRARY
    }

    fun startAndroidAutoPicker() {
        _appPickerTarget.value = AppPickerTarget.ANDROID_AUTO
        _nav.value = NavDestination.APP_LIBRARY
    }

    fun startPipPicker() {
        _appPickerTarget.value = AppPickerTarget.PIP
        _nav.value = NavDestination.APP_LIBRARY
    }

    fun startRadioPicker() {
        _appPickerTarget.value = AppPickerTarget.RADIO
        _nav.value = NavDestination.APP_LIBRARY
    }

    fun startAutostartPicker(slot: Int) {
        _appPickerTarget.value = when (slot) {
            0 -> AppPickerTarget.AUTOSTART_1
            1 -> AppPickerTarget.AUTOSTART_2
            2 -> AppPickerTarget.AUTOSTART_3
            3 -> AppPickerTarget.AUTOSTART_4
            else -> AppPickerTarget.AUTOSTART_1
        }
        _nav.value = NavDestination.APP_LIBRARY
    }

    private fun assignAutostart(index: Int, packageName: String) {
        updateSettings {
            val list = autostartPackages.toMutableList()
            while (list.size <= index) list.add("")
            list[index] = packageName
            copy(autostartPackages = list)
        }
    }

    fun assignPickerApp(app: AppInfo) {
        when (_appPickerTarget.value) {
            AppPickerTarget.CARPLAY      -> updateSettings { copy(carPlayPackage = app.packageName) }
            AppPickerTarget.ANDROID_AUTO -> updateSettings { copy(androidAutoPackage = app.packageName) }
            AppPickerTarget.PIP          -> updateSettings { copy(pipAppPackage = app.packageName) }
            AppPickerTarget.RADIO        -> updateSettings { copy(radioPackage = app.packageName) }
            AppPickerTarget.AUTOSTART_1  -> assignAutostart(0, app.packageName)
            AppPickerTarget.AUTOSTART_2  -> assignAutostart(1, app.packageName)
            AppPickerTarget.AUTOSTART_3  -> assignAutostart(2, app.packageName)
            AppPickerTarget.AUTOSTART_4  -> assignAutostart(3, app.packageName)
            null -> {}
        }
        _appPickerTarget.value = null
        _nav.value = NavDestination.HOME
    }

    fun clearCarPlayApp()      { updateSettings { copy(carPlayPackage = "") } }
    fun clearAndroidAutoApp()  { updateSettings { copy(androidAutoPackage = "") } }
    fun clearPipApp()          { updateSettings { copy(pipAppPackage = "") } }
    fun clearRadioApp()        { updateSettings { copy(radioPackage = "") } }
    fun clearAutostartApp(index: Int) {
        updateSettings {
            val list = autostartPackages.toMutableList()
            if (index in list.indices) {
                list[index] = ""
                copy(autostartPackages = list)
            } else this
        }
    }

    fun updateWidgetConfig(id: String, spanX: Int, spanY: Int) {
        updateSettings {
            val resized = widgetLayout.map { w ->
                if (w.id == id) w.copy(
                    spanX = spanX.coerceIn(1, GRID_COLS - w.gridX),
                    spanY = spanY.coerceIn(1, GRID_ROWS - w.gridY)
                ) else w
            }
            // Re-run collision resolution so enlarging a widget pushes neighbors
            // aside instead of stacking on top of them
            val activeIds = activeWidgetIds()
            val active    = resized.filter { it.enabled && it.id in activeIds }
            val inactive  = resized.filter { !it.enabled || it.id !in activeIds }
            val target    = active.find { it.id == id }
            copy(widgetLayout = if (target != null)
                computeWidgetMove(active, id, target.gridX, target.gridY) + inactive
            else resized)
        }
    }

    fun moveWidgetConfig(id: String, gridX: Int, gridY: Int) {
        updateSettings {
            val activeIds = activeWidgetIds()
            val active   = widgetLayout.filter { it.enabled && it.id in activeIds }
            val inactive = widgetLayout.filter { !it.enabled || it.id !in activeIds }
            copy(widgetLayout = computeWidgetMove(active, id, gridX, gridY) + inactive)
        }
    }

    fun addWidget(id: String) {
        updateSettings {
            val activeIds = activeWidgetIds()
            var layout    = widgetLayout
            var cell      = freeCellIn(layout, activeIds)

            // If grid is full, shrink the largest multi-cell widget by one span to make room
            if (cell == null) {
                val candidate = layout
                    .filter { it.enabled && it.id in activeIds && it.spanX * it.spanY > 1 }
                    .maxByOrNull { it.spanX * it.spanY }
                if (candidate != null) {
                    layout = layout.map { w ->
                        if (w.id == candidate.id)
                            if (w.spanY > 1) w.copy(spanY = w.spanY - 1) else w.copy(spanX = w.spanX - 1)
                        else w
                    }
                    cell = freeCellIn(layout, activeIds)
                }
            }

            val cell_ = cell ?: return@updateSettings this

            val withShow = when (id) {
                "CLOCK"       -> copy(showClock = true)
                "WEATHER"     -> copy(showWeather = true)
                "NOW_PLAYING" -> copy(showNowPlaying = true)
                "TELEMETRY"   -> copy(showTelemetry = true)
                "ALTIMETER"   -> copy(showAltimeter = true)
                "SPEEDOMETER" -> copy(showSpeedometer = true)
                "VITALS"      -> copy(showVitals = true)
                "TRIP_TRACKER" -> copy(showTripTracker = true)
                "SOUNDBOARD"  -> copy(showSoundboard = true)
                "MAP" -> copy(showMap = true)
                else          -> this
            }
            val idx       = layout.indexOfFirst { it.id == id }
            val newLayout = if (idx >= 0) layout.toMutableList().also { list ->
                val w = list[idx]
                // Try to keep the widget's previous span; if no free area fits it,
                // fall back to 1×1 at the free cell so it can't overlap neighbors
                val area = if (w.spanX > 1 || w.spanY > 1) freeAreaIn(layout, activeIds, w.spanX, w.spanY) else null
                list[idx] = if (area != null)
                    w.copy(enabled = true, gridX = area.first, gridY = area.second)
                else
                    w.copy(enabled = true, gridX = cell_.first, gridY = cell_.second, spanX = 1, spanY = 1)
            } else {
                layout + com.openlauncher.app.data.WidgetConfig(id, cell_.first, cell_.second)
            }
            withShow.copy(widgetLayout = newLayout)
        }
    }

    fun toggleMapProvider() {
        updateSettings {
            copy(mapProvider = if (mapProvider == MapProvider.OSM) MapProvider.GOOGLE else MapProvider.OSM)
        }
    }

    fun setMapType(type: com.openlauncher.app.data.MapType) {
        updateSettings {
            copy(mapType = type)
        }
    }

    fun toggleTraffic() {
        updateSettings {
            copy(showTraffic = !showTraffic)
        }
    }

    fun calculateRoute(startLat: Double, startLon: Double, destLat: Double, destLon: Double, name: String?) {
        destinationCoords = Pair(destLat, destLon)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val req = ValhallaRequest(
                    locations = listOf(
                        ValhallaLocation(startLat, startLon),
                        ValhallaLocation(destLat, destLon)
                    )
                )
                val response = ValhallaApi.service.getRoute(req)
                val trip = response.trip
                val leg = trip?.legs?.firstOrNull()
                val shapeEncoded = leg?.shape
                if (shapeEncoded != null) {
                    val points = ValhallaPolylineDecoder.decode(shapeEncoded)
                    _activeRoutePoints.value = points
                    _routeManeuvers.value = leg.maneuvers
                    _routeSummary.value = trip.summary
                    _destinationName.value = name ?: "Destino seleccionado"
                    _isNavigating.value = false // preview mode first
                    _currentManeuver.value = leg.maneuvers?.firstOrNull()
                    _distanceToManeuver.value = leg.maneuvers?.firstOrNull()?.distance?.let { it * 1000.0 }
                    // Update initial segment bearing
                    if (points.size >= 2) {
                        _routeBearing.value = RouteTracker.getRouteSegmentBearing(points[0], points[1])
                    } else {
                        _routeBearing.value = null
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun startNavigation() {
        _isNavigating.value = true
    }

    fun stopNavigation() {
        _isNavigating.value = false
        _activeRoutePoints.value = null
        _routeManeuvers.value = null
        _routeSummary.value = null
        _destinationName.value = null
        _currentManeuver.value = null
        _distanceToManeuver.value = null
        _routeBearing.value = null
        destinationCoords = null
    }

    fun removeWidget(id: String) {
        updateSettings {
            when (id) {
                "CLOCK"       -> copy(showClock = false)
                "WEATHER"     -> copy(showWeather = false)
                "NOW_PLAYING" -> copy(showNowPlaying = false)
                "TELEMETRY"   -> copy(showTelemetry = false)
                "ALTIMETER"   -> copy(showAltimeter = false)
                "SPEEDOMETER" -> copy(showSpeedometer = false)
                "VITALS"      -> copy(showVitals = false)
                "TRIP_TRACKER" -> copy(showTripTracker = false)
                "SOUNDBOARD"  -> copy(showSoundboard = false)
                "MAP" -> copy(showMap = false)
                else          -> this
            }
        }
    }

    fun updateSoundboardPad(index: Int, pad: SoundPadConfig) {
        updateSettings {
            // Persisted lists from older versions may be shorter than the 6 pads
            // the widget displays — pad before assigning to avoid IndexOutOfBounds
            val padded = soundboardPads.toMutableList()
            while (padded.size <= index) padded.add(SoundPadConfig("+", synthType = ""))
            padded[index] = pad
            copy(soundboardPads = padded)
        }
    }

    private fun freeCellIn(
        layout: List<com.openlauncher.app.data.WidgetConfig>,
        activeIds: Set<String>
    ): Pair<Int, Int>? = freeAreaIn(layout, activeIds, 1, 1)

    private fun freeAreaIn(
        layout: List<com.openlauncher.app.data.WidgetConfig>,
        activeIds: Set<String>,
        spanX: Int,
        spanY: Int
    ): Pair<Int, Int>? {
        val occupied = buildSet<Pair<Int, Int>> {
            layout.filter { it.enabled && it.id in activeIds }.forEach { w ->
                for (dx in 0 until w.spanX) for (dy in 0 until w.spanY) add(w.gridX + dx to w.gridY + dy)
            }
        }
        for (row in 0 until GRID_ROWS) for (col in 0 until GRID_COLS) {
            if (col + spanX > GRID_COLS || row + spanY > GRID_ROWS) continue
            if ((0 until spanX).all { dx -> (0 until spanY).all { dy -> (col + dx to row + dy) !in occupied } })
                return col to row
        }
        return null
    }

    fun cancelCarPlayPicker() {
        _appPickerTarget.value = null
    }

    // ── Rearrange mode ────────────────────────────────────────────────────────
    private val _rearrangeMode = MutableStateFlow(false)
    val rearrangeMode: StateFlow<Boolean> = _rearrangeMode

    fun toggleRearrangeMode() { _rearrangeMode.value = !_rearrangeMode.value }
    fun exitRearrangeMode()   { _rearrangeMode.value = false }

    // ── Installed apps ────────────────────────────────────────────────────────
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps

    private val _appsLoading = MutableStateFlow(false)
    val appsLoading: StateFlow<Boolean> = _appsLoading

    fun loadInstalledApps() {
        if (_appsLoading.value) return

            viewModelScope.launch(Dispatchers.IO) {
                _appsLoading.value = true

                val pm = getApplication<Application>().packageManager

                // Todas las apps que tienen icono en algún launcher
                val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }

                val launcherPackages = pm.queryIntentActivities(launcherIntent, 0)
                .map { it.activityInfo.packageName }
                .toSet()

                _apps.value = pm.getInstalledApplications(0)
                .filter { appInfo ->
                    launcherPackages.contains(appInfo.packageName)
                }
                .mapNotNull { appInfo ->
                    try {
                        val label = pm.getApplicationLabel(appInfo).toString()

                        AppInfo(
                            packageName = appInfo.packageName,
                            appName = label,
                            icon = pm.getApplicationIcon(appInfo),
                                isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                        )
                    } catch (_: Exception) {
                        null
                    }
                }
                .distinctBy { it.packageName }
                .sortedBy { it.appName.lowercase() }

                _appsLoading.value = false
            }
    }

    fun launchApp(packageName: String) {
        val app = getApplication<Application>()
        val pm  = app.packageManager
        // Try standard launch intent first; fall back to first ACTION_MAIN activity in package
        val intent = pm.getLaunchIntentForPackage(packageName)
            ?: pm.queryIntentActivities(
                Intent(Intent.ACTION_MAIN).setPackage(packageName), 0
            ).firstOrNull()?.activityInfo?.let { ai ->
                Intent(Intent.ACTION_MAIN).apply {
                    setClassName(ai.packageName, ai.name)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        intent?.let {
            runCatching {
                app.startActivity(it)
            }.onFailure { e ->
                android.util.Log.e("LauncherVM", "Failed to launch app: $packageName", e)
            }
        }
    }

    // ── Now Playing ───────────────────────────────────────────────────────────
    val nowPlaying: StateFlow<NowPlayingState?> = MediaListenerService.nowPlaying
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        fun playPause(context: Context) {
            val ctrl = nowPlaying.value?.controller
            if (ctrl != null) {
                val state = ctrl.playbackState?.state
                if (state == android.media.session.PlaybackState.STATE_PLAYING)
                    ctrl.transportControls?.pause()
                    else
                        ctrl.transportControls?.play()
            } else {
                playLastOrOpenActive(context)
            }
        }

    fun skipNext() { nowPlaying.value?.controller?.transportControls?.skipToNext() }
    fun skipPrev() { nowPlaying.value?.controller?.transportControls?.skipToPrevious() }

    private fun getFallbackMusicPackage(context: Context): String {
        val pm = context.packageManager
        val candidates = listOf(
            "com.jetappfactory.jetaudio",
            "com.maxmpz.audioplayer",
            "com.spotify.music",
            "com.google.android.apps.youtube.music",
            "com.apple.android.music",
            "org.videolan.vlc",
            "com.pandora.android",
            "com.deezer.android"
        )
        for (pkg in candidates) {
            try {
                pm.getPackageInfo(pkg, 0)
                return pkg
            } catch (_: Exception) {}
        }
        return ""
    }

    fun playLastOrOpenActive(context: Context) {
        val state = nowPlaying.value
        val controller = state?.controller
        val pkg = controller?.packageName ?: MediaListenerService.lastMediaPackage.ifEmpty {
            getFallbackMusicPackage(context)
        }

        // 1. Lanzar la App visualmente
        if (pkg.isNotEmpty()) {
            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }

            // TRUCO PARA HIBERNACIÓN: Enviar un Intent directo para despertar su servicio de música subyacente
            try {
                val mediaIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                    `package` = pkg
                    putExtra(Intent.EXTRA_KEY_EVENT, android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PLAY))
                }
                context.sendBroadcast(mediaIntent)
            } catch (_: Exception) {}
        }

        // 2. Controlar la reproducción forzando el despertar
        if (state?.isPlaying != true) {

            // Si hay controlador, intentamos usarlo de inmediato por si acaso
            if (controller != null) {
                try {
                    controller.transportControls?.play()
                } catch (_: Exception) {}
            }

            // RESPALDÓ AGRESIVO CON RETRASO: Usamos una corrutina ligera (o un Handler)
            // para lanzar las llaves del AudioManager 300ms después.
            // Esto le da tiempo a la app suspendida de registrarse de nuevo en el sistema de audio.
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager

            // Ejecutamos en el hilo principal con un leve desfase para asegurar el tiro
            context.mainExecutor.execute {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        val eventDown = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PLAY)
                        audioManager.dispatchMediaKeyEvent(eventDown)
                        val eventUp = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_PLAY)
                        audioManager.dispatchMediaKeyEvent(eventUp)
                    } catch (_: Exception) {}
                }, 350) // 350 milisegundos son imperceptibles para el usuario pero una eternidad para el procesador
            }
        }
    }

 /*   fun playLastOrOpenActive(context: Context) {
        val state = nowPlaying.value
        val controller = state?.controller
        val pkg = controller?.packageName ?: MediaListenerService.lastMediaPackage.ifEmpty {
            getFallbackMusicPackage(context)
        }

        if (pkg.isEmpty()) return

            // Caso 1: Si milagrosamente el controlador existe pero no estaba reproduciendo
            if (controller != null) {
                if (state?.isPlaying != true) {
                    controller.transportControls?.play()
                }
                return
            }

            // Caso 2: El carro se reinició (controller == null). Forzamos el despertar de la app específica.
            try {
                // Enviamos el comando PLAY apuntando DIRECTAMENTE a la app que queremos despertar
                val eventDown = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PLAY)
                val intentDown = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                    `package` = pkg
                    putExtra(Intent.EXTRA_KEY_EVENT, eventDown)
                }
                context.sendBroadcast(intentDown)

                val eventUp = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_PLAY)
                val intentUp = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                    `package` = pkg
                    putExtra(Intent.EXTRA_KEY_EVENT, eventUp)
                }
                context.sendBroadcast(intentUp)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Como respaldo absoluto (si la app estaba profundamente dormida), abrimos su interfaz
            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                context.startActivity(intent)
            }
    } */

    // ── Weather ───────────────────────────────────────────────────────────────
    private val _weather = MutableStateFlow<WeatherState?>(null)
    val weather: StateFlow<WeatherState?> = _weather

    private val _weatherError = MutableStateFlow<String?>(null)
    val weatherError: StateFlow<String?> = _weatherError

    private var weatherJob: Job? = null

        // CORREGIDO: Usamos 'application' en lugar de 'context'
        private val sharedPrefs = application.getSharedPreferences("weather_cache", Context.MODE_PRIVATE)
        private val gson = Gson()

        fun fetchWeather(lat: Double, lon: Double, metric: Boolean) {
            weatherJob?.cancel()
            weatherJob = viewModelScope.launch {
                try {
                    // INTENTAR TRAER DE INTERNET (Hay Wi-Fi/Datos)
                    val resp = WeatherApi.service.getForecast(lat, lon, temperatureUnit = "celsius")
                    val daily = resp.dailyData
                    val current = resp.currentWeather

                    if (daily != null) {
                        val daysList = daily.dates.mapIndexed { index, date ->
                            com.openlauncher.app.model.DailyForecast(
                                date = date,
                                maxTemperatureCelsius = daily.maxTemperatures.getOrNull(index) ?: 0.0,
                                                                     minTemperatureCelsius = daily.minTemperatures.getOrNull(index) ?: 0.0,
                                                                     weatherCode = daily.weatherCodes.getOrNull(index) ?: 0
                            )
                        }

                        val nuevoEstado = WeatherState(
                            currentTemperature = current?.temperature,
                            forecastDays = daysList,
                                isLoading = false,
                                error = null
                        )

                        // Almacenamos en caché el JSON de los 7 días de forma asíncrona
                        withContext(Dispatchers.IO) {
                            val json = gson.toJson(nuevoEstado)
                            sharedPrefs.edit().putString("cached_state", json).apply()
                        }

                        _weather.value = nuevoEstado
                        _weatherError.value = null
                    } // CORREGIDO: Se eliminó el caracter '/' sobrante que causaba el error de sintaxis
                } catch (e: Exception) {
                    // MODALIDAD OFFLINE: Si falla internet, cargamos del caché
                    val jsonGuardado = sharedPrefs.getString("cached_state", null)
                    if (!jsonGuardado.isNullOrEmpty()) {
                        // CORREGIDO: Casteo explícito seguro para evitar la confusión de GSON con Map.Entry
                        val estadoRecuperado = gson.fromJson(jsonGuardado, WeatherState::class.java) as WeatherState

                        // CORREGIDO: Reconstruimos el estado clonando únicamente los días para evitar invocar 'currentTemperature'
                        _weather.value = WeatherState(
                            currentTemperature = estadoRecuperado.currentTemperature,
                            forecastDays = estadoRecuperado.forecastDays,
                                isLoading = false,
                                error = null
                        )
                        _weatherError.value = null
                    } else {
                        _weatherError.value = e.message
                    }
                }
            }
        }


    // ── Location & Compass ────────────────────────────────────────────────────
    val location: StateFlow<LocationData?> = locationMgr.location
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val compassBearing: StateFlow<Float> = locationMgr.bearing
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0f)

    // Re-evaluated every minute: a parked car produces no location updates
    // (minDistance filters), so AUTO mode must also flip on time alone.
    private val minuteTicker = flow { while (true) { emit(Unit); delay(60_000L) } }

    val isDayMode: StateFlow<Boolean> = combine(settings, locationMgr.location, minuteTicker) { s, loc, _ ->
        when (s.dayNightMode) {
            DayNightMode.DARK   -> false
            DayNightMode.LIGHT  -> true
            DayNightMode.AUTO   -> if (loc != null) SunriseSunset.isDay(loc.latitude, loc.longitude) else false
            DayNightMode.SYSTEM -> false // placeholder — overridden in MainActivity via isSystemInDarkTheme()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun startLocationUpdates() = locationMgr.start()
    fun stopLocationUpdates()  = locationMgr.stop()

    // ── Connectivity ──────────────────────────────────────────────────────────
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _wifiLevel = MutableStateFlow(-1) // 0-4, -1 = disconnected
    val wifiLevel: StateFlow<Int> = _wifiLevel

    private val _mobileLevel = MutableStateFlow(-1) // 0-4, -1 = disconnected
    val mobileLevel: StateFlow<Int> = _mobileLevel

    private var telephonyManager: TelephonyManager? = null
    private var phoneStateListener: PhoneStateListener? = null

    private fun startSignalListeners() {
        telephonyManager = getApplication<Application>().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        phoneStateListener = object : PhoneStateListener() {
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                super.onSignalStrengthsChanged(signalStrength)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    _mobileLevel.value = signalStrength?.level ?: -1
                } else {
                    // Fallback for API < 23 if necessary, but Junsun is Android 10
                    _mobileLevel.value = -1 
                }
            }
        }
        telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
    }

    fun refreshMedia() {
        MediaListenerService.requestRefresh()
    }

    // ── Radio ─────────────────────────────────────────────────────────────────
    // Android has no public FM tuner API (the Broadcast Radio HAL is @SystemApi),
    // so the radio widget mirrors the unit's real tuner through two backends:
    //   1. szchoiceway MCU units — Settings.Global JSON + canbus broadcasts.
    //      Full control: seek, band switching, direct frequency tuning.
    //   2. Any other unit — the vendor radio app's MediaSession. Frequency and
    //      station are parsed from session metadata; seek maps to skip next/prev
    //      (how steering-wheel keys drive these apps). No direct tuning.
    data class HardwareRadioState(
        val band: String,
        val freq: String,
        val stationName: String? = null,
        // Direct tuning + FM1/FM2/FM3/AM switching — vendor MCU backend only
        val canTune: Boolean = false
    ) {
        val display get() = "$band  $freq"
        val isAm    get() = band.equals("AM", ignoreCase = true)
    }

    private val _mcuRadio = MutableStateFlow<HardwareRadioState?>(null)

    private fun packageInstalled(pkg: String) = runCatching {
        getApplication<Application>().packageManager.getPackageInfo(pkg, 0)
    }.isSuccess

    private val hasSzchoicewayMcu: Boolean by lazy {
        packageInstalled("com.szchoiceway.radio") ||
        packageInstalled("com.szchoiceway.eventcenter") ||
        runCatching {
            AndroidSettings.Global.getString(
                getApplication<Application>().contentResolver, "SYS_MEDIA_INFO_JSON"
            ) != null
        }.getOrDefault(false)
    }

    private fun parseRadioJson(): HardwareRadioState? {
        return try {
            val json = AndroidSettings.Global.getString(
                getApplication<Application>().contentResolver, "SYS_MEDIA_INFO_JSON"
            ) ?: return null
            val title = org.json.JSONObject(json).optString("mediaTitle", "") // e.g. "FM1 90.10"
            if (title.isEmpty()) return null
            val parts = title.trim().split("\\s+".toRegex())
            if (parts.size < 2) return null
            HardwareRadioState(band = parts[0], freq = parts[1], canTune = true)
        } catch (e: Exception) {
            android.util.Log.e("RadioMcu", "parseRadioJson error", e)
            null
        }
    }

    private var radioObserver: ContentObserver? = null

    private fun startHardwareRadioObserver() {
        if (radioObserver != null) return
        _mcuRadio.value = parseRadioJson()
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                _mcuRadio.value = parseRadioJson()
            }
        }
        getApplication<Application>().contentResolver.registerContentObserver(
            AndroidSettings.Global.getUriFor("SYS_MEDIA_INFO_JSON"),
            false, observer
        )
        radioObserver = observer
    }

    // ── MediaSession radio backend (universal fallback) ───────────────────────

    private fun looksLikeRadioPackage(pkg: String): Boolean {
        val p = pkg.lowercase()
        return "radio" in p || "fmradio" in p || "tuner" in p || p.endsWith(".fm") || ".fm." in p
    }

    private fun isRadioSessionPackage(pkg: String): Boolean {
        val assigned = settings.value.radioPackage
        return if (assigned.isNotEmpty()) pkg == assigned else looksLikeRadioPackage(pkg)
    }

    // Matches "FM1 90.10", "99.9 MHz", "FM 99.9 WXYZ", "1040 kHz", "99,9" …
    private val sessionFreqPattern =
        Regex("""(?:\b(FM\s?\d?|AM)\b)?\s*(\d{2,4}(?:[.,]\d{1,2})?)\s*(MHz|kHz)?""", RegexOption.IGNORE_CASE)

    private fun parseSessionRadio(np: NowPlayingState): HardwareRadioState? {
        val pkg = np.controller?.packageName ?: return null
        if (!isRadioSessionPackage(pkg)) return null

        val text = listOf(np.title, np.artist)
            .filter { it.isNotBlank() && it != "Unknown" }
            .joinToString("  ")
        val match   = sessionFreqPattern.find(text)
        val rawFreq = match?.groupValues?.get(2)?.replace(',', '.')?.takeIf { it.isNotEmpty() }
        val freqVal = rawFreq?.toFloatOrNull()
        val explicitBand = match?.groupValues?.get(1)?.replace(" ", "")?.uppercase().orEmpty()
        val band = when {
            explicitBand.isNotEmpty()                 -> explicitBand
            freqVal != null && freqVal in 520f..1710f -> "AM"
            else                                      -> "FM"
        }
        // Whatever isn't the frequency is the station / RDS text
        val station = (if (match != null) text.replace(match.value, "") else text)
            .trim(' ', '-', '|', '/', '•')
            .takeIf { it.isNotBlank() }
        return HardwareRadioState(band = band, freq = rawFreq ?: "—", stationName = station, canTune = false)
    }

    // MCU backend wins when present; otherwise mirror the radio app's session
    val hardwareRadio: StateFlow<HardwareRadioState?> =
        combine(_mcuRadio, nowPlaying, settings) { mcu, np, _ ->
            mcu ?: np?.let { parseSessionRadio(it) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun radioSessionController() = nowPlaying.value?.controller?.takeIf { c ->
        c.packageName?.let { isRadioSessionPackage(it) } == true
    }

    // Send a radio keyCode to the MCU via the EventService broadcast channel.
    // Permission com.szchoiceway.permission.broadcast (prot=normal) is required and declared.
    // keyCodes: 15=seek_up, 14=seek_down, 30=fm_cycle, 31=am
    private fun sendMcuByteArray(bytes: ByteArray) {
        runCatching {
            val intent = Intent("com.szchoiceway.eventcenter.EventUtils.ACTION_MCU_CMD_EVENT").apply {
                putExtra("EventUtils.MCU_CMD_DATA", bytes)
            }
            getApplication<Application>().sendBroadcast(intent)
        }
    }

    private fun sendMcuBytes(vararg bytes: Byte) = sendMcuByteArray(bytes)

    // Tune to a specific frequency.
    // Canbus frame: 0x5a 0xa5 0x0d [0x91 bandByte bank(2) zeros... freqAscii] checksum
    // Checksum = low byte of sum of all preceding bytes.
    // Wrapped with 0x0d 0x08 sub-command prefix for MCU_CMD_DATA.
    fun radioTune(band: String, freqMhz: Float) {
        if (!hasSzchoicewayMcu) return // direct tuning is MCU-only
        val bandByte  = when (band) { "FM3" -> 0x03.toByte(); "AM" -> 0x04.toByte(); else -> 0x01.toByte() }
        val bankStr   = if (band == "FM2") "02" else "01"
        val freqStr   = if (band == "AM") String.format(java.util.Locale.US, "%.0f", freqMhz) else String.format(java.util.Locale.US, "%.1f", freqMhz)
        val padding   = 9 - freqStr.length
        val header    = byteArrayOf(0x5a.toByte(), 0xa5.toByte(), 0x0d.toByte())
        val data      = byteArrayOf(0x91.toByte(), bandByte,
                            bankStr[0].code.toByte(), bankStr[1].code.toByte()) +
                        ByteArray(padding) +
                        freqStr.map { it.code.toByte() }.toByteArray()
        val frame     = header + data
        val checksum  = (frame.sumOf { it.toInt() and 0xFF } and 0xFF).toByte()
        val finalPayload = byteArrayOf(0x0d.toByte(), 0x08.toByte()) + frame + byteArrayOf(checksum)
        sendMcuByteArray(finalPayload)
    }

    private val mcuActive get() = _mcuRadio.value != null

    // Seek routes to the MCU when that backend is live, otherwise to the radio
    // app's MediaSession — skip next/prev is how these apps expose seek.
    fun radioSeekUp()   { if (mcuActive) sendMcuBytes(0x02, 0x0f) else radioSessionController()?.transportControls?.skipToNext() }
    fun radioSeekDown() { if (mcuActive) sendMcuBytes(0x02, 0x0e) else radioSessionController()?.transportControls?.skipToPrevious() }
    // Band switching and direct tuning only exist on the MCU backend
    fun radioCycleFm()  { if (mcuActive) sendMcuBytes(0x02, 0x1e) }
    fun radioSwitchAm() { if (mcuActive) sendMcuBytes(0x02, 0x1f) }
    fun radioStart()    { sendMcuBytes(0x01, 0x01) }
    fun radioStop()     { sendMcuBytes(0x01, 0x63) }

    fun stopHardwareRadioApp() {
        if (mcuActive) radioStop()
        else radioSessionController()?.transportControls?.pause()
    }

    fun launchHardwareRadioApp() {
        if (hasSzchoicewayMcu) radioStart()
        val pkg = settings.value.radioPackage.ifEmpty {
            when {
                hasSzchoicewayMcu -> "com.szchoiceway.radio"
                else              -> radioSessionController()?.packageName ?: ""
            }
        }
        if (pkg.isNotEmpty()) {
            runCatching {
                val intent = getApplication<Application>().packageManager
                    .getLaunchIntentForPackage(pkg)
                    ?: Intent(Intent.ACTION_MAIN).apply {
                        `package` = pkg
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                getApplication<Application>().startActivity(intent)
            }
        }
        // Resume playback if the session is just paused
        radioSessionController()?.transportControls?.play()
    }

    fun refreshConnectivity() {
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wm = getApplication<Application>().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(network)
            _isConnected.value = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            
            // Wifi Strength
            if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                val info = wm.connectionInfo
                _wifiLevel.value = WifiManager.calculateSignalLevel(info.rssi, 5)
            } else {
                _wifiLevel.value = -1
            }
        } else {
            @Suppress("DEPRECATION")
            val info = cm.activeNetworkInfo
            val connected = info?.isConnected == true
            _isConnected.value = connected
            
            if (connected && info?.type == ConnectivityManager.TYPE_WIFI) {
                val wifiInfo = wm.connectionInfo
                _wifiLevel.value = WifiManager.calculateSignalLevel(wifiInfo.rssi, 5)
            } else {
                _wifiLevel.value = -1
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationMgr.stop()
        radioObserver?.let { getApplication<Application>().contentResolver.unregisterContentObserver(it) }
        radioObserver = null
        telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
    }

    init {
        loadInstalledApps()
        refreshConnectivity()
        startSignalListeners()
        if (hasSzchoicewayMcu) startHardwareRadioObserver()
        // Fetch weather on first location fix, then every 30 minutes.
        // The minute ticker covers the parked case where no location updates arrive.
        viewModelScope.launch {
            var lastFetchMs = 0L
            merge(
                locationMgr.location.filterNotNull(),
                minuteTicker.mapNotNull { locationMgr.location.value }
            ).collect { loc ->
                val now = System.currentTimeMillis()
                if (now - lastFetchMs >= 30 * 60 * 1_000L) {
                    lastFetchMs = now
                    fetchWeather(loc.latitude, loc.longitude, settings.value.unitSystem.name == "METRIC")
                }
            }
        }
        viewModelScope.launch {
            locationMgr.location.filterNotNull().collect { loc ->
                if (_isNavigating.value) {
                    val points = _activeRoutePoints.value
                    if (points != null && points.size >= 2) {
                        val currentLoc = GeoPoint(loc.latitude, loc.longitude)
                        val closestIdx = RouteTracker.findClosestSegmentIndex(currentLoc, points)
                        
                        // Check if user is off-route (e.g. > 70 meters)
                        val distToRoute = RouteTracker.getDistanceToSegment(currentLoc, points[closestIdx], points[closestIdx + 1])
                        if (distToRoute > 70.0) {
                            val dest = destinationCoords
                            if (dest != null) {
                                calculateRoute(loc.latitude, loc.longitude, dest.first, dest.second, _destinationName.value)
                                // Trigger navigation automatically on the recalculated route
                                _isNavigating.value = true
                            }
                            return@collect
                        }
                        
                        // Determine segment bearing for the arrow
                        val segBearing = RouteTracker.getRouteSegmentBearing(points[closestIdx], points[closestIdx + 1])
                        _routeBearing.value = segBearing
                        
                        // Update active maneuver based on closest segment index
                        val maneuvers = _routeManeuvers.value
                        if (maneuvers != null) {
                            val activeManeuver = maneuvers.firstOrNull { m ->
                                closestIdx >= m.beginShapeIndex && closestIdx <= m.endShapeIndex
                            } ?: maneuvers.lastOrNull()
                            
                            _currentManeuver.value = activeManeuver
                            
                            if (activeManeuver != null) {
                                val targetPoint = points.getOrNull(activeManeuver.endShapeIndex)
                                if (targetPoint != null) {
                                    val distanceToManeuverEnd = currentLoc.distanceToAsDouble(targetPoint)
                                    _distanceToManeuver.value = distanceToManeuverEnd
                                }
                            }
                        }
                    }
                } else {
                    _routeBearing.value = null
                }
            }
        }
    }
}
