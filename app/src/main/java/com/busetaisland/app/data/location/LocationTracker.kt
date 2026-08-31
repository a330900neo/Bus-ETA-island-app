package com.busetaisland.app.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class UserLocationState(
    val latitude: Double = 22.3193, // Default Hong Kong coordinates
    val longitude: Double = 114.1694,
    val isSimulated: Boolean = false,
    val hasRealLocation: Boolean = false,
    val accuracy: Float = 10f,
    val timestamp: Long = System.currentTimeMillis()
)

class LocationTracker(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _locationState = MutableStateFlow(UserLocationState())
    val locationState: StateFlow<UserLocationState> = _locationState.asStateFlow()

    private var isTracking = false
    private var locationCallback: LocationCallback? = null
    private var isLowPowerScreenOffMode = false

    @SuppressLint("MissingPermission")
    fun startTracking(isScreenOff: Boolean = false) {
        if (_locationState.value.isSimulated) return

        if (isTracking) {
            stopTracking()
        }

        isLowPowerScreenOffMode = isScreenOff

        try {
            fusedClient.lastLocation.addOnSuccessListener { loc: Location? ->
                loc?.let {
                    if (!_locationState.value.isSimulated) {
                        _locationState.value = UserLocationState(
                            latitude = it.latitude,
                            longitude = it.longitude,
                            isSimulated = false,
                            hasRealLocation = true,
                            accuracy = it.accuracy,
                            timestamp = System.currentTimeMillis()
                        )
                    }
                }
            }

            val intervalMs = if (isScreenOff) 300000L else 15000L // 5 min on screen off, 15s on active
            val minIntervalMs = if (isScreenOff) 60000L else 10000L
            val priority = if (isScreenOff) Priority.PRIORITY_LOW_POWER else Priority.PRIORITY_BALANCED_POWER_ACCURACY

            val request = LocationRequest.Builder(priority, intervalMs)
                .setMinUpdateIntervalMillis(minIntervalMs)
                .setMinUpdateDistanceMeters(if (isScreenOff) 80f else 15f)
                .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val loc = result.lastLocation ?: return
                    if (!_locationState.value.isSimulated) {
                        val current = _locationState.value
                        val distMoved = calculateDistanceMeters(current.latitude, current.longitude, loc.latitude, loc.longitude)
                        if (!current.hasRealLocation || distMoved >= 5.0) {
                            _locationState.value = UserLocationState(
                                latitude = loc.latitude,
                                longitude = loc.longitude,
                                isSimulated = false,
                                hasRealLocation = true,
                                accuracy = loc.accuracy,
                                timestamp = System.currentTimeMillis()
                            )
                        }
                    }
                }
            }

            fusedClient.requestLocationUpdates(request, locationCallback!!, Looper.getMainLooper())
            isTracking = true
        } catch (e: SecurityException) {
            fallbackToLocationManager()
        } catch (e: Exception) {
            // Graceful fallback
        }
    }

    fun setScreenState(isScreenOn: Boolean) {
        if (_locationState.value.isSimulated) return
        startTracking(isScreenOff = !isScreenOn)
    }

    @SuppressLint("MissingPermission")
    fun requestImmediateLocationUpdate(onUpdated: (() -> Unit)? = null) {
        if (_locationState.value.isSimulated) {
            onUpdated?.invoke()
            return
        }
        try {
            fusedClient.lastLocation.addOnSuccessListener { loc: Location? ->
                loc?.let {
                    _locationState.value = UserLocationState(
                        latitude = it.latitude,
                        longitude = it.longitude,
                        isSimulated = false,
                        hasRealLocation = true,
                        accuracy = it.accuracy,
                        timestamp = System.currentTimeMillis()
                    )
                    onUpdated?.invoke()
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    @SuppressLint("MissingPermission")
    private fun fallbackToLocationManager() {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val provider = LocationManager.NETWORK_PROVIDER
            locationManager?.getLastKnownLocation(provider)?.let { loc ->
                if (!_locationState.value.isSimulated) {
                    _locationState.value = UserLocationState(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        isSimulated = false,
                        hasRealLocation = true,
                        accuracy = loc.accuracy
                    )
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun stopTracking() {
        locationCallback?.let { fusedClient.removeLocationUpdates(it) }
        locationCallback = null
        isTracking = false
    }

    fun setSimulatedLocation(lat: Double, lng: Double) {
        _locationState.value = UserLocationState(
            latitude = lat,
            longitude = lng,
            isSimulated = true,
            hasRealLocation = true,
            accuracy = 5f,
            timestamp = System.currentTimeMillis()
        )
    }

    fun setSimulatedDistanceToStop(stopLat: Double, stopLng: Double, distanceMeters: Float) {
        // Offset latitude roughly: 1 deg lat ≈ 111,000 meters
        val latOffset = (distanceMeters / 111000.0)
        setSimulatedLocation(stopLat + latOffset, stopLng)
    }

    fun disableSimulation() {
        _locationState.value = _locationState.value.copy(isSimulated = false)
        startTracking()
    }

    companion object {
        fun calculateDistanceMeters(
            lat1: Double,
            lon1: Double,
            lat2: Double,
            lon2: Double
        ): Float {
            val results = FloatArray(1)
            Location.distanceBetween(lat1, lon1, lat2, lon2, results)
            return results[0]
        }

        /**
         * Ray-casting algorithm for Point-in-Polygon detection.
         * Returns true if point (lat, lng) is inside the polygon defined by vertices.
         */
        fun isPointInPolygon(
            pointLat: Double,
            pointLng: Double,
            polygon: List<com.busetaisland.app.data.local.GeoPoint>
        ): Boolean {
            if (polygon.size < 3) return false
            var inside = false
            var j = polygon.size - 1
            for (i in polygon.indices) {
                val pi = polygon[i]
                val pj = polygon[j]
                val intersect = ((pi.lat > pointLat) != (pj.lat > pointLat)) &&
                        (pointLng < (pj.lng - pi.lng) * (pointLat - pi.lat) / (pj.lat - pi.lat) + pi.lng)
                if (intersect) {
                    inside = !inside
                }
                j = i
            }
            return inside
        }
    }
}
