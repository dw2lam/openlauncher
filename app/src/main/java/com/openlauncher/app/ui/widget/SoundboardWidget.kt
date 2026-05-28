package com.openlauncher.app.ui.widget

import android.content.Intent
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.openlauncher.app.data.SoundPadConfig
import kotlin.math.sin

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SoundboardWidget(
    pads: List<SoundPadConfig>,
    accent: Color,
    isDayMode: Boolean = false,
    isEditing: Boolean = false,
    onUpdatePad: (index: Int, pad: SoundPadConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val context      = LocalContext.current
    val contentColor = if (isDayMode) Color(0xFF111111) else Color.White
    val dimColor     = if (isDayMode) Color(0xFF888888) else Color(0xFF555555)
    val borderColor  = if (isDayMode) Color(0xFFE5E7EB) else Color(0xFF1D2024)

    var activePadIndex by remember { mutableStateOf<Int?>(null) }
    var assigningIndex by remember { mutableStateOf<Int?>(null) }

    val safePads = remember(pads) {
        if (pads.size >= 6) pads.take(6)
        else pads + List(6 - pads.size) { SoundPadConfig("PAD ${pads.size + it + 1}") }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(2) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(3) { col ->
                    val idx = row * 3 + col
                    val pad = safePads[idx]
                    val isActive = activePadIndex == idx
                    val hasCustomAudio = pad.audioUri.isNotEmpty()

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(
                                1.dp,
                                if (isActive) accent else borderColor,
                                RoundedCornerShape(3.dp)
                            )
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isActive) accent.copy(alpha = 0.12f) else Color.Transparent)
                            .then(
                                if (!isEditing) Modifier.combinedClickable(
                                    onClick = {
                                        activePadIndex = idx
                                        playSoundPad(
                                            context = context,
                                            pad = pad,
                                            onDone = { activePadIndex = null }
                                        )
                                    },
                                    onLongClick = { assigningIndex = idx }
                                ) else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = pad.label.uppercase(),
                                color = if (isActive) accent else contentColor,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            if (hasCustomAudio) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(if (isActive) accent else dimColor.copy(alpha = 0.6f))
                                )
                            } else {
                                Text(
                                    text = pad.synthType,
                                    color = dimColor.copy(alpha = if (isActive) 0.8f else 0.5f),
                                    fontSize = 6.sp,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    assigningIndex?.let { idx ->
        PadAssignDialog(
            pad = safePads[idx],
            accent = accent,
            isDayMode = isDayMode,
            onDismiss = { assigningIndex = null },
            onSave = { updated ->
                onUpdatePad(idx, updated)
                assigningIndex = null
            }
        )
    }
}

@Composable
private fun PadAssignDialog(
    pad: SoundPadConfig,
    accent: Color,
    isDayMode: Boolean,
    onDismiss: () -> Unit,
    onSave: (SoundPadConfig) -> Unit
) {
    val context    = LocalContext.current
    val menuBg     = if (isDayMode) Color(0xFFF0F0F0) else Color(0xFF111111)
    val menuBorder = if (isDayMode) Color(0xFFCCCCCC) else Color(0xFF1E1E1E)
    val contentColor = if (isDayMode) Color(0xFF111111) else Color.White
    val dimColor   = if (isDayMode) Color(0xFF888888) else Color(0xFF555555)
    val fieldBorder = if (isDayMode) Color(0xFFCCCCCC) else Color(0xFF2E3238)

    var labelText   by remember { mutableStateOf(pad.label) }
    var synthType   by remember { mutableStateOf(pad.synthType) }
    var audioUri    by remember { mutableStateOf(pad.audioUri) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            audioUri = uri.toString()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(menuBg)
                .border(1.dp, menuBorder, RoundedCornerShape(4.dp))
                .padding(16.dp)
                .width(220.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "ASSIGN PAD",
                color = contentColor,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            // Label field
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("LABEL", color = dimColor, fontSize = 7.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
                BasicTextField(
                    value = labelText,
                    onValueChange = { if (it.length <= 10) labelText = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = contentColor,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    cursorBrush = SolidColor(accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, fieldBorder, RoundedCornerShape(2.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }

            // Synth sound type
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("SYNTH TYPE", color = dimColor, fontSize = 7.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("BEEP", "KICK", "SNARE", "BASS", "HORN", "ALERT", "FART").forEach { type ->
                        val active = synthType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(22.dp)
                                .border(1.dp, if (active) accent else fieldBorder, RoundedCornerShape(2.dp))
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (active) accent.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable { synthType = type; audioUri = "" },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                type,
                                color = if (active) accent else dimColor,
                                fontSize = 5.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Custom audio file
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("AUDIO FILE", color = dimColor, fontSize = 7.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { filePicker.launch(arrayOf("audio/*")) },
                        modifier = Modifier.weight(1f).height(30.dp),
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (audioUri.isNotEmpty()) accent else fieldBorder),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.AudioFile, null, tint = accent, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (audioUri.isNotEmpty()) "ASSIGNED" else "PICK FILE",
                            color = accent,
                            fontSize = 7.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (audioUri.isNotEmpty()) {
                        IconButton(
                            onClick = { audioUri = "" },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Clear, null, tint = dimColor, modifier = Modifier.size(14.dp))
                        }
                    }
                }
                if (audioUri.isNotEmpty()) {
                    Text(
                        audioUri.substringAfterLast('/').take(24),
                        color = dimColor.copy(alpha = 0.6f),
                        fontSize = 6.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Save / Cancel row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(30.dp),
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = dimColor),
                    border = androidx.compose.foundation.BorderStroke(1.dp, fieldBorder),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("CANCEL", color = dimColor, fontSize = 7.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        onSave(SoundPadConfig(
                            label     = labelText.trim().ifEmpty { pad.label },
                            audioUri  = audioUri,
                            synthType = synthType
                        ))
                    },
                    modifier = Modifier.weight(1f).height(30.dp),
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("SAVE", color = if (isDayMode) Color.White else Color.Black, fontSize = 7.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun playSoundPad(context: android.content.Context, pad: SoundPadConfig, onDone: () -> Unit) {
    if (pad.audioUri.isNotEmpty()) {
        Thread {
            val player = MediaPlayer()
            try {
                player.setDataSource(context, android.net.Uri.parse(pad.audioUri))
                player.prepare()
                player.setOnCompletionListener { mp ->
                    mp.release()
                    onDone()
                }
                player.start()
            } catch (_: Exception) {
                player.release()
                playSynthPad(pad.synthType, onDone)
            }
        }.start()
    } else {
        playSynthPad(pad.synthType, onDone)
    }
}

private fun playSynthPad(type: String, onDone: () -> Unit) {
    Thread {
        val sampleRate = 22050
        val durationMs = when (type) {
            "KICK"  -> 220
            "SNARE" -> 160
            "BASS"  -> 450
            "HORN"  -> 600
            "ALERT" -> 350
            "FART"  -> 700
            else    -> 280
        }
        val sampleCount = (sampleRate * (durationMs / 1000f)).toInt()
        if (sampleCount <= 0) { onDone(); return@Thread }
        val buffer = ShortArray(sampleCount)
        val rng = java.util.Random()

        for (i in 0 until sampleCount) {
            val t = i.toFloat() / sampleRate
            val env = 1f - (i.toFloat() / sampleCount)
            buffer[i] = when (type) {
                "KICK" -> {
                    val freq = 140.0 - 100.0 * (i.toDouble() / sampleCount)
                    (sin(2.0 * Math.PI * freq * t) * 28000.0 * env).toInt()
                }
                "SNARE" -> {
                    val noise = rng.nextGaussian() * 12000.0
                    val pop   = sin(2.0 * Math.PI * 180.0 * t) * 5000.0
                    ((noise + pop) * env).toInt()
                }
                "BASS" -> {
                    val freq = 65.0
                    val period = (sampleRate / freq).toInt()
                    val wave = if (period > 0 && (i % period) < (period / 2)) 1.0 else -1.0
                    (wave * 14000.0 * env).toInt()
                }
                "HORN" -> {
                    val h1 = sin(2.0 * Math.PI * 440.0 * t) * 10000.0
                    val h2 = sin(2.0 * Math.PI * 550.0 * t) * 6000.0
                    val h3 = sin(2.0 * Math.PI * 660.0 * t) * 3000.0
                    ((h1 + h2 + h3) * env).toInt()
                }
                "ALERT" -> {
                    val freq = if ((i / (sampleRate / 8)) % 2 == 0) 880.0 else 1100.0
                    (sin(2.0 * Math.PI * freq * t) * 20000.0 * env).toInt()
                }
                "FART" -> {
                    // Low rumble with flapping modulation and turbulent noise
                    val progress = i.toDouble() / sampleCount
                    val baseFreq = 55.0 + sin(progress * Math.PI * 3.0) * 25.0
                    val flutter  = sin(progress * Math.PI * 18.0) * 0.45 + 0.55
                    val period   = (sampleRate / baseFreq).toInt().coerceAtLeast(1)
                    val square   = if ((i % period) < (period / 2)) 1.0 else -1.0
                    val noise    = rng.nextGaussian() * 4000.0
                    val fartEnv  = when {
                        progress < 0.08 -> progress / 0.08
                        progress > 0.75 -> (1.0 - progress) / 0.25
                        else -> 1.0
                    }
                    ((square * 18000.0 * flutter + noise) * fartEnv).toInt()
                }
                else -> {
                    val v = sin(2.0 * Math.PI * 660.0 * t) + sin(2.0 * Math.PI * 792.0 * t) * 0.4
                    (v * 12000.0 * env).toInt()
                }
            }.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        try {
            val minBuf = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val track = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBuf, buffer.size * 2),
                AudioTrack.MODE_STATIC
            )
            track.write(buffer, 0, buffer.size)
            track.play()
            Thread.sleep(durationMs.toLong() + 80L)
            track.stop()
            track.release()
        } catch (_: Exception) {}
        onDone()
    }.start()
}
