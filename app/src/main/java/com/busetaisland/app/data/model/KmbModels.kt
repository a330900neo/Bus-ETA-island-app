package com.busetaisland.app.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class KmbApiResponse<T>(
    @Json(name = "type") val type: String? = null,
    @Json(name = "version") val version: String? = null,
    @Json(name = "generated_timestamp") val generatedTimestamp: String? = null,
    @Json(name = "data") val data: T? = null
)

@JsonClass(generateAdapter = true)
data class KmbRouteData(
    @Json(name = "co") val co: String = "KMB",
    @Json(name = "route") val route: String = "",
    @Json(name = "bound") val bound: String = "O", // 'O' = Outbound, 'I' = Inbound
    @Json(name = "service_type") val serviceType: String = "1",
    @Json(name = "orig_en") val origEn: String = "",
    @Json(name = "orig_tc") val origTc: String = "",
    @Json(name = "orig_sc") val origSc: String = "",
    @Json(name = "dest_en") val destEn: String = "",
    @Json(name = "dest_tc") val destTc: String = "",
    @Json(name = "dest_sc") val destSc: String = "",
    @Json(name = "data_timestamp") val dataTimestamp: String? = null
)

@JsonClass(generateAdapter = true)
data class KmbRouteStopData(
    @Json(name = "co") val co: String = "KMB",
    @Json(name = "route") val route: String = "",
    @Json(name = "bound") val bound: String? = null,
    @Json(name = "service_type") val serviceType: String = "1",
    @Json(name = "seq") val seq: Int = 1,
    @Json(name = "stop") val stop: String = "",
    @Json(name = "data_timestamp") val dataTimestamp: String? = null
)

@JsonClass(generateAdapter = true)
data class KmbStopDetail(
    @Json(name = "stop") val stop: String = "",
    @Json(name = "name_en") val nameEn: String = "",
    @Json(name = "name_tc") val nameTc: String = "",
    @Json(name = "name_sc") val nameSc: String = "",
    @Json(name = "lat") val lat: String = "0.0",
    @Json(name = "long") val long: String = "0.0",
    @Json(name = "data_timestamp") val dataTimestamp: String? = null
) {
    val latitude: Double get() = lat.toDoubleOrNull() ?: 0.0
    val longitude: Double get() = long.toDoubleOrNull() ?: 0.0
}

@JsonClass(generateAdapter = true)
data class KmbEtaData(
    @Json(name = "co") val co: String = "KMB",
    @Json(name = "route") val route: String = "",
    @Json(name = "dir") val dir: String = "",
    @Json(name = "service_type") val serviceType: Any? = null,
    @Json(name = "seq") val seq: Int? = null,
    @Json(name = "stop") val stop: String = "",
    @Json(name = "dest_en") val destEn: String = "",
    @Json(name = "dest_tc") val destTc: String = "",
    @Json(name = "dest_sc") val destSc: String = "",
    @Json(name = "eta_seq") val etaSeq: Int = 1,
    @Json(name = "eta") val eta: String? = null, // ISO-8601 string, e.g. "2026-08-26T15:48:00+08:00"
    @Json(name = "rmk_en") val rmkEn: String = "",
    @Json(name = "rmk_tc") val rmkTc: String = "",
    @Json(name = "rmk_sc") val rmkSc: String = "",
    @Json(name = "data_timestamp") val dataTimestamp: String? = null
)

data class FormattedEtaItem(
    val etaSeq: Int,
    val etaTimeString: String, // e.g. "15:48"
    val minutesLeft: Int,      // e.g. 4
    val isDeparted: Boolean,
    val remark: String = "",
    val rmkTc: String = "",
    val isScheduled: Boolean = false, // "原定班次" (KMB) or "未開出" (GMB)
    val rawTimestamp: String? = null
) {
    /**
     * Determines whether the ETA should appear in grey color:
     * - KMB / CTB / NWFB: grey if rmk_tc == "原定班次" or remark indicates scheduled
     * - GMB: grey if remarks_tc == "未開出" or remark indicates not yet departed
     */
    fun isGrey(co: String = "KMB"): Boolean {
        return if (co.equals("GMB", ignoreCase = true)) {
            rmkTc.contains("未開出") || remark.contains("Not yet departed", ignoreCase = true) || isScheduled
        } else {
            rmkTc.contains("原定班次") || remark.contains("Scheduled", ignoreCase = true) || isScheduled
        }
    }
}

enum class EtaDisplayUnit {
    MINUTES,     // "4m", "12m"
    EXACT_TIME   // "15:48", "15:56"
}

data class TrackedBusInfo(
    val co: String = "KMB",
    val route: String,
    val bound: String,
    val serviceType: String,
    val stopId: String,
    val stopNameEn: String,
    val stopNameTc: String,
    val destEn: String,
    val destTc: String,
    val seq: Int,
    val stopLat: Double,
    val stopLng: Double,
    val radiusMeters: Float = 300f,
    val triggerType: String = "RADIUS",
    val areaId: Long? = null,
    val areaName: String? = null,
    val isGeofenceEnabled: Boolean = true,
    val eta1: FormattedEtaItem? = null,
    val eta2: FormattedEtaItem? = null,
    val eta3: FormattedEtaItem? = null,
    val distanceMeters: Float? = null,
    val isInRange: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis(),
    val error: String? = null
) {
    // Check if the 1st ETA should be greyed out ("原定班次" for KMB, "未開出" for GMB)
    val isFirstEtaScheduled: Boolean
        get() = eta1?.isGrey(co) == true

    // Check if the 2nd ETA should be greyed out ("原定班次" for KMB, "未開出" for GMB)
    val isSecondEtaScheduled: Boolean
        get() = eta2?.isGrey(co) == true

    // Chinese destination (fallback to English if TC is blank)
    val chineseDestination: String
        get() = destTc.ifBlank { destEn }

    fun formattedEta1(unit: EtaDisplayUnit): String {
        val e = eta1 ?: return "--"
        return when (unit) {
            EtaDisplayUnit.MINUTES -> if (e.minutesLeft <= 0) "即將抵達" else "${e.minutesLeft}分"
            EtaDisplayUnit.EXACT_TIME -> e.etaTimeString.ifBlank { if (e.minutesLeft <= 0) "Due" else "${e.minutesLeft}m" }
        }
    }

    fun formattedEta2(unit: EtaDisplayUnit): String {
        val e = eta2 ?: return "--"
        return when (unit) {
            EtaDisplayUnit.MINUTES -> if (e.minutesLeft <= 0) "即將抵達" else "${e.minutesLeft}分"
            EtaDisplayUnit.EXACT_TIME -> e.etaTimeString.ifBlank { if (e.minutesLeft <= 0) "Due" else "${e.minutesLeft}m" }
        }
    }

    // One line representation as requested: "route number, destination and 1st and 2nd eta"
    fun toOneLineSummary(unit: EtaDisplayUnit = EtaDisplayUnit.MINUTES): String {
        val dest = chineseDestination
        val e1 = formattedEta1(unit)
        val e2 = formattedEta2(unit)
        return "$route ➔ $dest | $e1 | $e2"
    }

    fun toOneLineCompact(unit: EtaDisplayUnit = EtaDisplayUnit.MINUTES): String {
        val dest = chineseDestination
        val e1 = formattedEta1(unit)
        val e2 = formattedEta2(unit)
        return "$route → $dest • $e1, $e2"
    }
}
