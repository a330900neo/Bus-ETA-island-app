package com.busetaisland.app.service

import android.content.Context
import android.content.SharedPreferences
import com.busetaisland.app.data.model.EtaDisplayUnit
import com.busetaisland.app.data.model.TrackedBusInfo
import com.busetaisland.app.data.model.TrackedFlightInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OverlayDisplayConfig(
    val isOverlayEnabled: Boolean = false,
    val isCollapsed: Boolean = false, // false = Dynamic Island Pill, true = Mini Floating Circle
    val showWeatherInfo: Boolean = false, // Weather info line visibility (click island again to show, swipe up to hide)
    val showNowcastMap: Boolean = false, // Rain nowcast radar map visibility below warnings
    val showTimelineBar: Boolean = false, // Big horizontal upcoming bus timeline bar below routes (default OFF)
    val timelineWindowMinutes: Int = 30, // User-tuneable timeline scale limit (e.g. 15, 30, 45, 60 min)
    val isFlightTrackingEnabled: Boolean = false, // Optional flight tracking module
    val trackedFlightQuery: String = "B-LRA", // Callsign / Registration e.g. "B-LRA" or "B-LNJ"
    val flightdataEmail: String = "", // pyflightdata / FlightRadar24 login email
    val flightdataPassword: String = "", // pyflightdata / FlightRadar24 login password
    val flightdataAuthToken: String = "", // cached session / user token
    val flightdataLoginStatus: String = "訪客模式 (Guest)", // Login status
    val nowcastCycleDurationSec: Float = 3.0f, // One loop cycle duration for nowcast animation (e.g. 1.0s to 8.0s)
    val nowcastCooldownMinutes: Int = 12, // Cooldown in minutes: 0 = Off, 5, 10, 12, 15, 30 min
    val nowcastCenterGps: Boolean = true, // Center rain nowcast map on user's GPS location
    val collapsedRadiusDp: Int = 24, // Radius of collapsed circle in dp (diameter = collapsedRadiusDp * 2)
    val etaUnit: EtaDisplayUnit = EtaDisplayUnit.MINUTES, // MINUTES vs EXACT_TIME
    val isRefreshing: Boolean = false,
    val autoExpandOnScreenOn: Boolean = true,
    val showOnLockscreen: Boolean = true,
    val autoCenterOnExpand: Boolean = true, // Auto-snap to center X on expand
    val lockTopPositionOnExpand: Boolean = true, // Auto-snap to top vertical offset on expand
    val expandDurationMs: Int = 220, // Snappy expand animation duration in ms
    val collapseDurationMs: Int = 180, // Responsive collapse animation duration in ms
    val circlePosX: Int = 0, // Saved dragged circle X position
    val circlePosY: Int = 80, // Saved dragged circle Y position
    val topVerticalOffset: Int = 24, // Top Limit / Offset from screen top in Settings
    val isWindowAtTop: Boolean = false, // True when window manager is at top center
    val isServiceRunning: Boolean = false,
    val isAccessibilityEnabled: Boolean = false,
    val activeStopIndex: Int = 0
) {
    val posX: Int get() = circlePosX
    val posY: Int get() = topVerticalOffset
}

object OverlayStateHolder {
    private const val PREFS_NAME = "dynamic_island_overlay_prefs"
    private const val KEY_OVERLAY_ENABLED = "overlay_enabled"
    private const val KEY_IS_COLLAPSED = "is_collapsed"
    private const val KEY_ETA_UNIT = "eta_unit"
    private const val KEY_AUTO_EXPAND = "auto_expand_screen_on"
    private const val KEY_SHOW_LOCKSCREEN = "show_on_lockscreen"
    private const val KEY_AUTO_CENTER = "auto_center_expand"
    private const val KEY_LOCK_TOP = "lock_top_position"
    private const val KEY_EXPAND_DURATION = "expand_duration_ms"
    private const val KEY_COLLAPSE_DURATION = "collapse_duration_ms"
    private const val KEY_CIRCLE_POS_X = "circle_pos_x"
    private const val KEY_CIRCLE_POS_Y = "circle_pos_y"
    private const val KEY_TOP_OFFSET = "top_vertical_offset"
    private const val KEY_COLLAPSED_RADIUS = "collapsed_radius_dp"
    private const val KEY_NOWCAST_CYCLE_SEC = "nowcast_cycle_sec"
    private const val KEY_NOWCAST_COOLDOWN_MIN = "nowcast_cooldown_min"
    private const val KEY_NOWCAST_CENTER_GPS = "nowcast_center_gps"
    private const val KEY_SHOW_TIMELINE_BAR = "show_timeline_bar"
    private const val KEY_TIMELINE_WINDOW_MIN = "timeline_window_min"
    private const val KEY_FLIGHT_TRACKING_ENABLED = "flight_tracking_enabled"
    private const val KEY_TRACKED_FLIGHT_QUERY = "tracked_flight_query"
    private const val KEY_FLIGHTDATA_EMAIL = "flightdata_email"
    private const val KEY_FLIGHTDATA_PASSWORD = "flightdata_password"
    private const val KEY_FLIGHTDATA_AUTH_TOKEN = "flightdata_auth_token"

