package com.busetaisland.app.ui.overlay

import android.graphics.Matrix
import android.graphics.Shader
import android.graphics.SweepGradient
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.busetaisland.app.data.model.EtaDisplayUnit
import com.busetaisland.app.data.model.MtrRegistry
import com.busetaisland.app.data.model.TrackedBusInfo
import com.busetaisland.app.data.model.TrackedFlightInfo
import com.busetaisland.app.data.model.WeatherInfo
import com.busetaisland.app.service.OverlayStateHolder
import com.busetaisland.app.ui.components.FlightTrackingProgressRow
import com.busetaisland.app.ui.components.RainNowcastMapView
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DynamicIslandOverlayContent(
    modifier: Modifier = Modifier,
    onExpandToggle: () -> Unit = {},
    onDrag: (dx: Float, dy: Float) -> Unit = { _, _ -> },
    onDragEnd: () -> Unit = {},
    onClose: () -> Unit = {}
) {
    val config by OverlayStateHolder.config.collectAsState()
    val allBuses by OverlayStateHolder.allTrackedBuses.collectAsState()
    val singleTrackedBus by OverlayStateHolder.trackedBus.collectAsState()
    val weatherInfo by (com.busetaisland.app.BusApp.instance.repository.weatherState).collectAsState()
    val rainNowcast by (com.busetaisland.app.BusApp.instance.repository.rainNowcastState).collectAsState()
    val trackedFlight by OverlayStateHolder.trackedFlight.collectAsState()
    val userLocation by (com.busetaisland.app.BusApp.instance.repository.locationTracker.locationState).collectAsState()
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    // Determine the list of tracked buses that have an active ETA and are in range
    val rawBusList: List<TrackedBusInfo> = if (allBuses.isNotEmpty()) {
        allBuses
    } else {
        listOfNotNull(singleTrackedBus)
    }

    // A bus only appears on the Dynamic Island if it has an ETA AND is within range (or geofencing is disabled)
    // Sorted by arrival time: nearest arriving (fastest ETA to now / lowest minutesLeft) appears at the top
    val busList: List<TrackedBusInfo> = rawBusList.filter { bus ->
        bus.eta1 != null && (!bus.isGeofenceEnabled || bus.isInRange)
    }.sortedWith(
        compareBy<TrackedBusInfo> { it.eta1?.minutesLeft ?: Int.MAX_VALUE }
            .thenBy { it.distanceMeters ?: Float.MAX_VALUE }
            .thenBy { it.route }
    )

    val isCollapsed = config.isCollapsed
    val showNowcastMap = config.showNowcastMap && !isCollapsed
    val showWeatherInfo = (config.showWeatherInfo || config.showNowcastMap) && !isCollapsed
    val hasInRangeBus = busList.isNotEmpty()
    val hasOutOfRangeBus = rawBusList.isNotEmpty() && rawBusList.any { it.isGeofenceEnabled && !it.isInRange }

    // Flight tracking module height (always visible when enabled, no extra click required)
    val hasFlightModule = config.isFlightTrackingEnabled && trackedFlight != null && !isCollapsed
    val flightRowHeight = if (hasFlightModule) 91.dp else 0.dp

    val hasWarnings = showWeatherInfo && weatherInfo?.warnings?.isNotEmpty() == true
    // Timeline bar height (at bottom of all routes)
    val showTimelineBar = config.showTimelineBar && !isCollapsed
    val hasTimeline = showTimelineBar && busList.isNotEmpty()
    val timelineRowHeight = if (hasTimeline) 50.dp else 0.dp

    // Pre-calculate exact target expanded height tightly fitting all content with zero trailing blank space
    val weatherRowHeight = if (showWeatherInfo) {
        var h = if (hasWarnings) 79.dp else 56.dp
        if (showNowcastMap) {
            h += 356.dp // RainNowcastMapView with 1:1 radar map and frame time header
        }
        h
    } else {
        0.dp
    }
    val targetExpandedHeight: Dp = if (busList.isEmpty()) {
        (56.dp + flightRowHeight + timelineRowHeight + weatherRowHeight)
    } else {
        (18 + (busList.size * 40)).dp + flightRowHeight + timelineRowHeight + weatherRowHeight
    }

    val animDuration = if (isCollapsed) config.collapseDurationMs else config.expandDurationMs
    val easing = FastOutSlowInEasing

    val collapsedRadius = config.collapsedRadiusDp.dp
    val collapsedDiameter = (config.collapsedRadiusDp * 2).dp

    val transition = updateTransition(
        targetState = isCollapsed,
        label = "DynamicIslandMorph"
    )

    val animatedWidth by transition.animateDp(
        transitionSpec = { tween(durationMillis = animDuration, easing = easing) },
        label = "IslandWidth"
    ) { collapsed ->
        if (collapsed) collapsedDiameter else if (showNowcastMap) 344.dp else 336.dp
    }

    val targetHeight = if (isCollapsed) collapsedDiameter else targetExpandedHeight
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = tween(durationMillis = animDuration, easing = easing),
        label = "IslandHeight"
    )

    val animatedRadius by transition.animateDp(
        transitionSpec = { tween(durationMillis = animDuration, easing = easing) },
        label = "IslandRadius"
    ) { collapsed ->
        if (collapsed) collapsedRadius else 20.dp
    }

    val collapsedAlpha by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                tween(
                    durationMillis = (animDuration * 0.45f).toInt().coerceAtLeast(30),
                    delayMillis = (animDuration * 0.45f).toInt(),
                    easing = easing
                )
            } else {
                tween(
                    durationMillis = (animDuration * 0.35f).toInt().coerceAtLeast(25),
                    easing = easing
                )
            }
        },
        label = "CollapsedAlpha"
    ) { collapsed ->
        if (collapsed) 1f else 0f
    }

    val collapsedScale by transition.animateFloat(
        transitionSpec = { tween(durationMillis = animDuration, easing = easing) },
        label = "CollapsedScale"
    ) { collapsed ->
        if (collapsed) 1f else 0.75f
    }

    val expandedAlpha by transition.animateFloat(
        transitionSpec = {
            if (!targetState) {
                tween(
                    durationMillis = (animDuration * 0.55f).toInt().coerceAtLeast(40),
                    delayMillis = (animDuration * 0.35f).toInt(),
                    easing = easing
                )
            } else {
                tween(
                    durationMillis = (animDuration * 0.35f).toInt().coerceAtLeast(25),
                    easing = easing
                )
            }
        },
        label = "ExpandedAlpha"
    ) { collapsed ->
        if (collapsed) 0f else 1f
    }

    val expandedScale by transition.animateFloat(
        transitionSpec = { tween(durationMillis = animDuration, easing = easing) },
        label = "ExpandedScale"
    ) { collapsed ->
        if (collapsed) 0.92f else 1f
    }

    val isMarqueeEnabled = !isCollapsed && transition.currentState == false && transition.targetState == false
    val morphShape = RoundedCornerShape(animatedRadius)

    // Border modifier: when refreshing, lights up blue along perimeter and spins.
    // During idle (99.9% of time), avoids running infinite transition to ensure 0% CPU idle load.
    val surfaceBorderModifier = if (config.isRefreshing) {
        val infiniteTransition = rememberInfiniteTransition(label = "IslandSpinningBorder")
        val spinAngle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1100, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "SpinAngle"
        )
        val spinningBlueBorderBrush = remember(spinAngle) {
            object : ShaderBrush() {
                override fun createShader(size: Size): Shader {
                    val shader = SweepGradient(
                        size.width / 2f,
                        size.height / 2f,
                        intArrayOf(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.parseColor("#002979FF"),
                            android.graphics.Color.parseColor("#4D2979FF"),
                            android.graphics.Color.parseColor("#FF2979FF"),
                            android.graphics.Color.parseColor("#FF00E5FF"), // Bright electric cyan-blue beam head
                            android.graphics.Color.parseColor("#FF2979FF"),
                            android.graphics.Color.parseColor("#002979FF"),
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        ),
                        floatArrayOf(0.0f, 0.04f, 0.12f, 0.25f, 0.35f, 0.40f, 0.48f, 0.55f, 1.0f)
                    )
                    val matrix = Matrix()
                    matrix.postRotate(spinAngle, size.width / 2f, size.height / 2f)
                    shader.setLocalMatrix(matrix)
                    return shader
                }
            }
        }
        Modifier
            .border(
                width = 1.dp,
                color = Color(0x332979FF),
                shape = morphShape
            )
            .border(
                width = 2.2.dp,
                brush = spinningBlueBorderBrush,
                shape = morphShape
            )
    } else {
        Modifier.border(
            width = if (isCollapsed) 1.2.dp else 0.8.dp,
            color = if (!hasInRangeBus && isCollapsed) Color(0x558E8E93) else Color(0x38FFFFFF),
            shape = morphShape
        )
    }

    Box(
        modifier = modifier
            .wrapContentSize()
            .testTag("dynamic_island_overlay_root"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .size(width = animatedWidth, height = animatedHeight)
                .clip(morphShape)
                .then(surfaceBorderModifier)
                .pointerInput(Unit) {
                    handleIslandTouch(
                        isCollapsed = { OverlayStateHolder.config.value.isCollapsed },
                        isWeatherVisible = { OverlayStateHolder.config.value.showWeatherInfo || OverlayStateHolder.config.value.showNowcastMap },
                        onTap = {
                            if (OverlayStateHolder.config.value.isCollapsed) {
                                onExpandToggle()
                            } else {
                                // When expanded: clicking the island body toggles the weather info and rain nowcast radar map
                                val isNowcastOpen = OverlayStateHolder.config.value.showNowcastMap
                                val nextState = !isNowcastOpen
                                OverlayStateHolder.updateConfig {
                                    it.copy(
                                        showWeatherInfo = true,
                                        showNowcastMap = nextState
                                    )
                                }
                                if (nextState) {
                                    coroutineScope.launch {
                                        com.busetaisland.app.BusApp.instance.repository.fetchRainNowcast(force = false)
                                    }
                                }
                            }
                        },
                        onLongPress = {
                            try {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            } catch (e: Exception) {
                                // Ignore
                            }
                            OverlayStateHolder.requestManualRefresh()
                        },
                        onDrag = onDrag,
                        onDragEnd = onDragEnd,
                        onSwipeUp = {
                            if (!OverlayStateHolder.config.value.isCollapsed) {
                                if (OverlayStateHolder.config.value.showNowcastMap) {
                                    // Swipe up collapses nowcast radar map
                                    OverlayStateHolder.updateConfig { it.copy(showNowcastMap = false) }
                                } else if (OverlayStateHolder.config.value.showWeatherInfo) {
                                    // Swipe up closes the weather info line
                                    OverlayStateHolder.updateConfig { it.copy(showWeatherInfo = false, showNowcastMap = false) }
                                } else {
                                    // Swipe up collapses the dynamic island
                                    onExpandToggle()
                                }
                            }
                        }
                    )
                }
                .testTag(if (isCollapsed) "island_collapsed_circle" else "island_expanded_pill"),
            color = Color(0xFF000000),
            shape = morphShape
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = if (isCollapsed) Alignment.Center else Alignment.TopCenter
            ) {
                // Calculate dynamic transform origin so morph scales away from screen edge
                val originPivotX = when {
                    config.circlePosX < -60 -> 0.1f // Near left wall -> anchor on left, expand rightwards
                    config.circlePosX > 60 -> 0.9f  // Near right wall -> anchor on right, expand leftwards
                    else -> 0.5f                    // Center
                }

                // Collapsed Circle Content
                if (collapsedAlpha > 0.005f) {
                    Box(
                        modifier = Modifier
                            .size(collapsedDiameter)
                            .graphicsLayer {
                                alpha = collapsedAlpha
                                scaleX = collapsedScale
                                scaleY = collapsedScale
                                transformOrigin = TransformOrigin(originPivotX, 0.5f)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        CollapsedCircleInnerContent(
                            hasInRangeBus = hasInRangeBus,
                            isRefreshing = config.isRefreshing,
                            circleDiameter = collapsedDiameter
                        )
                    }
                }

                // Expanded Island Content (Smoothly scaled from top origin, never jumping)
                if (expandedAlpha > 0.005f || !isCollapsed) {
                    Box(
                        modifier = Modifier
                            .width(if (showNowcastMap) 344.dp else 336.dp)
                            .fillMaxHeight()
                            .graphicsLayer {
                                alpha = expandedAlpha
                                scaleX = expandedScale
                                scaleY = expandedScale
                                transformOrigin = TransformOrigin(originPivotX, 0f)
                            },
                        contentAlignment = Alignment.TopCenter
                    ) {
                        ExpandedMultiBusIslandPillContent(
                            inRangeBuses = busList,
                            hasOutOfRangeBus = hasOutOfRangeBus,
                            config = config,
                            rainNowcast = rainNowcast,
                            trackedFlight = trackedFlight,
                            userLocation = userLocation,
                            etaUnit = config.etaUnit,
                            isMarqueeEnabled = isMarqueeEnabled,
                            showWeatherInfo = showWeatherInfo,
                            weatherInfo = weatherInfo
                        )
                    }
                }
            }
        }
    }
}

