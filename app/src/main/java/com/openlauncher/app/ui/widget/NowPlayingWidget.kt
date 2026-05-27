package com.openlauncher.app.ui.widget

import android.media.MediaMetadata
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlauncher.app.model.NowPlayingState
import com.openlauncher.app.service.MediaListenerService
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
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Album art background
        if (state?.albumArt != null) {
            Image(
                bitmap             = state.albumArt.asImageBitmap(),
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.45f), Color.Black.copy(alpha = 0.88f))
                        )
                    )
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0B0B0B)))
        }

        if (state == null) {
            val isConnected by MediaListenerService.isConnected.collectAsState()
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val hasCarPlay  = carPlayPackage.isNotEmpty()
                val hasAutoApp  = androidAutoPackage.isNotEmpty()

                if (!isConnected && !hasCarPlay && !hasAutoApp) {
                    // No access, no shortcuts — quiet placeholder
                    Icon(Icons.Default.MusicNote, null, tint = Color(0xFF1E1E1E), modifier = Modifier.size(24.dp))
                } else if (hasCarPlay || hasAutoApp) {
                    // Show pinned shortcut buttons filling the entire box
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        if (hasCarPlay) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { onLaunchCarPlay() }
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
                                color    = Color(0xFF1E1E1E)
                            )
                        }
                        if (hasAutoApp) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { onLaunchAndroidAuto() }
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.MusicNote, null, tint = Color(0xFF2A2A2A), modifier = Modifier.size(28.dp))
                        Spacer(Modifier.height(4.dp))
                        Text("No media playing", style = MaterialTheme.typography.labelSmall, color = Color(0xFF333333))
                    }
                }
            }
        } else {
            var positionMs by remember { mutableLongStateOf(state.controller?.playbackState?.position ?: 0L) }
            val durationMs = state.controller?.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L

            LaunchedEffect(state.isPlaying, state.title) {
                while (state.isPlaying) {
                    positionMs = state.controller?.playbackState?.position ?: positionMs
                    delay(500)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Track info (top — tappable to open source app)
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .padding(top = 18.dp)
                        .clickable { onTapToOpenApp() }
                ) {
                    Text(
                        text     = state.title,
                        style    = MaterialTheme.typography.titleMedium,
                        color    = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp
                    )
                    Text(
                        text     = state.artist.ifEmpty { "Unknown" },
                        style    = MaterialTheme.typography.bodySmall,
                        color    = Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 11.sp
                    )
                }

                // Progress + controls (bottom)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (durationMs > 0) {
                        LinearProgressIndicator(
                            progress     = { (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) },
                            modifier     = Modifier.fillMaxWidth().height(2.dp),
                            color        = accent,
                            trackColor   = Color.White.copy(alpha = 0.15f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatMs(positionMs), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.45f), fontSize = 9.sp)
                            Text(formatMs(durationMs), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.45f), fontSize = 9.sp)
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment     = Alignment.CenterVertically,
                        modifier              = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = onPrev, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.SkipPrevious, "Prev", tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(20.dp))
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(accent.copy(alpha = 0.9f))
                        ) {
                            IconButton(onClick = onPlayPause, modifier = Modifier.size(42.dp)) {
                                Icon(
                                    imageVector        = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                                    tint               = Color.Black,
                                    modifier           = Modifier.size(22.dp)
                                )
                            }
                        }
                        IconButton(onClick = onNext, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.SkipNext, "Next", tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(20.dp))
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
