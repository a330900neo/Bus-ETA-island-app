package com.busetaisland.app.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GmbApiResponse<T>(
    @Json(name = "type") val type: String? = null,
    @Json(name = "version") val version: String? = null,
    @Json(name = "generated_timestamp") val generatedTimestamp: String? = null,
    @Json(name = "data") val data: T? = null
)

@JsonClass(generateAdapter = true)
data class GmbRoutesAllData(
    @Json(name = "routes") val routes: Map<String, List<String>>? = null
)

@JsonClass(generateAdapter = true)
data class GmbRouteInfo(
    @Json(name = "region") val region: String? = null,
    @Json(name = "route_code") val routeCode: String = "",
    @Json(name = "route_id") val routeId: Long = 0,
    @Json(name = "description_tc") val descriptionTc: String? = null,
    @Json(name = "description_en") val descriptionEn: String? = null,
    @Json(name = "directions") val directions: List<GmbRouteDirection>? = null
)

@JsonClass(generateAdapter = true)
data class GmbRouteDirection(
    @Json(name = "route_seq") val routeSeq: Int = 1,
    @Json(name = "orig_tc") val origTc: String = "",
    @Json(name = "orig_en") val origEn: String = "",
    @Json(name = "dest_tc") val destTc: String = "",
    @Json(name = "dest_en") val destEn: String = "",
    @Json(name = "remarks_tc") val remarksTc: String? = null
)

@JsonClass(generateAdapter = true)
data class GmbRouteStopsData(
    @Json(name = "route_stops") val routeStops: List<GmbRouteStopItem>? = null
)

@JsonClass(generateAdapter = true)
data class GmbRouteStopItem(
    @Json(name = "stop_seq") val stopSeq: Int = 1,
    @Json(name = "stop_id") val stopId: Long = 0,
    @Json(name = "name_tc") val nameTc: String = "",
    @Json(name = "name_en") val nameEn: String = ""
)

@JsonClass(generateAdapter = true)
data class GmbStopData(
    @Json(name = "coordinates") val coordinates: GmbCoordinates? = null,
    @Json(name = "enabled") val enabled: Boolean = true
)

@JsonClass(generateAdapter = true)
data class GmbCoordinates(
    @Json(name = "wgs84") val wgs84: GmbWgs84? = null
)

@JsonClass(generateAdapter = true)
data class GmbWgs84(
    @Json(name = "latitude") val latitude: Double = 0.0,
    @Json(name = "longitude") val longitude: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class GmbEtaRouteStopData(
    @Json(name = "enabled") val enabled: Boolean = true,
    @Json(name = "stop_id") val stopId: Long? = null,
    @Json(name = "eta") val eta: List<GmbEtaEntry>? = null
)

@JsonClass(generateAdapter = true)
data class GmbEtaEntry(
    @Json(name = "eta_seq") val etaSeq: Int = 1,
    @Json(name = "diff") val diff: Int = 0,
    @Json(name = "timestamp") val timestamp: String? = null,
    @Json(name = "remarks_tc") val remarksTc: String? = null,
    @Json(name = "remarks_en") val remarksEn: String? = null
)
