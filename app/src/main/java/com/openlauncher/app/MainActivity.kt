package com.openlauncher.app

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.openlauncher.app.model.NavDestination
import com.openlauncher.app.ui.components.Sidebar
import com.openlauncher.app.ui.screen.*
import com.openlauncher.app.ui.theme.OpenLauncherTheme
import com.openlauncher.app.viewmodel.LauncherViewModel

class MainActivity : ComponentActivity() {

    private val vm: LauncherViewModel by viewModels()

    private val locationPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            vm.startLocationUpdates()
        }
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()

        locationPermissions.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        setContent {
            val settings    by vm.settings.collectAsStateWithLifecycle()
            val nav         by vm.nav.collectAsStateWithLifecycle()
            val apps        by vm.apps.collectAsStateWithLifecycle()
            val appsLoading by vm.appsLoading.collectAsStateWithLifecycle()
            val nowPlaying  by vm.nowPlaying.collectAsStateWithLifecycle()
            val weather     by vm.weather.collectAsStateWithLifecycle()
            val location    by vm.location.collectAsStateWithLifecycle()
            val bearing     by vm.compassBearing.collectAsStateWithLifecycle()
            val isWifi      by vm.isWifi.collectAsStateWithLifecycle()
            val isData      by vm.isData.collectAsStateWithLifecycle()
            val pickerSlot      by vm.shortcutPickerSlot.collectAsStateWithLifecycle()
            val appPickerTarget by vm.appPickerTarget.collectAsStateWithLifecycle()

            val accent         = Color(settings.accentColor)
            val bg             = Color(settings.backgroundColor)
            val bgGradientEnd  = Color(settings.gradientEndColor)
            val bgBrush        = if (settings.useGradient)
                androidx.compose.ui.graphics.Brush.linearGradient(listOf(bg, bgGradientEnd))
            else null

            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density   = baseDensity.density * settings.uiScale,
                    fontScale = baseDensity.fontScale
                )
            ) {
            OpenLauncherTheme(
                accent     = accent,
                background = bg,
                fontBold   = settings.fontBold,
                textScale  = settings.textScale,
                appFont    = settings.appFont
            ) {
                Box(modifier = Modifier.fillMaxSize().let { m ->
                    if (bgBrush != null) m.background(bgBrush) else m.background(bg)
                }) {
                    // Optional wallpaper layer
                    if (settings.wallpaperUri.isNotEmpty()) {
                        AsyncImage(
                            model              = android.net.Uri.parse(settings.wallpaperUri),
                            contentDescription = null,
                            contentScale       = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier           = Modifier.fillMaxSize()
                        )
                        Box(modifier = Modifier.fillMaxSize()
                            .background(Color.Black.copy(alpha = settings.wallpaperDim)))
                    }

                    Row(modifier = Modifier.fillMaxSize()) {
                        val sidebar: @Composable () -> Unit = {
                            Sidebar(
                                currentDest   = nav,
                                settings      = settings,
                                installedIconFor = { pkg ->
                                    apps.find { it.packageName == pkg }?.icon
                                },
                                onNavigate    = { dest ->
                                    vm.cancelShortcutPicker()
                                    vm.cancelCarPlayPicker()
                                    vm.exitRearrangeMode()
                                    vm.navigate(dest)
                                },
                                onShortcutClick = { slot ->
                                    val shortcut = settings.shortcuts[slot]
                                    if (shortcut.packageName.isNotEmpty()) {
                                        vm.launchApp(shortcut.packageName)
                                    }
                                },
                                onShortcutLongPress  = { slot -> vm.startShortcutPicker(slot) },
                                onShortcutRemove     = { slot -> vm.removeShortcut(slot) },
                                onShortcutSetIcon    = { slot, icon -> vm.setShortcutIcon(slot, icon) },
                                onReorder            = { from, to -> vm.reorderShortcut(from, to) }
                            )
                        }
                        val divider: @Composable () -> Unit = {
                            androidx.compose.material3.VerticalDivider(
                                modifier = Modifier.fillMaxHeight(),
                                color    = Color(0xFF1A1A1A)
                            )
                        }

                        if (!settings.rightHandDrive) {
                            sidebar()
                            divider()
                        }

                        // ── Main content pane ────────────────────────────────
                        AnimatedContent(
                            targetState   = nav,
                            transitionSpec = {
                                fadeIn() + slideInHorizontally { it / 10 } togetherWith
                                fadeOut() + slideOutHorizontally { -it / 10 }
                            },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            label    = "pane_transition"
                        ) { destination ->
                            when (destination) {
                                NavDestination.HOME -> HomeScreen(
                                    settings            = settings,
                                    weather             = weather,
                                    nowPlaying          = nowPlaying,
                                    location            = location,
                                    bearing             = bearing,
                                    isWifi              = isWifi,
                                    isData              = isData,
                                    onPlayPause         = vm::playPause,
                                    onNext              = vm::skipNext,
                                    onPrev              = vm::skipPrev,
                                    onLaunchCarPlay     = { vm.launchApp(settings.carPlayPackage) },
                                    onLaunchAndroidAuto = { vm.launchApp(settings.androidAutoPackage) },
                                    onAssignCarPlay     = { vm.startCarPlayPicker() },
                                    onAssignAndroidAuto = { vm.startAndroidAutoPicker() },
                                    onTapNowPlaying     = {
                                        val pkg = nowPlaying?.controller?.packageName
                                        if (!pkg.isNullOrEmpty()) vm.launchApp(pkg)
                                    },
                                    onUpdateWidget      = { id, sx, sy -> vm.updateWidgetConfig(id, sx, sy) }
                                )

                                NavDestination.APP_LIBRARY -> AppLibraryScreen(
                                    apps                = apps,
                                    isLoading           = appsLoading,
                                    isPickerMode        = pickerSlot != null,
                                    pickerSlot          = pickerSlot,
                                    isCarPlayPickerMode = appPickerTarget != null,
                                    carPlayPickerLabel  = when (appPickerTarget) {
                                        com.openlauncher.app.viewmodel.LauncherViewModel.AppPickerTarget.ANDROID_AUTO -> "CHOOSE ANDROID AUTO APP"
                                        else -> "CHOOSE CARPLAY APP"
                                    },
                                    accent              = accent,
                                    onAppClick          = { app -> vm.launchApp(app.packageName) },
                                    onPickerSelect      = { slot, app -> vm.assignShortcut(slot, app) },
                                    onCarPlaySelect     = { app -> vm.assignPickerApp(app) }
                                )

                                NavDestination.SETTINGS -> SettingsScreen(
                                    settings                 = settings,
                                    accent                   = accent,
                                    onUpdate                 = { block -> vm.updateSettings(block) },
                                    onReset                  = { vm.resetSettings() },
                                    onStartCarPlayPicker     = { vm.startCarPlayPicker() },
                                    onStartAndroidAutoPicker = { vm.startAndroidAutoPicker() }
                                )
                            }
                        }

                        if (settings.rightHandDrive) {
                            divider()
                            sidebar()
                        }
                    }
                }
            }
            } // CompositionLocalProvider
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshConnectivity()
        vm.refreshMedia()
    }

    override fun onStop() {
        super.onStop()
        vm.stopLocationUpdates()
    }

    override fun onStart() {
        super.onStart()
        vm.startLocationUpdates()
    }
}
