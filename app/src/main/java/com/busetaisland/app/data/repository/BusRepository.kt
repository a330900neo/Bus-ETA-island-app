package com.busetaisland.app.data.repository

import android.content.Context
import com.busetaisland.app.data.api.KmbApiService
import com.busetaisland.app.data.local.AppDatabase
import com.busetaisland.app.data.local.GeoPoint
import com.busetaisland.app.data.local.GeofenceAreaEntity
import com.busetaisland.app.data.local.PinnedStopEntity
import com.busetaisland.app.data.location.LocationTracker
import com.busetaisland.app.data.model.FormattedEtaItem
import com.busetaisland.app.data.model.KmbEtaData
import com.busetaisland.app.data.model.KmbRouteData
import com.busetaisland.app.data.model.KmbRouteStopData
import com.busetaisland.app.data.model.KmbStopDetail
import com.busetaisland.app.data.model.TrackedBusInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

import com.busetaisland.app.data.api.CtbApiService
import com.busetaisland.app.data.api.GmbApiService
import com.busetaisland.app.data.api.MtrApiService
import com.busetaisland.app.data.api.WeatherApiService
import com.busetaisland.app.data.model.HkoStationRegistry
import com.busetaisland.app.data.model.MtrRegistry
import com.busetaisland.app.data.model.WeatherInfo
import com.busetaisland.app.data.model.WeatherWarning
import com.busetaisland.app.data.model.toKmbRouteData
import com.busetaisland.app.data.model.toKmbRouteStopData
import com.busetaisland.app.data.model.toKmbStopDetail
import com.busetaisland.app.data.model.toKmbEtaData
import com.busetaisland.app.data.api.FlightApiService
import com.busetaisland.app.data.model.TrackedFlightInfo
import com.busetaisland.app.data.model.RainNowcastData
import com.busetaisland.app.data.model.RainNowcastFrame
import com.busetaisland.app.data.model.RainGridPoint
import com.busetaisland.app.service.OverlayStateHolder