    private var sharedPreferences: SharedPreferences? = null

    private val _config = MutableStateFlow(OverlayDisplayConfig())
    val config: StateFlow<OverlayDisplayConfig> = _config.asStateFlow()

    private val _trackedBus = MutableStateFlow<TrackedBusInfo?>(null)
    val trackedBus: StateFlow<TrackedBusInfo?> = _trackedBus.asStateFlow()

    // Support tracking multiple bus stops simultaneously
    private val _allTrackedBuses = MutableStateFlow<List<TrackedBusInfo>>(emptyList())
    val allTrackedBuses: StateFlow<List<TrackedBusInfo>> = _allTrackedBuses.asStateFlow()

    // Live tracked flight state
    private val _trackedFlight = MutableStateFlow<TrackedFlightInfo?>(null)
    val trackedFlight: StateFlow<TrackedFlightInfo?> = _trackedFlight.asStateFlow()

    private val _currentBusIndex = MutableStateFlow(0)
    val currentBusIndex: StateFlow<Int> = _currentBusIndex.asStateFlow()

    // Shared flow to trigger immediate manual refresh from Island hold gesture
    private val _refreshRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val refreshRequests: SharedFlow<Unit> = _refreshRequests.asSharedFlow()

    fun init(context: Context) {
        if (sharedPreferences != null) return
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences = prefs

        val isEnabled = prefs.getBoolean(KEY_OVERLAY_ENABLED, false)
        val isCollapsed = prefs.getBoolean(KEY_IS_COLLAPSED, false)
        val collapsedRadius = prefs.getInt(KEY_COLLAPSED_RADIUS, 24)
        val etaUnitStr = prefs.getString(KEY_ETA_UNIT, EtaDisplayUnit.MINUTES.name) ?: EtaDisplayUnit.MINUTES.name
        val etaUnit = try { EtaDisplayUnit.valueOf(etaUnitStr) } catch (e: Exception) { EtaDisplayUnit.MINUTES }
        val autoExpand = prefs.getBoolean(KEY_AUTO_EXPAND, true)
        val showLockscreen = prefs.getBoolean(KEY_SHOW_LOCKSCREEN, true)
        val autoCenter = prefs.getBoolean(KEY_AUTO_CENTER, true)
        val lockTop = prefs.getBoolean(KEY_LOCK_TOP, true)
        val expandDuration = prefs.getInt(KEY_EXPAND_DURATION, 220)
        val collapseDuration = prefs.getInt(KEY_COLLAPSE_DURATION, 180)
        val circleX = prefs.getInt(KEY_CIRCLE_POS_X, 0)
        val circleY = prefs.getInt(KEY_CIRCLE_POS_Y, 80)
        val topOffset = prefs.getInt(KEY_TOP_OFFSET, 24)
        val nowcastCycle = prefs.getFloat(KEY_NOWCAST_CYCLE_SEC, 3.0f)
        val nowcastCooldown = prefs.getInt(KEY_NOWCAST_COOLDOWN_MIN, 12)
        val nowcastCenterGps = prefs.getBoolean(KEY_NOWCAST_CENTER_GPS, true)
        val showTimelineBar = prefs.getBoolean(KEY_SHOW_TIMELINE_BAR, false)
        val timelineWindow = prefs.getInt(KEY_TIMELINE_WINDOW_MIN, 30)
        val flightTrackingEnabled = prefs.getBoolean(KEY_FLIGHT_TRACKING_ENABLED, false)
        val trackedFlightQuery = prefs.getString(KEY_TRACKED_FLIGHT_QUERY, "B-LRA") ?: "B-LRA"
        val flightEmail = prefs.getString(KEY_FLIGHTDATA_EMAIL, "") ?: ""
        val flightPassword = prefs.getString(KEY_FLIGHTDATA_PASSWORD, "") ?: ""
        val flightToken = prefs.getString(KEY_FLIGHTDATA_AUTH_TOKEN, "") ?: ""
        val initialStatus = if (flightEmail.isNotBlank()) "已設定帳號 (Configured)" else "訪客模式 (Guest)"

        _config.value = _config.value.copy(
            isOverlayEnabled = isEnabled,
            isCollapsed = isCollapsed,
            collapsedRadiusDp = collapsedRadius,
            etaUnit = etaUnit,
            autoExpandOnScreenOn = autoExpand,
            showOnLockscreen = showLockscreen,
            autoCenterOnExpand = autoCenter,
            lockTopPositionOnExpand = lockTop,
            expandDurationMs = expandDuration,
            collapseDurationMs = collapseDuration,
            circlePosX = circleX,
            circlePosY = circleY,
            topVerticalOffset = topOffset,
            nowcastCycleDurationSec = nowcastCycle,
            nowcastCooldownMinutes = nowcastCooldown,
            nowcastCenterGps = nowcastCenterGps,
            showTimelineBar = showTimelineBar,
            timelineWindowMinutes = timelineWindow,
            isFlightTrackingEnabled = flightTrackingEnabled,
            trackedFlightQuery = trackedFlightQuery,
            flightdataEmail = flightEmail,
            flightdataPassword = flightPassword,
            flightdataAuthToken = flightToken,
            flightdataLoginStatus = initialStatus
        )
    }

