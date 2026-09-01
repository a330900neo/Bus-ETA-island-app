package com.busetaisland.app.data.api

import com.busetaisland.app.data.model.HkoFndResponse
import com.busetaisland.app.data.model.HkoRhrreadResponse
import com.busetaisland.app.data.model.HkoWarningItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface WeatherApiService {
    @GET("weather.php")
    suspend fun getRegionalWeather(
        @Query("dataType") dataType: String = "rhrread",
        @Query("lang") lang: String = "tc"
    ): Response<HkoRhrreadResponse>

    @GET("weather.php")
    suspend fun getNineDayForecast(
        @Query("dataType") dataType: String = "fnd",
        @Query("lang") lang: String = "tc"
    ): Response<HkoFndResponse>

    @GET("weather.php")
    suspend fun getWeatherWarnings(
        @Query("dataType") dataType: String = "warnsum",
        @Query("lang") lang: String = "tc"
    ): Response<Map<String, HkoWarningItem>>

    @GET("https://data.weather.gov.hk/weatherAPI/hko_data/F3/Gridded_rainfall_nowcast.csv")
    suspend fun getGriddedRainfallNowcastCsv(): Response<okhttp3.ResponseBody>

    companion object {
            private const val BASE_URL = "https://data.weather.gov.hk/weatherAPI/opendata/"

        fun create(): WeatherApiService {
            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(WeatherApiService::class.java)
        }
    }
}
