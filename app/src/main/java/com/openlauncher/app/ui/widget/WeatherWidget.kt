package com.openlauncher.app.ui.widget

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlauncher.app.model.WeatherState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun WeatherWidget(
    state: WeatherState?,
    accent: Color,
    metric: Boolean,
    isDayMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val contentColor = if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val subColor     = if (isDayMode) Color(0xFF888888) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)

    val todayString = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    // Corregido: cambiamos 'weather' por 'state'
    val todayWeather = state?.forecastDays?.find { day -> day.date == todayString }

    Box(modifier = modifier) {
        if (todayWeather != null) {

            // Lógica inteligente de temperatura (Actual si está disponible / Promedio como fallback)
            val tempAImprimir = if (state.currentTemperature != null) {
                if (metric) "${Math.round(state.currentTemperature)}°C"
                    else "${Math.round(state.currentTemperature * 9.0 / 5.0 + 32.0)}°F"
            } else {
                todayWeather.temperatureDisplay(metric)
            }

            Column(
                modifier            = Modifier.fillMaxSize().padding(start = 14.dp, bottom = 14.dp),
                   verticalArrangement = Arrangement.Bottom,
                   horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text     = todayWeather.conditionIcon,
                     fontSize = 34.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text       = tempAImprimir,
                     color      = contentColor,
                     fontSize   = 32.sp,
                     fontWeight = FontWeight.Light,
                     letterSpacing = 1.sp
                )
                Text(
                    text          = todayWeather.conditionLabel.uppercase(),
                     color         = subColor,
                     fontSize      = 9.sp,
                     letterSpacing = 1.sp
                )
            }
        }
    }
}
