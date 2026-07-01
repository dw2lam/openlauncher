package com.openlauncher.app.ui.widget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlauncher.app.data.ClockStyle
import kotlinx.coroutines.delay
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

import androidx.compose.material3.MaterialTheme

@Composable
fun ClockWidget(
    style: ClockStyle,
    accent: Color,
    isDayMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    var calendar by remember { mutableStateOf(Calendar.getInstance()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            calendar = Calendar.getInstance()
        }
    }

    val contentColor = if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val subColor     = if (isDayMode) Color(0xFF888888) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)

    Box(modifier = modifier) {
        when (style) {
            ClockStyle.DIGITAL -> DigitalClock(calendar, contentColor, subColor)
            ClockStyle.ANALOG  -> AnalogClock(calendar, accent, isDayMode)
        }
    }
}

@Composable
private fun DigitalClock(cal: Calendar, contentColor: Color, subColor: Color) {
    val hour   = cal.get(Calendar.HOUR_OF_DAY)
    val minute = cal.get(Calendar.MINUTE)

    Column(
        modifier            = Modifier.fillMaxSize().padding(start = 14.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text          = "%02d:%02d".format(hour, minute),
            color         = contentColor,
            fontSize      = 48.sp,
            fontWeight    = androidx.compose.ui.text.font.FontWeight.Light,
            letterSpacing = 1.sp
        )
        Text(
            text     = buildDateString(cal),
            color    = subColor,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun AnalogClock(cal: Calendar, accent: Color, isDayMode: Boolean = false) {
    val hour   = cal.get(Calendar.HOUR).toFloat()
    val minute = cal.get(Calendar.MINUTE).toFloat()
    val second = cal.get(Calendar.SECOND).toFloat()

    val ringColor = if (isDayMode) Color(0xFFCCCCCC) else Color(0xFF2A2A2A)
    val minuteHandColor = if (isDayMode) Color(0xFF222222) else MaterialTheme.colorScheme.onBackground
    val pivotBg = if (isDayMode) Color(0xFFEEEEEE) else Color(0xFF1E1E1E)

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx     = size.width / 2f
            val cy     = size.height / 2f
            val radius = size.minDimension / 2f * 0.82f

            // Outer hairline ring
            drawCircle(
                color  = ringColor,
                radius = radius,
                center = Offset(cx, cy),
                style  = Stroke(1.dp.toPx())
            )

            // Tick marks — minimal, hairline
            for (i in 0 until 60) {
                val angle = (Math.PI * 2 / 60 * i - Math.PI / 2).toFloat()
                val isHour = i % 5 == 0
                val isQuarter = i % 15 == 0
                val inner = when {
                    isQuarter -> 0.80f
                    isHour    -> 0.85f
                    else      -> 0.90f
                }
                drawLine(
                    color       = when {
                        isQuarter -> accent.copy(alpha = 0.9f)
                        isHour    -> if (isDayMode) Color(0xFF888888) else Color(0xFF555555)
                        else      -> if (isDayMode) Color(0xFFCCCCCC) else Color(0xFF2E2E2E)
                    },
                    start       = Offset(cx + cos(angle) * radius * inner, cy + sin(angle) * radius * inner),
                    end         = Offset(cx + cos(angle) * radius * 0.96f, cy + sin(angle) * radius * 0.96f),
                    strokeWidth = if (isQuarter) 1.5.dp.toPx() else 0.8.dp.toPx(),
                    cap         = StrokeCap.Round
                )
            }

            // Hour hand — thick, accent tinted
            val hAngle = ((hour / 12f + minute / 720f) * 2 * Math.PI - Math.PI / 2).toFloat()
            drawLine(
                color       = accent,
                start       = Offset(cx - cos(hAngle) * radius * 0.14f, cy - sin(hAngle) * radius * 0.14f),
                end         = Offset(cx + cos(hAngle) * radius * 0.50f, cy + sin(hAngle) * radius * 0.50f),
                strokeWidth = 3.dp.toPx(),
                cap         = StrokeCap.Round
            )

            // Minute hand
            val mAngle = ((minute / 60f) * 2 * Math.PI - Math.PI / 2).toFloat()
            drawLine(
                color       = minuteHandColor,
                start       = Offset(cx - cos(mAngle) * radius * 0.14f, cy - sin(mAngle) * radius * 0.14f),
                end         = Offset(cx + cos(mAngle) * radius * 0.74f, cy + sin(mAngle) * radius * 0.74f),
                strokeWidth = 1.5.dp.toPx(),
                cap         = StrokeCap.Round
            )

            // Second hand — accent, hairline
            val sAngle = ((second / 60f) * 2 * Math.PI - Math.PI / 2).toFloat()
            drawLine(
                color       = accent.copy(alpha = 0.75f),
                start       = Offset(cx - cos(sAngle) * radius * 0.22f, cy - sin(sAngle) * radius * 0.22f),
                end         = Offset(cx + cos(sAngle) * radius * 0.88f, cy + sin(sAngle) * radius * 0.88f),
                strokeWidth = 0.8.dp.toPx(),
                cap         = StrokeCap.Round
            )

            // Center pivot
            drawCircle(color = pivotBg, radius = 4.dp.toPx(), center = Offset(cx, cy))
            drawCircle(
                color  = accent,
                radius = 2.5.dp.toPx(),
                center = Offset(cx, cy),
                style  = Stroke(1.dp.toPx())
            )
        }

        // Date inset — centered, above 6 o'clock position like a real watch
        Text(
            text      = shortDateString(cal),
            color     = if (isDayMode) Color(0xFF999999) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            fontSize  = 9.sp,
            letterSpacing = 1.5.sp,
            modifier  = Modifier
                .align(Alignment.Center)
                .padding(bottom = 44.dp)
        )
    }
}

private fun shortDateString(cal: Calendar): String {
    val days   = arrayOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
    return "${days[cal.get(Calendar.DAY_OF_WEEK) - 1]} ${cal.get(Calendar.DAY_OF_MONTH)}"
}

private fun buildDateString(cal: Calendar): String {
    val days   = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    val months = arrayOf("January", "February", "March", "April", "May", "June",
                         "July", "August", "September", "October", "November", "December")
    return "${days[cal.get(Calendar.DAY_OF_WEEK) - 1]}, ${months[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.DAY_OF_MONTH)}"
}

fun clockTimeLabel(cal: Calendar): String = when (cal.get(Calendar.HOUR_OF_DAY)) {
    in 5..11  -> "MORNING"
    in 12..16 -> "AFTERNOON"
    in 17..20 -> "EVENING"
    else      -> "NIGHT"
}
