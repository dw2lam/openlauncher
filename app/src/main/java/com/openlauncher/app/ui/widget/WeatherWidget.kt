package com.openlauncher.app.ui.widget

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlauncher.app.model.WeatherState

@Composable
fun WeatherWidget(
    state: WeatherState?,
    accent: Color,
    metric: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (state != null) {
            Column(
                modifier            = Modifier.fillMaxSize().padding(start = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text     = state.conditionIcon,
                    fontSize = 34.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text       = state.temperatureDisplay(metric),
                    color      = Color.White,
                    fontSize   = 32.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 1.sp
                )
                Text(
                    text          = state.conditionLabel.uppercase(),
                    color         = Color(0xFF777777),
                    fontSize      = 9.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
