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
import androidx.compose.ui.platform.LocalDensity
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

    // CAMBIO 1: Usamos Box como raíz para permitir superposición y centro absoluto
    Box(
        modifier = modifier.padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        // ── Compass ring — Centrado absolutamente ─────────────────────────────
        BoxWithConstraints(
            modifier         = Modifier.fillMaxSize(), // Ahora llena todo el contenedor
                           contentAlignment = Alignment.Center
        ) {
            // Reducimos un poco el multiplicador (ej. de 0.65f a 0.55f) para que el anillo
            // no pise el texto de las coordenadas en pantallas pequeñas.
            val minDim = minOf(maxWidth, maxHeight)
            val radius = minDim * 0.55f

            val density = LocalDensity.current
            val radiusPx = with(density) { radius.toPx() }

            Box(
                modifier         = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = -bearing },
                    contentAlignment = Alignment.Center
                ) {
                    // 1. Dibujamos el anillo
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width  / 2f
                        val cy = size.height / 2f
                        drawCircle(
                            color  = ringColor,
                            radius = radiusPx,
                            center = Offset(cx, cy),
                                   style  = Stroke(width = 1.5.dp.toPx())
                        )
                    }

                    // 2. Posicionamiento Cardinal
                    val labelOffset = radius - 12.dp

                    Text(
                        text     = "N",
                         color    = cardinalMain,
                         fontSize = 11.sp,
                         modifier = Modifier.offset(y = -labelOffset)
                    )
                    Text(
                        text     = "S",
                         color    = cardinalMain,
                         fontSize = 11.sp,
                         modifier = Modifier.offset(y = labelOffset)
                    )
                    Text(
                        text     = "E",
                         color    = cardinalSub,
                         fontSize = 9.sp,
                         modifier = Modifier.offset(x = labelOffset)
                    )
                    Text(
                        text     = "W",
                         color    = cardinalSub,
                         fontSize = 9.sp,
                         modifier = Modifier.offset(x = -labelOffset)
                    )
                }
            }

            // Fixed layer: Flecha estática
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width  / 2f
                val cy = size.height / 2f

                val arrowH = radiusPx * 0.22f
                val arrowW = arrowH * 0.48f

                drawPath(
                    path  = Path().apply {
                        moveTo(cx, cy - arrowH)
                        lineTo(cx + arrowW, cy + arrowH * 0.42f)
                        lineTo(cx, cy + arrowH * 0.08f)
                        lineTo(cx - arrowW, cy + arrowH * 0.42f)
                        close()
                    },
                    color = arrowColor
                )

                drawCircle(
                    color  = Color(0xFF777777),
                           radius = 10.dp.toPx(),
                           center = Offset(cx, cy),
                           style  = Stroke(width = 1.5.dp.toPx())
                )
            }
        }

        // ── Lat / Lon at the bottom ──────────────────────────────────────────
        // CAMBIO 2: Alineamos esta fila abajo del Box usando Modifier.align
        Row(
            modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter),
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
