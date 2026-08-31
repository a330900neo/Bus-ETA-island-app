package com.busetaisland.app.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Model representing live flight tracking data (inspired by pyflightdata / FlightRadar24)
 */
data class TrackedFlightInfo(
    val flightNumber: String, // e.g. "CX880"
    val callsign: String, // e.g. "CPA880"
    val registration: String, // e.g. "B-LRA"
    val aircraftModel: String, // e.g. "A350-941"
    val airlineName: String, // e.g. "Cathay Pacific"
    val originIata: String, // e.g. "HKG"
    val originCity: String, // e.g. "Hong Kong"
    val destinationIata: String, // e.g. "LAX"
    val destinationCity: String, // e.g. "Los Angeles"
    val stdEpochSec: Long, // Scheduled Departure (epoch seconds)
    val atdEpochSec: Long?, // Actual Departure (epoch seconds, or ETD)
    val staEpochSec: Long, // Scheduled Arrival (epoch seconds)
    val etaEpochSec: Long?, // Estimated Arrival (epoch seconds, or ATA)
    val stdFormatted: String, // e.g. "00:15"
    val actualDepartureFormatted: String, // e.g. "00:28" or "ETD 00:20"
    val staFormatted: String, // e.g. "21:35"
    val etaFormatted: String, // e.g. "21:18" or "ATA 21:15"
    val statusText: String, // e.g. "En Route", "Landed", "Scheduled", "Delayed", "Departed"
    val isLive: Boolean = true,
    val progressPercent: Float, // 0.0f .. 1.0f
    val altitudeFt: Int? = null,
    val speedKts: Int? = null,
    val lastUpdatedEpochMs: Long = System.currentTimeMillis()
)

object FlightTimeFormatter {
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Asia/Hong_Kong")
    }

    private val fullDateTimeFormat = SimpleDateFormat("MM-dd HH:mm", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Asia/Hong_Kong")
    }

    fun formatTime(epochSec: Long): String {
        if (epochSec <= 0) return "--:--"
        return timeFormat.format(Date(epochSec * 1000L))
    }

    fun formatDateTime(epochSec: Long): String {
        if (epochSec <= 0) return "--:--"
        return fullDateTimeFormat.format(Date(epochSec * 1000L))
    }
}
