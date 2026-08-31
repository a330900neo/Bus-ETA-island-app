package com.busetaisland.app.data.api

import com.busetaisland.app.data.model.KmbApiResponse
import com.busetaisland.app.data.model.KmbEtaData
import com.busetaisland.app.data.model.KmbRouteData
import com.busetaisland.app.data.model.KmbRouteStopData
import com.busetaisland.app.data.model.KmbStopDetail
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

interface KmbApiService {

    @GET("v1/transport/kmb/route/")
    suspend fun getAllRoutes(): KmbApiResponse<List<KmbRouteData>>

    @GET("v1/transport/kmb/route-stop/{route}/{direction}/{service_type}")
    suspend fun getRouteStops(
        @Path("route") route: String,
        @Path("direction") direction: String, // "outbound" or "inbound" or "O" / "I"
        @Path("service_type") serviceType: String // e.g. "1"
    ): KmbApiResponse<List<KmbRouteStopData>>

    @GET("v1/transport/kmb/stop/{stop_id}")
    suspend fun getStopDetail(
        @Path("stop_id") stopId: String
    ): KmbApiResponse<KmbStopDetail>

    @GET("v1/transport/kmb/eta/{stop_id}/{route}/{service_type}")
    suspend fun getEtaForStopRoute(
        @Path("stop_id") stopId: String,
        @Path("route") route: String,
        @Path("service_type") serviceType: String
    ): KmbApiResponse<List<KmbEtaData>>

    @GET("v1/transport/kmb/stop-eta/{stop_id}")
    suspend fun getStopEta(
        @Path("stop_id") stopId: String
    ): KmbApiResponse<List<KmbEtaData>>

    @GET("v1/transport/kmb/route-eta/{route}/{service_type}")
    suspend fun getRouteEta(
        @Path("route") route: String,
        @Path("service_type") serviceType: String
    ): KmbApiResponse<List<KmbEtaData>>

    companion object {
        private const val BASE_URL = "https://data.etabus.gov.hk/"

        fun create(): KmbApiService {
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
                .create(KmbApiService::class.java)
        }
    }
}
