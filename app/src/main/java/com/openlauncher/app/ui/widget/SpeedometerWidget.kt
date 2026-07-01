package com.openlauncher.app.ui.widget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlauncher.app.util.LocationData
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SpeedometerWidget(
    location: LocationData?,
    isMetric: Boolean,
    accent: Color,
    isDayMode: Boolean = false,
    digitalOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    val maxSpeed     = if (isMetric) 200f else 124f
    // Obtenemos la velocidad en metros por segundo (o 0 si es nula)
    val rawSpeedMps = location?.speedMps ?: 0f
    // Filtro de ruido: si es menor a 0.5 m/s (~1.8 km/h), asumimos que el carro está parado
    val filteredSpeedMps = if (rawSpeedMps < 0.5f) 0f else rawSpeedMps
    // Calculamos la velocidad final a mostrar usando el valor filtrado
    val speedDisplay = (filteredSpeedMps * if (isMetric) 3.6f else 2.237f).coerceAtLeast(0f)
    val unitLabel    = if (isMetric) "KM/H" else "MPH"
    val trackAlpha   = if (isDayMode) 0.18f else 0.07f
    val tickAlphaMaj = if (isDayMode) 0.50f else 0.28f
    val tickAlphaMin = if (isDayMode) 0.25f else 0.13f

    val contentColor = if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val subAlpha     = if (isDayMode) 0.55f else 0.32f
    val tickBaseColor = if (isDayMode) Color(0xFF222222) else MaterialTheme.colorScheme.onBackground

    Box(
        modifier         = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (digitalOnly) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier            = Modifier.fillMaxSize()
            ) {
                Text(
                    text          = "%.0f".format(speedDisplay),
                    color         = contentColor,
                    fontSize      = 54.sp,
                    fontWeight    = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    letterSpacing = (-1.5).sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text          = unitLabel,
                    color         = contentColor.copy(alpha = subAlpha * 1.5f),
                    fontSize      = 10.sp,
                    fontWeight    = androidx.compose.ui.text.font.FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx    = size.width  / 2f
                val cy    = size.height / 2f
                val arcR  = minOf(size.width, size.height) * 0.37f
                val trackW = arcR * 0.13f
                val startAngle    = 150f
                val sweepTotal    = 240f
                val progressSweep = (speedDisplay / maxSpeed).coerceIn(0f, 1f) * sweepTotal

                val tl   = Offset(cx - arcR, cy - arcR)
                val sz   = Size(arcR * 2f, arcR * 2f)

                drawArc(
                    color      = contentColor.copy(alpha = trackAlpha),
                    startAngle = startAngle,
                    sweepAngle = sweepTotal,
                    useCenter  = false,
                    topLeft    = tl,
                    size       = sz,
                    style      = Stroke(width = trackW, cap = StrokeCap.Round)
                )

                if (progressSweep > 0.5f) {
                    drawArc(
                        color      = accent,
                        startAngle = startAngle,
                        sweepAngle = progressSweep,
                        useCenter  = false,
                        topLeft    = tl,
                        size       = sz,
                        style      = Stroke(width = trackW, cap = StrokeCap.Round)
                    )
                }

                for (i in 0..10) {
                    val angle   = startAngle + i * (sweepTotal / 10f)
                    val rad     = Math.toRadians(angle.toDouble())
                    val isMajor = i % 2 == 0
                    val outerR  = arcR - trackW / 2f - 3.dp.toPx()
                    val innerR  = outerR - if (isMajor) 7.dp.toPx() else 4.dp.toPx()
                    drawLine(
                        color       = tickBaseColor.copy(alpha = if (isMajor) tickAlphaMaj else tickAlphaMin),
                        start       = Offset(cx + (outerR * cos(rad)).toFloat(), cy + (outerR * sin(rad)).toFloat()),
                        end         = Offset(cx + (innerR * cos(rad)).toFloat(), cy + (innerR * sin(rad)).toFloat()),
                        strokeWidth = if (isMajor) 1.5.dp.toPx() else 0.8.dp.toPx()
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier            = Modifier.offset(y = (-4).dp)
            ) {
                Text(
                    text          = "%.0f".format(speedDisplay),
                    color         = contentColor,
                    fontSize      = 34.sp,
                    letterSpacing = (-1).sp
                )
                Text(
                    text          = unitLabel,
                    color         = contentColor.copy(alpha = subAlpha),
                    fontSize      = 8.sp,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}