/**
 * Robust gesture handling:
 * - Long-Press (400ms) with haptic feedback triggers immediate manual refresh
 * - Tap when collapsed expands the island
 * - Tap when expanded toggles the weather info line
 * - Drag moves the floating circle
 * - Swipe-Up collapses the weather line or the dynamic island
 */
private suspend fun PointerInputScope.handleIslandTouch(
    isCollapsed: () -> Boolean,
    isWeatherVisible: () -> Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onDrag: (dx: Float, dy: Float) -> Unit,
    onDragEnd: () -> Unit,
    onSwipeUp: () -> Unit
) {
    val touchSlop = viewConfiguration.touchSlop
    val longPressTimeout = 400L

    coroutineScope {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = true)
            var isDragging = false
            var isLongPressTriggered = false
            var totalDx = 0f
            var totalDy = 0f

            val longPressJob = launch {
                delay(longPressTimeout)
                if (!isDragging) {
                    isLongPressTriggered = true
                    onLongPress()
                }
            }

            try {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break

                    if (change.pressed) {
                        val dx = change.position.x - change.previousPosition.x
                        val dy = change.position.y - change.previousPosition.y
                        totalDx += dx
                        totalDy += dy
                        val totalDist = Math.hypot(totalDx.toDouble(), totalDy.toDouble()).toFloat()

                        if (totalDist > touchSlop) {
                            if (!isDragging) {
                                isDragging = true
                                longPressJob.cancel()
                            }
                        }

                        if (isDragging && !isLongPressTriggered) {
                            change.consume()
                            if (isCollapsed()) {
                                onDrag(dx, dy)
                            } else {
                                // Swipe up when expanded
                                if (totalDy < -touchSlop && Math.abs(totalDy) > Math.abs(totalDx)) {
                                    onSwipeUp()
                                    break
                                }
                            }
                        }
                    } else {
                        // Pointer lifted (Up event)
                        longPressJob.cancel()
                        if (isDragging) {
                            if (isCollapsed()) {
                                onDragEnd()
                            }
                        } else if (!isLongPressTriggered) {
                            // Clean Tap without long press
                            onTap()
                        }
                        break
                    }
                }
            } finally {
                longPressJob.cancel()
            }
        }
    }
}