    private var lastPersistedConfig: OverlayDisplayConfig? = null

    private fun persistConfig(c: OverlayDisplayConfig) {
        val last = lastPersistedConfig
        if (last != null &&
            last.isOverlayEnabled == c.isOverlayEnabled &&
            last.isCollapsed == c.isCollapsed &&
            last.collapsedRadiusDp == c.collapsedRadiusDp &&
            last.etaUnit == c.etaUnit &&
            last.autoExpandOnScreenOn == c.autoExpandOnScreenOn &&
            last.showOnLockscreen == c.showOnLockscreen &&
            last.autoCenterOnExpand == c.autoCenterOnExpand &&
            last.lockTopPositionOnExpand == c.lockTopPositionOnExpand &&
            last.expandDurationMs == c.expandDurationMs &&
            last.collapseDurationMs == c.collapseDurationMs &&
            last.circlePosX == c.circlePosX &&
            last.circlePosY == c.circlePosY &&
            last.topVerticalOffset == c.topVerticalOffset &&
            last.nowcastCycleDurationSec == c.nowcastCycleDurationSec &&
            last.nowcastCooldownMinutes == c.nowcastCooldownMinutes &&
            last.nowcastCenterGps == c.nowcastCenterGps &&
            last.showTimelineBar == c.showTimelineBar &&
            last.timelineWindowMinutes == c.timelineWindowMinutes &&
            last.isFlightTrackingEnabled == c.isFlightTrackingEnabled &&
            last.trackedFlightQuery == c.trackedFlightQuery &&
            last.flightdataEmail == c.flightdataEmail &&
            last.flightdataPassword == c.flightdataPassword &&
            last.flightdataAuthToken == c.flightdataAuthToken
        ) {
            return // Skip unnecessary disk I/O when persistent fields have not changed
        }
        lastPersistedConfig = c
        sharedPreferences?.edit()?.apply {
            putBoolean(KEY_OVERLAY_ENABLED, c.isOverlayEnabled)
            putBoolean(KEY_IS_COLLAPSED, c.isCollapsed)
            putInt(KEY_COLLAPSED_RADIUS, c.collapsedRadiusDp)
            putString(KEY_ETA_UNIT, c.etaUnit.name)
            putBoolean(KEY_AUTO_EXPAND, c.autoExpandOnScreenOn)
            putBoolean(KEY_SHOW_LOCKSCREEN, c.showOnLockscreen)
            putBoolean(KEY_AUTO_CENTER, c.autoCenterOnExpand)
            putBoolean(KEY_LOCK_TOP, c.lockTopPositionOnExpand)
            putInt(KEY_EXPAND_DURATION, c.expandDurationMs)
            putInt(KEY_COLLAPSE_DURATION, c.collapseDurationMs)
            putInt(KEY_CIRCLE_POS_X, c.circlePosX)
            putInt(KEY_CIRCLE_POS_Y, c.circlePosY)
            putInt(KEY_TOP_OFFSET, c.topVerticalOffset)
            putFloat(KEY_NOWCAST_CYCLE_SEC, c.nowcastCycleDurationSec)
            putInt(KEY_NOWCAST_COOLDOWN_MIN, c.nowcastCooldownMinutes)
            putBoolean(KEY_NOWCAST_CENTER_GPS, c.nowcastCenterGps)
            putBoolean(KEY_SHOW_TIMELINE_BAR, c.showTimelineBar)
            putInt(KEY_TIMELINE_WINDOW_MIN, c.timelineWindowMinutes)
            putBoolean(KEY_FLIGHT_TRACKING_ENABLED, c.isFlightTrackingEnabled)
            putString(KEY_TRACKED_FLIGHT_QUERY, c.trackedFlightQuery)
            putString(KEY_FLIGHTDATA_EMAIL, c.flightdataEmail)
            putString(KEY_FLIGHTDATA_PASSWORD, c.flightdataPassword)
            putString(KEY_FLIGHTDATA_AUTH_TOKEN, c.flightdataAuthToken)
            apply()
        }
    }

