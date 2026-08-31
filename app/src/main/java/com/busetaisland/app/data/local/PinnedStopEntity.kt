package com.busetaisland.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pinned_stops")
data class PinnedStopEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val co: String = "KMB",
    val route: String,
    val bound: String, // "O" or "I"
    val serviceType: String = "1",
    val stopId: String,
    val stopNameEn: String,
    val stopNameTc: String,
    val destEn: String,
    val destTc: String,
    val seq: Int,
    val stopLat: Double,
    val stopLng: Double,
    val radiusMeters: Float = 300f, // Proximity trigger radius (e.g. 50m, 100m, 300m, 500m)
    val triggerType: String = "RADIUS", // "RADIUS" or "AREA"
    val areaId: Long? = null, // Linked GeofenceAreaEntity id if triggerType == "AREA"
    val isGeofenceEnabled: Boolean = true,
    val isActive: Boolean = false, // Current selected bus stop shown on Dynamic Island
    val lastEta1: String? = null,
    val lastEta2: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