@Composable
private fun CollapsedCircleInnerContent(
    hasInRangeBus: Boolean,
    isRefreshing: Boolean,
    circleDiameter: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val iconSize = (circleDiameter * 0.5f).coerceIn(14.dp, 28.dp)
    Box(
        modifier = modifier.size(circleDiameter),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.DirectionsBus,
            contentDescription = "Bus Overlay",
            tint = when {
                isRefreshing -> Color(0xFF2979FF)
                hasInRangeBus -> Color(0xFFD0BCFF)
                else -> Color(0xFF8E8E93)
            },
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun ExpandedMultiBusIslandPillContent(
    inRangeBuses: List<TrackedBusInfo>,
    hasOutOfRangeBus: Boolean,
    config: com.busetaisland.app.service.OverlayDisplayConfig,
    rainNowcast: com.busetaisland.app.data.model.RainNowcastData?,
    trackedFlight: TrackedFlightInfo?,
    userLocation: com.busetaisland.app.data.location.UserLocationState?,
    etaUnit: EtaDisplayUnit,
    isMarqueeEnabled: Boolean,
    showWeatherInfo: Boolean,
    weatherInfo: WeatherInfo?,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    Column(
        modifier = modifier
            .padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 8.dp)
            .fillMaxWidth()
            .wrapContentHeight(align = Alignment.Top),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Subtle collapse bar indicator at the very top (exactly 10dp total vertical footprint)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 28.dp, height = 3.5.dp)
                    .background(Color(0x38FFFFFF), RoundedCornerShape(2.dp))
            )
        }

        if (inRangeBuses.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOff,
                    contentDescription = "No bus in range",
                    tint = if (hasOutOfRangeBus) Color(0xFFFFB4AB) else Color(0xFF8E8E93),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (hasOutOfRangeBus) "已超出地理範圍 (待機中)" else "暫無巴士即時班次",
                    color = if (hasOutOfRangeBus) Color(0xFFFFB4AB) else Color(0xFF8E8E93),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            // Display ALL tracked bus stops vertically stacked with EXACT height matching the animation
            inRangeBuses.forEachIndexed { index, bus ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BusStopItemRow(
                        bus = bus,
                        etaUnit = etaUnit,
                        isMarqueeEnabled = isMarqueeEnabled
                    )
                }

                if (index < inRangeBuses.size - 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 4.dp),
                            thickness = 0.5.dp,
                            color = Color(0x28FFFFFF)
                        )
                    }
                }
            }
        }

        // Big Horizontal Bus Timeline Bar placed at bottom of all routes
        if (config.showTimelineBar && inRangeBuses.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                contentAlignment = Alignment.Center
            ) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    thickness = 0.5.dp,
                    color = Color(0x28FFFFFF)
                )
            }

            UpcomingBusTimelineBar(
                buses = inRangeBuses,
                etaUnit = etaUnit,
                windowMinutes = config.timelineWindowMinutes,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 1.dp)
            )
        }

        // Flight tracking module: placed between bus stops and weather info, visible without extra click
        if (config.isFlightTrackingEnabled && trackedFlight != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                contentAlignment = Alignment.Center
            ) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    thickness = 0.5.dp,
                    color = Color(0x28FFFFFF)
                )
            }

            FlightTrackingProgressRow(
                flight = trackedFlight,
                modifier = Modifier.padding(top = 1.dp)
            )
        }

        // Weather info section placed below all the ETA rows
        if (showWeatherInfo) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                contentAlignment = Alignment.Center
            ) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    thickness = 0.5.dp,
                    color = Color(0x28FFFFFF)
                )
            }

            // Line 1: Weather Stats
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                contentAlignment = Alignment.Center
            ) {
                WeatherInfoRow(weather = weatherInfo)
            }

            // Line 2: Active Weather Warnings (New Line with Official Warning Logos)
            if (weatherInfo?.warnings?.isNotEmpty() == true) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(22.dp)
                        .padding(top = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    WeatherWarningsBannerRow(warnings = weatherInfo.warnings)
                }
            }

            // Line 3: Rain Nowcast Bar (placed below weather warnings; click to toggle map)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .padding(top = 2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (config.showNowcastMap) Color(0x3300E5FF) else Color(0x1AFFFFFF))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val nextState = !config.showNowcastMap
                        OverlayStateHolder.updateConfig {
                            it.copy(
                                showWeatherInfo = true,
                                showNowcastMap = nextState
                            )
                        }
                        if (nextState) {
                            coroutineScope.launch {
                                com.busetaisland.app.BusApp.instance.repository.fetchRainNowcast(force = false)
                            }
                        }
                    }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🌧️",
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "降雨臨近預報地圖",
                        color = if (config.showNowcastMap) Color(0xFF00E5FF) else Color(0xFFE2E8F0),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val frameCount = rainNowcast?.frames?.size ?: 0
                    if (frameCount > 0) {
                        Text(
                            text = "${frameCount}幀",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    Text(
                        text = if (config.showNowcastMap) "▲ 收起" else "▼ 展開",
                        color = if (config.showNowcastMap) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Line 4: Rain Nowcast Radar Map Canvas (when expanded)
            if (config.showNowcastMap) {
                RainNowcastMapView(
                    nowcastData = rainNowcast,
                    userLocation = userLocation,
                    config = config,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun WeatherInfoRow(
    weather: WeatherInfo?,
    modifier: Modifier = Modifier
) {
    val district = weather?.districtName ?: "天文台"
    val curTemp = weather?.currentTemp ?: 26
    val minTemp = weather?.minTemp ?: 23
    val maxTemp = weather?.maxTemp ?: 29
    val humidity = weather?.humidity ?: 78
    val rainProb = weather?.rainProbability ?: "低"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .testTag("island_weather_info_row"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // District + Current Temperature
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$district ",
                color = Color(0xFFD0BCFF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${curTemp}°C",
                color = Color(0xFFFFFFFF),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Today Highest / Lowest temperature
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "今日 ",
                color = Color(0xFF9A92A6),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = "${maxTemp}°",
                color = Color(0xFFFF8A80),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "/",
                color = Color(0xFF7A757F),
                fontSize = 10.sp
            )
            Text(
                text = "${minTemp}°",
                color = Color(0xFF82B1FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Humidity
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "濕度 ",
                color = Color(0xFF9A92A6),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = "${humidity}%",
                color = Color(0xFF80D8FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Rain Probability
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "降雨 ",
                color = Color(0xFF9A92A6),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = rainProb,
                color = if (rainProb.contains("高") || rainProb.equals("High", ignoreCase = true)) {
                    Color(0xFFFF5252)
                } else if (rainProb.contains("中") || rainProb.equals("Medium", ignoreCase = true)) {
                    Color(0xFFFFD740)
                } else {
                    Color(0xFFB9F6CA)
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Dedicated new-line banner row for active HKO weather warnings.
 * Uses official graphical logos only (no text explanation) for clean minimalist UI.
 */
@Composable
private fun WeatherWarningsBannerRow(
    warnings: List<com.busetaisland.app.data.model.WeatherWarning>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .testTag("island_weather_warnings_container"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        warnings.take(5).forEach { warning ->
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(warning.colorHex).copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(warning.colorHex).copy(alpha = 0.45f))
            ) {
                Box(
                    modifier = Modifier.padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    WeatherWarningLogo(
                        warning = warning,
                        size = 18.dp
                    )
                }
            }
        }
    }
}

/**
 * Weather Warning Logo Component:
 * 1. Loads the official Hong Kong Observatory PNG warning icon via Coil AsyncImage.
 * 2. If loading or offline, renders a crisp custom Vector/Canvas graphic emblem (not an emoji).
 */
@Composable
fun WeatherWarningLogo(
    warning: com.busetaisland.app.data.model.WeatherWarning,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (warning.iconUrl.isNotBlank()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(warning.iconUrl)
                    .memoryCacheKey(warning.iconUrl)
                    .diskCacheKey(warning.iconUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = warning.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                loading = {
                    VectorWeatherWarningLogo(warning = warning, modifier = Modifier.fillMaxSize())
                },
                error = {
                    VectorWeatherWarningLogo(warning = warning, modifier = Modifier.fillMaxSize())
                }
            )
        } else {
            VectorWeatherWarningLogo(warning = warning, modifier = Modifier.fillMaxSize())
        }
    }
}

/**
 * High-fidelity Vector/Canvas Logo for HKO Weather Warnings (No emoji text).
 * Renders recognizable geometrical symbols matching Hong Kong Observatory warning emblems.
 */
@Composable
fun VectorWeatherWarningLogo(
    warning: com.busetaisland.app.data.model.WeatherWarning,
    modifier: Modifier = Modifier
) {
    val code = warning.code.uppercase()
    val color = Color(warning.colorHex)

    Canvas(modifier = modifier) {
        val w = this.size.width
        val h = this.size.height

        when {
            // Thunderstorm Warning (WTS): Yellow badge with black Lightning Bolt
            code == "WTS" || warning.name.contains("雷暴") -> {
                val trianglePath = Path().apply {
                    moveTo(w * 0.5f, h * 0.05f)
                    lineTo(w * 0.95f, h * 0.92f)
                    lineTo(w * 0.05f, h * 0.92f)
                    close()
                }
                drawPath(trianglePath, color = Color(0xFFFFD54F), style = Fill)
                drawPath(trianglePath, color = Color(0xFFE65100), style = Stroke(width = 1.2f))

                // Lightning bolt
                val boltPath = Path().apply {
                    moveTo(w * 0.52f, h * 0.22f)
                    lineTo(w * 0.35f, h * 0.56f)
                    lineTo(w * 0.54f, h * 0.56f)
                    lineTo(w * 0.44f, h * 0.86f)
                    lineTo(w * 0.68f, h * 0.48f)
                    lineTo(w * 0.50f, h * 0.48f)
                    close()
                }
                drawPath(boltPath, color = Color(0xFF1E1E1E), style = Fill)
            }

            // Rainstorm Warnings (Amber, Red, Black)
            code.startsWith("WRAIN") || warning.name.contains("暴雨") -> {
                val rainColor = when {
                    code == "WRAINB" || warning.name.contains("黑") -> Color(0xFF212121)
                    code == "WRAINR" || warning.name.contains("紅") -> Color(0xFFFF1744)
                    else -> Color(0xFFFFD740)
                }
                // Background badge
                drawRoundRect(
                    color = rainColor,
                    size = Size(w, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.2f, w * 0.2f)
                )
                // Cloud and 3 rain slashes
                val slashColor = if (code == "WRAINB" || warning.name.contains("黑")) Color(0xFFFFFFFF) else Color(0xFF1A1A1A)
                drawLine(slashColor, Offset(w * 0.32f, h * 0.40f), Offset(w * 0.24f, h * 0.78f), strokeWidth = 1.8f, cap = StrokeCap.Round)
                drawLine(slashColor, Offset(w * 0.52f, h * 0.35f), Offset(w * 0.44f, h * 0.80f), strokeWidth = 1.8f, cap = StrokeCap.Round)
                drawLine(slashColor, Offset(w * 0.72f, h * 0.40f), Offset(w * 0.64f, h * 0.78f), strokeWidth = 1.8f, cap = StrokeCap.Round)
            }

            // Tropical Cyclone Signals (TC1, TC3, TC8, TC9, TC10)
            code.startsWith("TC") || warning.name.contains("風球") || warning.name.contains("氣旋") || warning.name.contains("信號") -> {
                val circleColor = if (warning.shortLabel.contains("8") || warning.shortLabel.contains("9") || warning.shortLabel.contains("10")) {
                    Color(0xFFFF1744)
                } else {
                    Color(0xFFFFB74D)
                }
                drawCircle(color = circleColor, radius = w * 0.48f, center = Offset(w * 0.5f, h * 0.5f))
                drawCircle(color = Color(0xFFFFFFFF), radius = w * 0.38f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = 1.2f))
                // Center vortex core
                drawCircle(color = Color(0xFFFFFFFF), radius = w * 0.16f, center = Offset(w * 0.5f, h * 0.5f))
            }

            // Very Hot / Fire Danger (WHOT / WFIRE)
            code == "WHOT" || code.startsWith("WFIRE") || warning.name.contains("熱") || warning.name.contains("火") -> {
                val flameColor = if (code == "WFIRER" || warning.name.contains("紅")) Color(0xFFFF3D00) else Color(0xFFFF9100)
                drawCircle(color = flameColor, radius = w * 0.46f, center = Offset(w * 0.5f, h * 0.5f))
                val innerPath = Path().apply {
                    moveTo(w * 0.5f, h * 0.18f)
                    cubicTo(w * 0.75f, h * 0.42f, w * 0.8f, h * 0.78f, w * 0.5f, h * 0.84f)
                    cubicTo(w * 0.2f, h * 0.78f, w * 0.25f, h * 0.42f, w * 0.5f, h * 0.18f)
                    close()
                }
                drawPath(innerPath, color = Color(0xFFFFEB3B), style = Fill)
            }

            // Cold / Frost (WCOLD / WFROST)
            code == "WCOLD" || code == "WFROST" || warning.name.contains("冷") || warning.name.contains("霜") -> {
                drawCircle(color = Color(0xFF0288D1), radius = w * 0.46f, center = Offset(w * 0.5f, h * 0.5f))
                // Snowflake / crystal lines
                val strokeW = 1.6f
                drawLine(Color(0xFFFFFFFF), Offset(w * 0.5f, h * 0.2f), Offset(w * 0.5f, h * 0.8f), strokeWidth = strokeW, cap = StrokeCap.Round)
                drawLine(Color(0xFFFFFFFF), Offset(w * 0.2f, h * 0.5f), Offset(w * 0.8f, h * 0.5f), strokeWidth = strokeW, cap = StrokeCap.Round)
                drawLine(Color(0xFFFFFFFF), Offset(w * 0.28f, h * 0.28f), Offset(w * 0.72f, h * 0.72f), strokeWidth = strokeW, cap = StrokeCap.Round)
                drawLine(Color(0xFFFFFFFF), Offset(w * 0.72f, h * 0.28f), Offset(w * 0.28f, h * 0.72f), strokeWidth = strokeW, cap = StrokeCap.Round)
            }

            // Strong Monsoon / Landslip / Flooding / Default
            else -> {
                drawRoundRect(
                    color = color,
                    size = Size(w, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.22f, w * 0.22f)
                )
                // Exclamation mark
                drawLine(Color(0xFF1E1E1E), Offset(w * 0.5f, h * 0.22f), Offset(w * 0.5f, h * 0.58f), strokeWidth = 2f, cap = StrokeCap.Round)
                drawCircle(Color(0xFF1E1E1E), radius = 1.2f, center = Offset(w * 0.5f, h * 0.76f))
            }
        }
    }
}

@Composable
private fun BusStopItemRow(
    bus: TrackedBusInfo?,
    etaUnit: EtaDisplayUnit,
    isMarqueeEnabled: Boolean
) {
    val route = bus?.route ?: "1A"
    val destChinese = bus?.chineseDestination ?: "尖沙咀碼頭"
    val co = bus?.co ?: "KMB"
    val isMtr = co.equals("MTR", ignoreCase = true) || MtrRegistry.findLine(route) != null
    val isGmb = co == "GMB"
    val isCtb = co == "CTB"
    val isNwfb = co == "NWFB"

    val mtrLine = if (isMtr) MtrRegistry.findLine(route) else null
    val mtrLineColor = mtrLine?.color ?: MtrRegistry.getRouteColor(co, route)

    val routeTextColor = when {
        isMtr -> Color.White
        isGmb -> Color(0xFF00E676)
        isCtb -> Color(0xFFFFD600)
        isNwfb -> Color(0xFFFF9100)
        else -> Color(0xFFD0BCFF)
    }
    val boxBorderColor = when {
        isMtr -> mtrLineColor.copy(alpha = 0.5f)
        isGmb -> Color(0x5500E676)
        isCtb -> Color(0x55FFD600)
        isNwfb -> Color(0x55FF9100)
        else -> Color(0x38D0BCFF)
    }
    val boxBgColor = when {
        isMtr -> Color(0xFF191820)
        else -> Color(0xFF191820)
    }
    val destTextColor = Color(0xFFE6E1E5)
    
    val isEta1Grey = bus?.eta1?.isGrey(bus.co) == true
    val isEta2Grey = bus?.eta2?.isGrey(bus.co) == true

    // Normal is white (#FFFFFF), Grey (#8E8E93) when "原定班次" (KMB) or "未開出" (GMB)
    val eta1TextColor = if (isEta1Grey) Color(0xFF8E8E93) else Color(0xFFFFFFFF)
    val eta2TextColor = if (isEta2Grey) Color(0xFF8E8E93) else Color(0xFFFFFFFF)

    val eta1Text = bus?.formattedEta1(etaUnit) ?: "--"
    val eta2Text = bus?.formattedEta2(etaUnit) ?: "--"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side: Compact Route Box + Destination
        Box(
            modifier = Modifier
                .background(
                    color = boxBgColor,
                    shape = RoundedCornerShape(10.dp)
                )
                .border(
                    width = 0.8.dp,
                    color = boxBorderColor,
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 7.dp, vertical = 2.5.dp)
                .widthIn(min = 72.dp, max = 155.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isMtr) {
                    // MTR Line Route Label shown filled solid color and route name white
                    Box(
                        modifier = Modifier
                            .background(
                                color = mtrLineColor,
                                shape = RoundedCornerShape(5.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 1.5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = route,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.5.sp,
                            letterSpacing = 0.2.sp,
                            maxLines = 1,
                            modifier = Modifier.testTag("island_route_text_${route}")
                        )
                    }
                } else {
                    // Route Number (e.g. 1A, 74B, 16M)
                    Text(
                        text = route,
                        color = routeTextColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        letterSpacing = 0.2.sp,
                        maxLines = 1,
                        modifier = Modifier.testTag("island_route_text_${route}")
                    )
                }

                // Arrow
                Text(
                    text = "➔",
                    color = Color(0xFF9A92A6),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold
                )

                // Chinese Destination with marquee rolling
                val textModifier = if (isMarqueeEnabled) {
                    Modifier
                        .weight(1f, fill = false)
                        .basicMarquee(iterations = Int.MAX_VALUE)
                        .testTag("island_dest_text_${route}")
                } else {
                    Modifier
                        .weight(1f, fill = false)
                        .testTag("island_dest_text_${route}")
                }

                Text(
                    text = destChinese,
                    color = destTextColor,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = textModifier
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Right side: Single-line ETA times for this stop
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF141318))
                .border(
                    width = 0.6.dp,
                    color = Color(0x22FFFFFF),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 7.dp, vertical = 1.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            // 1st ETA
            Text(
                text = eta1Text,
                color = eta1TextColor,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                modifier = Modifier.testTag("island_eta1_${route}")
            )

            // 2nd ETA (only show if available, do NOT show dot or fallback dash if no 2nd ETA)
            if (bus?.eta2 != null) {
                // Dot separator
                Text(
                    text = " • ",
                    color = Color(0xFF7A757F),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                // 2nd ETA
                Text(
                    text = eta2Text,
                    color = eta2TextColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    modifier = Modifier.testTag("island_eta2_${route}")
                )
            }
        }
    }
}

private data class BusTimelinePoint(
    val route: String,
    val company: String,
    val etaSeq: Int,
    val minutesLeft: Int,
    val isGrey: Boolean,
    val color: Color,
    val formattedTime: String
)

@Composable
private fun UpcomingBusTimelineBar(
    buses: List<TrackedBusInfo>,
    etaUnit: EtaDisplayUnit,
    windowMinutes: Int,
    modifier: Modifier = Modifier
) {
    val maxWindow = windowMinutes.coerceIn(10, 120)

    val points = remember(buses, etaUnit, maxWindow) {
        val list = mutableListOf<BusTimelinePoint>()
        buses.forEach { bus ->
            val route = bus.route
            val co = bus.co
            val isMtr = co.equals("MTR", ignoreCase = true) || MtrRegistry.findLine(route) != null
            val isGmb = co == "GMB"
            val isCtb = co == "CTB"
            val isNwfb = co == "NWFB"
            val mtrLine = if (isMtr) MtrRegistry.findLine(route) else null
            val badgeColor = when {
                isMtr -> mtrLine?.color ?: MtrRegistry.getRouteColor(co, route)
                isGmb -> Color(0xFF00E676)
                isCtb -> Color(0xFFFFD600)
                isNwfb -> Color(0xFFFF9100)
                else -> Color(0xFFD0BCFF)
            }

            bus.eta1?.let { e1 ->
                if (e1.minutesLeft in 0..maxWindow) {
                    list.add(
                        BusTimelinePoint(
                            route = route,
                            company = co,
                            etaSeq = 1,
                            minutesLeft = e1.minutesLeft,
                            isGrey = bus.isFirstEtaScheduled,
                            color = badgeColor,
                            formattedTime = bus.formattedEta1(etaUnit)
                        )
                    )
                }
            }

            bus.eta2?.let { e2 ->
                if (e2.minutesLeft in 0..maxWindow) {
                    list.add(
                        BusTimelinePoint(
                            route = route,
                            company = co,
                            etaSeq = 2,
                            minutesLeft = e2.minutesLeft,
                            isGrey = bus.isSecondEtaScheduled,
                            color = badgeColor,
                            formattedTime = bus.formattedEta2(etaUnit)
                        )
                    )
                }
            }
        }
        list.sortedBy { it.minutesLeft }
    }

    val tick1 = maxWindow / 3
    val tick2 = (maxWindow * 2) / 3

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF000000))
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .testTag("island_upcoming_bus_timeline")
    ) {
        // Horizontal Timeline Track Area with Alternating Top/Bottom Minute Numbers
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val totalWidthDp = this.maxWidth
            val usableWidthDp = (totalWidthDp - 16.dp).coerceAtLeast(1.dp)

            // Track Bar Line (Centered in the BoxWithConstraints)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.5.dp)
                    .align(Alignment.Center)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF00E676),
                                Color(0xFF00E5FF),
                                Color(0xFF2979FF),
                                Color(0x44FFFFFF)
                            )
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
            )

            // Tick Marks along the line
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.size(5.dp).background(Color(0xFF00E676), CircleShape))
                Box(modifier = Modifier.size(width = 1.dp, height = 5.dp).background(Color(0x66FFFFFF)))
                Box(modifier = Modifier.size(width = 1.dp, height = 5.dp).background(Color(0x66FFFFFF)))
                Box(modifier = Modifier.size(width = 1.dp, height = 5.dp).background(Color(0x66FFFFFF)))
            }

            // Render Bus Points locked EXACTLY on the track line with alternating top/bottom minute labels
            points.take(12).forEachIndexed { idx, pt ->
                val ratio = (pt.minutesLeft.coerceIn(0, maxWindow).toFloat() / maxWindow.toFloat()).coerceIn(0f, 1f)
                val dotXOffset = (ratio * usableWidthDp.value).dp
                val isArrivingSoon = pt.minutesLeft <= 3
                val pointColor = if (pt.isGrey) Color(0xFF8E8E93) else if (isArrivingSoon) Color(0xFF00E676) else pt.color
                val showOnTop = idx % 2 == 0
                val minuteText = if (pt.minutesLeft <= 0) "即" else pt.minutesLeft.toString()

                // Dot is locked dead-center onto the track line
                Box(
                    modifier = Modifier
                        .offset(x = dotXOffset)
                        .align(Alignment.CenterStart),
                    contentAlignment = Alignment.Center
                ) {
                    if (isArrivingSoon) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(Color(0x4400E676), CircleShape)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(8.5.dp)
                            .background(pointColor, CircleShape)
                            .border(
                                width = 0.8.dp,
                                color = Color.White.copy(alpha = 0.85f),
                                shape = CircleShape
                            )
                    )

                    // Minute text floating above (y = -12.5dp) or below (y = 12.5dp) the dot's center
                    val yTextOffset = if (showOnTop) (-12.5).dp else 12.5.dp
                    Text(
                        text = minuteText,
                        color = if (isArrivingSoon) Color(0xFF00E676) else Color.White,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.offset(y = yTextOffset)
                    )
                }
            }
        }

        // Scale Labels below track
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 1.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("現在", fontSize = 8.sp, color = Color(0xFF00E676), fontWeight = FontWeight.Bold)
            Text("${tick1}m", fontSize = 8.sp, color = Color(0xFF8E8E93))
            Text("${tick2}m", fontSize = 8.sp, color = Color(0xFF8E8E93))
            Text("${maxWindow}m", fontSize = 8.sp, color = Color(0xFF8E8E93))
        }
    }
}
