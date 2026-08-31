package com.busetaisland.app.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CtbApiResponse<T>(
    @Json(name = "type") val type: String? = null,
    @Json(name = "version") val version: String? = null,
    @Json(name = "generated_timestamp") val generatedTimestamp: String? = null,
    @Json(name = "data") val data: T? = null
)

@JsonClass(generateAdapter = true)
data class CtbRouteData(
    @Json(name = "co") val co: String = "CTB",
    @Json(name = "route") val route: String = "",
    @Json(name = "orig_en") val origEn: String = "",
    @Json(name = "orig_tc") val origTc: String = "",
    @Json(name = "orig_sc") val origSc: String = "",
    @Json(name = "dest_en") val destEn: String = "",
    @Json(name = "dest_tc") val destTc: String = "",
    @Json(name = "dest_sc") val destSc: String = "",
    @Json(name = "data_timestamp") val dataTimestamp: String? = null
)

@JsonClass(generateAdapter = true)
data class CtbRouteStopData(
    @Json(name = "co") val co: String = "CTB",
    @Json(name = "route") val route: String = "",
    @Json(name = "dir") val dir: String = "", // 'I' or 'O'
    @Json(name = "seq") val seq: Int = 1,
    @Json(name = "stop") val stop: String = "",
    @Json(name = "data_timestamp") val dataTimestamp: String? = null
)

@JsonClass(generateAdapter = true)
data class CtbStopDetail(
    @Json(name = "stop") val stop: String = "",
    @Json(name = "name_en") val nameEn: String = "",
    @Json(name = "name_tc") val nameTc: String = "",
    @Json(name = "name_sc") val nameSc: String = "",
    @Json(name = "lat") val lat: Double = 0.0,
    @Json(name = "long") val long: Double = 0.0,
    @Json(name = "data_timestamp") val dataTimestamp: String? = null
)

@JsonClass(generateAdapter = true)
data class CtbEtaData(
    @Json(name = "co") val co: String = "CTB",
    @Json(name = "route") val route: String = "",
    @Json(name = "dir") val dir: String = "",
    @Json(name = "seq") val seq: Int? = null,
    @Json(name = "stop") val stop: String = "",
    @Json(name = "dest_en") val destEn: String = "",
    @Json(name = "dest_tc") val destTc: String = "",
    @Json(name = "dest_sc") val destSc: String = "",
    @Json(name = "eta_seq") val etaSeq: Int = 1,
    @Json(name = "eta") val eta: String? = null,
    @Json(name = "rmk_en") val rmkEn: String = "",
    @Json(name = "rmk_tc") val rmkTc: String = "",
    @Json(name = "rmk_sc") val rmkSc: String = "",
    @Json(name = "data_timestamp") val dataTimestamp: String? = null
)
