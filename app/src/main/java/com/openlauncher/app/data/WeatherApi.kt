package com.openlauncher.app.data

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class OpenMeteoResponse(
    @SerializedName("current_weather") val currentWeather: CurrentWeather?,
                             @SerializedName("daily") val dailyData: DailyData?
)

data class CurrentWeather(
    @SerializedName("temperature") val temperature: Double,
                          @SerializedName("weathercode") val weathercode: Int
)

// ESTA CLASE FALTABA O ESTABA EN OTRO LUGAR
data class DailyData(
    @SerializedName("time") val dates: List<String>,
                     @SerializedName("temperature_2m_max") val maxTemperatures: List<Double>,
                     @SerializedName("temperature_2m_min") val minTemperatures: List<Double>,
                     @SerializedName("weathercode") val weatherCodes: List<Int>
)

interface WeatherApiService {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
                            @Query("longitude") longitude: Double,
                            @Query("current_weather") currentWeather: Boolean = true,
                            @Query("daily") dailyVariables: String = "temperature_2m_max,temperature_2m_min,weathercode",
                            @Query("timezone") timezone: String = "auto",
                            @Query("temperature_unit") temperatureUnit: String = "celsius"
    ): OpenMeteoResponse
}

object WeatherApi {
    private val client = OkHttpClient.Builder().build()

    val service: WeatherApiService = Retrofit.Builder()
    .baseUrl("https://api.open-meteo.com/")
    .client(client)
    .addConverterFactory(GsonConverterFactory.create())
    .build()
    .create(WeatherApiService::class.java)
}
