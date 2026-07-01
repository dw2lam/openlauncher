package com.openlauncher.app.ui.widget

import android.media.MediaMetadata
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlauncher.app.model.NowPlayingState
import com.openlauncher.app.service.MediaListenerService
import kotlin.math.abs
import kotlinx.coroutines.delay

@Composable
fun NowPlayingWidget(
    state: NowPlayingState?,
    accent: Color,
    carPlayPackage: String,
    androidAutoPackage: String,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onLaunchCarPlay: () -> Unit,
    onLaunchAndroidAuto: () -> Unit,
    onTapToOpenApp: () -> Unit,
    modifier: Modifier = Modifier,
    isEditing: Boolean = false,
    isDayMode: Boolean = false,
    hardwareRadio: com.openlauncher.app.viewmodel.LauncherViewModel.HardwareRadioState? = null,
    onLaunchHardwareRadio: () -> Unit = {},
    onStopHardwareRadio: () -> Unit = {},
    onRadioSeekUp: () -> Unit = {},
    onRadioSeekDown: () -> Unit = {},
    onRadioCycleFm: () -> Unit = {},
    onRadioSwitchAm: () -> Unit = {},
    onRadioTune: (band: String, freq: Float) -> Unit = { _, _ -> },
    onAssignRadio: () -> Unit = {}
) {
    val context     = LocalContext.current
    val isConnected by MediaListenerService.isConnected.collectAsState()
    val hasCarPlay  = carPlayPackage.isNotEmpty()
    val hasAutoApp  = androidAutoPackage.isNotEmpty()
    val hasContent  = state != null && state.title.isNotEmpty()

    var selectedSource by rememberSaveable { mutableStateOf("Any Player") }

    // Auto-switch to radio view when hardware radio is detected.
    // Keyed on presence (not the state object) so freq/RDS updates don't
    // keep forcing the radio view after the user picks "Any Player".
    val hasHardwareRadio = hardwareRadio != null
    LaunchedEffect(hasHardwareRadio) {
        if (hasHardwareRadio) selectedSource = "FM/AM Radio"
        else if (selectedSource == "FM/AM Radio") selectedSource = "Any Player"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(4.dp))
    ) {
        // 1. CONDITIONAL VIEW TOGGLE
        if (selectedSource == "FM/AM Radio") {
            // Real-tuner radio deck — mirrors the MCU or the radio app's session
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 8.dp)
            ) {
                RadioDeck(
                    accent = accent,
                    isDayMode = isDayMode,
                    hardwareRadio = hardwareRadio,
                    onLaunchHardwareRadio = onLaunchHardwareRadio,
                    onStopHardwareRadio = onStopHardwareRadio,
                    onRadioSeekUp = onRadioSeekUp,
                    onRadioSeekDown = onRadioSeekDown,
                    onRadioCycleFm = onRadioCycleFm,
                    onRadioSwitchAm = onRadioSwitchAm,
                    onRadioTune = onRadioTune,
                    onAssignRadio = onAssignRadio,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            // Standard Elegant Modern Media Player
            StandardMinimalPlayer(
                state = state,
                accent = accent,
                hasContent = hasContent,
                isEditing = isEditing,
                isDayMode = isDayMode,
                isConnected = isConnected,
                hasCarPlay = hasCarPlay,
                hasAutoApp = hasAutoApp,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onPrev = onPrev,
                onLaunchCarPlay = onLaunchCarPlay,
                onLaunchAndroidAuto = onLaunchAndroidAuto,
                onTapToOpenApp = onTapToOpenApp,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. FLOATING MULTI-SOURCE SELECTOR (Top-Right, always overlayed)
        var menuExpanded by remember { mutableStateOf(false) }
        val selectorIconColor = if (isDayMode) Color.Black.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
        
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 4.dp, top = 4.dp)
        ) {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Source Selector",
                    tint = selectorIconColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            val dropdownBg   = if (isDayMode) Color(0xFFF0F0F0) else MaterialTheme.colorScheme.background
            val dropdownText = if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(dropdownBg)
            ) {
                DropdownMenuItem(
                    text = { Text("Any Player", color = dropdownText, fontSize = 11.sp) },
                    onClick = {
                        selectedSource = "Any Player"
                        menuExpanded = false
                    },
                    leadingIcon = { Icon(Icons.Default.MusicNote, null, tint = accent, modifier = Modifier.size(14.dp)) }
                )
                DropdownMenuItem(
                    text = { Text("FM/AM Radio", color = dropdownText, fontSize = 11.sp) },
                    onClick = {
                        selectedSource = "FM/AM Radio"
                        menuExpanded = false
                    },
                    leadingIcon = { Icon(Icons.Default.Radio, null, tint = accent, modifier = Modifier.size(14.dp)) }
                )
            }
        }
    }
}


