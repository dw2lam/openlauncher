package com.openlauncher.app.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.openlauncher.app.data.AppSettings
import com.openlauncher.app.data.GRID_COLS
import com.openlauncher.app.data.GRID_ROWS
import com.openlauncher.app.data.WidgetConfig
import com.openlauncher.app.model.NowPlayingState
import com.openlauncher.app.model.WeatherState
import com.openlauncher.app.ui.widget.*
import java.util.Calendar
import com.openlauncher.app.util.LocationData

private val WIDGET_RADIUS = RoundedCornerShape(0.dp)

@Composable
fun HomeScreen(
    settings: AppSettings,
    weather: WeatherState?,
    nowPlaying: NowPlayingState?,
    location: LocationData?,
    bearing: Float,
    isWifi: Boolean,
    isData: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onLaunchCarPlay: () -> Unit,
    onLaunchAndroidAuto: () -> Unit,
    onAssignCarPlay: () -> Unit,
    onAssignAndroidAuto: () -> Unit,
    onTapNowPlaying: () -> Unit,
    onUpdateWidget: (id: String, spanX: Int, spanY: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent      = Color(settings.accentColor)
    val gap         = 6.dp
    val hasWallpaper = settings.wallpaperUri.isNotEmpty()
    val widgetBg     = if (hasWallpaper) Color(0xCC000000) else Color(0xFF0B0B0B)
    val widgetBorder = if (hasWallpaper) Color(0x22FFFFFF)  else Color(0xFF1A1A1A)

    var resizingId    by remember { mutableStateOf<String?>(null) }
    var contextMenuId by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize()) {

        // ── Header ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text          = settings.vehicleName.uppercase(),
                style         = MaterialTheme.typography.titleLarge,
                color         = accent,
                letterSpacing = 3.sp,
                fontSize      = 14.sp
            )
            Spacer(Modifier.weight(1f))
            AnimatedVisibility(visible = isWifi, enter = fadeIn(), exit = fadeOut()) {
                Icon(Icons.Default.Wifi, "WiFi", tint = Color(0xFF666666), modifier = Modifier.size(16.dp))
            }
            if (isWifi) Spacer(Modifier.width(6.dp))
            AnimatedVisibility(visible = isData, enter = fadeIn(), exit = fadeOut()) {
                Icon(Icons.Default.SignalCellularAlt, "Data", tint = Color(0xFF666666), modifier = Modifier.size(16.dp))
            }
        }

        HorizontalDivider(color = Color(0xFF141414))

        // ── Widget Grid ─────────────────────────────────────────────────────
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(gap)
        ) {
            val cellW = (maxWidth  - gap * (GRID_COLS - 1)) / GRID_COLS
            val cellH = (maxHeight - gap * (GRID_ROWS - 1)) / GRID_ROWS

            val visibleIds = buildSet {
                if (settings.showClock) add("CLOCK")
                if (settings.showWeather && weather != null) add("WEATHER")
                if (settings.showNowPlaying) add("NOW_PLAYING")
                if (settings.showTelemetry) add("TELEMETRY")
            }

            // Keep only visible widgets, then auto-expand each widget's spanX to fill
            // any empty columns immediately to its right in the same row band.
            val visible = settings.widgetLayout.filter { it.enabled && it.id in visibleIds }
            val rendered = visible.map { w ->
                var spanX = w.spanX
                outer@ for (col in (w.gridX + w.spanX) until GRID_COLS) {
                    for (other in visible) {
                        if (other.id == w.id) continue
                        val colOverlap = col >= other.gridX && col < other.gridX + other.spanX
                        val rowOverlap = w.gridY < other.gridY + other.spanY &&
                                         w.gridY + w.spanY > other.gridY
                        if (colOverlap && rowOverlap) break@outer
                    }
                    spanX++
                }
                w.copy(spanX = spanX)
            }

            rendered.forEach { w ->
                val xOff   = (cellW + gap) * w.gridX
                val yOff   = (cellH + gap) * w.gridY
                val width  = cellW * w.spanX + gap * (w.spanX - 1)
                val height = cellH * w.spanY + gap * (w.spanY - 1)

                val label = when (w.id) {
                    "CLOCK"       -> clockTimeLabel(Calendar.getInstance())
                    "WEATHER"     -> "WEATHER"
                    "NOW_PLAYING" -> "NOW PLAYING"
                    "TELEMETRY"   -> "COMPASS"
                    else          -> w.id
                }

                @OptIn(ExperimentalFoundationApi::class)
                Box(
                    modifier = Modifier
                        .absoluteOffset(x = xOff, y = yOff)
                        .size(width, height)
                        .clip(WIDGET_RADIUS)
                        .background(widgetBg)
                        .border(1.dp, widgetBorder, WIDGET_RADIUS)
                        .combinedClickable(
                            indication       = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick          = {},
                            onLongClick      = { contextMenuId = w.id }
                        )
                ) {
                    when (w.id) {
                        "CLOCK" -> ClockWidget(
                            style    = settings.clockStyle,
                            accent   = accent,
                            modifier = Modifier.fillMaxSize()
                        )
                        "WEATHER" -> WeatherWidget(
                            state    = weather,
                            accent   = accent,
                            metric   = settings.unitSystem.name == "METRIC",
                            modifier = Modifier.fillMaxSize()
                        )
                        "NOW_PLAYING" -> NowPlayingWidget(
                            state               = nowPlaying,
                            accent              = accent,
                            carPlayPackage      = settings.carPlayPackage,
                            androidAutoPackage  = settings.androidAutoPackage,
                            onPlayPause         = onPlayPause,
                            onNext              = onNext,
                            onPrev              = onPrev,
                            onLaunchCarPlay     = onLaunchCarPlay,
                            onLaunchAndroidAuto = onLaunchAndroidAuto,
                            onTapToOpenApp      = onTapNowPlaying,
                            modifier            = Modifier.fillMaxSize()
                        )
                        "TELEMETRY" -> TelemetryWidget(
                            location = location,
                            bearing  = bearing,
                            accent   = accent,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Label — bottom-left of header row
                    Text(
                        text          = label,
                        style         = MaterialTheme.typography.labelSmall,
                        color         = Color(0xFF3A3A3A),
                        letterSpacing = 2.sp,
                        fontSize      = 8.sp,
                        modifier      = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 10.dp, top = 7.dp)
                    )

                }
            }
        }
    }

    // ── Widget context menu (long-press any cell) ────────────────────────────
    contextMenuId?.let { id ->
        WidgetContextMenu(
            widgetId            = id,
            accent              = accent,
            onResize            = { contextMenuId = null; resizingId = id },
            onAssignCarPlay     = { contextMenuId = null; onAssignCarPlay() },
            onAssignAndroidAuto = { contextMenuId = null; onAssignAndroidAuto() },
            onDismiss           = { contextMenuId = null }
        )
    }

    // ── Resize dialog ────────────────────────────────────────────────────────
    resizingId?.let { id ->
        val config = settings.widgetLayout.find { it.id == id }
        if (config != null) {
            WidgetResizeDialog(
                config    = config,
                accent    = accent,
                onDismiss = { resizingId = null },
                onConfirm = { sx, sy ->
                    onUpdateWidget(id, sx, sy)
                    resizingId = null
                }
            )
        }
    }
}

