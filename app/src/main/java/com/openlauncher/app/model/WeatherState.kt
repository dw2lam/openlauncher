package com.openlauncher.app.model

data class WeatherState(
    val currentTemperature: Double? = null, // Guardará la actual (si hay Wi-Fi)
    val forecastDays: List<DailyForecast> = emptyList(),
                        val isLoading: Boolean = false,
                        val error: String? = null
)

data class DailyForecast(
    val date: String,
    val maxTemperatureCelsius: Double,
    val minTemperatureCelsius: Double,
    val weatherCode: Int
) {
    // Si no hay Wi-Fi, este método calculará y mostrará el promedio del día
    fun temperatureDisplay(metric: Boolean): String {
        val promedio = (minTemperatureCelsius + maxTemperatureCelsius) / 2.0
        return if (metric) "${Math.round(promedio)}°C"
        else "${Math.round(celsiusToFahrenheit(promedio))}°F"
    }

    val conditionLabel: String get() = wmoCodeToLabel(weatherCode)
    val conditionIcon: String get() = wmoCodeToEmoji(weatherCode, isDay = true)
}

private fun celsiusToFahrenheit(c: Double) = c * 9.0 / 5.0 + 32.0

    private fun wmoCodeToLabel(code: Int): String = when (code) {
        0 -> "Clear"
        1, 2, 3 -> "Cloudy"
        45, 48 -> "Foggy"
        51, 53, 55 -> "Drizzle"
        61, 63, 65 -> "Rain"
        71, 73, 75 -> "Snow"
        80, 81, 82 -> "Showers"
        95 -> "Thunderstorm"
        96, 99 -> "Hail"
        else -> "Unknown"
    }

    private fun wmoCodeToEmoji(code: Int, isDay: Boolean): String = when (code) {
        0 -> if (isDay) "☀️" else "🌙"
        1, 2 -> if (isDay) "⛅" else "🌤"
        3 -> "☁️"
        45, 48 -> "🌫️"
        51, 53, 55, 61, 63, 65, 80, 81, 82 -> "🌧️"
        71, 73, 75 -> "❄️"
        95, 96, 99 -> "⛈️"
        else -> "🌡️"
    }