/**
 * Radio deck backed by a REAL tuner only — either the vendor MCU (full control,
 * canTune = true) or the radio app's MediaSession (seek/open/stop, read-only
 * band). No simulated static, no demo stations: when no source exists the deck
 * says so and offers to assign the unit's radio app.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RadioDeck(
    accent: Color,
    isDayMode: Boolean,
    hardwareRadio: com.openlauncher.app.viewmodel.LauncherViewModel.HardwareRadioState?,
    onLaunchHardwareRadio: () -> Unit,
    onStopHardwareRadio: () -> Unit,
    onRadioSeekUp: () -> Unit,
    onRadioSeekDown: () -> Unit,
    onRadioCycleFm: () -> Unit,
    onRadioSwitchAm: () -> Unit,
    onRadioTune: (band: String, freq: Float) -> Unit,
    onAssignRadio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val dimColor     = if (isDayMode) Color(0xFF444444) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
    val borderColor  = if (isDayMode) Color(0xFF777777) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)

    if (hardwareRadio == null) {
        // No real tuner detected — be honest about it instead of simulating one
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Radio, null, tint = dimColor, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                "NO RADIO SOURCE",
                color = contentColor.copy(alpha = 0.85f),
                fontSize = 9.sp, fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "Start your head unit's radio app — or assign it below so Open Launcher can mirror and control it",
                color = dimColor, fontSize = 7.sp, fontFamily = FontFamily.Monospace,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 10.sp,
                modifier = Modifier.padding(horizontal = 28.dp)
            )
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .height(26.dp)
                    .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                    .clip(RoundedCornerShape(3.dp))
                    .clickable { onAssignRadio() }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "ASSIGN RADIO APP",
                    color = accent, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                )
            }
        }
        return
    }

    // Power is a local veil over the live source: stopping doesn't always clear
    // the vendor state, so a fresh emission flips it back on (radio re-engaged)
    var isManuallyOff by remember { mutableStateOf(false) }
    LaunchedEffect(hardwareRadio) {
        if (isManuallyOff) isManuallyOff = false
    }
    val powerOn = !isManuallyOff

    val displayBand = hardwareRadio.band.uppercase()
    val freqClean   = hardwareRadio.freq.replace(Regex("[^0-9.]"), "")
    val freqFloat   = freqClean.toFloatOrNull()
    val displayFreq = freqClean.ifEmpty { hardwareRadio.freq }
    val displayUnit = if (hardwareRadio.isAm) "kHz" else "MHz"

    val chipInactiveBg = if (isDayMode) Color(0xFFE0E0E0) else Color(0xFF1A1A1A)
    val chipActiveBg   = if (isDayMode) Color(0xFF222222) else Color(0xFFDDDDDD)
    val chipActiveText = if (isDayMode) Color.White else Color(0xFF111111)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // ── Row 1: band (chips when switchable, read-only chip otherwise) + power
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hardwareRadio.canTune) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("FM1", "FM2", "FM3", "AM").forEach { b ->
                        val active = displayBand == b
                        Box(
                            modifier = Modifier
                                .height(22.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (active) chipActiveBg else chipInactiveBg)
                                .border(1.dp, if (active) borderColor else borderColor.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                                .clickable { if (b == "AM") onRadioSwitchAm() else onRadioCycleFm() }
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                b,
                                color = if (active) chipActiveText else dimColor,
                                fontSize = 8.sp, fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            } else {
                // Session-mirrored tuner: band is whatever the radio app reports
                Box(
                    modifier = Modifier
                        .height(22.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(chipActiveBg)
                        .border(1.dp, borderColor, RoundedCornerShape(3.dp))
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        displayBand,
                        color = chipActiveText,
                        fontSize = 8.sp, fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (powerOn) chipActiveBg else chipInactiveBg)
                    .border(1.5.dp, borderColor, CircleShape)
                    .clickable {
                        if (powerOn) { onStopHardwareRadio(); isManuallyOff = true }
                        else { onLaunchHardwareRadio(); isManuallyOff = false }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PowerSettingsNew, "PWR",
                    tint = if (powerOn) chipActiveText else dimColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // ── Row 2: live frequency + station/RDS line ──────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    displayFreq,
                    color = if (powerOn) contentColor else contentColor.copy(alpha = 0.3f),
                    fontSize = 34.sp, fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.Monospace, letterSpacing = 0.sp
                )
                Text(
                    displayUnit,
                    color = dimColor, fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 5.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = (if (!powerOn) "RADIO OFF" else hardwareRadio.stationName ?: "LIVE").uppercase(),
                    color = if (powerOn) accent else dimColor.copy(alpha = 0.5f),
                    fontSize = 8.sp, fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (powerOn) {
                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(accent))
                }
            }
        }

        // ── Row 3: seek + open ────────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RadioFlatButton(
                label = "◄ SEEK", enabled = powerOn, active = false,
                accent = accent, borderColor = borderColor, dimColor = dimColor,
                modifier = Modifier.weight(1f),
                onClick = onRadioSeekDown
            )
            RadioFlatButton(
                label = "OPEN", enabled = true, active = false,
                accent = accent, borderColor = borderColor, dimColor = dimColor,
                modifier = Modifier.weight(1f),
                onClick = onLaunchHardwareRadio
            )
            RadioFlatButton(
                label = "SEEK ►", enabled = powerOn, active = false,
                accent = accent, borderColor = borderColor, dimColor = dimColor,
                modifier = Modifier.weight(1f),
                onClick = onRadioSeekUp
            )
        }

        // ── Presets: only when the backend supports direct frequency tuning ──
        if (hardwareRadio.canTune) {
            var fmPresets by rememberSaveable { mutableStateOf(listOf(88.5f, 91.5f, 98.1f, 101.9f, 104.3f, 107.5f)) }
            var amPresets by rememberSaveable { mutableStateOf(listOf(540f, 680f, 820f, 1040f, 1260f, 1420f)) }
            val isFm           = !hardwareRadio.isAm
            val currentPresets = if (isFm) fmPresets else amPresets
            val tolerance      = if (isFm) 0.15f else 5f

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (pIdx in 0 until 6) {
                    val presetFreq = currentPresets[pIdx]
                    val isTuned    = freqFloat != null && abs(freqFloat - presetFreq) < tolerance
                    val presetBg = when {
                        isTuned && isDayMode -> Color(0xFF222222)
                        isTuned              -> accent.copy(alpha = 0.12f)
                        else                 -> Color.Transparent
                    }
                    val presetBorderColor = when {
                        isTuned && isDayMode -> Color(0xFF222222)
                        isTuned              -> accent
                        isDayMode            -> Color(0xFFCCCCCC)
                        else                 -> Color(0xFF1D2024)
                    }
                    val presetNumColor = when {
                        isTuned && isDayMode -> Color.White
                        isTuned              -> accent
                        isDayMode            -> Color(0xFF444444)
                        else                 -> Color(0xFF777777)
                    }
                    val presetFreqColor = when {
                        isTuned && isDayMode -> Color.White.copy(alpha = 0.9f)
                        isTuned              -> accent.copy(alpha = 0.9f)
                        isDayMode            -> Color(0xFF666666)
                        else                 -> Color(0xFF777777).copy(alpha = 0.7f)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .border(1.dp, presetBorderColor, RoundedCornerShape(2.dp))
                            .clip(RoundedCornerShape(2.dp))
                            .background(presetBg)
                            .combinedClickable(
                                enabled = powerOn,
                                onClick = { onRadioTune(displayBand, presetFreq) },
                                onLongClick = {
                                    // Store the REAL current frequency on this preset
                                    freqFloat?.let { f ->
                                        if (isFm) fmPresets = fmPresets.toMutableList().also { it[pIdx] = f }
                                        else      amPresets = amPresets.toMutableList().also { it[pIdx] = f }
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text("${pIdx + 1}", color = presetNumColor, fontSize = 7.sp,
                                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text(
                                if (isFm) "%.1f".format(presetFreq) else "%.0f".format(presetFreq),
                                color = presetFreqColor, fontSize = 6.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RadioFlatButton(
    label: String,
    enabled: Boolean,
    active: Boolean,
    accent: Color,
    borderColor: Color,
    dimColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(28.dp)
            .border(1.dp, if (active) accent else borderColor, RoundedCornerShape(3.dp))
            .clip(RoundedCornerShape(3.dp))
            .background(if (active) accent.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = when {
                !enabled -> dimColor.copy(alpha = 0.35f)
                active   -> accent
                else     -> dimColor
            },
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
private fun StandardMinimalPlayer(
    state: NowPlayingState?,
    accent: Color,
    hasContent: Boolean,
    isEditing: Boolean,
    isDayMode: Boolean,
    isConnected: Boolean,
    hasCarPlay: Boolean,
    hasAutoApp: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onLaunchCarPlay: () -> Unit,
    onLaunchAndroidAuto: () -> Unit,
    onTapToOpenApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // UI Theme colors
    val idleIconColor = if (isDayMode) Color(0xFF555555) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.30f)
    val idleTextColor = if (isDayMode) Color(0xFF555555) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.30f)
    val contentTextColor = if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val subTextColor = if (isDayMode) Color(0xFF666666) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.30f)

    Box(modifier = modifier) {
        if (!hasContent) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (hasCarPlay || hasAutoApp) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (hasCarPlay) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .let { if (!isEditing) it.clickable { onLaunchCarPlay() } else it }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.PhoneAndroid, null, tint = accent.copy(alpha = 0.7f), modifier = Modifier.size(28.dp))
                                    Text("CARPLAY", color = accent.copy(alpha = 0.6f), fontSize = 8.sp, letterSpacing = 2.sp)
                                }
                            }
                        }
                        if (hasCarPlay && hasAutoApp) {
                            androidx.compose.material3.VerticalDivider(
                                modifier = Modifier.fillMaxHeight().padding(vertical = 16.dp),
                                color = if (isDayMode) Color(0xFFBBBBBB) else Color(0xFF1E1E1E)
                            )
                        }
                        if (hasAutoApp) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .let { if (!isEditing) it.clickable { onLaunchAndroidAuto() } else it }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.DirectionsCar, null, tint = accent.copy(alpha = 0.7f), modifier = Modifier.size(28.dp))
                                    Text("ANDROID AUTO", color = accent.copy(alpha = 0.6f), fontSize = 8.sp, letterSpacing = 2.sp)
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.MusicNote, null, tint = idleIconColor, modifier = Modifier.size(24.dp))
                        Text("NO MEDIA PLAYING", color = idleTextColor, fontSize = 7.sp, letterSpacing = 1.sp)
                    }
                }
            }
        } else {
            // Non-null playing track state
            val nonNullState = state!!
            var positionMs by remember { mutableLongStateOf(nonNullState.controller?.playbackState?.position ?: 0L) }
            val durationMs = nonNullState.controller?.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L

            LaunchedEffect(nonNullState.isPlaying, nonNullState.title) {
                while (nonNullState.isPlaying) {
                    positionMs = nonNullState.controller?.playbackState?.position ?: positionMs
                    delay(500)
                }
            }

            // Draw Album Art as background with smooth blur overlay if present
            val hasAlbumArt = nonNullState.albumArt != null
            val useDarkTheme = hasAlbumArt || !isDayMode

            val currentTextColor = if (hasAlbumArt) Color.White else if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
            val currentSubTextColor = if (hasAlbumArt) Color.White.copy(alpha = 0.6f) else if (isDayMode) Color(0xFF666666) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            val currentProgressColor = if (useDarkTheme) accent else if (isDayMode) Color(0xFF111111) else accent
            val currentProgressTrack = currentTextColor.copy(alpha = 0.15f)
            val currentIconColor = currentTextColor.copy(alpha = 0.75f)
            val currentPlayBgColor = if (useDarkTheme) accent.copy(alpha = 0.9f) else if (isDayMode) Color(0xFF111111) else accent.copy(alpha = 0.9f)
            val currentPlayIconColor = if (useDarkTheme) Color.Black else Color.White

            if (hasAlbumArt) {
                // Prefer the full-resolution art URI when the source app provides
                // one — the metadata bitmap is often a downscaled notification
                // thumbnail that looks soft stretched across the widget. Falls back
                // to the bitmap if the URI fails to load, and renders with high
                // filter quality so upscaling stays smooth either way.
                coil.compose.AsyncImage(
                    model = nonNullState.artUri ?: nonNullState.albumArt,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    filterQuality = androidx.compose.ui.graphics.FilterQuality.High,
                    error = nonNullState.albumArt?.let {
                        androidx.compose.ui.graphics.painter.BitmapPainter(it.asImageBitmap())
                    },
                    modifier = Modifier.fillMaxSize()
                )
                // 25% dimming layer overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f))
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Track info (top — clickable to open app)
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .let { if (!isEditing) it.clickable { onTapToOpenApp() } else it }
                ) {
                    Text(
                        text = nonNullState.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = currentTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp
                    )
                    Text(
                        text = nonNullState.artist.ifEmpty { "Unknown" },
                        style = MaterialTheme.typography.bodySmall,
                        color = currentSubTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 11.sp
                    )
                }

                // Progress + controls (bottom)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (durationMs > 0) {
                        LinearProgressIndicator(
                            progress = { (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            color = currentProgressColor,
                            trackColor = currentProgressTrack
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatMs(positionMs), style = MaterialTheme.typography.labelSmall, color = currentSubTextColor.copy(alpha = 0.75f), fontSize = 9.sp)
                            Text(formatMs(durationMs), style = MaterialTheme.typography.labelSmall, color = currentSubTextColor.copy(alpha = 0.75f), fontSize = 9.sp)
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = { if (!isEditing) onPrev() }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.SkipPrevious, "Prev", tint = currentIconColor, modifier = Modifier.size(20.dp))
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(currentPlayBgColor)
                        ) {
                            IconButton(onClick = { if (!isEditing) onPlayPause() }, modifier = Modifier.size(42.dp)) {
                                Icon(
                                    imageVector = if (nonNullState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (nonNullState.isPlaying) "Pause" else "Play",
                                    tint = currentPlayIconColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        IconButton(onClick = { if (!isEditing) onNext() }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.SkipNext, "Next", tint = currentIconColor, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

