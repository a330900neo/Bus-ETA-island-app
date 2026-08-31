package com.busetaisland.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.busetaisland.app.BusApp
import com.busetaisland.app.data.local.PinnedStopEntity
import com.busetaisland.app.data.model.FormattedEtaItem
import com.busetaisland.app.data.model.KmbRouteData
import com.busetaisland.app.data.model.KmbRouteStopData
import com.busetaisland.app.data.model.KmbStopDetail
import com.busetaisland.app.data.model.MtrRegistry
import com.busetaisland.app.data.model.TrackedBusInfo
import com.busetaisland.app.data.repository.BusRepository
import com.busetaisland.app.service.BusOverlayService
import com.busetaisland.app.service.OverlayDisplayConfig
import com.busetaisland.app.service.OverlayStateHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BusViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BusRepository = (application as BusApp).repository

    val trackedBus: StateFlow<TrackedBusInfo?> = repository.trackedBusState
    val allTrackedBuses: StateFlow<List<TrackedBusInfo>> = repository.allTrackedBusesState
    val weatherState = repository.weatherState
    val rainNowcastState = repository.rainNowcastState
    val trackedFlight = repository.trackedFlightState
    val activeBusIndex: StateFlow<Int> = OverlayStateHolder.currentBusIndex
    val overlayConfig: StateFlow<OverlayDisplayConfig> = OverlayStateHolder.config
    val userLocation = repository.locationTracker.locationState

    val allPinnedStops: StateFlow<List<PinnedStopEntity>> = repository.allPinnedStops.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allGeofenceAreas: StateFlow<List<com.busetaisland.app.data.local.GeofenceAreaEntity>> = repository.allGeofenceAreas.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allRoutes: StateFlow<List<KmbRouteData>> = repository.routesCache
    val isLoadingRoutes: StateFlow<Boolean> = repository.isLoadingRoutes

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCompanyFilter = MutableStateFlow("ALL") // "ALL", "MTR", "KMB", "CTB", "GMB"
    val selectedCompanyFilter: StateFlow<String> = _selectedCompanyFilter.asStateFlow()

    fun setCompanyFilter(co: String) {
        _selectedCompanyFilter.value = co
    }

    val filteredRoutes: StateFlow<List<KmbRouteData>> = combine(allRoutes, _searchQuery, _selectedCompanyFilter) { routes, query, coFilter ->
        val byCo = if (coFilter == "ALL") routes else routes.filter { it.co.equals(coFilter, ignoreCase = true) }
        if (query.isBlank()) {
            if (coFilter == "MTR") byCo else byCo.take(50)
        } else {
            val q = query.trim().uppercase()
            byCo.filter {
                (it.route?.uppercase()?.contains(q) == true) ||
                (it.origEn?.uppercase()?.contains(q) == true) ||
                (it.destEn?.uppercase()?.contains(q) == true) ||
                (it.origTc?.contains(q) == true) ||
                (it.destTc?.contains(q) == true) ||
                (it.co.equals("MTR", ignoreCase = true) && (
                    q == "MTR" || q == "港鐵" || q == "地鐵" || q == "火車" || q == "輕鐵" ||
                    it.serviceType.uppercase().contains(q) ||
                    MtrRegistry.findLine(it.serviceType)?.stations?.any { st -> st.nameTc.contains(q) || st.nameEn.uppercase().contains(q) } == true
                ))
            }.sortedWith(
                compareBy<KmbRouteData> { 
                    val r = it.route ?: ""
                    when {
                        r.equals(q, ignoreCase = true) -> 0
                        r.startsWith(q, ignoreCase = true) -> 1
                        r.contains(q, ignoreCase = true) -> 2
                        else -> 3
                    }
                }.thenBy { (it.route ?: "").length }.thenBy { it.route ?: "" }
            ).take(80)
        }
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedRoute = MutableStateFlow<KmbRouteData?>(null)
    val selectedRoute: StateFlow<KmbRouteData?> = _selectedRoute.asStateFlow()

    private val _selectedBound = MutableStateFlow("O") // "O" = Outbound, "I" = Inbound
    val selectedBound: StateFlow<String> = _selectedBound.asStateFlow()

    private val _routeStops = MutableStateFlow<List<Pair<KmbRouteStopData, KmbStopDetail?>>>(emptyList())
    val routeStops: StateFlow<List<Pair<KmbRouteStopData, KmbStopDetail?>>> = _routeStops.asStateFlow()

    private val _isLoadingStops = MutableStateFlow(false)
    val isLoadingStops: StateFlow<Boolean> = _isLoadingStops.asStateFlow()

    // Live preview ETAs for stops in route stops screen
    private val _stopPreviewEtas = MutableStateFlow<Map<String, List<FormattedEtaItem>>>(emptyMap())
    val stopPreviewEtas: StateFlow<Map<String, List<FormattedEtaItem>>> = _stopPreviewEtas.asStateFlow()

    init {
        // Automatically start location tracking
        repository.locationTracker.startTracking()
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun selectRoute(routeData: KmbRouteData?) {
        _selectedRoute.value = routeData
        if (routeData != null) {
            _selectedBound.value = routeData.bound
            loadStopsForRoute(routeData.co, routeData.route, routeData.bound, routeData.serviceType)
        } else {
            _routeStops.value = emptyList()
        }
    }

    fun clearSelectedRoute() {
        _selectedRoute.value = null
        _routeStops.value = emptyList()
    }

    fun toggleRouteBound(bound: String) {
        _selectedBound.value = bound
        val route = _selectedRoute.value ?: return
        loadStopsForRoute(route.co, route.route, bound, route.serviceType)
    }

    fun loadStopsForRoute(co: String, route: String, bound: String, serviceType: String = "1") {
        viewModelScope.launch {
            _isLoadingStops.value = true
            _stopPreviewEtas.value = emptyMap()
            val stops = repository.getStopsForRoute(co, route, bound, serviceType)
            _routeStops.value = stops
            _isLoadingStops.value = false

            // Pre-fetch ETA for top 4 stops for quick preview
            stops.take(4).forEach { (stopData, _) ->
                fetchEtaPreviewForStop(co, stopData.stop, route, serviceType, stopData.bound ?: bound, stopData.seq)
            }
        }
    }

    fun fetchEtaPreviewForStop(
        co: String,
        stopId: String,
        route: String,
        serviceType: String = "1",
        bound: String = "O",
        seq: Int = 1
    ) {
        viewModelScope.launch {
            val etas = repository.getEta(co, stopId, route, serviceType, bound, seq)
            _stopPreviewEtas.value = _stopPreviewEtas.value + (stopId to etas)
        }
    }

    fun trackStopOnIsland(
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
        radiusMeters: Float = 300f
    ) {
        viewModelScope.launch {
            repository.setActiveTrackedStop(
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
                isGeofenceEnabled = true
            )
            // Auto start overlay service & enable overlay
            BusOverlayService.start(getApplication())
            OverlayStateHolder.setOverlayEnabled(true)
        }
    }

    fun selectPinnedStopToTrack(pinned: PinnedStopEntity) {
        viewModelScope.launch {
            repository.setActiveTrackedStop(
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
                isGeofenceEnabled = pinned.isGeofenceEnabled
            )
            BusOverlayService.start(getApplication())
            OverlayStateHolder.setOverlayEnabled(true)
        }
    }

    fun deletePinned(id: Long) {
        viewModelScope.launch {
            repository.deletePinnedStop(id)
        }
    }

    fun setOverlayEnabled(enabled: Boolean) {
        if (enabled) {
            BusOverlayService.start(getApplication())
            OverlayStateHolder.setOverlayEnabled(true)
        } else {
            OverlayStateHolder.setOverlayEnabled(false)
        }
    }

    fun toggleOverlayCollapsed() {
        OverlayStateHolder.toggleCollapsed()
    }

    fun toggleEtaUnit() {
        OverlayStateHolder.toggleEtaUnit()
    }

    fun setEtaUnit(unit: com.busetaisland.app.data.model.EtaDisplayUnit) {
        OverlayStateHolder.setEtaUnit(unit)
    }

    fun updateRadius(radiusMeters: Float, isGeofenceEnabled: Boolean = true) {
        viewModelScope.launch {
            repository.updateRadiusSetting(radiusMeters, isGeofenceEnabled)
        }
    }

    fun updateStopSpecificRadius(id: Long, radiusMeters: Float) {
        viewModelScope.launch {
            repository.updatePinnedStopRadius(id, radiusMeters)
        }
    }

    fun setVerticalOffset(offsetY: Int) {
        OverlayStateHolder.setVerticalOffset(offsetY)
    }

    fun setCollapsedRadiusDp(radiusDp: Int) {
        OverlayStateHolder.setCollapsedRadiusDp(radiusDp)
    }

    fun saveGeofenceArea(name: String, points: List<com.busetaisland.app.data.local.GeoPoint>, colorHex: Long = 0xFF7C4DFF, id: Long = 0) {
        viewModelScope.launch {
            repository.saveGeofenceArea(name, points, colorHex, id)
        }
    }

    fun deleteGeofenceArea(id: Long) {
        viewModelScope.launch {
            repository.deleteGeofenceArea(id)
        }
    }

    fun updateStopTriggerMode(id: Long, triggerType: String, areaId: Long?, radiusMeters: Float) {
        viewModelScope.launch {
            repository.updateStopTriggerMode(id, triggerType, areaId, radiusMeters)
        }
    }

    fun setAutoCenterOnExpand(enabled: Boolean) {
        OverlayStateHolder.setAutoCenterOnExpand(enabled)
    }

    fun setAutoExpandOnScreenOn(enabled: Boolean) {
        OverlayStateHolder.setAutoExpandOnScreenOn(enabled)
    }

    fun setLockTopPositionOnExpand(enabled: Boolean) {
        OverlayStateHolder.setLockTopPositionOnExpand(enabled)
    }

    fun setAnimationDurations(expandMs: Int, collapseMs: Int) {
        OverlayStateHolder.setAnimationDurations(expandMs, collapseMs)
    }

    fun toggleNowcastMap() {
        OverlayStateHolder.toggleNowcastMap()
        viewModelScope.launch {
            repository.fetchRainNowcast(force = false)
        }
    }

    fun setNowcastCycleDurationSec(sec: Float) {
        OverlayStateHolder.setNowcastCycleDurationSec(sec)
    }

    fun setNowcastCooldownMinutes(minutes: Int) {
        OverlayStateHolder.setNowcastCooldownMinutes(minutes)
    }

    fun setNowcastCenterGps(enabled: Boolean) {
        OverlayStateHolder.setNowcastCenterGps(enabled)
    }

    fun refreshNowcastData() {
        viewModelScope.launch {
            repository.fetchRainNowcast(force = true)
        }
    }

    fun selectNextBus() {
        OverlayStateHolder.selectNextBus()
    }

    fun selectPreviousBus() {
        OverlayStateHolder.selectPreviousBus()
    }

    fun selectBusIndex(index: Int) {
        OverlayStateHolder.selectBusIndex(index)
    }

    fun triggerManualRefresh() {
        viewModelScope.launch {
            OverlayStateHolder.requestManualRefresh()
        }
    }

    fun simulateDistance(distanceMeters: Float) {
        val current = trackedBus.value
        if (current != null) {
            repository.locationTracker.setSimulatedDistanceToStop(
                current.stopLat,
                current.stopLng,
                distanceMeters
            )
        } else {
            repository.locationTracker.setSimulatedLocation(22.318856, 114.231353)
        }
    }

    fun resetSimulationToRealGPS() {
        repository.locationTracker.disableSimulation()
    }

    fun refreshActiveEta() {
        OverlayStateHolder.requestManualRefresh()
    }

    fun setFlightTrackingEnabled(enabled: Boolean) {
        OverlayStateHolder.setFlightTrackingEnabled(enabled)
        if (enabled) {
            viewModelScope.launch {
                repository.fetchFlightData()
            }
        }
    }

    fun setTrackedFlightQuery(query: String) {
        OverlayStateHolder.setTrackedFlightQuery(query)
        viewModelScope.launch {
            repository.fetchFlightData(query)
        }
    }

    fun updateFlightdataCredentials(email: String, pwd: String) {
        OverlayStateHolder.setFlightdataCredentials(email, pwd)
    }

    fun loginFlightdata(email: String, pwd: String, onResult: (Boolean, String) -> Unit) {
        OverlayStateHolder.setFlightdataCredentials(email, pwd)
        viewModelScope.launch {
            val res = repository.loginFlightdata(email, pwd)
            onResult(res.first, res.second)
        }
    }

    fun logoutFlightdata() {
        OverlayStateHolder.setFlightdataCredentials("", "")
        OverlayStateHolder.setFlightdataAuthToken("")
        OverlayStateHolder.setFlightdataLoginStatus("訪客模式 (Guest)")
    }
}
