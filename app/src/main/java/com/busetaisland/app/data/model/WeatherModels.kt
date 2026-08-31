package com.busetaisland.app.data.model

import com.busetaisland.app.data.location.LocationTracker
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

data class WeatherWarning(
    val code: String = "",
    val name: String = "",
    val shortLabel: String = "",
    val iconEmoji: String = "⚠️",
    val iconUrl: String = "",
    val colorHex: Long = 0xFFFFB74D
)

data class WeatherInfo(
    val districtName: String = "天文台",
    val currentTemp: Int = 0,
    val minTemp: Int = 0,
    val maxTemp: Int = 0,
    val humidity: Int = 0,
    val rainProbability: String = "低", // e.g. "低", "中低", "中", "中高", "高" or "0%"
    val iconId: Int = 0,
    val description: String = "",
    val warnings: List<WeatherWarning> = emptyList()
) {
    /**
     * Formatted into a clean single line:
     * e.g. "沙田 26°C (22°~28°C) • 💧78% • 🌧️低 • ⚡雷暴"
     */
    fun toOneLineSummary(): String {
        val tempRange = if (minTemp > 0 && maxTemp > 0) " (${minTemp}°~${maxTemp}°C)" else ""
        val warningStr = if (warnings.isNotEmpty()) " • " + warnings.joinToString(" ") { "${it.iconEmoji}${it.shortLabel}" } else ""
        return "[$districtName] ${currentTemp}°C$tempRange • 💧${humidity}% • 🌧️${rainProbability}$warningStr"
    }
}

data class HkoStationLocation(
    val name: String,
    val shortName: String,
    val latitude: Double,
    val longitude: Double
)

object HkoStationRegistry {
    val STATIONS = listOf(
        HkoStationLocation("香港天文台", "尖沙咀", 22.3022, 114.1741),
        HkoStationLocation("京士柏", "京士柏", 22.3114, 114.1728),
        HkoStationLocation("香港公園", "中環", 22.2778, 114.1606),
        HkoStationLocation("跑馬地", "跑馬地", 22.2708, 114.1836),
        HkoStationLocation("黃竹坑", "香港仔", 22.2478, 114.1736),
        HkoStationLocation("赤柱", "赤柱", 22.2186, 114.2125),
        HkoStationLocation("筲箕灣", "筲箕灣", 22.2817, 114.2361),
        HkoStationLocation("西灣河", "西灣河", 22.2850, 114.2230),
        HkoStationLocation("山頂", "山頂", 22.2694, 114.1489),
        HkoStationLocation("啟德跑道公園", "啟德", 22.3089, 114.2136),
        HkoStationLocation("九龍城", "九龍城", 22.3328, 114.1878),
        HkoStationLocation("觀塘", "觀塘", 22.3161, 114.2250),
        HkoStationLocation("深水埗", "深水埗", 22.3308, 114.1569),
        HkoStationLocation("黃大仙", "黃大仙", 22.3417, 114.1950),
        HkoStationLocation("將軍澳", "將軍澳", 22.3158, 114.2556),
        HkoStationLocation("西貢", "西貢", 22.3836, 114.2744),
        HkoStationLocation("清水灣", "清水灣", 22.2908, 114.2956),
        HkoStationLocation("沙田", "沙田", 22.4025, 114.2100),
        HkoStationLocation("大埔", "大埔", 22.4450, 114.1789),
        HkoStationLocation("大美督", "大美督", 22.4753, 114.2367),
        HkoStationLocation("上水", "上水", 22.5019, 114.1286),
        HkoStationLocation("打鼓嶺", "打鼓嶺", 22.5286, 114.1567),
        HkoStationLocation("石崗", "石崗", 22.4361, 114.0850),
        HkoStationLocation("荃灣城門谷", "荃灣", 22.3789, 114.1256),
        HkoStationLocation("荃灣可觀", "荃灣", 22.3850, 114.1089),
        HkoStationLocation("青衣", "青衣", 22.3442, 114.1100),
        HkoStationLocation("屯門", "屯門", 22.3908, 113.9767),
        HkoStationLocation("元朗公園", "元朗", 22.4419, 114.0183),
        HkoStationLocation("流浮山", "流浮山", 22.4689, 113.9836),
        HkoStationLocation("赤鱲角", "機場", 22.3094, 113.9219),
        HkoStationLocation("昂坪", "昂坪", 22.2561, 113.9039),
        HkoStationLocation("長洲", "長洲", 22.2011, 114.0267),
        HkoStationLocation("坪洲", "坪洲", 22.2858, 114.0381),
        HkoStationLocation("大帽山", "大帽山", 22.4108, 114.1242),
        HkoStationLocation("大老山", "大老山", 22.3606, 114.2239),
        HkoStationLocation("橫瀾島", "橫瀾島", 22.1819, 114.3017)
    )

