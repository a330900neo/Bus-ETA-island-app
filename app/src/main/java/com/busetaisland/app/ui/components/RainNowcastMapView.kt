package com.busetaisland.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.busetaisland.app.data.location.UserLocationState
import com.busetaisland.app.data.model.RainNowcastData
import com.busetaisland.app.data.model.RainNowcastFrame
import com.busetaisland.app.service.OverlayDisplayConfig
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

/**
 * Rain Nowcast Radar Map with official OpenStreetMap background
 * - Official OpenStreetMap tile servers (tile.openstreetmap.org) with high-contrast dark palette
 * - Crisp square pixels for radar rainfall data (no blurred circles)
 * - Clean frame time display with continuous auto-looping animation
 * - User GPS marker & intensity legend
 * - Hold for 1 second gesture to manually refresh rain map only (does not refresh whole island)
 */
@Composable
fun RainNowcastMapView(
    nowcastData: RainNowcastData?,
    userLocation: UserLocationState?,
    config: OverlayDisplayConfig,
    modifier: Modifier = Modifier
) {
    val frames = nowcastData?.frames ?: emptyList()
    var currentFrameIndex by remember { mutableIntStateOf(0) }

    // Manual refresh hold states
    var isRefreshingMap by remember { mutableStateOf(false) }
    var pressHoldProgress by remember { mutableFloatStateOf(0f) }
    var updateSuccessMsg by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // Auto-fetch radar nowcast frames when view enters composition if data is not yet loaded
    LaunchedEffect(Unit) {
        if (nowcastData == null || frames.isEmpty()) {
            com.busetaisland.app.BusApp.instance.repository.fetchRainNowcast(force = false)
        }
    }

    // Playback loop duration from user configuration
    val cycleDurationMs = (config.nowcastCycleDurationSec * 1000L).toLong().coerceIn(1000L, 10000L)
    val frameDurationMs = if (frames.isNotEmpty()) (cycleDurationMs / frames.size).coerceAtLeast(300L) else 800L

    // Continuous auto-animation across forecast frames
    LaunchedEffect(frames.size, frameDurationMs) {
        if (frames.isNotEmpty()) {
            while (true) {
                delay(frameDurationMs)
                currentFrameIndex = (currentFrameIndex + 1) % frames.size
            }
        }
    }

    val activeIndex = if (frames.isNotEmpty()) currentFrameIndex.coerceIn(0, frames.size - 1) else 0
    val activeFrame: RainNowcastFrame? = frames.getOrNull(activeIndex)

    // Pulse animation for user GPS location
    val infiniteTransition = rememberInfiniteTransition(label = "RadarGpsPulse")
    val gpsPulseRadius by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "GpsPulse"
    )
    val gpsPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "GpsPulseAlpha"
    )

    // Clean neutral dark color matrix for OpenStreetMap tiles to provide a neutral dark background without blue color casting
    val osmDarkColorMatrix = remember {
        ColorMatrix(
            floatArrayOf(
                -0.65f,  0.00f,  0.00f, 0.0f, 175f,
                 0.00f, -0.65f,  0.00f, 0.0f, 175f,
                 0.00f,  0.00f, -0.65f, 0.0f, 175f,
                 0.00f,  0.00f,  0.00f, 1.0f,   0f
            )
        )
    }

    // Pointer input modifier to handle holding rain map for 1 second (1000ms) to trigger manual refresh
    val rainMapPointerModifier = Modifier.pointerInput(Unit) {
        val touchSlop = viewConfiguration.touchSlop
        val holdTimeoutMs = 1000L

        coroutineScope {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = true)
                down.consume()
                var isDragging = false
                var isCompleted = false
                var totalDx = 0f
                var totalDy = 0f

                val holdJob = launch {
                    val startTime = System.currentTimeMillis()
                    while (true) {
                        val elapsed = System.currentTimeMillis() - startTime
                        pressHoldProgress = (elapsed.toFloat() / holdTimeoutMs).coerceIn(0f, 1f)
                        if (elapsed >= holdTimeoutMs) {
                            break
                        }
                        delay(16L)
                    }

                    if (!isDragging) {
                        isCompleted = true
                        try {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        } catch (e: Exception) {
                            // Ignore
                        }
                        isRefreshingMap = true
                        pressHoldProgress = 0f

                        val result = com.busetaisland.app.BusApp.instance.repository.fetchRainNowcast(force = true)
                        isRefreshingMap = false
                        updateSuccessMsg = if (result != null) "✓ 雷達地圖已即時更新" else "✓ 已刷新數據"
                        delay(2200L)
                        updateSuccessMsg = null
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
                                    holdJob.cancel()
                                    pressHoldProgress = 0f
                                }
                            }
                            change.consume()
                        } else {
                            holdJob.cancel()
                            pressHoldProgress = 0f
                            change.consume()
                            break
                        }
                    }
                } finally {
                    holdJob.cancel()
                    pressHoldProgress = 0f
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(16.dp))
            .padding(8.dp)
            .testTag("rain_nowcast_map_container")
    ) {
        // Progress indicator bar during 1-second press or background refresh
        if (pressHoldProgress > 0f || isRefreshingMap) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF1E293B))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (pressHoldProgress > 0f) pressHoldProgress else 1f)
                        .fillMaxHeight()
                        .background(Color(0xFF00E5FF))
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Header: Clean Frame Time Display (No pause button, no 1234 numbers)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = "Rain Nowcast",
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "降雨臨近預報",
                    color = Color(0xFFE2E8F0),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                // Time of current active frame
                Box(
                    modifier = Modifier
                        .background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = activeFrame?.formattedTime ?: "預報載入中...",
                        color = Color(0xFF38BDF8),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Subtitle status or hold gesture progress
            Text(
                text = when {
                    pressHoldProgress > 0f -> "⏱️ 長按1秒更新 (${(pressHoldProgress * 100).toInt()}%)"
                    isRefreshingMap -> "⚡ 正在更新雷達..."
                    updateSuccessMsg != null -> updateSuccessMsg!!
                    else -> "💡 長按1秒單獨更新地圖"
                },
                color = when {
                    pressHoldProgress > 0f || isRefreshingMap -> Color(0xFF00E5FF)
                    updateSuccessMsg != null -> Color(0xFF00E676)
                    else -> Color(0xFF64748B)
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Radar Map Container with OpenStreetMap Tiles & Square Radar Pixels (1:1 Ratio)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF090D16))
                .border(0.5.dp, Color(0x3338BDF8), RoundedCornerShape(12.dp))
                .then(rainMapPointerModifier)
        ) {
            val density = LocalDensity.current
            val canvasW = with(density) { maxWidth.toPx() }
            val canvasH = with(density) { maxHeight.toPx() }

            val zoom = 11
            val tileSize = 256f

            // Determine map center
            val defaultCenterLat = 22.34
            val defaultCenterLng = 114.15
            var centerLat = defaultCenterLat
            var centerLng = defaultCenterLng

            if (config.nowcastCenterGps && userLocation != null && userLocation.hasRealLocation) {
                if (userLocation.latitude in 21.9..22.8 && userLocation.longitude in 113.6..114.6) {
                    centerLat = userLocation.latitude
                    centerLng = userLocation.longitude
                }
            }

            val centerTileX = lon2tileX(centerLng, zoom)
            val centerTileY = lat2tileY(centerLat, zoom)

            fun geoToPixel(lat: Double, lng: Double): Offset {
                val tx = lon2tileX(lng, zoom)
                val ty = lat2tileY(lat, zoom)
                val px = canvasW / 2f + ((tx - centerTileX) * tileSize).toFloat()
                val py = canvasH / 2f + ((ty - centerTileY) * tileSize).toFloat()
                return Offset(px, py)
            }

            // 1. Official OpenStreetMap Tile Background Layer (tile.openstreetmap.org)
            val minTileX = floor(centerTileX - (canvasW / 2f / tileSize) - 1).toInt()
            val maxTileX = floor(centerTileX + (canvasW / 2f / tileSize) + 1).toInt()
            val minTileY = floor(centerTileY - (canvasH / 2f / tileSize) - 1).toInt()
            val maxTileY = floor(centerTileY + (canvasH / 2f / tileSize) + 1).toInt()

            Box(modifier = Modifier.fillMaxSize()) {
                val context = LocalContext.current
                for (tx in minTileX..maxTileX) {
                    for (ty in minTileY..maxTileY) {
                        val tileLeft = canvasW / 2f + ((tx - centerTileX) * tileSize).toFloat()
                        val tileTop = canvasH / 2f + ((ty - centerTileY) * tileSize).toFloat()
                        val tileLeftDp = with(density) { tileLeft.toDp() }
                        val tileTopDp = with(density) { tileTop.toDp() }

                        // Standard official OpenStreetMap tile endpoint
                        val tileUrl = "https://tile.openstreetmap.org/$zoom/$tx/$ty.png"

                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(tileUrl)
                                .setHeader("User-Agent", "HKBusDynamicIsland/1.0 (Android)")
                                .crossfade(true)
                                .build(),
                            contentDescription = "OpenStreetMap Tile",
                            colorFilter = ColorFilter.colorMatrix(osmDarkColorMatrix),
                            modifier = Modifier
                                .offset(x = tileLeftDp, y = tileTopDp)
                                .size(with(density) { tileSize.toDp() }),
                            contentScale = ContentScale.FillBounds
                        )
                    }
                }

                // Neutral dark atmospheric tint overlay over OSM tiles for crisp radar contrast
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x33000000))
                )
            }

            // 2. Square Pixel Radar Rainfall & GPS Dot Overlay Canvas
            Canvas(modifier = Modifier.fillMaxSize().testTag("rain_nowcast_canvas")) {
                // Render crisp square pixel cells for radar rainfall data
                val points = activeFrame?.points ?: emptyList()

                // Calculate square cell dimensions based on 0.02 deg grid
                val p0 = geoToPixel(centerLat, centerLng)
                val p1 = geoToPixel(centerLat + 0.02, centerLng + 0.02)
                val cellW = abs(p1.x - p0.x).coerceIn(6f, 24f)
                val cellH = abs(p1.y - p0.y).coerceIn(6f, 24f)

                for (pt in points) {
                    if (pt.rainfallMm < 0.2) continue
                    val p = geoToPixel(pt.lat, pt.lng)

                    // Skip points outside viewport
                    if (p.x < -cellW || p.x > canvasW + cellW || p.y < -cellH || p.y > canvasH + cellH) {
                        continue
                    }

                    // Official HKO radar reflectivity color scale with crisp, vibrant true-to-life colors
                    val rainColor = when {
                        pt.rainfallMm >= 50.0 -> Color(0xFFD500F9) // Vivid Magenta / Purple (Extreme Torrential Downpour)
                        pt.rainfallMm >= 30.0 -> Color(0xFFFF1744) // Bright Crimson Red (Violent / Black Rainstorm)
                        pt.rainfallMm >= 20.0 -> Color(0xFFFF6D00) // Vibrant Orange (Heavy / Red Rainstorm)
                        pt.rainfallMm >= 10.0 -> Color(0xFFFFD600) // Pure Golden Yellow (Amber Rainstorm)
                        pt.rainfallMm >= 5.0  -> Color(0xFF00E676) // Vivid Emerald Green (Moderate Rain)
                        pt.rainfallMm >= 2.0  -> Color(0xFF76FF03) // Lime Green (Light-to-Moderate Rain)
                        pt.rainfallMm >= 0.5  -> Color(0xFF00E5FF) // Crisp Sky Cyan (Light Rain)
                        else                  -> Color(0xFF38BDF8) // Soft Blue-Cyan (Drizzle / Trace)
                    }

                    // High opacity (0.85 - 0.96) so background map color does not shift or blue-tint rain pixels
                    val alpha = (0.85f + (pt.rainfallMm / 50.0).toFloat() * 0.11f).coerceIn(0.85f, 0.96f)
                    val topLeft = Offset(p.x - cellW / 2f, p.y - cellH / 2f)
                    val cellSize = Size(cellW, cellH)

                    // Draw crisp square radar pixel
                    drawRect(
                        color = rainColor.copy(alpha = alpha),
                        topLeft = topLeft,
                        size = cellSize
                    )

                    // High intensity crisp border accent
                    if (pt.rainfallMm >= 10.0) {
                        drawRect(
                            color = rainColor.copy(alpha = 1.0f),
                            topLeft = topLeft,
                            size = cellSize,
                            style = Stroke(width = 1f)
                        )
                    }
                }

                // 3. User GPS Marker
                if (userLocation != null && userLocation.hasRealLocation) {
                    val userPixel = geoToPixel(userLocation.latitude, userLocation.longitude)

                    // Radar pulse wave
                    drawCircle(
                        color = Color(0xFF00E5FF).copy(alpha = gpsPulseAlpha),
                        radius = gpsPulseRadius * 2f,
                        center = userPixel,
                        style = Stroke(width = 2f)
                    )

                    // Outer white ring
                    drawCircle(
                        color = Color(0xFFFFFFFF),
                        radius = 5.5f,
                        center = userPixel
                    )

                    // Core luminous cyan dot
                    drawCircle(
                        color = Color(0xFF00E5FF),
                        radius = 4f,
                        center = userPixel
                    )
                }
            }

            // 4. Map Attribution & Radar Legend
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .background(Color(0xAA0B111E), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "© OpenStreetMap contributors",
                    color = Color(0xFF94A3B8),
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Radar Intensity Color Scale Legend (Bottom Right)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(Color(0xCC0B111E), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "mm: ",
                    color = Color(0xFF94A3B8),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(modifier = Modifier.size(6.dp).background(Color(0xFF00E5FF), RoundedCornerShape(1.dp)))
                Text("0.5", color = Color(0xFFCBD5E1), fontSize = 7.5.sp)
                Box(modifier = Modifier.size(6.dp).background(Color(0xFF76FF03), RoundedCornerShape(1.dp)))
                Text("2", color = Color(0xFFCBD5E1), fontSize = 7.5.sp)
                Box(modifier = Modifier.size(6.dp).background(Color(0xFF00E676), RoundedCornerShape(1.dp)))
                Text("5", color = Color(0xFFCBD5E1), fontSize = 7.5.sp)
                Box(modifier = Modifier.size(6.dp).background(Color(0xFFFFD600), RoundedCornerShape(1.dp)))
                Text("10", color = Color(0xFFCBD5E1), fontSize = 7.5.sp)
                Box(modifier = Modifier.size(6.dp).background(Color(0xFFFF6D00), RoundedCornerShape(1.dp)))
                Text("20", color = Color(0xFFCBD5E1), fontSize = 7.5.sp)
                Box(modifier = Modifier.size(6.dp).background(Color(0xFFFF1744), RoundedCornerShape(1.dp)))
                Text("30", color = Color(0xFFCBD5E1), fontSize = 7.5.sp)
                Box(modifier = Modifier.size(6.dp).background(Color(0xFFD500F9), RoundedCornerShape(1.dp)))
                Text("50+", color = Color(0xFFCBD5E1), fontSize = 7.5.sp)
            }

            // GPS Center status badge (Top Left)
            if (config.nowcastCenterGps) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .background(Color(0xAA000000), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "GPS Centered",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "GPS置中",
                        color = Color(0xFFE2E8F0),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// Helpers for OpenStreetMap Web Mercator projection
private fun lon2tileX(lon: Double, zoom: Int): Double {
    return (lon + 180.0) / 360.0 * (1 shl zoom)
}

private fun lat2tileY(lat: Double, zoom: Int): Double {
    val latRad = Math.toRadians(lat)
    return (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * (1 shl zoom)
}
