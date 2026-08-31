package com.busetaisland.app.data.api

import com.busetaisland.app.data.model.CtbApiResponse
import com.busetaisland.app.data.model.CtbEtaData
import com.busetaisland.app.data.model.CtbRouteData
import com.busetaisland.app.data.model.CtbRouteStopData
import com.busetaisland.app.data.model.CtbStopDetail
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

interface CtbApiService {

    @GET("v1/transport/citybus-nwfb/route/{company_id}")
    suspend fun getAllRoutes(
        @Path("company_id") companyId: String = "CTB"
    ): CtbApiResponse<List<CtbRouteData>>

    @GET("v1/transport/citybus-nwfb/route-stop/{company_id}/{route}/{direction}")
    suspend fun getRouteStops(
        @Path("route") route: String,
        @Path("direction") direction: String, // "outbound" or "inbound"
        @Path("company_id") companyId: String = "CTB"
    ): CtbApiResponse<List<CtbRouteStopData>>

    @GET("v1/transport/citybus-nwfb/stop/{stop_id}")
    suspend fun getStopDetail(
        @Path("stop_id") stopId: String
    ): CtbApiResponse<CtbStopDetail>

    @GET("v1/transport/citybus-nwfb/eta/{company_id}/{stop_id}/{route}")
    suspend fun getEtaForStopRoute(
        @Path("stop_id") stopId: String,
        @Path("route") route: String,
        @Path("company_id") companyId: String = "CTB"
    ): CtbApiResponse<List<CtbEtaData>>

    companion object {
        private const val BASE_URL = "https://rt.data.gov.hk/"

        fun create(): CtbApiService {
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
                .create(CtbApiService::class.java)
        }
    }
}