    fun updateConfig(update: (OverlayDisplayConfig) -> OverlayDisplayConfig) {
        val newConf = update(_config.value)
        _config.value = newConf
        persistConfig(newConf)
    }

    fun setOverlayEnabled(enabled: Boolean) {
        updateConfig { it.copy(isOverlayEnabled = enabled) }
    }

    fun toggleCollapsed() {
        val next = !_config.value.isCollapsed
        updateConfig { it.copy(isCollapsed = next) }
    }

    fun setCollapsed(collapsed: Boolean) {
        updateConfig { it.copy(isCollapsed = collapsed) }
    }

    fun toggleWeatherInfo() {
        updateConfig { it.copy(showWeatherInfo = !it.showWeatherInfo) }
    }

    fun setWeatherInfoVisible(visible: Boolean) {
        updateConfig { it.copy(showWeatherInfo = visible) }
    }

    fun toggleNowcastMap() {
        updateConfig { it.copy(showNowcastMap = !it.showNowcastMap) }
    }

    fun setNowcastMapVisible(visible: Boolean) {
        updateConfig { it.copy(showNowcastMap = visible) }
    }

    fun setNowcastCycleDurationSec(sec: Float) {
        updateConfig { it.copy(nowcastCycleDurationSec = sec.coerceIn(1.0f, 10.0f)) }
    }

    fun setNowcastCooldownMinutes(minutes: Int) {
        updateConfig { it.copy(nowcastCooldownMinutes = minutes) }
    }

    fun setNowcastCenterGps(enabled: Boolean) {
        updateConfig { it.copy(nowcastCenterGps = enabled) }
    }

    fun setTimelineBarVisible(visible: Boolean) {
        updateConfig { it.copy(showTimelineBar = visible) }
    }

    fun setTimelineWindowMinutes(minutes: Int) {
        updateConfig { it.copy(timelineWindowMinutes = minutes.coerceIn(10, 120)) }
    }

    fun setFlightTrackingEnabled(enabled: Boolean) {
        updateConfig { it.copy(isFlightTrackingEnabled = enabled) }
    }

    fun setTrackedFlightQuery(query: String) {
        updateConfig { it.copy(trackedFlightQuery = query.trim().uppercase()) }
    }

    fun setFlightdataCredentials(email: String, pwd: String) {
        updateConfig { it.copy(flightdataEmail = email.trim(), flightdataPassword = pwd) }
    }

