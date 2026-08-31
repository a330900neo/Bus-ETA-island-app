package com.busetaisland.app.data.model

data class RainGridPoint(
    val lat: Double,
    val lng: Double,
    val rainfallMm: Double
)

data class RainNowcastFrame(
    val updateTime: String, // e.g. "202608301500"
    val endTime: String,    // e.g. "202608301530"
    val formattedTime: String, // e.g. "15:30 (+30分)"
    val points: List<RainGridPoint>
)

data class RainNowcastData(
    val lastFetchedTimestamp: Long = 0L,
    val updateTime: String = "",
    val frames: List<RainNowcastFrame> = emptyList()
)
