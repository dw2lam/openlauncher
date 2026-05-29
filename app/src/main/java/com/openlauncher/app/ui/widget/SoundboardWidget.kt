package com.openlauncher.app.ui.widget

import android.content.Intent
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
    val contentColor = if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val dimColor     = if (isDayMode) Color(0xFF888888) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
    val borderColor  = if (isDayMode) Color(0xFFE5E7EB) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)

    var activePadIndex by remember { mutableStateOf<Int?>(null) }
    var assigningIndex by remember { mutableStateOf<Int?>(null) }

    val safePads = remember(pads) {
        if (pads.size >= 6) pads.take(6)
        else pads + List(6 - pads.size) { SoundPadConfig("+", synthType = "") }
    }

    // Outer grid Column with top padding = 22.dp to leave room for card header label "SOUNDBOARD"
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 10.dp, end = 10.dp, top = 22.dp, bottom = 8.dp),
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
                                        if (pad.label == "+" || (pad.audioUri.isEmpty() && pad.synthType.isEmpty())) {
                                            assigningIndex = idx
                                        } else {
                                            activePadIndex = idx
                                            playSoundPad(
                                                context = context,
                                                pad = pad,
                                                onDone = { activePadIndex = null }
                                            )
                                        }
                                    },
                                    onLongClick = { assigningIndex = idx }
                                ) else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val isPlus = pad.label == "+"
                        Text(
                            text = pad.label,
                            color = if (isActive) accent else if (isPlus) dimColor else contentColor,
                            fontSize = if (isPlus) 16.sp else 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
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
    val menuBg     = if (isDayMode) Color(0xFFF0F0F0) else MaterialTheme.colorScheme.background
    val menuBorder = if (isDayMode) Color(0xFFCCCCCC) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
    val contentColor = if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val dimColor   = if (isDayMode) Color(0xFF888888) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
    val fieldBorder = if (isDayMode) Color(0xFFCCCCCC) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)

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
            val rawName = uri.path?.substringAfterLast('/') ?: "custom_sound"
            labelText = rawName.substringAfterLast(':').substringBeforeLast('.')
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(menuBg)
                .border(1.dp, menuBorder, RoundedCornerShape(6.dp))
                .padding(18.dp)
                .width(340.dp), // Fixed size: increased from 220dp to 340dp for landscape headunit displays
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "ASSIGN PAD SOUND",
                color = contentColor,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            // Label field
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("PAD LABEL", color = dimColor, fontSize = 7.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
                BasicTextField(
                    value = labelText,
                    onValueChange = { if (it.length <= 12) labelText = it },
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

            // Preloaded Audio Selector (replaces old raw waveform synth generation)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("PRELOADED AUDIO", color = dimColor, fontSize = 7.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val preloadedSounds = listOf(
                        "mario_jump" to "mario_jump",
                        "mario_coin" to "mario_coin",
                        "boom" to "boom",
                        "loud_fart" to "loud_fart"
                    )
                    preloadedSounds.forEach { (type, chipLabel) ->
                        val active = synthType == type && audioUri.isEmpty()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                                .border(1.dp, if (active) accent else fieldBorder, RoundedCornerShape(3.dp))
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (active) accent.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable { 
                                    synthType = type
                                    audioUri = ""
                                    labelText = type
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                chipLabel,
                                color = if (active) accent else dimColor,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Custom audio file picker
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("CUSTOM AUDIO FILE", color = dimColor, fontSize = 7.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
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
                            if (audioUri.isNotEmpty()) "CUSTOM FILE ASSIGNED" else "PICK AUDIO FILE",
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
                        audioUri.substringAfterLast('/').take(36),
                        color = dimColor.copy(alpha = 0.6f),
                        fontSize = 6.5.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (pad.label != "+" || pad.audioUri.isNotEmpty() || pad.synthType.isNotEmpty()) {
                OutlinedButton(
                    onClick = {
                        onSave(SoundPadConfig(
                            label     = "+",
                            audioUri  = "",
                            synthType = ""
                        ))
                    },
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF884444)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF884444)),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("CLEAR SOUND", color = Color(0xFF884444), fontSize = 7.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }

            // Save / Cancel row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(32.dp),
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = dimColor),
                    border = androidx.compose.foundation.BorderStroke(1.dp, fieldBorder),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("CANCEL", color = dimColor, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        onSave(SoundPadConfig(
                            label     = labelText.trim().ifEmpty { pad.label },
                            audioUri  = audioUri,
                            synthType = synthType
                        ))
                    },
                    modifier = Modifier.weight(1f).height(32.dp),
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("SAVE SOUND", color = if (isDayMode) Color.White else Color.Black, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun playSoundPad(context: android.content.Context, pad: SoundPadConfig, onDone: () -> Unit) {
    val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    mainHandler.post {
        try {
            if (pad.audioUri.isNotEmpty()) {
                val player = MediaPlayer()
                player.setDataSource(context, android.net.Uri.parse(pad.audioUri))
                player.prepare()
                player.setOnCompletionListener { mp ->
                    mp.release()
                    onDone()
                }
                player.start()
            } else {
                var resName = pad.synthType.lowercase().trim()
                var resId = context.resources.getIdentifier(resName, "raw", context.packageName)
                if (resId == 0) {
                    // Fallback mapping for legacy synth types saved in user settings
                    resName = when (resName) {
                        "horn", "beep", "alert" -> "mario_jump"
                        "kick", "snare", "bass" -> "mario_coin"
                        else -> "mario_jump"
                    }
                    resId = context.resources.getIdentifier(resName, "raw", context.packageName)
                }
                if (resId != 0) {
                    val player = MediaPlayer.create(context, resId)
                    if (player != null) {
                        player.setOnCompletionListener { mp ->
                            mp.release()
                            onDone()
                        }
                        player.start()
                    } else {
                        onDone()
                    }
                } else {
                    onDone()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("Soundboard", "Error playing pad", e)
            onDone()
        }
    }
}
