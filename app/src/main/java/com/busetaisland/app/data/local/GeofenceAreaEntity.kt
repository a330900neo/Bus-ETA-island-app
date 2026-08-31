package com.busetaisland.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

data class GeoPoint(val lat: Double, val lng: Double)

@Entity(tableName = "geofence_areas")
data class GeofenceAreaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val pointsJson: String, // Format: "lat1,lng1;lat2,lng2;lat3,lng3"
    val colorHex: Long = 0xFF7C4DFF, // Accent color for map/polygon visual
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getPoints(): List<GeoPoint> {
        return try {
            if (pointsJson.isBlank()) emptyList()
            else {
                pointsJson.split(";").mapNotNull { pair ->
                    val parts = pair.split(",")
                    if (parts.size >= 2) {
                        val lat = parts[0].toDoubleOrNull()
                        val lng = parts[1].toDoubleOrNull()
                        if (lat != null && lng != null) GeoPoint(lat, lng) else null
                    } else null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    val centerLat: Double
        get() {
            val pts = getPoints()
            return if (pts.isEmpty()) 22.3193 else pts.map { it.lat }.average()
        }

    val centerLng: Double
        get() {
            val pts = getPoints()
            return if (pts.isEmpty()) 114.1694 else pts.map { it.lng }.average()
        }

    companion object {
        fun serializePoints(points: List<GeoPoint>): String {
            return points.joinToString(";") { "${it.lat},${it.lng}" }
        }

        // Helper to generate a regular polygon around center coordinates (meters to approx degrees)
        fun createBoxAround(centerLat: Double, centerLng: Double, sizeMeters: Double = 200.0): List<GeoPoint> {
            val latDelta = (sizeMeters / 2.0) / 111000.0
            val lngDelta = (sizeMeters / 2.0) / (111000.0 * kotlin.math.cos(Math.toRadians(centerLat)))
            return listOf(
                GeoPoint(centerLat + latDelta, centerLng - lngDelta),
                GeoPoint(centerLat + latDelta, centerLng + lngDelta),
                GeoPoint(centerLat - latDelta, centerLng + lngDelta),
                GeoPoint(centerLat - latDelta, centerLng - lngDelta)
            )
        }

        fun createHexagonAround(centerLat: Double, centerLng: Double, radiusMeters: Double = 150.0): List<GeoPoint> {
            val points = mutableListOf<GeoPoint>()
            for (i in 0 until 6) {
                val angle = Math.toRadians((i * 60).toDouble())
                val latOffset = (radiusMeters * kotlin.math.sin(angle)) / 111000.0
                val lngOffset = (radiusMeters * kotlin.math.cos(angle)) / (111000.0 * kotlin.math.cos(Math.toRadians(centerLat)))
                points.add(GeoPoint(centerLat + latOffset, centerLng + lngOffset))
            }
            return points
        }
    }
}