    fun findNearestStation(lat: Double, lon: Double): HkoStationLocation {
        return STATIONS.minByOrNull {
            LocationTracker.calculateDistanceMeters(lat, lon, it.latitude, it.longitude)
        } ?: STATIONS.first()
    }
}

// HKO Regional Weather Report (dataType=rhrread)
@JsonClass(generateAdapter = true)
data class HkoRhrreadResponse(
    @Json(name = "temperature") val temperature: HkoTempData? = null,
    @Json(name = "humidity") val humidity: HkoHumidityData? = null,
    @Json(name = "rainfall") val rainfall: HkoRainfallData? = null,
    @Json(name = "icon") val icon: List<Int>? = null,
    @Json(name = "warningMessage") val warningMessage: List<String>? = null,
    @Json(name = "updateTime") val updateTime: String? = null
)

// HKO Warning Summary Item (dataType=warnsum)
@JsonClass(generateAdapter = true)
data class HkoWarningItem(
    @Json(name = "name") val name: String? = null,
    @Json(name = "code") val code: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "actionCode") val actionCode: String? = null
)

@JsonClass(generateAdapter = true)
data class HkoTempData(
    @Json(name = "data") val data: List<HkoTempItem>? = null,
    @Json(name = "recordTime") val recordTime: String? = null
)

@JsonClass(generateAdapter = true)
data class HkoTempItem(
    @Json(name = "place") val place: String = "",
    @Json(name = "value") val value: Double = 0.0,
    @Json(name = "unit") val unit: String = "C"
)

@JsonClass(generateAdapter = true)
data class HkoHumidityData(
    @Json(name = "data") val data: List<HkoHumidityItem>? = null,
    @Json(name = "recordTime") val recordTime: String? = null
)

@JsonClass(generateAdapter = true)
data class HkoHumidityItem(
    @Json(name = "place") val place: String = "",
    @Json(name = "value") val value: Int = 0,
    @Json(name = "unit") val unit: String = "percent"
)

@JsonClass(generateAdapter = true)
data class HkoRainfallData(
    @Json(name = "data") val data: List<HkoRainfallItem>? = null
)

@JsonClass(generateAdapter = true)
data class HkoRainfallItem(
    @Json(name = "place") val place: String = "",
    @Json(name = "max") val max: Double? = null,
    @Json(name = "min") val min: Double? = null,
    @Json(name = "unit") val unit: String = "mm"
)

// HKO 9-Day Forecast (dataType=fnd)
@JsonClass(generateAdapter = true)
data class HkoFndResponse(
    @Json(name = "weatherForecast") val weatherForecast: List<HkoDailyForecast>? = null
)

@JsonClass(generateAdapter = true)
data class HkoDailyForecast(
    @Json(name = "forecastDate") val forecastDate: String = "",
    @Json(name = "forecastWeather") val forecastWeather: String = "",
    @Json(name = "forecastMaxtemp") val forecastMaxtemp: HkoTempValue? = null,
    @Json(name = "forecastMintemp") val forecastMintemp: HkoTempValue? = null,
    @Json(name = "forecastMaxrh") val forecastMaxrh: HkoTempValue? = null,
    @Json(name = "forecastMinrh") val forecastMinrh: HkoTempValue? = null,
    @Json(name = "ForecastIcon") val forecastIcon: Int = 0,
    @Json(name = "PSR") val psr: String = "" // Significant Probability of Precipitation: "高", "中高", "中", "中低", "低"
)

@JsonClass(generateAdapter = true)
data class HkoTempValue(
    @Json(name = "value") val value: Double = 0.0,
    @Json(name = "unit") val unit: String = "C"
)
