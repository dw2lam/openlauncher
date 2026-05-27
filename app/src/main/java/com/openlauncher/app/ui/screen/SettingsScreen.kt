package com.openlauncher.app.ui.screen

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlauncher.app.data.AppFont
import com.openlauncher.app.data.AppSettings
import com.openlauncher.app.data.ClockStyle
import com.openlauncher.app.data.ShortcutConfig
import com.openlauncher.app.data.UnitSystem
import com.openlauncher.app.ui.components.ColorPickerDialog
import com.openlauncher.app.ui.components.ConfirmDialog

private val FLAT_DIVIDER = Color(0xFF141414)
private val SECTION_DIVIDER = Color(0xFF1E1E1E)

@Composable
fun SettingsScreen(
    settings: AppSettings,
    accent: Color,
    onUpdate: (AppSettings.() -> AppSettings) -> Unit,
    onReset: () -> Unit,
    onStartCarPlayPicker: () -> Unit,
    onStartAndroidAutoPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showResetDialog       by remember { mutableStateOf(false) }
    var showAccentPicker      by remember { mutableStateOf(false) }
    var showBgPicker          by remember { mutableStateOf(false) }
    var showGradientEndPicker by remember { mutableStateOf(false) }

    val wallpaperPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Persist read permission so URI is accessible after reboot
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onUpdate { copy(wallpaperUri = it.toString()) }
        }
    }

    // Explicit background so the pane is always opaque — not transparent over wallpaper
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── Title ────────────────────────────────────────────────────────────
        Text(
            text          = "SETTINGS",
            style         = MaterialTheme.typography.titleLarge,
            color         = accent,
            letterSpacing = 3.sp,
            fontSize      = 14.sp
        )

        Spacer(Modifier.height(4.dp))

        // ── System Launcher ──────────────────────────────────────────────────
        SettingsSection("System") {
            SettingsButton(
                label    = "Set as Default Launcher",
                sublabel = "Open Android home app settings",
                icon     = Icons.Default.Home,
                accent   = accent,
                onClick  = {
                    context.startActivity(
                        Intent(Settings.ACTION_HOME_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            )
        }

        // ── Permissions ──────────────────────────────────────────────────────
        SettingsSection("Permissions") {
            val isMediaConnected by com.openlauncher.app.service.MediaListenerService.isConnected.collectAsState()
            SettingsButton(
                label    = "Notification Access",
                sublabel = if (isMediaConnected) "Granted — media controls active" else "Required for Now Playing widget",
                icon     = if (isMediaConnected) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                accent   = if (isMediaConnected) accent else Color(0xFF993333),
                onClick  = {
                    context.startActivity(
                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            )
        }

        // ── Vehicle Name ─────────────────────────────────────────────────────
        SettingsSection("Vehicle") {
            var nameInput by remember(settings.vehicleName) { mutableStateOf(settings.vehicleName) }
            SettingsRow(
                label    = "Vehicle Name",
                sublabel = "",
                icon     = Icons.Default.DirectionsCar
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value         = nameInput,
                        onValueChange = { nameInput = it },
                        placeholder   = { Text("MY CAR", color = Color(0xFF444444), fontSize = 12.sp) },
                        singleLine    = true,
                        textStyle     = LocalTextStyle.current.copy(fontSize = 12.sp, color = Color.White),
                        colors        = outlinedFieldColors(accent),
                        modifier      = Modifier.width(120.dp).height(40.dp)
                    )
                    if (nameInput != settings.vehicleName) {
                        IconButton(onClick = { onUpdate { copy(vehicleName = nameInput) } }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Check, "Save", tint = accent, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            SettingsDivider()

            SettingsRow(
                label    = "Right Hand Drive",
                sublabel = "Move sidebar to the right side",
                icon     = Icons.Default.SwapHoriz
            ) {
                Switch(
                    checked         = settings.rightHandDrive,
                    onCheckedChange = { onUpdate { copy(rightHandDrive = it) } },
                    colors          = switchColors(accent)
                )
            }
        }

        // ── Sidebar Shortcuts ─────────────────────────────────────────────────
        SettingsSection("Sidebar") {
            settings.shortcuts.forEachIndexed { index, shortcut ->
                if (index > 0) SettingsDivider()
                SettingsRow(
                    label    = "Slot ${index + 1}",
                    sublabel = when {
                        shortcut.label.isNotEmpty()       -> shortcut.label
                        shortcut.packageName.isNotEmpty() -> shortcut.packageName
                        else                              -> "Empty"
                    },
                    icon     = Icons.Default.Apps
                ) {
                    if (settings.shortcuts.size > 1) {
                        IconButton(
                            onClick  = {
                                onUpdate {
                                    copy(shortcuts = shortcuts.toMutableList().also { it.removeAt(index) })
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, null, tint = Color(0xFF993333), modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            SettingsDivider()

            SettingsButton(
                label    = "Add Slot",
                sublabel = "Append an empty shortcut to the sidebar",
                icon     = Icons.Default.Add,
                accent   = accent,
                onClick  = { onUpdate { copy(shortcuts = shortcuts + ShortcutConfig()) } }
            )
        }

        // ── Appearance ───────────────────────────────────────────────────────
        SettingsSection("Appearance") {
            // Accent color
            SettingsRow(
                label    = "Accent Color",
                sublabel = "UI highlight color",
                icon     = Icons.Default.Palette
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(settings.accentColor))
                        .clickable { showAccentPicker = true }
                )
            }

            SettingsDivider()

            // Background color + gradient
            SettingsRow(
                label    = "Background",
                sublabel = if (settings.useGradient) "Gradient" else "Solid color",
                icon     = Icons.Default.FormatColorFill
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    // Start color swatch
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(settings.backgroundColor))
                            .clickable { showBgPicker = true }
                    )
                    if (settings.useGradient) {
                        androidx.compose.material3.Icon(
                            Icons.Default.ArrowForward, null,
                            tint = Color(0xFF555555), modifier = Modifier.size(14.dp)
                        )
                        // End color swatch
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(settings.gradientEndColor))
                                .clickable { showGradientEndPicker = true }
                        )
                    }
                }
            }

            SettingsDivider()

            SettingsRow(label = "Use Gradient", sublabel = "Blend two colors as background", icon = Icons.Default.Gradient) {
                Switch(checked = settings.useGradient,
                    onCheckedChange = { onUpdate { copy(useGradient = it) } },
                    colors = switchColors(accent))
            }

            SettingsDivider()

            // Wallpaper
            SettingsButton(
                label    = "Set Wallpaper",
                sublabel = if (settings.wallpaperUri.isNotEmpty()) "Custom wallpaper active" else "Choose image from gallery",
                icon     = Icons.Default.Wallpaper,
                accent   = accent,
                onClick  = { wallpaperPicker.launch("image/*") }
            )
            if (settings.wallpaperUri.isNotEmpty()) {
                Column {
                    SettingsRow(
                        label    = "Wallpaper Dim",
                        sublabel = "${"%.0f".format(settings.wallpaperDim * 100)}%",
                        icon     = Icons.Default.BrightnessLow
                    ) {}
                    Slider(
                        value         = settings.wallpaperDim,
                        onValueChange = { onUpdate { copy(wallpaperDim = it) } },
                        valueRange    = 0f..0.95f,
                        steps         = 18,
                        colors        = sliderColors(accent),
                        modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick  = { onUpdate { copy(wallpaperUri = "") } },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("REMOVE WALLPAPER", color = Color(0xFF993333), fontSize = 9.sp, letterSpacing = 1.sp)
                    }
                }
            }
        }

        // ── Typography ───────────────────────────────────────────────────────
        SettingsSection("Typography") {
            SettingsRow(label = "Font", sublabel = fontDisplayName(settings.appFont), icon = Icons.Default.FontDownload) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    com.openlauncher.app.data.AppFont.entries.forEach { font ->
                        FilterChip(
                            selected = settings.appFont == font,
                            onClick  = { onUpdate { copy(appFont = font) } },
                            label    = { Text(fontDisplayName(font), fontSize = 9.sp, letterSpacing = 0.5.sp) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accent,
                                selectedLabelColor     = Color.Black
                            )
                        )
                    }
                }
            }

            SettingsDivider()

            SettingsRow(label = "Bold Font", sublabel = "Heavier weight across all text", icon = Icons.Default.FormatBold) {
                Switch(
                    checked         = settings.fontBold,
                    onCheckedChange = { onUpdate { copy(fontBold = it) } },
                    colors          = switchColors(accent)
                )
            }

            SettingsDivider()

            Column {
                SettingsRow(
                    label    = "Text Scale",
                    sublabel = "${"%.0f".format(settings.textScale * 100)}%",
                    icon     = Icons.Default.TextFields
                ) {}
                Slider(
                    value         = settings.textScale,
                    onValueChange = { onUpdate { copy(textScale = it) } },
                    valueRange    = 0.8f..1.4f,
                    steps         = 5,
                    colors        = sliderColors(accent),
                    modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
            }

            SettingsDivider()

            Column {
                SettingsRow(
                    label    = "UI Scale",
                    sublabel = "${"%.0f".format(settings.uiScale * 100)}%  — scales all elements",
                    icon     = Icons.Default.ZoomIn
                ) {}
                Slider(
                    value         = settings.uiScale,
                    onValueChange = { onUpdate { copy(uiScale = it) } },
                    valueRange    = 0.7f..1.5f,
                    steps         = 7,
                    colors        = sliderColors(accent),
                    modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
            }
        }

        // ── Home Widgets ─────────────────────────────────────────────────────
        SettingsSection("Home Widgets") {
            SettingsRow(label = "Clock", sublabel = "Show clock widget", icon = Icons.Default.Schedule) {
                Switch(checked = settings.showClock,
                    onCheckedChange = { onUpdate { copy(showClock = it) } },
                    colors = switchColors(accent))
            }
            SettingsDivider()
            SettingsRow(label = "Weather", sublabel = "Show weather widget", icon = Icons.Default.WbSunny) {
                Switch(checked = settings.showWeather,
                    onCheckedChange = { onUpdate { copy(showWeather = it) } },
                    colors = switchColors(accent))
            }
            SettingsDivider()
            SettingsRow(label = "Telemetry / Compass", sublabel = "GPS coordinates & heading", icon = Icons.Default.Explore) {
                Switch(checked = settings.showTelemetry,
                    onCheckedChange = { onUpdate { copy(showTelemetry = it) } },
                    colors = switchColors(accent))
            }
            SettingsDivider()
            SettingsRow(label = "Now Playing", sublabel = "Media controls widget", icon = Icons.Default.MusicNote) {
                Switch(checked = settings.showNowPlaying,
                    onCheckedChange = { onUpdate { copy(showNowPlaying = it) } },
                    colors = switchColors(accent))
            }
            SettingsDivider()
            AppShortcutRow(
                label    = "CarPlay App",
                pkg      = settings.carPlayPackage,
                icon     = Icons.Default.PhoneAndroid,
                accent   = accent,
                onClear  = { onUpdate { copy(carPlayPackage = "") } },
                onPick   = onStartCarPlayPicker
            )
            SettingsDivider()
            AppShortcutRow(
                label    = "Android Auto App",
                pkg      = settings.androidAutoPackage,
                icon     = Icons.Default.DirectionsCar,
                accent   = accent,
                onClear  = { onUpdate { copy(androidAutoPackage = "") } },
                onPick   = onStartAndroidAutoPicker
            )
            SettingsDivider()

            // Clock style toggle
            SettingsRow(label = "Clock Style", sublabel = settings.clockStyle.name.lowercase().replaceFirstChar { it.uppercaseChar() }, icon = Icons.Default.Watch) {
                Row {
                    FilterChip(
                        selected = settings.clockStyle == ClockStyle.DIGITAL,
                        onClick  = { onUpdate { copy(clockStyle = ClockStyle.DIGITAL) } },
                        label    = { Text("Digital", fontSize = 11.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent,
                            selectedLabelColor     = Color.Black
                        )
                    )
                    Spacer(Modifier.width(6.dp))
                    FilterChip(
                        selected = settings.clockStyle == ClockStyle.ANALOG,
                        onClick  = { onUpdate { copy(clockStyle = ClockStyle.ANALOG) } },
                        label    = { Text("Analog", fontSize = 11.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent,
                            selectedLabelColor     = Color.Black
                        )
                    )
                }
            }
        }

        // ── Units ────────────────────────────────────────────────────────────
        SettingsSection("Units") {
            SettingsRow(label = "Unit System", sublabel = if (settings.unitSystem == UnitSystem.METRIC) "Metric (°C, km)" else "Imperial (°F, mi)", icon = Icons.Default.Straighten) {
                Row {
                    FilterChip(
                        selected = settings.unitSystem == UnitSystem.METRIC,
                        onClick  = { onUpdate { copy(unitSystem = UnitSystem.METRIC) } },
                        label    = { Text("Metric", fontSize = 11.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent,
                            selectedLabelColor     = Color.Black
                        )
                    )
                    Spacer(Modifier.width(6.dp))
                    FilterChip(
                        selected = settings.unitSystem == UnitSystem.IMPERIAL,
                        onClick  = { onUpdate { copy(unitSystem = UnitSystem.IMPERIAL) } },
                        label    = { Text("Imperial", fontSize = 11.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent,
                            selectedLabelColor     = Color.Black
                        )
                    )
                }
            }
        }

        // ── Maintenance ──────────────────────────────────────────────────────
        SettingsSection("Maintenance") {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick  = { showResetDialog = true },
                shape    = RoundedCornerShape(4.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A0000)),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Icon(Icons.Default.RestartAlt, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Reset to Defaults", color = MaterialTheme.colorScheme.error, fontSize = 13.sp, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text          = "Made by David Lam  ·  2026",
            color         = Color(0xFF2A2A2A),
            fontSize      = 10.sp,
            letterSpacing = 1.sp,
            modifier      = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 16.dp)
        )
    }
    } // end Box

    // ── Dialogs ──────────────────────────────────────────────────────────────
    if (showResetDialog) {
        ConfirmDialog(
            title        = "Reset Settings",
            message      = "Are you sure you want to reset all settings to default? This cannot be undone.",
            confirmLabel = "Reset",
            onConfirm    = { onReset(); showResetDialog = false },
            onDismiss    = { showResetDialog = false }
        )
    }

    if (showAccentPicker) {
        ColorPickerDialog(
            title           = "Accent Color",
            initialColor    = Color(settings.accentColor),
            onColorSelected = { c -> onUpdate { copy(accentColor = c.toArgb()) } },
            onDismiss       = { showAccentPicker = false }
        )
    }

    if (showBgPicker) {
        ColorPickerDialog(
            title           = "Background Color",
            initialColor    = Color(settings.backgroundColor),
            onColorSelected = { c -> onUpdate { copy(backgroundColor = c.toArgb()) } },
            onDismiss       = { showBgPicker = false }
        )
    }

    if (showGradientEndPicker) {
        ColorPickerDialog(
            title           = "Gradient End Color",
            initialColor    = Color(settings.gradientEndColor),
            onColorSelected = { c -> onUpdate { copy(gradientEndColor = c.toArgb()) } },
            onDismiss       = { showGradientEndPicker = false }
        )
    }
}

// ── Helpers ─────────────────────────────────────────────────────────────────

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(
            text          = title.uppercase(),
            style         = MaterialTheme.typography.labelSmall,
            color         = Color(0xFF3A3A3A),
            letterSpacing = 2.sp,
            modifier      = Modifier.padding(top = 16.dp, bottom = 6.dp)
        )
        HorizontalDivider(color = SECTION_DIVIDER)
        Column(modifier = Modifier.fillMaxWidth(), content = content)
        HorizontalDivider(color = SECTION_DIVIDER)
    }
}

@Composable
private fun SettingsRow(
    label: String,
    sublabel: String = "",
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFDDDDDD), fontSize = 13.sp)
            if (sublabel.isNotEmpty())
                Text(sublabel, style = MaterialTheme.typography.labelSmall, color = Color(0xFF444444), fontSize = 11.sp)
        }
        content()
    }
}

