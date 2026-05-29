package com.openlauncher.app.ui.widget

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlauncher.app.util.LocationData
import kotlin.math.*

import androidx.compose.material3.MaterialTheme

@Composable
fun AltimeterWidget(
    location: LocationData?,
    isMetric: Boolean,
    accent: Color,
    isDayMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val iconTint = if (isDayMode) Color(0xFF333333) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
    val labelColor = if (isDayMode) Color(0xFF888888) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.30f)
    val context = LocalContext.current

    // Accelerometer sensor state
    var rollDeg  by remember { mutableFloatStateOf(0f) }
    var pitchDeg by remember { mutableFloatStateOf(0f) }
    val gravBuf  = remember { FloatArray(3) { 0f } }
    var hasSensor by remember { mutableStateOf(false) }

    // GPS fallback state for sensor-less devices
    var lastLoc by remember { mutableStateOf<LocationData?>(null) }
    var gpsPitch by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                hasSensor = true
                val alpha = 0.12f
                gravBuf[0] = alpha * event.values[0] + (1f - alpha) * gravBuf[0]
                gravBuf[1] = alpha * event.values[1] + (1f - alpha) * gravBuf[1]
                gravBuf[2] = alpha * event.values[2] + (1f - alpha) * gravBuf[2]
                rollDeg  = Math.toDegrees(
                    atan2(gravBuf[1].toDouble(), gravBuf[2].toDouble())
                ).toFloat()
                pitchDeg = Math.toDegrees(
                    atan2(-gravBuf[0].toDouble(),
                        sqrt(gravBuf[1].toDouble().pow(2) + gravBuf[2].toDouble().pow(2)))
                ).toFloat()
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { sm.unregisterListener(listener) }
    }

    // Mathematical GPS incline (pitch) calculation when offline/no accelerometer
    LaunchedEffect(location) {
        val newLoc = location ?: return@LaunchedEffect
        val oldLoc = lastLoc
        if (oldLoc != null && (oldLoc.latitude != newLoc.latitude || oldLoc.longitude != newLoc.longitude)) {
            val dist = FloatArray(1)
            runCatching {
                android.location.Location.distanceBetween(
                    oldLoc.latitude, oldLoc.longitude,
                    newLoc.latitude, newLoc.longitude,
                    dist
                )
                val distance = dist[0]
                val heightDiff = newLoc.altitude - oldLoc.altitude
                if (distance > 3f) {
                    val pitchRad = atan2(heightDiff, distance.toDouble())
                    val newPitchDeg = Math.toDegrees(pitchRad).toFloat().coerceIn(-45f, 45f)
                    val alpha = 0.15f
                    gpsPitch = alpha * newPitchDeg + (1f - alpha) * gpsPitch
                    lastLoc = newLoc
                }
            }
        } else {
            lastLoc = newLoc
        }
    }

    val displayRoll = if (hasSensor) rollDeg else 0f
    val displayPitch = if (hasSensor) pitchDeg else gpsPitch

    Box(
        modifier          = modifier,
        contentAlignment  = Alignment.Center
    ) {
        // Prominent Altitude Display at the top
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ALTITUDE",
                color = labelColor,
                fontSize = 7.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (location != null) {
                    val altVal = if (isMetric) location.altitude else location.altitude * 3.28084
                    val unit = if (isMetric) "m" else "ft"
                    "%,.0f %s".format(altVal, unit)
                } else "—",
                color = if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Center car icon showing roll angle
        Icon(
            imageVector        = Icons.Default.DirectionsCar,
            contentDescription = null,
            tint               = iconTint,
            modifier           = Modifier
                .size(56.dp)
                .padding(top = 10.dp)
                .graphicsLayer { rotationZ = displayRoll }
        )

        // Bottom Inclinometer telemetry labels (Roll & Pitch)
        Row(
            modifier              = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.Bottom
        ) {
            Column {
                Text("ROLL",  color = labelColor, fontSize = 7.sp, letterSpacing = 1.sp)
                Text("%.1f°".format(displayRoll),  color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("PITCH", color = labelColor, fontSize = 7.sp, letterSpacing = 1.sp, textAlign = TextAlign.End)
                Text("%.1f°".format(displayPitch), color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
            }
        }
    }
}