@Composable
private fun WidgetContextMenu(
    widgetId: String,
    accent: Color,
    onResize: () -> Unit,
    onAssignCarPlay: () -> Unit,
    onAssignAndroidAuto: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF111111))
                .border(1.dp, Color(0xFF1E1E1E), RoundedCornerShape(4.dp))
                .padding(vertical = 4.dp)
                .width(200.dp)
        ) {
            ContextRow("RESIZE", Icons.Default.OpenWith, accent, onResize)
            if (widgetId == "NOW_PLAYING") {
                HorizontalDivider(color = Color(0xFF1A1A1A))
                ContextRow("ASSIGN CARPLAY APP",      Icons.Default.PhoneAndroid,  accent, onAssignCarPlay)
                HorizontalDivider(color = Color(0xFF1A1A1A))
                ContextRow("ASSIGN ANDROID AUTO APP", Icons.Default.DirectionsCar, accent, onAssignAndroidAuto)
            }
        }
    }
}

@Composable
private fun ContextRow(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        Text(label, color = tint, fontSize = 10.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun WidgetResizeDialog(
    config: WidgetConfig,
    accent: Color,
    onDismiss: () -> Unit,
    onConfirm: (spanX: Int, spanY: Int) -> Unit
) {
    var spanX by remember { mutableStateOf(config.spanX) }
    var spanY by remember { mutableStateOf(config.spanY) }

    val maxSpanX = GRID_COLS - config.gridX
    val maxSpanY = GRID_ROWS - config.gridY

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text          = config.id.replace('_', ' '),
                color         = Color.White,
                fontSize      = 11.sp,
                letterSpacing = 2.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                SpanRow(label = "WIDTH",  value = spanX, min = 1, max = maxSpanX, accent = accent) { spanX = it }
                SpanRow(label = "HEIGHT", value = spanY, min = 1, max = maxSpanY, accent = accent) { spanY = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(spanX, spanY) }) {
                Text("APPLY", color = accent, fontSize = 11.sp, letterSpacing = 1.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color(0xFF555555), fontSize = 11.sp, letterSpacing = 1.sp)
            }
        },
        containerColor    = Color(0xFF0E0E0E),
        titleContentColor = Color.White,
        textContentColor  = Color.White
    )
}

@Composable
private fun SpanRow(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    accent: Color,
    onChange: (Int) -> Unit
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text          = label,
            color         = Color(0xFF666666),
            fontSize      = 10.sp,
            letterSpacing = 1.sp,
            modifier      = Modifier.width(52.dp)
        )
        IconButton(
            onClick  = { if (value > min) onChange(value - 1) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Remove, null,
                tint     = if (value > min) Color.White else Color(0xFF333333),
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text      = "$value",
            color     = Color.White,
            fontSize  = 16.sp,
            textAlign = TextAlign.Center,
            modifier  = Modifier.width(24.dp)
        )
        IconButton(
            onClick  = { if (value < max) onChange(value + 1) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Add, null,
                tint     = if (value < max) accent else Color(0xFF333333),
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.weight(1f))
        // Visual grid indicator
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(max) { i ->
                Box(
                    modifier = Modifier
                        .size(width = 14.dp, height = 10.dp)
                        .background(
                            if (i < value) accent.copy(alpha = 0.7f) else Color(0xFF2A2A2A),
                            RoundedCornerShape(1.dp)
                        )
                )
            }
        }
    }
}
