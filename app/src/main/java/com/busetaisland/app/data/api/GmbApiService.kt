package com.busetaisland.app.data.api

import com.busetaisland.app.data.model.GmbApiResponse
import com.busetaisland.app.data.model.GmbEtaRouteStopData
import com.busetaisland.app.data.model.GmbRouteInfo
import com.busetaisland.app.data.model.GmbRouteStopsData
import com.busetaisland.app.data.model.GmbRoutesAllData
import com.busetaisland.app.data.model.GmbStopData
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

interface GmbApiService {

    @GET("route")
    suspend fun getAllRoutes(): GmbApiResponse<GmbRoutesAllData>

    @GET("route/{region}/{route_code}")
    suspend fun getRouteDetail(
        @Path("region") region: String,
        @Path("route_code") routeCode: String
    ): GmbApiResponse<List<GmbRouteInfo>>

    @GET("route-stop/{route_id}/{route_seq}")
    suspend fun getRouteStops(
        @Path("route_id") routeId: Long,
        @Path("route_seq") routeSeq: Int
    ): GmbApiResponse<GmbRouteStopsData>

    @GET("stop/{stop_id}")
    suspend fun getStopCoordinates(
        @Path("stop_id") stopId: Long
    ): GmbApiResponse<GmbStopData>

    @GET("eta/route-stop/{route_id}/{route_seq}/{stop_seq}")
    suspend fun getEtaByStopSeq(
        @Path("route_id") routeId: Long,
        @Path("route_seq") routeSeq: Int,
        @Path("stop_seq") stopSeq: Int
    ): GmbApiResponse<GmbEtaRouteStopData>

    companion object {
        private const val BASE_URL = "https://data.etagmb.gov.hk/"

        fun create(): GmbApiService {
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
                .create(GmbApiService::class.java)
        }
    }
}
