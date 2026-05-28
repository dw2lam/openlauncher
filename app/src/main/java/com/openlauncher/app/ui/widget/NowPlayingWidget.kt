package com.openlauncher.app.ui.widget

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaMetadata
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlauncher.app.model.NowPlayingState
import com.openlauncher.app.service.MediaListenerService
import java.util.Random
import kotlin.math.abs
import kotlin.math.sin
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
    isDayMode: Boolean = false
) {
    val context     = LocalContext.current
    val isConnected by MediaListenerService.isConnected.collectAsState()
    val hasCarPlay  = carPlayPackage.isNotEmpty()
    val hasAutoApp  = androidAutoPackage.isNotEmpty()
    val hasContent  = state != null && state.title.isNotEmpty()

    var selectedSource by rememberSaveable { mutableStateOf("Any Player") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(4.dp))
    ) {
        // 1. CONDITIONAL VIEW TOGGLE
        if (selectedSource == "FM/AM Radio") {
            // Retro Hardware Radio Deck Console
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 8.dp)
            ) {
                PioneerDEHP7600MPDeck(
                    state = state,
                    accent = accent,
                    selectedSource = selectedSource,
                    onSourceChange = { selectedSource = it },
                    hasContent = hasContent,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onPrev = onPrev,
                    onLaunchCarPlay = onLaunchCarPlay,
                    onLaunchAndroidAuto = onLaunchAndroidAuto,
                    onTapToOpenApp = onTapToOpenApp,
                    isEditing = isEditing,
                    isDayMode = isDayMode,
                    isConnected = isConnected,
                    hasCarPlay = hasCarPlay,
                    hasAutoApp = hasAutoApp,
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
        val selectorIconColor = if (isDayMode) Color.Black.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.4f)
        
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
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(Color(0xFF141414))
            ) {
                DropdownMenuItem(
                    text = { Text("Any Player", color = Color.White, fontSize = 11.sp) },
                    onClick = {
                        selectedSource = "Any Player"
                        menuExpanded = false
                    },
                    leadingIcon = { Icon(Icons.Default.MusicNote, null, tint = accent, modifier = Modifier.size(14.dp)) }
                )
                DropdownMenuItem(
                    text = { Text("FM/AM Radio", color = Color.White, fontSize = 11.sp) },
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PioneerDEHP7600MPDeck(
    state: NowPlayingState?,
    accent: Color,
    selectedSource: String,
    onSourceChange: (String) -> Unit,
    hasContent: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onLaunchCarPlay: () -> Unit,
    onLaunchAndroidAuto: () -> Unit,
    onTapToOpenApp: () -> Unit,
    isEditing: Boolean,
    isDayMode: Boolean,
    isConnected: Boolean,
    hasCarPlay: Boolean,
    hasAutoApp: Boolean,
    modifier: Modifier = Modifier
) {
    var isPowerOn by rememberSaveable { mutableStateOf(true) }
    var isMuted by rememberSaveable { mutableStateOf(false) }
    var band by rememberSaveable { mutableStateOf("FM1") }
    var radioFreq by rememberSaveable { mutableStateOf(99.9f) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekDirection by remember { mutableStateOf(1) }

    var fmPresets by rememberSaveable { mutableStateOf(listOf(88.5f, 91.5f, 98.1f, 101.9f, 104.3f, 107.5f)) }
    var amPresets by rememberSaveable { mutableStateOf(listOf(540f, 680f, 820f, 1040f, 1260f, 1420f)) }

    val fmStations = listOf(88.5f, 91.5f, 98.1f, 101.9f, 104.3f, 107.5f)
    val amStations = listOf(540f, 680f, 820f, 1040f, 1260f, 1420f)

    val activeStations  = if (band.startsWith("FM")) fmStations else amStations
    val currentPresets  = if (band.startsWith("FM")) fmPresets  else amPresets
    val fmTolerance     = if (band.startsWith("FM")) 0.15f else 5f
    val isActiveStation = activeStations.any { abs(radioFreq - it) < fmTolerance }

    val synth = remember { CyberRadioSynth() }
    DisposableEffect(isPowerOn, radioFreq, isActiveStation, isMuted, selectedSource) {
        if (isPowerOn && selectedSource == "FM/AM Radio") {
            synth.setParams(radioFreq, isActiveStation, isPowerOn, isMuted)
            synth.start()
        } else {
            synth.stop()
        }
        onDispose { synth.stop() }
    }

    LaunchedEffect(isSeeking) {
        if (!isSeeking) return@LaunchedEffect
        while (isSeeking) {
            delay(70)
            if (band.startsWith("FM")) {
                radioFreq += 0.2f * seekDirection
                if (radioFreq > 108.0f) radioFreq = 87.5f
                if (radioFreq < 87.5f)  radioFreq = 108.0f
                val hit = activeStations.find { abs(radioFreq - it) < 0.15f }
                if (hit != null) { radioFreq = hit; isSeeking = false }
            } else {
                radioFreq += 10f * seekDirection
                if (radioFreq > 1700f) radioFreq = 530f
                if (radioFreq < 530f)  radioFreq = 1700f
                val hit = activeStations.find { abs(radioFreq - it) < 5f }
                if (hit != null) { radioFreq = hit; isSeeking = false }
            }
        }
    }

    val stationName = if (!isPowerOn || isMuted) "" else if (isActiveStation) {
        when {
            band.startsWith("FM") -> when {
                abs(radioFreq - 88.5f)  < 0.15f -> "NIGHTDRIVE FM"
                abs(radioFreq - 91.5f)  < 0.15f -> "NEON RETRO 91.5"
                abs(radioFreq - 98.1f)  < 0.15f -> "RECEIVER VFD-8"
                abs(radioFreq - 101.9f) < 0.15f -> "CYBERPUNK RADIO"
                abs(radioFreq - 104.3f) < 0.15f -> "SYNTHWAVE CHILL"
                abs(radioFreq - 107.5f) < 0.15f -> "RADICAL FM"
                else -> "STATION"
            }
            else -> when {
                abs(radioFreq - 540f)  < 5f -> "NEWS 540"
                abs(radioFreq - 680f)  < 5f -> "TRAFFIC RADAR"
                abs(radioFreq - 820f)  < 5f -> "COMMUTER TALK"
                abs(radioFreq - 1040f) < 5f -> "SPORT CLASH"
                abs(radioFreq - 1260f) < 5f -> "WEATHER ADVISORY"
                abs(radioFreq - 1420f) < 5f -> "GOLD RETRO"
                else -> "STATION"
            }
        }
    } else if (isSeeking) "SCANNING" else "NO SIGNAL"

    val contentColor = if (isDayMode) Color(0xFF111111) else Color.White
    val dimColor     = if (isDayMode) Color(0xFF888888) else Color(0xFF555555)
    val borderColor  = if (isDayMode) Color(0xFFE5E7EB) else Color(0xFF1D2024)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // ── Header: band chips + stereo dot + power ───────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("FM1", "FM2", "AM").forEach { b ->
                    val active = band == b && isPowerOn
                    Box(
                        modifier = Modifier
                            .height(16.dp)
                            .border(
                                1.dp,
                                if (active) accent else borderColor,
                                RoundedCornerShape(2.dp)
                            )
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (active) accent.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable {
                                band = b
                                radioFreq = if (b.startsWith("FM")) 99.9f else 1040f
                                isSeeking = false
                                isMuted = false
                            }
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            b,
                            color = if (active) accent else dimColor,
                            fontSize = 7.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isPowerOn && isActiveStation && !isMuted) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            "ST",
                            color = dimColor,
                            fontSize = 7.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(accent)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(if (!isPowerOn) accent.copy(alpha = 0.12f) else Color.Transparent)
                        .border(1.dp, if (!isPowerOn) accent else borderColor, CircleShape)
                        .clickable { isPowerOn = !isPowerOn; if (!isPowerOn) isSeeking = false },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PowerSettingsNew,
                        contentDescription = "PWR",
                        tint = if (!isPowerOn) accent else dimColor,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }

        // ── Frequency display ─────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val freqText = if (band.startsWith("FM")) "%.1f".format(radioFreq)
                               else "%.0f".format(radioFreq)
                val unitText = if (band.startsWith("FM")) "MHz" else "kHz"
                Text(
                    freqText,
                    color = if (isPowerOn && !isMuted) contentColor else contentColor.copy(alpha = 0.25f),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.sp
                )
                Text(
                    unitText,
                    color = dimColor,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 5.dp)
                )
            }
            Text(
                text = when {
                    !isPowerOn -> "RADIO OFF"
                    isMuted    -> "MUTED"
                    else       -> stationName
                }.uppercase(),
                color = when {
                    !isPowerOn -> dimColor.copy(alpha = 0.5f)
                    isMuted    -> accent.copy(alpha = 0.6f)
                    isActiveStation && !isSeeking -> accent
                    else -> dimColor
                },
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // ── Controls: SEEK ◄ | MUT | SEEK ► ──────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioFlatButton(
                label = "◄ SEEK",
                enabled = isPowerOn,
                active = isSeeking && seekDirection == -1,
                accent = accent,
                borderColor = borderColor,
                dimColor = dimColor,
                modifier = Modifier.weight(1f),
                onClick = {
                    if (isSeeking && seekDirection == -1) {
                        isSeeking = false
                    } else {
                        seekDirection = -1
                        isSeeking = true
                        isMuted = false
                    }
                }
            )
            RadioFlatButton(
                label = "MUT",
                enabled = isPowerOn,
                active = isMuted,
                accent = accent,
                borderColor = borderColor,
                dimColor = dimColor,
                modifier = Modifier.weight(1f),
                onClick = { isMuted = !isMuted; if (isMuted) isSeeking = false }
            )
            RadioFlatButton(
                label = "SEEK ►",
                enabled = isPowerOn,
                active = isSeeking && seekDirection == 1,
                accent = accent,
                borderColor = borderColor,
                dimColor = dimColor,
                modifier = Modifier.weight(1f),
                onClick = {
                    if (isSeeking && seekDirection == 1) {
                        isSeeking = false
                    } else {
                        seekDirection = 1
                        isSeeking = true
                        isMuted = false
                    }
                }
            )
        }

        // ── Preset buttons ────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (pIdx in 0 until 6) {
                val presetFreq   = currentPresets[pIdx]
                val isTuned      = isPowerOn && !isSeeking && abs(radioFreq - presetFreq) < fmTolerance
                val displayFreq  = if (band.startsWith("FM")) "%.1f".format(presetFreq)
                                   else "%.0f".format(presetFreq / 10f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .border(1.dp, if (isTuned) accent else borderColor, RoundedCornerShape(2.dp))
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isTuned) accent.copy(alpha = 0.12f) else Color.Transparent)
                        .combinedClickable(
                            enabled = isPowerOn,
                            onClick = { radioFreq = presetFreq; isMuted = false; isSeeking = false },
                            onLongClick = {
                                if (band.startsWith("FM"))
                                    fmPresets = fmPresets.toMutableList().also { it[pIdx] = radioFreq }
                                else
                                    amPresets = amPresets.toMutableList().also { it[pIdx] = radioFreq }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            "${pIdx + 1}",
                            color = if (isTuned) accent else dimColor,
                            fontSize = 6.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            displayFreq,
                            color = if (isTuned) accent.copy(alpha = 0.8f) else dimColor.copy(alpha = 0.6f),
                            fontSize = 5.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
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
            .height(22.dp)
            .border(1.dp, if (active) accent else borderColor, RoundedCornerShape(2.dp))
            .clip(RoundedCornerShape(2.dp))
            .background(if (active) accent.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = when {
                !enabled -> dimColor.copy(alpha = 0.3f)
                active   -> accent
                else     -> dimColor
            },
            fontSize = 7.sp,
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
    val idleIconColor = if (isDayMode) Color(0xFF888888) else Color.White.copy(alpha = 0.30f)
    val idleTextColor = if (isDayMode) Color(0xFF888888) else Color.White.copy(alpha = 0.30f)
    val contentTextColor = if (isDayMode) Color(0xFF111111) else Color.White
    val subTextColor = if (isDayMode) Color(0xFF888888) else Color.White.copy(alpha = 0.30f)

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
                                color = if (isDayMode) Color(0xFFE5E7EB) else Color(0xFF1E1E1E)
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
            if (nonNullState.albumArt != null) {
                Image(
                    bitmap = nonNullState.albumArt.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    alpha = 0.12f // delicate ambient background highlight
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
                        color = contentTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp
                    )
                    Text(
                        text = nonNullState.artist.ifEmpty { "Unknown" },
                        style = MaterialTheme.typography.bodySmall,
                        color = subTextColor,
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
                            color = accent,
                            trackColor = contentTextColor.copy(alpha = 0.15f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatMs(positionMs), style = MaterialTheme.typography.labelSmall, color = subTextColor.copy(alpha = 0.75f), fontSize = 9.sp)
                            Text(formatMs(durationMs), style = MaterialTheme.typography.labelSmall, color = subTextColor.copy(alpha = 0.75f), fontSize = 9.sp)
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = { if (!isEditing) onPrev() }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.SkipPrevious, "Prev", tint = contentTextColor.copy(alpha = 0.75f), modifier = Modifier.size(20.dp))
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(accent.copy(alpha = 0.9f))
                        ) {
                            IconButton(onClick = { if (!isEditing) onPlayPause() }, modifier = Modifier.size(42.dp)) {
                                Icon(
                                    imageVector = if (nonNullState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (nonNullState.isPlaying) "Pause" else "Play",
                                    tint = if (isDayMode) Color.White else Color.Black,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        IconButton(onClick = { if (!isEditing) onNext() }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.SkipNext, "Next", tint = contentTextColor.copy(alpha = 0.75f), modifier = Modifier.size(20.dp))
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

// ==========================================================
// PIONEER FM/AM RADIO HARDWARE SYNTHESIZER SIMULATOR CLASS
// ==========================================================

class CyberRadioSynth {
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private var thread: Thread? = null
    private var currentFreq = 99.9f
    private var activeState = false
    private var powerState = true
    private var pausedState = false

    fun setParams(freq: Float, isActive: Boolean, power: Boolean, paused: Boolean) {
        currentFreq = freq
        activeState = isActive
        powerState = power
        pausedState = paused
    }

    fun start() {
        if (isPlaying) return
        isPlaying = true
        val sampleRate = 22050
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        try {
            audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize,
                AudioTrack.MODE_STREAM
            )
            audioTrack?.play()
        } catch (_: Exception) {
            return
        }

        thread = Thread {
            val buffer = ShortArray(1024)
            val random = Random()
            var phase = 0.0
            
            while (isPlaying) {
                val power = powerState
                val active = activeState
                val freq = currentFreq
                val paused = pausedState
                
                for (i in buffer.indices) {
                    if (!power || paused) {
                        buffer[i] = 0
                    } else if (active) {
                        // Cyberpunk low-pitched warm synthesizer chord hum
                        val baseHz = 110.0 + (freq.toInt() % 12) * 8.0
                        val toneVal = sin(phase) * 5500.0 + sin(phase * 1.5) * 2500.0
                        val noiseVal = random.nextGaussian() * 600.0 // slight atmospheric analog crackle
                        buffer[i] = (toneVal + noiseVal).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                        
                        phase += (2.0 * Math.PI * baseHz) / sampleRate
                        if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI
                    } else {
                        // Realistic analogue white noise radio static
                        buffer[i] = (random.nextGaussian() * 2500.0).toInt().toShort()
                    }
                }
                try {
                    audioTrack?.write(buffer, 0, buffer.size)
                } catch (_: Exception) {
                    break
                }
            }
        }
        thread?.start()
    }

    fun stop() {
        isPlaying = false
        try {
            thread?.join(150)
        } catch (_: Exception) {}
        thread = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}