class BusRepository(
    private val context: Context,
    private val apiService: KmbApiService = KmbApiService.create(),
    private val ctbApiService: CtbApiService = CtbApiService.create(),
    private val gmbApiService: GmbApiService = GmbApiService.create(),
    private val mtrApiService: MtrApiService = MtrApiService.create(),
    private val weatherApiService: WeatherApiService = WeatherApiService.create(),
    private val flightApiService: FlightApiService = FlightApiService.getInstance(),
    private val database: AppDatabase = AppDatabase.getDatabase(context),
    val locationTracker: LocationTracker = LocationTracker(context)
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pinnedDao = database.pinnedStopDao()
    private val areaDao = database.geofenceAreaDao()

    val allPinnedStops: Flow<List<PinnedStopEntity>> = pinnedDao.getAllPinnedStops()
    val activeTrackedStopFromDb: Flow<PinnedStopEntity?> = pinnedDao.getActiveTrackedStop()
    val allGeofenceAreas: Flow<List<GeofenceAreaEntity>> = areaDao.getAllAreas()

    private val _areasCache = MutableStateFlow<Map<Long, GeofenceAreaEntity>>(emptyMap())

    private val _trackedBusState = MutableStateFlow<TrackedBusInfo?>(null)
    val trackedBusState: StateFlow<TrackedBusInfo?> = _trackedBusState.asStateFlow()

    private val _allTrackedBusesState = MutableStateFlow<List<TrackedBusInfo>>(emptyList())
    val allTrackedBusesState: StateFlow<List<TrackedBusInfo>> = _allTrackedBusesState.asStateFlow()

    private val _weatherState = MutableStateFlow<WeatherInfo?>(null)
    val weatherState: StateFlow<WeatherInfo?> = _weatherState.asStateFlow()

    private val _rainNowcastState = MutableStateFlow<RainNowcastData?>(null)
    val rainNowcastState: StateFlow<RainNowcastData?> = _rainNowcastState.asStateFlow()

    private val _trackedFlightState = MutableStateFlow<TrackedFlightInfo?>(null)
    val trackedFlightState: StateFlow<TrackedFlightInfo?> = _trackedFlightState.asStateFlow()

    private val _routesCache = MutableStateFlow<List<KmbRouteData>>(emptyList())
    val routesCache: StateFlow<List<KmbRouteData>> = _routesCache.asStateFlow()

    private val _isLoadingRoutes = MutableStateFlow(false)
    val isLoadingRoutes: StateFlow<Boolean> = _isLoadingRoutes.asStateFlow()

    // Cached stops by stopId (thread-safe for concurrent parallel lookups)
    private val stopDetailCache = java.util.concurrent.ConcurrentHashMap<String, KmbStopDetail>()

    init {
        // Preload default route dataset and start polling
        repositoryScope.launch {
            loadRoutes()
        }

        // Cache geofence areas and immediately re-evaluate bus geofences
        repositoryScope.launch {
            allGeofenceAreas.collectLatest { areas ->
                _areasCache.value = areas.associateBy { it.id }
                val currentList = _allTrackedBusesState.value
                if (currentList.isNotEmpty()) {
                    val updatedList = currentList.map { bus ->
                        val (inRange, dist) = isStopInRange(
                            triggerType = bus.triggerType,
                            areaId = bus.areaId,
                            radiusMeters = bus.radiusMeters,
                            stopLat = bus.stopLat,
                            stopLng = bus.stopLng,
                            isGeofenceEnabled = bus.isGeofenceEnabled
                        )
                        val areaName = if (bus.triggerType == "AREA" && bus.areaId != null) {
                            areas.find { it.id == bus.areaId }?.name ?: bus.areaName
                        } else null
                        bus.copy(isInRange = inRange, distanceMeters = dist, areaName = areaName)
                    }
                    _allTrackedBusesState.value = updatedList
                    val currentActive = _trackedBusState.value
                    val matched = updatedList.find { it.stopId == currentActive?.stopId && it.route == currentActive?.route }
                        ?: updatedList.firstOrNull()
                    _trackedBusState.value = matched
                }
            }
        }

        // Real-time location tracker observer: dynamically update in-range status on GPS or simulation movement
        repositoryScope.launch {
            locationTracker.locationState.collectLatest { loc ->
                val currentList = _allTrackedBusesState.value
                if (currentList.isNotEmpty()) {
                    var hasMeaningfulChange = false
                    val updatedList = currentList.map { bus ->
                        val (inRange, dist) = isStopInRange(
                            triggerType = bus.triggerType,
                            areaId = bus.areaId,
                            radiusMeters = bus.radiusMeters,
                            stopLat = bus.stopLat,
                            stopLng = bus.stopLng,
                            isGeofenceEnabled = bus.isGeofenceEnabled
                        )
                        val prevDist = bus.distanceMeters ?: -1f
                        val newDist = dist ?: -1f
                        if (bus.isInRange != inRange || kotlin.math.abs(prevDist - newDist) >= 10f) {
                            hasMeaningfulChange = true
                        }
                        bus.copy(isInRange = inRange, distanceMeters = dist)
                    }
                    if (hasMeaningfulChange) {
                        _allTrackedBusesState.value = updatedList
                        val currentActive = _trackedBusState.value
                        val matched = updatedList.find { it.stopId == currentActive?.stopId && it.route == currentActive?.route }
                            ?: updatedList.firstOrNull()
                        _trackedBusState.value = matched
                    }
                }
            }
        }

        // Listen for all pinned stops in DB and keep all of them tracked with live ETAs
        repositoryScope.launch {
            allPinnedStops.collectLatest { pinnedList ->
                if (pinnedList.isNotEmpty()) {
                    refreshAllTrackedBuses(pinnedList)
                } else {
                    val defaultStop = getFallbackDefaultStop()
                    _trackedBusState.value = defaultStop
                    _allTrackedBusesState.value = listOf(defaultStop)
                }
            }
        }

        // Periodic ETA & Geofence updater loop (Parallelized async for low CPU wake time & battery saving)
        repositoryScope.launch {
            while (true) {
                try {
                    val currentList = _allTrackedBusesState.value
                    if (currentList.isNotEmpty()) {
                        // Concurrent async parallel fetching minimizes cellular radio wake time
                        val updatedList = coroutineScope {
                            currentList.map { bus ->
                                async { computeUpdatedBusInfo(bus) }
                            }.awaitAll()
                        }
                        _allTrackedBusesState.value = updatedList
                        val currentActive = _trackedBusState.value
                        val matched = updatedList.find { it.stopId == currentActive?.stopId && it.route == currentActive?.route }
                            ?: updatedList.firstOrNull()
                        _trackedBusState.value = matched
                    }
                } catch (e: Exception) {
                    // Ignore background loop error
                }
                delay(30000L) // Refresh every 30 seconds
            }
        }

        // Periodic Weather Info updater loop (every 5 minutes or on demand)
        repositoryScope.launch {
            while (true) {
                try {
                    fetchWeatherInfo()
                } catch (e: Exception) {
                    // Ignore
                }
                delay(300000L) // 5 minutes
            }
        }

        // Periodic Rain Nowcast updater loop (respects cooldown setting; 0 = Off)
        repositoryScope.launch {
            while (true) {
                try {
                    val cooldownMin = OverlayStateHolder.config.value.nowcastCooldownMinutes
                    if (cooldownMin > 0) {
                        fetchRainNowcast(force = false)
                    }
                } catch (e: Exception) {
                    // Ignore
                }
                val cooldownMin = OverlayStateHolder.config.value.nowcastCooldownMinutes
                val waitMs = if (cooldownMin > 0) (cooldownMin * 60000L).coerceAtLeast(30000L) else 60000L
                delay(waitMs)
            }
        }

        // Periodic Flight Tracking updater loop (refreshes every 30s when enabled)
        repositoryScope.launch {
            while (true) {
                try {
                    val conf = OverlayStateHolder.config.value
                    if (conf.isFlightTrackingEnabled && conf.trackedFlightQuery.isNotBlank()) {
                        fetchFlightData(conf.trackedFlightQuery)
                    }
                } catch (e: Exception) {
                    // Ignore
                }
                delay(30000L) // 30 seconds
            }
        }

        // Auto-refresh district weather when user moves to a new district / GPS fix updates
        repositoryScope.launch {
            var lastLat = 0.0
            var lastLng = 0.0
            locationTracker.locationState.collect { loc ->
                val dist = LocationTracker.calculateDistanceMeters(lastLat, lastLng, loc.latitude, loc.longitude)
                if (dist > 800 || (lastLat == 0.0 && lastLng == 0.0 && loc.hasRealLocation)) {
                    lastLat = loc.latitude
                    lastLng = loc.longitude
                    try {
                        fetchWeatherInfo()
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            }
        }
    }

    suspend fun fetchWeatherInfo(): WeatherInfo? {
        return try {
            val rhrreadResponse = try { weatherApiService.getRegionalWeather() } catch (e: Exception) { null }
            val fndResponse = try { weatherApiService.getNineDayForecast() } catch (e: Exception) { null }
            val warnsumResponse = try { weatherApiService.getWeatherWarnings() } catch (e: Exception) { null }

            val rhrread = if (rhrreadResponse?.isSuccessful == true) rhrreadResponse.body() else null
            val fnd = if (fndResponse?.isSuccessful == true) fndResponse.body() else null
            val warnsumMap = if (warnsumResponse?.isSuccessful == true) warnsumResponse.body() else null

            // Determine active weather warnings
            val warningList = mutableListOf<WeatherWarning>()

            // 1. Check warnsum map
            warnsumMap?.forEach { (key, item) ->
                val action = item.actionCode?.uppercase() ?: ""
                if (action != "CANCEL" && action.isNotBlank()) {
                    val code = item.code ?: key
                    val name = item.name ?: ""
                    val warning = mapToWeatherWarning(key, code, name, item.type)
                    if (warningList.none { it.code == warning.code }) {
                        warningList.add(warning)
                    }
                }
            }

            // 2. Secondary fallback from rhrread.warningMessage if warnsum is empty
            if (warningList.isEmpty()) {
                rhrread?.warningMessage?.forEach { msg ->
                    if (msg.isNotBlank()) {
                        when {
                            msg.contains("雷暴") && warningList.none { it.code == "WTS" } ->
                                warningList.add(WeatherWarning(code = "WTS", name = "雷暴警告", shortLabel = "雷暴", iconEmoji = "⚡", iconUrl = "https://www.hko.gov.hk/en/wxinfo/dailywx/images/ts.gif", colorHex = 0xFFFFD54F))
                            msg.contains("黑") && msg.contains("暴雨") && warningList.none { it.code == "WRAINB" } ->
                                warningList.add(WeatherWarning(code = "WRAINB", name = "黑色暴雨警告", shortLabel = "黑雨", iconEmoji = "🌧️", iconUrl = "https://www.hko.gov.hk/en/wxinfo/dailywx/images/rainb.gif", colorHex = 0xFF212121))
                            msg.contains("紅") && msg.contains("暴雨") && warningList.none { it.code == "WRAINR" } ->
                                warningList.add(WeatherWarning(code = "WRAINR", name = "紅色暴雨警告", shortLabel = "紅雨", iconEmoji = "🌧️", iconUrl = "https://www.hko.gov.hk/en/wxinfo/dailywx/images/rainr.gif", colorHex = 0xFFFF5252))
                            msg.contains("黃") && msg.contains("暴雨") && warningList.none { it.code == "WRAINA" } ->
                                warningList.add(WeatherWarning(code = "WRAINA", name = "黃色暴雨警告", shortLabel = "黃雨", iconEmoji = "🌧️", iconUrl = "https://www.hko.gov.hk/en/wxinfo/dailywx/images/raina.gif", colorHex = 0xFFFFD740))
                            msg.contains("酷熱") && warningList.none { it.code == "WHOT" } ->
                                warningList.add(WeatherWarning(code = "WHOT", name = "酷熱天氣警告", shortLabel = "酷熱", iconEmoji = "🔥", iconUrl = "https://www.hko.gov.hk/en/wxinfo/dailywx/images/vhot.gif", colorHex = 0xFFFF7043))
                            msg.contains("寒冷") && warningList.none { it.code == "WCOLD" } ->
                                warningList.add(WeatherWarning(code = "WCOLD", name = "寒冷天氣警告", shortLabel = "寒冷", iconEmoji = "❄️", iconUrl = "https://www.hko.gov.hk/en/wxinfo/dailywx/images/cold.gif", colorHex = 0xFF81D4FA))
                            msg.contains("強烈季候風") && warningList.none { it.code == "WMSGNL" } ->
                                warningList.add(WeatherWarning(code = "WMSGNL", name = "強烈季候風信號", shortLabel = "季候風", iconEmoji = "💨", iconUrl = "https://www.hko.gov.hk/en/wxinfo/dailywx/images/sms.gif", colorHex = 0xFFB0BEC5))
                            (msg.contains("十號") || msg.contains("10號")) && warningList.none { it.code == "TC10" } ->
                                warningList.add(WeatherWarning(code = "TC10", name = "十號颶風信號", shortLabel = "10號", iconEmoji = "🌪️", iconUrl = "https://www.hko.gov.hk/en/wxinfo/dailywx/images/tc10.gif", colorHex = 0xFFFF1744))
                            (msg.contains("九號") || msg.contains("9號")) && warningList.none { it.code == "TC9" } ->
                                warningList.add(WeatherWarning(code = "TC9", name = "九號風球", shortLabel = "9號", iconEmoji = "🌪️", iconUrl = "https://www.hko.gov.hk/en/wxinfo/dailywx/images/tc9.gif", colorHex = 0xFFFF1744))
                            (msg.contains("八號") || msg.contains("8號")) && warningList.none { it.code.startsWith("TC8") } ->
                                warningList.add(WeatherWarning(code = "TC8", name = "八號烈風信號", shortLabel = "8號", iconEmoji = "🌪️", iconUrl = "https://www.hko.gov.hk/en/wxinfo/dailywx/images/tc8ne.gif", colorHex = 0xFFFF5252))
                            (msg.contains("三號") || msg.contains("3號")) && warningList.none { it.code == "TC3" } ->
                                warningList.add(WeatherWarning(code = "TC3", name = "三號強風信號", shortLabel = "3號", iconEmoji = "🌪️", iconUrl = "https://www.hko.gov.hk/en/wxinfo/dailywx/images/tc3.gif", colorHex = 0xFFFFB74D))
                            (msg.contains("一號") || msg.contains("1號")) && warningList.none { it.code == "TC1" } ->
                                warningList.add(WeatherWarning(code = "TC1", name = "一號戒備信號", shortLabel = "1號", iconEmoji = "🌪️", iconUrl = "https://www.hko.gov.hk/en/wxinfo/dailywx/images/tc1.gif", colorHex = 0xFFFFD54F))
                            msg.contains("紅色火災") && warningList.none { it.code == "WFIRER" } ->
                                warningList.add(WeatherWarning(code = "WFIRER", name = "紅色火災危險警告", shortLabel = "紅火", iconEmoji = "🔥", iconUrl = "https://www.hko.gov.hk/en/wxinfo/dailywx/images/firer.gif", colorHex = 0xFFFF5252))
                            msg.contains("黃色火災") && warningList.none { it.code == "WFIREY" } ->
                                warningList.add(WeatherWarning(code = "WFIREY", name = "黃色火災危險警告", shortLabel = "黃火", iconEmoji = "🔥", iconUrl = "https://www.hko.gov.hk/en/wxinfo/dailywx/images/firey.gif", colorHex = 0xFFFFD740))
                            msg.contains("山泥傾瀉") && warningList.none { it.code == "WL" } ->
                                warningList.add(WeatherWarning(code = "WL", name = "山泥傾瀉警告", shortLabel = "山泥", iconEmoji = "⛰️", iconUrl = "https://www.hko.gov.hk/en/wxinfo/dailywx/images/landslip.gif", colorHex = 0xFFBCAAA4))
                            (msg.contains("新界北部水浸") || msg.contains("水浸")) && warningList.none { it.code == "WFNTSA" } ->
                                warningList.add(WeatherWarning(code = "WFNTSA", name = "水浸特別報告", shortLabel = "水浸", iconEmoji = "🌊", iconUrl = "https://www.hko.gov.hk/en/wxinfo/dailywx/images/ntfl.gif", colorHex = 0xFF80D8FF))
                        }
                    }
                }
            }

            // Use current GPS location to find the nearest HKO district weather station
            val userLoc = locationTracker.locationState.value
            val nearestStation = HkoStationRegistry.findNearestStation(userLoc.latitude, userLoc.longitude)

            // Look up temperature for the nearest district station in HKO's regional temperature list
            val tempList = rhrread?.temperature?.data ?: emptyList()
            val matchedTempItem = tempList.find { it.place == nearestStation.name }
                ?: tempList.find { it.place.contains(nearestStation.shortName) || nearestStation.name.contains(it.place) }
                ?: tempList.find { it.place == "香港天文台" }
                ?: tempList.firstOrNull()

            val currentTemp = matchedTempItem?.value?.toInt() ?: 26
            val districtDisplayName = when {
                matchedTempItem != null -> {
                    // Match short name for compact clean display (e.g. "沙田", "中環", "尖沙咀", "屯門")
                    val reg = HkoStationRegistry.STATIONS.find { it.name == matchedTempItem.place }
                    reg?.shortName ?: matchedTempItem.place.replace("香港", "")
                }
                else -> nearestStation.shortName
            }

            // Determine humidity: prefer matched district station, otherwise HKO HQ / King's Park
            val humidityList = rhrread?.humidity?.data ?: emptyList()
            val matchedHumidityItem = humidityList.find { it.place == nearestStation.name }
                ?: humidityList.find { it.place == "香港天文台" }
                ?: humidityList.find { it.place == "京士柏" }
                ?: humidityList.firstOrNull()

            val humidityValue = matchedHumidityItem?.value ?: 75

            // Determine today's forecast max & min temp and PSR rain probability from fnd (9-day forecast)
            val todayForecast = fnd?.weatherForecast?.firstOrNull()
            val minTemp = todayForecast?.forecastMintemp?.value?.toInt() ?: (currentTemp - 3)
            val maxTemp = todayForecast?.forecastMaxtemp?.value?.toInt() ?: (currentTemp + 3)
            val psrRaw = todayForecast?.psr?.trim() ?: ""
            val rainProb = when {
                psrRaw.isNotBlank() -> psrRaw
                else -> "低"
            }
            val icon = todayForecast?.forecastIcon ?: rhrread?.icon?.firstOrNull() ?: 50

            val weatherInfo = WeatherInfo(
                districtName = districtDisplayName,
                currentTemp = currentTemp,
                minTemp = minTemp,
                maxTemp = maxTemp,
                humidity = humidityValue,
                rainProbability = rainProb,
                iconId = icon,
                description = todayForecast?.forecastWeather ?: "大致多雲",
                warnings = warningList
            )
            _weatherState.value = weatherInfo
            weatherInfo
        } catch (e: Exception) {
            val fallback = WeatherInfo(
                districtName = "天文台",
                currentTemp = 26,
                minTemp = 23,
                maxTemp = 29,
                humidity = 78,
                rainProbability = "低",
                iconId = 50,
                description = "多雲",
                warnings = emptyList()
            )
            _weatherState.value = fallback
            fallback
        }
    }

    private fun mapToWeatherWarning(key: String, code: String, name: String, type: String?): WeatherWarning {
        return when {
            key == "WTCSGNL" || code.startsWith("TC") -> {
                val label = when {
                    code == "TC10" || name.contains("十號") || name.contains("10") -> "10號"
                    code == "TC9" || name.contains("九號") || name.contains("9") -> "9號"
                    code.startsWith("TC8") || name.contains("八號") || name.contains("8") -> "8號"
                    code == "TC3" || name.contains("三號") || name.contains("3") -> "3號"
                    code == "TC1" || name.contains("一號") || name.contains("1") -> "1號"
                    else -> "風球"
                }
                val iconFile = when {
                    code == "TC10" || label == "10號" -> "tc10.gif"
                    code == "TC9" || label == "9號" -> "tc9.gif"
                    code.equals("TC8NE", ignoreCase = true) -> "tc8ne.gif"
                    code.equals("TC8SE", ignoreCase = true) -> "tc8se.gif"
                    code.equals("TC8NW", ignoreCase = true) -> "tc8nw.gif"
                    code.equals("TC8SW", ignoreCase = true) -> "tc8sw.gif"
                    label == "8號" -> "tc8ne.gif"
                    code == "TC3" || label == "3號" -> "tc3.gif"
                    else -> "tc1.gif"
                }
                val color = if (label == "8號" || label == "9號" || label == "10號") 0xFFFF5252 else 0xFFFFB74D
                WeatherWarning(
                    code = code,
                    name = name.ifBlank { "熱帶氣旋警告" },
                    shortLabel = label,
                    iconEmoji = "🌪️",
                    iconUrl = "https://www.hko.gov.hk/en/wxinfo/dailywx/images/$iconFile",
                    colorHex = color
                )
            }
            key == "WRAIN" || code.startsWith("WRAIN") -> {
                val (label, color, iconFile) = when {
                    code == "WRAINB" || name.contains("黑") || type?.contains("黑") == true ->
                        Triple("黑雨", 0xFF212121, "rainb.gif")
                    code == "WRAINR" || name.contains("紅") || type?.contains("紅") == true ->
                        Triple("紅雨", 0xFFFF5252, "rainr.gif")
                    else ->
                        Triple("黃雨", 0xFFFFD740, "raina.gif")
                }
                WeatherWarning(
                    code = code,
                    name = name.ifBlank { "暴雨警告" },
                    shortLabel = label,
                    iconEmoji = "🌧️",
                    iconUrl = "https://www.hko.gov.hk/en/wxinfo/dailywx/images/$iconFile",
                    colorHex = color
                )
            }
            key == "WTS" || code == "WTS" ->
                WeatherWarning(
                    code = "WTS",
                    name = name.ifBlank { "雷暴警告" },
                    shortLabel = "雷暴",
                    iconEmoji = "⚡",
                    iconUrl = "https://www.hko.gov.hk/en/wxinfo/dailywx/images/ts.gif",
                    colorHex = 0xFFFFD54F
                )
            key == "WHOT" || code == "WHOT" ->
                WeatherWarning(
                    code = "WHOT",
                    name = name.ifBlank { "酷熱天氣警告" },
                    shortLabel = "酷熱",
                    iconEmoji = "🔥",
                    iconUrl = "https://www.hko.gov.hk/en/wxinfo/dailywx/images/vhot.gif",
                    colorHex = 0xFFFF7043
                )
            key == "WCOLD" || code == "WCOLD" ->
                WeatherWarning(
                    code = "WCOLD",
                    name = name.ifBlank { "寒冷天氣警告" },
                    shortLabel = "寒冷",
                    iconEmoji = "❄️",
                    iconUrl = "https://www.hko.gov.hk/en/wxinfo/dailywx/images/cold.gif",
                    colorHex = 0xFF81D4FA
                )
            key == "WFIRE" || code.startsWith("WFIRE") -> {
                val isRed = code == "WFIRER" || name.contains("紅") || type?.contains("紅") == true
                val iconFile = if (isRed) "firer.gif" else "firey.gif"
                WeatherWarning(
                    code = code,
                    name = name.ifBlank { "火災危險警告" },
                    shortLabel = if (isRed) "紅火" else "黃火",
                    iconEmoji = "🔥",
                    iconUrl = "https://www.hko.gov.hk/en/wxinfo/dailywx/images/$iconFile",
                    colorHex = if (isRed) 0xFFFF5252 else 0xFFFFD740
                )
            }
            key == "WMSGNL" || code == "WMSGNL" ->
                WeatherWarning(
                    code = "WMSGNL",
                    name = name.ifBlank { "強烈季候風信號" },
                    shortLabel = "季候風",
                    iconEmoji = "💨",
                    iconUrl = "https://www.hko.gov.hk/en/wxinfo/dailywx/images/sms.gif",
                    colorHex = 0xFFB0BEC5
                )
            key == "WL" || code == "WL" ->
                WeatherWarning(
                    code = "WL",
                    name = name.ifBlank { "山泥傾瀉警告" },
                    shortLabel = "山泥",
                    iconEmoji = "⛰️",
                    iconUrl = "https://www.hko.gov.hk/en/wxinfo/dailywx/images/landslip.gif",
                    colorHex = 0xFFBCAAA4
                )
            key == "WFNTSA" || code == "WFNTSA" ->
                WeatherWarning(
                    code = "WFNTSA",
                    name = name.ifBlank { "水浸特別報告" },
                    shortLabel = "水浸",
                    iconEmoji = "🌊",
                    iconUrl = "https://www.hko.gov.hk/en/wxinfo/dailywx/images/ntfl.gif",
                    colorHex = 0xFF80D8FF
                )
            key == "WFROST" || code == "WFROST" ->
                WeatherWarning(
                    code = "WFROST",
                    name = name.ifBlank { "霜凍警告" },
                    shortLabel = "霜凍",
                    iconEmoji = "🧊",
                    iconUrl = "https://www.hko.gov.hk/en/wxinfo/dailywx/images/frost.gif",
                    colorHex = 0xFF80DEEA
                )
            key == "WTMW" || code == "WTMW" ->
                WeatherWarning(
                    code = "WTMW",
                    name = name.ifBlank { "海嘯警告" },
                    shortLabel = "海嘯",
                    iconEmoji = "🌊",
                    iconUrl = "https://www.hko.gov.hk/en/wxinfo/dailywx/images/tsunami.gif",
                    colorHex = 0xFFFF8A80
                )
            else ->
                WeatherWarning(
                    code = code,
                    name = name,
                    shortLabel = name.take(3),
                    iconEmoji = "⚠️",
                    iconUrl = "",
                    colorHex = 0xFFFFB74D
                )
        }
    }

    suspend fun fetchRainNowcast(force: Boolean = false): RainNowcastData? {
        val current = _rainNowcastState.value
        val cooldownMin = OverlayStateHolder.config.value.nowcastCooldownMinutes
        val now = System.currentTimeMillis()
        if (!force && current != null) {
            if (cooldownMin <= 0 || (now - current.lastFetchedTimestamp) < (cooldownMin * 60000L)) {
                return current
            }
        }

        try {
            val response = weatherApiService.getGriddedRainfallNowcastCsv()
            if (response.isSuccessful) {
                val bodyStr = response.body()?.string()
                if (!bodyStr.isNullOrBlank()) {
                    val parsed = parseNowcastCsv(bodyStr)
                    if (parsed != null && parsed.frames.isNotEmpty()) {
                        _rainNowcastState.value = parsed
                        return parsed
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore API network errors and use fallback
        }

        if (_rainNowcastState.value == null) {
            val fallback = generateSampleNowcastData()
            _rainNowcastState.value = fallback
            return fallback
        }
        return _rainNowcastState.value
    }

    private fun parseNowcastCsv(csvText: String): RainNowcastData? {
        return try {
            val lines = csvText.lines()
            if (lines.size < 2) return null

            // Group data by ending date and time
            val frameMap = linkedMapOf<String, MutableList<RainGridPoint>>()
            var baseUpdateTime = ""

            for (i in 1 until lines.size) {
                val line = lines[i].trim()
                if (line.isBlank()) continue
                val tokens = line.split(",")
                if (tokens.size >= 5) {
                    val updateTime = tokens[0].trim()
                    val endTime = tokens[1].trim()
                    val lat = tokens[2].trim().toDoubleOrNull() ?: continue
                    val lng = tokens[3].trim().toDoubleOrNull() ?: continue
                    val rainMm = tokens[4].trim().toDoubleOrNull() ?: 0.0

                    if (baseUpdateTime.isEmpty()) {
                        baseUpdateTime = updateTime
                    }

                    val points = frameMap.getOrPut(endTime) { mutableListOf() }
                    points.add(RainGridPoint(lat, lng, rainMm))
                }
            }

            if (frameMap.isEmpty()) return null

            val frames = frameMap.map { (endTime, pts) ->
                val formatted = formatNowcastTime(baseUpdateTime, endTime)
                RainNowcastFrame(
                    updateTime = baseUpdateTime,
                    endTime = endTime,
                    formattedTime = formatted,
                    points = pts
                )
            }

            RainNowcastData(
                lastFetchedTimestamp = System.currentTimeMillis(),
                updateTime = baseUpdateTime,
                frames = frames
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun formatNowcastTime(updateTime: String, endTime: String): String {
        return try {
            // Format: YYYYMMDDHHMM
            if (endTime.length >= 12) {
                val hour = endTime.substring(8, 10)
                val min = endTime.substring(10, 12)
                val diffMin = if (updateTime.length >= 12) {
                    val uH = updateTime.substring(8, 10).toIntOrNull() ?: 0
                    val uM = updateTime.substring(10, 12).toIntOrNull() ?: 0
                    val eH = hour.toIntOrNull() ?: 0
                    val eM = min.toIntOrNull() ?: 0
                    (eH * 60 + eM) - (uH * 60 + uM)
                } else 30

                if (diffMin > 0) "$hour:$min (+$diffMin 分鐘)" else "$hour:$min"
            } else {
                endTime
            }
        } catch (e: Exception) {
            endTime
        }
    }

    private fun generateSampleNowcastData(): RainNowcastData {
        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("yyyyMMddHHmm", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("GMT+8")
        val updateStr = sdf.format(Date(now))

        val frames = mutableListOf<RainNowcastFrame>()
        val offsets = listOf(30, 60, 90, 120)

        // Hong Kong grid center points (e.g. Sha Tin, Victoria Harbour, Lantau, Tai Po, Sai Kung)
        val clusters = listOf(
            Triple(22.38, 114.18, 14.5), // Sha Tin / Lion Rock
            Triple(22.31, 114.17, 8.2),  // Kowloon / Victoria Harbour
            Triple(22.45, 114.16, 22.0), // Tai Po / Tolo
            Triple(22.33, 114.26, 6.0),  // Sai Kung
            Triple(22.28, 114.15, 3.5),  // Central / HK Island
            Triple(22.26, 113.95, 12.0)  // Lantau
        )

        for (offset in offsets) {
            val endCalendar = java.util.Calendar.getInstance(TimeZone.getTimeZone("GMT+8"))
            endCalendar.timeInMillis = now + (offset * 60000L)
            val endStr = sdf.format(endCalendar.time)
            val h = String.format(Locale.US, "%02d", endCalendar.get(java.util.Calendar.HOUR_OF_DAY))
            val m = String.format(Locale.US, "%02d", endCalendar.get(java.util.Calendar.MINUTE))
            val formatted = "$h:$m (+$offset 分鐘)"

            val pts = mutableListOf<RainGridPoint>()
            // Generate realistic 0.04 deg grid across HK bounds (lat 22.18..22.52, lng 113.85..114.38)
            var lat = 22.18
            while (lat <= 22.52) {
                var lng = 113.85
                while (lng <= 114.38) {
                    var rain = 0.0
                    for (c in clusters) {
                        // Drift clusters slightly with each frame
                        val cLat = c.first + (offset * 0.0005)
                        val cLng = c.second + (offset * 0.0008)
                        val dLat = lat - cLat
                        val dLng = lng - cLng
                        val dSq = dLat * dLat + dLng * dLng
                        if (dSq < 0.006) {
                            val factor = (1.0 - (dSq / 0.006)).coerceAtLeast(0.0)
                            rain += c.third * factor
                        }
                    }
                    if (rain > 0.3) {
                        pts.add(RainGridPoint(lat, lng, (rain * 10).toInt() / 10.0))
                    }
                    lng += 0.02
                }
                lat += 0.02
            }

            frames.add(
                RainNowcastFrame(
                    updateTime = updateStr,
                    endTime = endStr,
                    formattedTime = formatted,
                    points = pts
                )
            )
        }

        return RainNowcastData(
            lastFetchedTimestamp = now,
            updateTime = updateStr,
            frames = frames
        )
    }

    suspend fun loginFlightdata(email: String, pwd: String): Pair<Boolean, String> {
        val res = flightApiService.login(email, pwd)
        if (res.first) {
            fetchFlightData()
        }
        return res
    }

    suspend fun fetchFlightData(query: String? = null): TrackedFlightInfo? {
        val q = query ?: OverlayStateHolder.config.value.trackedFlightQuery
        if (q.isBlank()) return null
        return try {
            val result = flightApiService.fetchFlightData(q)
            if (result != null) {
                _trackedFlightState.value = result
                OverlayStateHolder.updateTrackedFlight(result)
            }
            result
        } catch (e: Exception) {
            null
        }
    }

    suspend fun triggerImmediateRefresh() {
        locationTracker.requestImmediateLocationUpdate {
            repositoryScope.launch {
                val currentList = _allTrackedBusesState.value
                if (currentList.isNotEmpty()) {
                    val updatedList = currentList.map { computeUpdatedBusInfo(it) }
                    _allTrackedBusesState.value = updatedList
                    val currentActive = _trackedBusState.value
                    val matched = updatedList.find { it.stopId == currentActive?.stopId && it.route == currentActive?.route }
                        ?: updatedList.firstOrNull()
                    _trackedBusState.value = matched
                }
                fetchWeatherInfo()
            }
        }
    }

    suspend fun saveGeofenceArea(name: String, points: List<com.busetaisland.app.data.local.GeoPoint>, colorHex: Long = 0xFF7C4DFF, id: Long = 0): Long {
        val entity = com.busetaisland.app.data.local.GeofenceAreaEntity(
            id = id,
            name = name,
            pointsJson = com.busetaisland.app.data.local.GeofenceAreaEntity.serializePoints(points),
            colorHex = colorHex
        )
        return if (id == 0L) {
            areaDao.insertArea(entity)
        } else {
            areaDao.updateArea(entity)
            id
        }
    }

    suspend fun deleteGeofenceArea(id: Long) {
        areaDao.deleteAreaById(id)
    }

    private suspend fun isStopInRange(
        triggerType: String,
        areaId: Long?,
        radiusMeters: Float,
        stopLat: Double,
        stopLng: Double,
        isGeofenceEnabled: Boolean
    ): Pair<Boolean, Float?> {
        if (!isGeofenceEnabled) return Pair(true, 0f)

        val loc = locationTracker.locationState.value
        val distance = LocationTracker.calculateDistanceMeters(
            loc.latitude, loc.longitude,
            stopLat, stopLng
        )

        if (triggerType.equals("AREA", ignoreCase = true) && areaId != null) {
            val area = _areasCache.value[areaId]
            if (area != null) {
                val pts = area.getPoints()
                if (pts.size >= 3) {
                    val inPoly = LocationTracker.isPointInPolygon(loc.latitude, loc.longitude, pts)
                    return Pair(inPoly, distance)
                }
            }
        }

        // Default: RADIUS
        val inRange = distance <= radiusMeters
        return Pair(inRange, distance)
    }

    private suspend fun refreshAllTrackedBuses(pinnedList: List<PinnedStopEntity>) {
        val list = coroutineScope {
            pinnedList.map { pinned ->
                async(Dispatchers.IO) {
                    val etas = getEta(pinned.co, pinned.stopId, pinned.route, pinned.serviceType, pinned.bound, pinned.seq)
                    val (isInRange, distance) = isStopInRange(
                        triggerType = pinned.triggerType,
                        areaId = pinned.areaId,
                        radiusMeters = pinned.radiusMeters,
                        stopLat = pinned.stopLat,
                        stopLng = pinned.stopLng,
                        isGeofenceEnabled = pinned.isGeofenceEnabled
                    )

                    val areaName = if (pinned.triggerType == "AREA" && pinned.areaId != null) {
                        _areasCache.value[pinned.areaId]?.name
                    } else null

                    TrackedBusInfo(
                        co = pinned.co,
                        route = pinned.route,
                        bound = pinned.bound,
                        serviceType = pinned.serviceType,
                        stopId = pinned.stopId,
                        stopNameEn = pinned.stopNameEn,
                        stopNameTc = pinned.stopNameTc,
                        destEn = pinned.destEn,
                        destTc = pinned.destTc,
                        seq = pinned.seq,
                        stopLat = pinned.stopLat,
                        stopLng = pinned.stopLng,
                        radiusMeters = pinned.radiusMeters,
                        triggerType = pinned.triggerType,
                        areaId = pinned.areaId,
                        areaName = areaName,
                        isGeofenceEnabled = pinned.isGeofenceEnabled,
                        eta1 = etas.getOrNull(0),
                        eta2 = etas.getOrNull(1),
                        eta3 = etas.getOrNull(2),
                        distanceMeters = distance,
                        isInRange = isInRange,
                        lastUpdated = System.currentTimeMillis()
                    )
                }
            }.awaitAll()
        }
        val sortedList = list.sortedWith(
            compareByDescending<TrackedBusInfo> { !it.isGeofenceEnabled || it.isInRange }
                .thenBy { it.eta1?.minutesLeft ?: Int.MAX_VALUE }
                .thenBy { it.distanceMeters ?: Float.MAX_VALUE }
                .thenBy { it.route }
        )
        _allTrackedBusesState.value = sortedList
        val activeEntity = pinnedList.find { it.isActive } ?: pinnedList.firstOrNull()
        if (activeEntity != null) {
            val activeInfo = sortedList.find { it.stopId == activeEntity.stopId && it.route == activeEntity.route } ?: sortedList.first()
            _trackedBusState.value = activeInfo
        } else if (sortedList.isNotEmpty()) {
            _trackedBusState.value = sortedList.first()
        }
    }

    private suspend fun computeUpdatedBusInfo(current: TrackedBusInfo): TrackedBusInfo {
        val etas = getEta(current.co, current.stopId, current.route, current.serviceType, current.bound, current.seq)
        val (isInRange, distance) = isStopInRange(
            triggerType = current.triggerType,
            areaId = current.areaId,
            radiusMeters = current.radiusMeters,
            stopLat = current.stopLat,
            stopLng = current.stopLng,
            isGeofenceEnabled = current.isGeofenceEnabled
        )

        val areaName = if (current.triggerType == "AREA" && current.areaId != null) {
            _areasCache.value[current.areaId]?.name ?: current.areaName
        } else null

        return current.copy(
            eta1 = etas.getOrNull(0),
            eta2 = etas.getOrNull(1),
            eta3 = etas.getOrNull(2),
            distanceMeters = distance,
            isInRange = isInRange,
            areaName = areaName,
            lastUpdated = System.currentTimeMillis()
        )
    }

    suspend fun loadRoutes(): List<KmbRouteData> {
        if (_routesCache.value.isNotEmpty()) return _routesCache.value
        _isLoadingRoutes.value = true
        val combinedRoutes = mutableListOf<KmbRouteData>()

        try {
            coroutineScope {
                val kmbDeferred = async(Dispatchers.IO) {
                    try {
                        apiService.getAllRoutes().data ?: emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
                val ctbDeferred = async(Dispatchers.IO) {
                    try {
                        val list = ctbApiService.getAllRoutes("CTB").data ?: emptyList()
                        list.flatMap { listOf(it.toKmbRouteData("O"), it.toKmbRouteData("I")) }
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
                val nwfbDeferred = async(Dispatchers.IO) {
                    try {
                        val list = ctbApiService.getAllRoutes("NWFB").data ?: emptyList()
                        list.flatMap { listOf(it.toKmbRouteData("O"), it.toKmbRouteData("I")) }
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
                val gmbDeferred = async(Dispatchers.IO) {
                    try {
                        val res = gmbApiService.getAllRoutes()
                        val gmbList = mutableListOf<KmbRouteData>()
                        res.data?.routes?.forEach { (region, routeCodes) ->
                            val regionNameTc = when (region) {
                                "HKI" -> "港島專線小巴"
                                "KLN" -> "九龍專線小巴"
                                else -> "新界專線小巴"
                            }
                            val regionNameEn = when (region) {
                                "HKI" -> "Hong Kong Island"
                                "KLN" -> "Kowloon"
                                else -> "New Territories"
                            }
                            routeCodes.forEach { code ->
                                gmbList.add(
                                    KmbRouteData(
                                        co = "GMB",
                                        route = code,
                                        bound = region,
                                        serviceType = region,
                                        origEn = regionNameEn,
                                        origTc = regionNameTc,
                                        origSc = regionNameTc,
                                        destEn = "Minibus $code",
                                        destTc = "$code 號線",
                                        destSc = "$code 号线"
                                    )
                                )
                            }
                        }
                        gmbList
                    } catch (e: Exception) {
                        emptyList()
                    }
                }

                combinedRoutes.addAll(kmbDeferred.await())
                combinedRoutes.addAll(ctbDeferred.await())
                combinedRoutes.addAll(nwfbDeferred.await())
                combinedRoutes.addAll(gmbDeferred.await())

                // Add MTR Lines (route number is route Chinese name, e.g. 荃灣綫, 觀塘綫, 港島綫, etc.)
                val mtrRoutes = MtrRegistry.LINES.map { line ->
                    KmbRouteData(
                        co = "MTR",
                        route = line.nameTc,
                        bound = "O",
                        serviceType = line.lineCode,
                        origEn = line.downDestinationEn,
                        origTc = line.downDestinationTc,
                        origSc = line.downDestinationTc,
                        destEn = line.upDestinationEn,
                        destTc = line.upDestinationTc,
                        destSc = line.upDestinationTc
                    )
                }
                combinedRoutes.addAll(mtrRoutes)
            }
        } catch (e: Exception) {
            // Log/Fallback
        }
        
        if (combinedRoutes.isNotEmpty()) {
            _routesCache.value = combinedRoutes
            _isLoadingRoutes.value = false
            return combinedRoutes
        }
        val fallback = getFallbackRoutes()
        _routesCache.value = fallback
        _isLoadingRoutes.value = false
        return fallback
    }

    suspend fun getStopsForRoute(co: String, route: String, bound: String, serviceType: String = "1"): List<Pair<KmbRouteStopData, KmbStopDetail?>> {
        val directionParam = if (bound.equals("O", ignoreCase = true) || bound.equals("outbound", ignoreCase = true)) "outbound" else "inbound"

        try {
            if (co == "MTR") {
                val line = MtrRegistry.findLine(serviceType) ?: MtrRegistry.findLine(route)
                if (line != null) {
                    val isOutbound = bound.equals("O", ignoreCase = true) || bound.equals("outbound", ignoreCase = true)
                    val stations = if (isOutbound) line.stations else line.stations.reversed()
                    return stations.mapIndexed { index, st ->
                        val rsd = KmbRouteStopData(
                            co = "MTR",
                            route = line.nameTc,
                            bound = if (isOutbound) "O" else "I",
                            serviceType = line.lineCode,
                            seq = index + 1,
                            stop = st.code,
                            dataTimestamp = null
                        )
                        val detail = KmbStopDetail(
                            stop = st.code,
                            nameEn = st.nameEn,
                            nameTc = st.nameTc,
                            nameSc = st.nameTc,
                            lat = st.lat.toString(),
                            long = st.lng.toString()
                        )
                        stopDetailCache[st.code] = detail
                        Pair(rsd, detail)
                    }
                }
            } else if (co == "GMB") {
                val region = if (serviceType in listOf("HKI", "KLN", "NT")) serviceType else "HKI"
                val routeDetail = gmbApiService.getRouteDetail(region, route)
                val routeList = routeDetail.data
                if (!routeList.isNullOrEmpty()) {
                    val firstRoute = routeList.first()
                    val routeId = firstRoute.routeId
                    val routeSeq = if (bound == "2" || bound.equals("I", ignoreCase = true) || bound.equals("inbound", ignoreCase = true)) 2 else 1
                    val stopsRes = gmbApiService.getRouteStops(routeId, routeSeq)
                    val gmbStops = stopsRes.data?.routeStops
                    if (!gmbStops.isNullOrEmpty()) {
                        return coroutineScope {
                            gmbStops.map { gs ->
                                async(Dispatchers.IO) {
                                    val rsd = KmbRouteStopData(
                                        co = "GMB",
                                        route = route,
                                        bound = "$routeSeq",
                                        serviceType = "$routeId",
                                        seq = gs.stopSeq,
                                        stop = "${gs.stopId}",
                                        dataTimestamp = null
                                    )
                                    val detail = getStopDetail("GMB", "${gs.stopId}", gs.nameTc, gs.nameEn)
                                    Pair(rsd, detail)
                                }
                            }.awaitAll()
                        }
                    }
                }
            } else {
                val routeStops = if (co == "CTB" || co == "NWFB") {
                    val dir = if (bound.equals("O", ignoreCase = true)) "outbound" else "inbound"
                    val res = ctbApiService.getRouteStops(route, dir, co)
                    res.data?.map { it.toKmbRouteStopData() }
                } else {
                    val res = apiService.getRouteStops(route, directionParam, serviceType)
                    res.data
                }
                
                if (!routeStops.isNullOrEmpty()) {
                    val stopsResult = coroutineScope {
                        routeStops.map { rs ->
                            async(Dispatchers.IO) {
                                val detail = getStopDetail(co, rs.stop)
                                Pair(rs, detail)
                            }
                        }.awaitAll()
                    }
                    return stopsResult
                }
            }
        } catch (e: Exception) {
            // Fallback
        }

        // Return fallback stops for route
        return getFallbackRouteStops(route, bound)
    }

    suspend fun getStopDetail(co: String, stopId: String, nameTc: String? = null, nameEn: String? = null): KmbStopDetail? {
        stopDetailCache[stopId]?.let { return it }
        try {
            if (co == "MTR") {
                val found = MtrRegistry.LINES.flatMap { it.stations }.find { it.code.equals(stopId, ignoreCase = true) }
                if (found != null) {
                    val detail = KmbStopDetail(
                        stop = found.code,
                        nameEn = nameEn ?: found.nameEn,
                        nameTc = nameTc ?: found.nameTc,
                        nameSc = nameTc ?: found.nameTc,
                        lat = found.lat.toString(),
                        long = found.lng.toString()
                    )
                    stopDetailCache[stopId] = detail
                    return detail
                }
            } else if (co == "GMB") {
                val stopIdLong = stopId.toLongOrNull() ?: 0L
                val res = gmbApiService.getStopCoordinates(stopIdLong)
                val coords = res.data?.coordinates?.wgs84
                val detail = KmbStopDetail(
                    stop = stopId,
                    nameEn = nameEn ?: "Minibus Stop $stopId",
                    nameTc = nameTc ?: "小巴站 $stopId",
                    nameSc = nameTc ?: "小巴站 $stopId",
                    lat = (coords?.latitude ?: 22.3193).toString(),
                    long = (coords?.longitude ?: 114.1694).toString()
                )
                stopDetailCache[stopId] = detail
                return detail
            } else if (co == "CTB" || co == "NWFB") {
                val response = ctbApiService.getStopDetail(stopId)
                response.data?.toKmbStopDetail()?.let {
                    stopDetailCache[stopId] = it
                    return it
                }
            } else {
                val response = apiService.getStopDetail(stopId)
                response.data?.let {
                    stopDetailCache[stopId] = it
                    return it
                }
            }
        } catch (e: Exception) {
            // Return fallback
        }
        return getFallbackStopDetail(stopId)
    }

    suspend fun getEta(
        co: String,
        stopId: String,
        route: String,
        serviceType: String = "1",
        bound: String = "O",
        seq: Int = 1
    ): List<FormattedEtaItem> {
        try {
            if (co == "MTR") {
                val lineDef = MtrRegistry.findLine(serviceType) ?: MtrRegistry.findLine(route)
                val lineCode = lineDef?.lineCode ?: if (serviceType.length <= 4 && serviceType != "1") serviceType else "TWL"
                val isUp = bound.equals("O", ignoreCase = true) || bound.equals("UP", ignoreCase = true) || bound.equals("outbound", ignoreCase = true)

                if (lineCode.equals("LRT", ignoreCase = true)) {
                    val stationId = stopId.toIntOrNull() ?: 1
                    val lrtRes = mtrApiService.getLrtSchedule(stationId)
                    val platforms = lrtRes.platformList ?: emptyList()
                    val allLrtTrains = mutableListOf<FormattedEtaItem>()
                    var seqIndex = 1
                    platforms.forEach { plat ->
                        plat.routeList?.forEach { item ->
                            val minLeft = item.timeTc?.filter { it.isDigit() }?.toIntOrNull()
                                ?: item.timeEn?.filter { it.isDigit() }?.toIntOrNull()
                                ?: 0
                            val destTc = item.destTc ?: ""
                            val routeNo = item.routeNo ?: ""
                            val now = System.currentTimeMillis()
                            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(now + minLeft * 60000L))
                            val rmk = "$routeNo 往 $destTc"
                            allLrtTrains.add(
                                FormattedEtaItem(
                                    etaSeq = seqIndex++,
                                    etaTimeString = timeStr,
                                    minutesLeft = minLeft,
                                    isDeparted = minLeft == 0,
                                    remark = rmk,
                                    rmkTc = rmk,
                                    isScheduled = false,
                                    rawTimestamp = null
                                )
                            )
                        }
                    }
                    if (allLrtTrains.isNotEmpty()) {
                        return allLrtTrains.take(4)
                    }
                } else {
                    val mtrRes = mtrApiService.getSchedule(line = lineCode, sta = stopId, lang = "TC")
                    val schedule = mtrRes.data?.get("${lineCode}-${stopId}")
                        ?: mtrRes.data?.values?.firstOrNull()
                    val trainList = if (isUp) schedule?.up else schedule?.down
                    if (!trainList.isNullOrEmpty()) {
                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                        sdf.timeZone = TimeZone.getTimeZone("GMT+8")
                        val outSdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                        return trainList.filter { !it.time.isNullOrBlank() }.mapIndexed { index, train ->
                            var minLeft = train.ttnt?.toIntOrNull()
                            var timeFormatted = "--:--"
                            try {
                                val parsed = sdf.parse(train.time!!)
                                if (parsed != null) {
                                    timeFormatted = outSdf.format(parsed)
                                    if (minLeft == null) {
                                        val diffMs = parsed.time - System.currentTimeMillis()
                                        minLeft = (diffMs / 60000).toInt().coerceAtLeast(0)
                                    }
                                }
                            } catch (e: Exception) {}

                            val destTc = MtrRegistry.getStationNameTc(train.dest ?: "")
                            val platInfo = if (!train.plat.isNullOrBlank()) "${train.plat}號月台" else ""
                            val rmk = if (platInfo.isNotBlank()) "往 $destTc ($platInfo)" else "往 $destTc"
                            FormattedEtaItem(
                                etaSeq = train.seq?.toIntOrNull() ?: (index + 1),
                                etaTimeString = timeFormatted,
                                minutesLeft = minLeft ?: 0,
                                isDeparted = (minLeft ?: 0) <= 0 && train.ttnt == "0",
                                remark = rmk,
                                rmkTc = rmk,
                                isScheduled = false,
                                rawTimestamp = train.time
                            )
                        }
                    }
                }
                return emptyList()
            }

            val list = if (co == "GMB") {
                var routeId = serviceType.toLongOrNull()
                val routeSeq = bound.toIntOrNull() ?: if (bound.equals("I", ignoreCase = true) || bound.equals("inbound", ignoreCase = true)) 2 else 1
                if (routeId == null || routeId == 0L) {
                    val region = if (serviceType in listOf("HKI", "KLN", "NT")) serviceType else "HKI"
                    val routeDetail = gmbApiService.getRouteDetail(region, route)
                    routeId = routeDetail.data?.firstOrNull()?.routeId
                }
                if (routeId != null && routeId > 0) {
                    val res = gmbApiService.getEtaByStopSeq(routeId, routeSeq, seq)
                    res.data?.eta?.map { it.toKmbEtaData(route, "$routeSeq", "$routeId", seq, stopId) }
                } else {
                    null
                }
            } else if (co == "CTB" || co == "NWFB") {
                val res = ctbApiService.getEtaForStopRoute(stopId, route, co)
                res.data?.map { it.toKmbEtaData() }
            } else {
                val res = apiService.getEtaForStopRoute(stopId, route, serviceType)
                res.data
            }
            if (!list.isNullOrEmpty()) {
                val formatted = list
                    .filter { it.route.equals(route, ignoreCase = true) && !it.eta.isNullOrBlank() }
                    .sortedBy { it.etaSeq }
                    .map { parseEtaItem(it) }
                return formatted
            }
        } catch (e: Exception) {
            // API failed or no network
        }
        return emptyList()
    }

    private fun parseEtaItem(data: KmbEtaData): FormattedEtaItem {
        val etaStr = data.eta ?: ""
        var minutesLeft = 0
        var timeStr = "--:--"
        var isDeparted = false

        try {
            // Format: "2026-08-26T15:48:00+08:00" or with milliseconds
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("GMT+8")
            val cleanStr = if (etaStr.contains("+")) etaStr.substringBefore("+") else etaStr
            val date = sdf.parse(cleanStr)
            if (date != null) {
                val now = System.currentTimeMillis()
                val diffMs = date.time - now
                minutesLeft = (diffMs / 60000).toInt()
                if (minutesLeft < 0) {
                    isDeparted = true
                    minutesLeft = 0
                }
                val outFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                timeStr = outFormat.format(date)
            }
        } catch (e: Exception) {
            // Fallback parsing
        }

        val isScheduled = if (data.co.equals("GMB", ignoreCase = true)) {
            data.rmkTc.contains("未開出") || data.rmkEn.contains("Not yet departed", ignoreCase = true)
        } else {
            data.rmkTc.contains("原定班次") || data.rmkEn.contains("Scheduled", ignoreCase = true)
        }
        return FormattedEtaItem(
            etaSeq = data.etaSeq,
            etaTimeString = timeStr,
            minutesLeft = minutesLeft,
            isDeparted = isDeparted,
            remark = data.rmkEn.ifBlank { data.rmkTc },
            rmkTc = data.rmkTc,
            isScheduled = isScheduled,
            rawTimestamp = data.eta
        )
    }

    suspend fun setActiveTrackedStop(
        co: String,
        route: String,
        bound: String,
        serviceType: String,
        stopId: String,
        stopNameEn: String,
        stopNameTc: String,
        destEn: String,
        destTc: String,
        seq: Int,
        stopLat: Double,
        stopLng: Double,
        radiusMeters: Float = 300f,
        triggerType: String = "RADIUS",
        areaId: Long? = null,
        isGeofenceEnabled: Boolean = true
    ) {
        pinnedDao.clearActiveFlag()

        val existing = pinnedDao.findPinnedStop(stopId, route, bound)
        val entity = if (existing != null) {
            existing.copy(
                co = co,
                isActive = true,
                radiusMeters = radiusMeters,
                triggerType = triggerType,
                areaId = areaId,
                isGeofenceEnabled = isGeofenceEnabled,
                stopLat = if (stopLat != 0.0) stopLat else existing.stopLat,
                stopLng = if (stopLng != 0.0) stopLng else existing.stopLng
            )
        } else {
            PinnedStopEntity(
                co = co,
                route = route,
                bound = bound,
                serviceType = serviceType,
                stopId = stopId,
                stopNameEn = stopNameEn,
                stopNameTc = stopNameTc,
                destEn = destEn,
                destTc = destTc,
                seq = seq,
                stopLat = stopLat,
                stopLng = stopLng,
                radiusMeters = radiusMeters,
                triggerType = triggerType,
                areaId = areaId,
                isGeofenceEnabled = isGeofenceEnabled,
                isActive = true
            )
        }
        val id = pinnedDao.insertPinnedStop(entity)
        pinnedDao.setActiveById(if (entity.id != 0L) entity.id else id)

        val updated = PinnedStopEntity(
            id = if (entity.id != 0L) entity.id else id,
            co = co,
            route = route,
            bound = bound,
            serviceType = serviceType,
            stopId = stopId,
            stopNameEn = stopNameEn,
            stopNameTc = stopNameTc,
            destEn = destEn,
            destTc = destTc,
            seq = seq,
            stopLat = stopLat,
            stopLng = stopLng,
            radiusMeters = radiusMeters,
            triggerType = triggerType,
            areaId = areaId,
            isGeofenceEnabled = isGeofenceEnabled,
            isActive = true
        )
        refreshTrackedBus(updated)
    }

    suspend fun refreshTrackedBus(pinned: PinnedStopEntity) {
        val etas = getEta(pinned.co, pinned.stopId, pinned.route, pinned.serviceType, pinned.bound, pinned.seq)
        val (isInRange, distance) = isStopInRange(
            triggerType = pinned.triggerType,
            areaId = pinned.areaId,
            radiusMeters = pinned.radiusMeters,
            stopLat = pinned.stopLat,
            stopLng = pinned.stopLng,
            isGeofenceEnabled = pinned.isGeofenceEnabled
        )

        val areaName = if (pinned.triggerType == "AREA" && pinned.areaId != null) {
            _areasCache.value[pinned.areaId]?.name
        } else null

        val info = TrackedBusInfo(
            co = pinned.co,
            route = pinned.route,
            bound = pinned.bound,
            serviceType = pinned.serviceType,
            stopId = pinned.stopId,
            stopNameEn = pinned.stopNameEn,
            stopNameTc = pinned.stopNameTc,
            destEn = pinned.destEn,
            destTc = pinned.destTc,
            seq = pinned.seq,
            stopLat = pinned.stopLat,
            stopLng = pinned.stopLng,
            radiusMeters = pinned.radiusMeters,
            triggerType = pinned.triggerType,
            areaId = pinned.areaId,
            areaName = areaName,
            isGeofenceEnabled = pinned.isGeofenceEnabled,
            eta1 = etas.getOrNull(0),
            eta2 = etas.getOrNull(1),
            eta3 = etas.getOrNull(2),
            distanceMeters = distance,
            isInRange = isInRange,
            lastUpdated = System.currentTimeMillis()
        )
        _trackedBusState.value = info
    }

    suspend fun updateRadiusSetting(radiusMeters: Float, isGeofenceEnabled: Boolean) {
        val current = _trackedBusState.value ?: return
        val activeEntity = pinnedDao.getActiveTrackedStopSync()
        if (activeEntity != null) {
            pinnedDao.updateRadius(activeEntity.id, radiusMeters)
            pinnedDao.updateGeofenceEnabled(activeEntity.id, isGeofenceEnabled)
        }
        val (inRange, distance) = isStopInRange(
            triggerType = current.triggerType,
            areaId = current.areaId,
            radiusMeters = radiusMeters,
            stopLat = current.stopLat,
            stopLng = current.stopLng,
            isGeofenceEnabled = isGeofenceEnabled
        )
        _trackedBusState.value = current.copy(
            radiusMeters = radiusMeters,
            isGeofenceEnabled = isGeofenceEnabled,
            distanceMeters = distance,
            isInRange = inRange
        )
    }

    suspend fun updateStopTriggerMode(id: Long, triggerType: String, areaId: Long?, radiusMeters: Float) {
        pinnedDao.updateTriggerMode(id, triggerType, areaId)
        pinnedDao.updateRadius(id, radiusMeters)
        triggerImmediateRefresh()
    }

    private suspend fun updateEtaAndGeofence(current: TrackedBusInfo) {
        val etas = getEta(current.co, current.stopId, current.route, current.serviceType, current.bound, current.seq)
        val loc = locationTracker.locationState.value
        val distance = LocationTracker.calculateDistanceMeters(
            loc.latitude, loc.longitude,
            current.stopLat, current.stopLng
        )
        val isInRange = !current.isGeofenceEnabled || distance <= current.radiusMeters

        _trackedBusState.value = current.copy(
            eta1 = etas.getOrNull(0),
            eta2 = etas.getOrNull(1),
            eta3 = etas.getOrNull(2),
            distanceMeters = distance,
            isInRange = isInRange,
            lastUpdated = System.currentTimeMillis()
        )
    }

    suspend fun updatePinnedStopRadius(id: Long, radiusMeters: Float) {
        pinnedDao.updateRadius(id, radiusMeters)
        val current = _trackedBusState.value
        val activeEntity = pinnedDao.getActiveTrackedStopSync()
        if (current != null && activeEntity != null && activeEntity.id == id) {
            updateRadiusSetting(radiusMeters, current.isGeofenceEnabled)
        }
    }

    suspend fun deletePinnedStop(id: Long) {
        pinnedDao.deleteById(id)
    }

    // --- Fallback Datasets (Hong Kong Major Routes & MTR Lines) ---
    private fun getFallbackRoutes(): List<KmbRouteData> {
        val mtrRoutes = MtrRegistry.LINES.map { line ->
            KmbRouteData(
                co = "MTR",
                route = line.nameTc,
                bound = "O",
                serviceType = line.lineCode,
                origEn = line.downDestinationEn,
                origTc = line.downDestinationTc,
                origSc = line.downDestinationTc,
                destEn = line.upDestinationEn,
                destTc = line.upDestinationTc,
                destSc = line.upDestinationTc
            )
        }
        return listOf(
            KmbRouteData("KMB", "1A", "O", "1", "SAU MAU PING (CENTRAL)", "中秀茂坪", "中秀茂坪", "STAR FERRY", "尖沙咀碼頭", "尖沙咀码头"),
            KmbRouteData("KMB", "1A", "I", "1", "STAR FERRY", "尖沙咀碼頭", "尖沙咀码头", "SAU MAU PING (CENTRAL)", "中秀茂坪", "中秀茂坪"),
            KmbRouteData("KMB", "74B", "O", "1", "TAI PO CENTRAL", "大埔中心", "大埔中心", "KWUN TONG FERRY", "觀塘碼頭", "观塘码头"),
            KmbRouteData("KMB", "74B", "I", "1", "KOWLOON BAY", "九龍灣", "九龙湾", "TAI PO CENTRAL", "大埔中心", "大埔中心"),
            KmbRouteData("KMB", "960", "O", "1", "HUNG SHUI KIU (HUNG YUEN ROAD)", "洪水橋(洪元路)", "洪水桥(洪元路)", "WAN CHAI NORTH", "灣仔北", "湾仔北"),
            KmbRouteData("KMB", "960", "I", "1", "WAN CHAI NORTH", "灣仔北", "湾仔北", "HUNG SHUI KIU (HUNG YUEN ROAD)", "洪水橋(洪元路)", "洪水桥(洪元路)"),
            KmbRouteData("KMB", "290A", "O", "1", "TSEUNG KWAN O (CHOI MING)", "將軍澳(彩明)", "将军澳(彩明)", "TSUEN WAN WEST STATION", "荃灣西站", "荃湾西站"),
            KmbRouteData("KMB", "290A", "I", "1", "TSUEN WAN WEST STATION", "荃灣西站", "荃湾西站", "TSEUNG KWAN O (CHOI MING)", "將軍澳(彩明)", "将军澳(彩明)"),
            KmbRouteData("KMB", "681", "O", "1", "MA ON SHAN TOWN CENTRE", "馬鞍山市中心", "马鞍山市中心", "CENTRAL (HONG KONG STATION)", "中環(香港站)", "中环(香港站)"),
            KmbRouteData("KMB", "102", "O", "1", "MEI FOO", "美孚", "美孚", "SHAU KEI WAN", "筲箕灣", "筲箕湾"),
            KmbRouteData("KMB", "87D", "O", "1", "KAM YING COURT", "錦英苑", "锦英苑", "HUNG HOM STATION", "紅磡站", "红磡站"),
            KmbRouteData("KMB", "270A", "O", "1", "SHEUNG SHUI", "上水", "上水", "TSIM SHA TSUI EAST (MODY ROAD)", "尖沙咀東(麼地道)", "尖沙咀东(么地道)"),
            KmbRouteData("KMB", "215X", "O", "1", "LAM TIN (KWONG TIN ESTATE)", "藍田(廣田邨)", "蓝田(广田邨)", "KOWLOON STATION", "九龍站", "九龙站"),
            KmbRouteData("KMB", "B1", "O", "1", "TIAN SHUI WAI (TIN TSZ ESTATE)", "天水圍(天慈邨)", "天水围(天慈邨)", "LOK MA CHAU STATION", "落馬洲站", "落马洲站")
        ) + mtrRoutes
    }

    private fun getFallbackRouteStops(route: String, bound: String): List<Pair<KmbRouteStopData, KmbStopDetail?>> {
        when (route.uppercase()) {
            "1A" -> {
                return listOf(
                    Pair(KmbRouteStopData("KMB", "1A", bound, "1", 1, "A3ADFCDF8487ADB9"), KmbStopDetail("A3ADFCDF8487ADB9", "Sau Mau Ping (Central)", "中秀茂坪", "中秀茂坪", "22.318856", "114.231353")),
                    Pair(KmbRouteStopData("KMB", "1A", bound, "1", 2, "STOP_1A_2"), KmbStopDetail("STOP_1A_2", "Sau Ming House", "秀明樓", "秀明楼", "22.320400", "114.229100")),
                    Pair(KmbRouteStopData("KMB", "1A", bound, "1", 3, "STOP_1A_3"), KmbStopDetail("STOP_1A_3", "Kwun Tong Court", "觀塘裁判法院", "观塘裁判法院", "22.312900", "114.225600")),
                    Pair(KmbRouteStopData("KMB", "1A", bound, "1", 4, "STOP_1A_4"), KmbStopDetail("STOP_1A_4", "Kwun Tong Town Centre", "觀塘市中心", "观塘市中心", "22.313800", "114.223800")),
                    Pair(KmbRouteStopData("KMB", "1A", bound, "1", 5, "STOP_1A_5"), KmbStopDetail("STOP_1A_5", "Kowloon Bay Station", "九龍灣站", "九龙湾站", "22.323500", "114.214100")),
                    Pair(KmbRouteStopData("KMB", "1A", bound, "1", 6, "STOP_1A_6"), KmbStopDetail("STOP_1A_6", "Mong Kok Market", "旺角街市", "旺角街市", "22.318200", "114.168500")),
                    Pair(KmbRouteStopData("KMB", "1A", bound, "1", 7, "STOP_1A_7"), KmbStopDetail("STOP_1A_7", "Star Ferry Pier", "尖沙咀碼頭", "尖沙咀码头", "22.294100", "114.168400"))
                )
            }
            "74B" -> {
                return listOf(
                    Pair(KmbRouteStopData("KMB", "74B", bound, "1", 1, "STOP_74B_1"), KmbStopDetail("STOP_74B_1", "Tai Po Central", "大埔中心", "大埔中心", "22.452600", "114.170200")),
                    Pair(KmbRouteStopData("KMB", "74B", bound, "1", 2, "STOP_74B_2"), KmbStopDetail("STOP_74B_2", "Kwong Fuk Estate", "廣福邨", "广福邨", "22.446800", "114.174100")),
                    Pair(KmbRouteStopData("KMB", "74B", bound, "1", 3, "STOP_74B_3"), KmbStopDetail("STOP_74B_3", "Kowloon Bay", "九龍灣", "九龙湾", "22.324200", "114.213600")),
                    Pair(KmbRouteStopData("KMB", "74B", bound, "1", 4, "STOP_74B_4"), KmbStopDetail("STOP_74B_4", "Kwun Tong Ferry", "觀塘碼頭", "观塘码头", "22.308800", "114.223500"))
                )
            }
            else -> {
                return listOf(
                    Pair(KmbRouteStopData("KMB", route, bound, "1", 1, "GEN_STOP_1"), KmbStopDetail("GEN_STOP_1", "$route Bus Terminus", "$route 總站", "$route 总站", "22.319300", "114.169400")),
                    Pair(KmbRouteStopData("KMB", route, bound, "1", 2, "GEN_STOP_2"), KmbStopDetail("GEN_STOP_2", "Central Plaza Stop", "中環廣場分站", "中环广场分站", "22.321000", "114.172000")),
                    Pair(KmbRouteStopData("KMB", route, bound, "1", 3, "GEN_STOP_3"), KmbStopDetail("GEN_STOP_3", "Grand Interchange", "轉乘中心", "转乘中心", "22.325000", "114.176000")),
                    Pair(KmbRouteStopData("KMB", route, bound, "1", 4, "GEN_STOP_4"), KmbStopDetail("GEN_STOP_4", "$route Destination", "$route 終點站", "$route 终点站", "22.330000", "114.180000"))
                )
            }
        }
    }

    private fun getFallbackStopDetail(stopId: String): KmbStopDetail {
        return KmbStopDetail(
            stop = stopId,
            nameEn = "Selected Bus Stop",
            nameTc = "所選巴士站",
            nameSc = "所选巴士站",
            lat = "22.318856",
            long = "114.231353"
        )
    }

    private fun getFallbackDefaultStop(): TrackedBusInfo {
        return TrackedBusInfo(
            route = "1A",
            bound = "O",
            serviceType = "1",
            stopId = "A3ADFCDF8487ADB9",
            stopNameEn = "Sau Mau Ping (Central)",
            stopNameTc = "中秀茂坪",
            destEn = "STAR FERRY",
            destTc = "尖沙咀碼頭",
            seq = 1,
            stopLat = 22.318856,
            stopLng = 114.231353,
            radiusMeters = 300f,
            isGeofenceEnabled = true,
            eta1 = null,
            eta2 = null,
            eta3 = null,
            distanceMeters = 0f,
            isInRange = true
        )
    }
}
