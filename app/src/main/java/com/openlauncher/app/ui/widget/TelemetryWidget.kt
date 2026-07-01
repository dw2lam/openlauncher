package com.openlauncher.app.ui.widget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlauncher.app.util.LocationData
import kotlin.math.abs

@Composable
fun TelemetryWidget(
    location: LocationData?,
    bearing: Float,
    accent: Color,
    isDayMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val contentColor = if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val subColor     = if (isDayMode) Color(0xFF888888) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
    val ringColor    = if (isDayMode) Color(0xFFCCCCCC) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
    val cardinalMain = if (isDayMode) Color(0xFF555555) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
    val cardinalSub  = if (isDayMode) Color(0xFF888888) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
    val arrowColor   = if (isDayMode) Color(0xFF222222) else MaterialTheme.colorScheme.onBackground
    Column(
        modifier = modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // ── Compass ring — fills available space ─────────────────────────────
        BoxWithConstraints(
            modifier         = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val minDim = minOf(maxWidth, maxHeight)
            val radius = minDim * 0.41f
            // Capture for use inside nested lambdas
            val capturedMaxWidth  = maxWidth
            val capturedMaxHeight = maxHeight

            // Rotating layer: ring + cardinal labels spin opposite to bearing
            Box(
                modifier         = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Ring canvas rotates with -bearing
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = -bearing }
                ) {
                    val cx = size.width  / 2f
                    val cy = size.height / 2f
                    val r  = radius.toPx()

                    drawCircle(
                        color  = ringColor,
                        radius = r,
                        center = Offset(cx, cy),
                        style  = Stroke(width = 1.5.dp.toPx())
                    )
                }

                // Cardinal labels rotate with ring
                Box(
                    modifier         = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = -bearing },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text     = "N",
                        color    = cardinalMain,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = (capturedMaxHeight / 2 - radius - 6.dp).coerceAtLeast(0.dp))
                    )
                    Text(
                        text     = "S",
                        color    = cardinalMain,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = (capturedMaxHeight / 2 - radius - 6.dp).coerceAtLeast(0.dp))
                    )
                    Text(
                        text     = "E",
                        color    = cardinalSub,
                        fontSize = 9.sp,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = (capturedMaxWidth / 2 - radius - 6.dp).coerceAtLeast(0.dp))
                    )
                    Text(
                        text     = "W",
                        color    = cardinalSub,
                        fontSize = 9.sp,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = (capturedMaxWidth / 2 - radius - 6.dp).coerceAtLeast(0.dp))
                    )
                }
            }

            // Fixed layer: small arrow always points up + pivot circle
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width  / 2f
                val cy = size.height / 2f
                val r  = radius.toPx()

                // Small fixed arrow pointing straight up
                val arrowH = r * 0.20f
                val arrowW = arrowH * 0.48f
                drawPath(
                    path  = Path().apply {
                        moveTo(cx, cy - arrowH)                   // tip
                        lineTo(cx + arrowW, cy + arrowH * 0.42f) // bottom-right
                        lineTo(cx, cy + arrowH * 0.08f)          // centre notch
                        lineTo(cx - arrowW, cy + arrowH * 0.42f) // bottom-left
                        close()
                    },
                    color = arrowColor
                )

                // Hollow pivot circle
                drawCircle(
                    color  = Color(0xFF777777),
                    radius = 3.dp.toPx(),
                    center = Offset(cx, cy),
                    style  = Stroke(width = 1.5.dp.toPx())
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── Lat / Lon at the bottom ──────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text          = "LATITUDE",
                    style         = MaterialTheme.typography.labelSmall,
                    color         = subColor,
                    letterSpacing = 1.sp,
                    fontSize      = 7.sp
                )
                Text(
                    text     = if (location != null) formatLat(location.latitude) else "—",
                    color    = contentColor,
                    fontSize = 11.sp
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text          = "LONGITUDE",
                    style         = MaterialTheme.typography.labelSmall,
                    color         = subColor,
                    letterSpacing = 1.sp,
                    fontSize      = 7.sp,
                    textAlign     = TextAlign.End
                )
                Text(
                    text      = if (location != null) formatLon(location.longitude) else "—",
                    color     = contentColor,
                    fontSize  = 11.sp,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

private fun formatLat(lat: Double) = "%.4f° %s".format(abs(lat), if (lat >= 0) "N" else "S")
private fun formatLon(lon: Double) = "%.4f° %s".format(abs(lon), if (lon >= 0) "E" else "W")
