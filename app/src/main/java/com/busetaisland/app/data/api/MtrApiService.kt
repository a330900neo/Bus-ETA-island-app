package com.busetaisland.app.data.api

import com.busetaisland.app.data.model.MtrApiResponse
import com.busetaisland.app.data.model.MtrLrtApiResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface MtrApiService {

    @GET("v1/transport/mtr/getSchedule.php")
    suspend fun getSchedule(
        @Query("line") line: String,
        @Query("sta") sta: String,
        @Query("lang") lang: String = "TC"
    ): MtrApiResponse

    @GET("v1/transport/mtr/lrt/getSchedule")
    suspend fun getLrtSchedule(
        @Query("station_id") stationId: Int
    ): MtrLrtApiResponse

    companion object {
        private const val BASE_URL = "https://rt.data.gov.hk/"

        fun create(): MtrApiService {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(MtrApiService::class.java)
        }
    }
}