@Composable
private fun ColumnScope.SettingsButton(
    label: String,
    sublabel: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 0.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFDDDDDD), fontSize = 13.sp)
            if (sublabel.isNotEmpty())
                Text(sublabel, style = MaterialTheme.typography.labelSmall, color = Color(0xFF444444), fontSize = 11.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF2A2A2A), modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun ColumnScope.SettingsDivider() {
    HorizontalDivider(color = FLAT_DIVIDER)
}

@Composable
private fun outlinedFieldColors(accent: Color) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = accent,
    unfocusedBorderColor = Color(0xFF2A2A2A),
    focusedTextColor     = Color.White,
    unfocusedTextColor   = Color.White,
    cursorColor          = accent,
    focusedLabelColor    = accent,
    unfocusedLabelColor  = Color(0xFF666666)
)

@Composable
private fun switchColors(accent: Color) = SwitchDefaults.colors(
    checkedThumbColor  = Color.Black,
    checkedTrackColor  = accent,
    uncheckedTrackColor = Color(0xFF2A2A2A)
)

@Composable
private fun sliderColors(accent: Color) = SliderDefaults.colors(
    thumbColor            = accent,
    activeTrackColor      = accent,
    inactiveTrackColor    = Color(0xFF2A2A2A)
)

@Composable
private fun ColumnScope.AppShortcutRow(
    label: String,
    pkg: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClear: () -> Unit,
    onPick: () -> Unit
) {
    SettingsRow(
        label    = label,
        sublabel = pkg.ifEmpty { "Not configured" },
        icon     = icon
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (pkg.isNotEmpty()) {
                TextButton(onClick = onClear) {
                    Text("CLEAR", color = Color(0xFF666666), fontSize = 9.sp, letterSpacing = 1.sp)
                }
            }
            TextButton(onClick = onPick) {
                Text("PICK", color = accent, fontSize = 9.sp, letterSpacing = 1.sp)
            }
        }
    }
}

private fun fontDisplayName(font: AppFont): String = when (font) {
    AppFont.SYSTEM          -> "System"
    AppFont.JETBRAINS_MONO  -> "JetBrains Mono"
    AppFont.SOURCE_CODE_PRO -> "Source Code Pro"
}