    fun setFlightdataAuthToken(token: String) {
        updateConfig { it.copy(flightdataAuthToken = token) }
    }

    fun setFlightdataLoginStatus(status: String) {
        _config.value = _config.value.copy(flightdataLoginStatus = status)
    }

    fun updateTrackedFlight(flight: TrackedFlightInfo?) {
        _trackedFlight.value = flight
    }

    fun toggleEtaUnit() {
        val nextUnit = if (_config.value.etaUnit == EtaDisplayUnit.MINUTES) {
            EtaDisplayUnit.EXACT_TIME
        } else {
            EtaDisplayUnit.MINUTES
        }
        updateConfig { it.copy(etaUnit = nextUnit) }
    }

    fun setEtaUnit(unit: EtaDisplayUnit) {
        updateConfig { it.copy(etaUnit = unit) }
    }

    fun setRefreshing(refreshing: Boolean) {
        _config.value = _config.value.copy(isRefreshing = refreshing)
    }

    private val stateScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun requestManualRefresh() {
        _refreshRequests.tryEmit(Unit)
        try {
            stateScope.launch {
                setRefreshing(true)
                com.busetaisland.app.BusApp.instance.repository.triggerImmediateRefresh()
                delay(1200L)
                setRefreshing(false)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun updateCirclePosition(x: Int, y: Int) {
        updateConfig { it.copy(circlePosX = x, circlePosY = y) }
    }

    fun updatePosition(x: Int, y: Int) {
        updateCirclePosition(x, y)
    }

    fun setVerticalOffset(offsetY: Int) {
        updateConfig { it.copy(topVerticalOffset = offsetY.coerceAtLeast(0)) }
    }

    fun setCollapsedRadiusDp(radiusDp: Int) {
        updateConfig { it.copy(collapsedRadiusDp = radiusDp.coerceIn(12, 50)) }
    }

    fun setAutoCenterOnExpand(enabled: Boolean) {
        updateConfig { it.copy(autoCenterOnExpand = enabled) }
    }

    fun setAutoExpandOnScreenOn(enabled: Boolean) {
        updateConfig { it.copy(autoExpandOnScreenOn = enabled) }
    }

    fun setLockTopPositionOnExpand(enabled: Boolean) {
        updateConfig { it.copy(lockTopPositionOnExpand = enabled) }
    }

    fun setAnimationDurations(expandMs: Int, collapseMs: Int) {
        updateConfig {
            it.copy(
                expandDurationMs = expandMs.coerceIn(100, 1000),
                collapseDurationMs = collapseMs.coerceIn(100, 1000)
            )
        }
    }

    fun updateTrackedBus(bus: TrackedBusInfo?) {
        _trackedBus.value = bus
        if (bus != null && _allTrackedBuses.value.isEmpty()) {
            _allTrackedBuses.value = listOf(bus)
        }
    }

    fun updateAllTrackedBuses(buses: List<TrackedBusInfo>) {
        _allTrackedBuses.value = buses
        if (buses.isNotEmpty()) {
            val validIndex = _currentBusIndex.value.coerceIn(0, buses.size - 1)
            _currentBusIndex.value = validIndex
            _trackedBus.value = buses[validIndex]
        }
    }

    fun selectNextBus() {
        val list = _allTrackedBuses.value
        if (list.size > 1) {
            val next = (_currentBusIndex.value + 1) % list.size
            _currentBusIndex.value = next
            _trackedBus.value = list[next]
        }
    }

    fun selectPreviousBus() {
        val list = _allTrackedBuses.value
        if (list.size > 1) {
            val prev = if (_currentBusIndex.value - 1 < 0) list.size - 1 else _currentBusIndex.value - 1
            _currentBusIndex.value = prev
            _trackedBus.value = list[prev]
        }
    }

    fun selectBusIndex(index: Int) {
        val list = _allTrackedBuses.value
        if (index in list.indices) {
            _currentBusIndex.value = index
            _trackedBus.value = list[index]
        }
    }

    fun getActiveBus(): TrackedBusInfo? {
        val list = _allTrackedBuses.value
        val idx = _currentBusIndex.value
        return if (list.isNotEmpty() && idx in list.indices) {
            list[idx]
        } else {
            _trackedBus.value
        }
    }
}
