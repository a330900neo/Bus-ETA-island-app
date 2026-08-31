package com.busetaisland.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.busetaisland.app.data.local.GeoPoint
import com.busetaisland.app.data.local.GeofenceAreaEntity
import com.busetaisland.app.data.location.LocationTracker
import com.busetaisland.app.ui.theme.BusDarkBackground
import com.busetaisland.app.ui.theme.BusDarkSurface
import com.busetaisland.app.ui.theme.BusDarkSurfaceElevated
import com.busetaisland.app.ui.theme.BusDarkSurfaceVariant
import com.busetaisland.app.ui.theme.BusEmeraldGreen
import com.busetaisland.app.ui.theme.BusLavenderOnPrimary
import com.busetaisland.app.ui.theme.BusLavenderPrimary
import com.busetaisland.app.ui.theme.BusRoseAlert
import com.busetaisland.app.ui.theme.BusSubtleBorder
import com.busetaisland.app.ui.theme.BusTextMuted
import com.busetaisland.app.ui.theme.BusTextPrimary
import com.busetaisland.app.ui.theme.BusTextSecondary
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sinh
import kotlin.math.tan

// Web Mercator coordinate utilities
private fun lngToWorldX(lng: Double, zoom: Double): Double {
    return (lng + 180.0) / 360.0 * 256.0 * 2.0.pow(zoom)
}

private fun latToWorldY(lat: Double, zoom: Double): Double {
    val latRad = Math.toRadians(lat.coerceIn(-85.0511, 85.0511))
    val y = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / Math.PI) / 2.0
    return y * 256.0 * 2.0.pow(zoom)
}

private fun worldXToLng(x: Double, zoom: Double): Double {
    return (x / (256.0 * 2.0.pow(zoom))) * 360.0 - 180.0
}

private fun worldYToLat(y: Double, zoom: Double): Double {
    val n = Math.PI * (1.0 - 2.0 * (y / (256.0 * 2.0.pow(zoom))))
    return Math.toDegrees(atan(sinh(n)))
}

enum class MapInteractionMode {
    PAN_MAP,    // Drag freely to pan map without touching points
    DRAW_EDIT   // Tap to add node, drag node to move, tap node to delete
}

@Composable
fun GeofenceAreaEditorDialog(
    initialArea: GeofenceAreaEntity? = null,
    userLat: Double = 22.3193,
    userLng: Double = 114.1694,
    onDismiss: () -> Unit,
    onSave: (name: String, points: List<GeoPoint>, colorHex: Long, id: Long) -> Unit,
    onDelete: ((Long) -> Unit)? = null
) {
    var areaName by remember { mutableStateOf(initialArea?.name ?: "自訂區域 ${System.currentTimeMillis() % 1000}") }
    val points = remember {
        mutableStateListOf<GeoPoint>().apply {
            if (initialArea != null) {
                addAll(initialArea.getPoints())
            } else {
                // Default square around user (~150m)
                val delta = 0.0015
                add(GeoPoint(userLat - delta, userLng - delta))
                add(GeoPoint(userLat + delta, userLng - delta))
                add(GeoPoint(userLat + delta, userLng + delta))
                add(GeoPoint(userLat - delta, userLng + delta))
            }
        }
    }

    var selectedColorHex by remember { mutableStateOf(initialArea?.colorHex ?: 0xFF7C4DFF) }

    // Map Center and Zoom level (integer zoom for crisp tiles)
    var centerLat by remember {
        mutableDoubleStateOf(
            if (points.isNotEmpty()) points.map { it.lat }.average() else userLat
        )
    }
    var centerLng by remember {
        mutableDoubleStateOf(
            if (points.isNotEmpty()) points.map { it.lng }.average() else userLng
        )
    }
    var zoomLevel by remember { mutableIntStateOf(16) } // Zoom 13 to 18
    var mapTheme by remember { mutableStateOf("dark") } // "dark" or "voyager"
    var interactionMode by remember { mutableStateOf(MapInteractionMode.DRAW_EDIT) }

    // Selected node index for deletion / editing
    var selectedNodeIndex by remember { mutableStateOf<Int?>(null) }
    var actionMessage by remember { mutableStateOf<String?>(null) }

    val isUserInside by remember {
        derivedStateOf {
            LocationTracker.isPointInPolygon(userLat, userLng, points.toList())
        }
    }

    val availableColors = listOf(
        0xFF7C4DFF, // Lavender / Purple
        0xFF00E676, // Emerald Green
        0xFFFF9100, // Amber Orange
        0xFF00E5FF, // Cyan
        0xFFFF5252  // Coral Rose
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(horizontal = 10.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.84f)
                    .testTag("geofence_area_editor_dialog"),
                shape = RoundedCornerShape(18.dp),
                color = BusDarkBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, BusSubtleBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    // Header Row (Compact)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (initialArea == null) "繪製多邊形區域" else "編輯多邊形區域",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = BusTextPrimary,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "真實地圖 • 支援手勢平移與點選節點",
                                style = MaterialTheme.typography.bodySmall,
                                color = BusTextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(28.dp)
                                .background(BusDarkSurface, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = BusTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Name input & Color Selector (Ultra compact row)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = areaName,
                            onValueChange = { areaName = it },
                            placeholder = { Text("區域名稱 (如: 屋企/公司)", fontSize = 12.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("input_area_name"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.5.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BusLavenderPrimary,
                                unfocusedBorderColor = BusSubtleBorder,
                                focusedContainerColor = BusDarkSurface,
                                unfocusedContainerColor = BusDarkSurface,
                                focusedTextColor = BusTextPrimary,
                                unfocusedTextColor = BusTextPrimary
                            )
                        )

                        // Color Pickers
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            availableColors.forEach { colHex ->
                                val isSel = selectedColorHex == colHex
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Color(colHex), CircleShape)
                                        .border(
                                            width = if (isSel) 2.dp else 1.dp,
                                            color = if (isSel) Color.White else Color(0x33FFFFFF),
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColorHex = colHex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSel) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.Black,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Controls Bar: Mode Switch + Status Badge + Map Layer & Navigation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mode Switch Chips: Move Map vs Draw/Edit
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = interactionMode == MapInteractionMode.PAN_MAP,
                                onClick = {
                                    interactionMode = MapInteractionMode.PAN_MAP
                                    selectedNodeIndex = null
                                    actionMessage = "模式：✋ 滑動移動地圖"
                                },
                                label = { Text("✋ 平移", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BusLavenderPrimary,
                                    selectedLabelColor = BusLavenderOnPrimary,
                                    containerColor = BusDarkSurface,
                                    labelColor = BusTextSecondary
                                ),
                                modifier = Modifier.height(28.dp)
                            )

                            FilterChip(
                                selected = interactionMode == MapInteractionMode.DRAW_EDIT,
                                onClick = {
                                    interactionMode = MapInteractionMode.DRAW_EDIT
                                    actionMessage = "模式：✏️ 點擊新增/刪除節點"
                                },
                                label = { Text("✏️ 繪圖", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BusLavenderPrimary,
                                    selectedLabelColor = BusLavenderOnPrimary,
                                    containerColor = BusDarkSurface,
                                    labelColor = BusTextSecondary
                                ),
                                modifier = Modifier.height(28.dp)
                            )

                            // Status badge
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isUserInside) Color(0x2E00E676) else Color(0x2EFF5252),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 5.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (isUserInside) "● 區內" else "○ 區外",
                                    color = if (isUserInside) BusEmeraldGreen else BusRoseAlert,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Right Action Buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Toggle map style
                            IconButton(
                                onClick = {
                                    mapTheme = if (mapTheme == "dark") "voyager" else "dark"
                                },
                                modifier = Modifier.size(28.dp).background(BusDarkSurface, RoundedCornerShape(6.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = "Toggle Map Style",
                                    tint = BusLavenderPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            // Center on GPS
                            IconButton(
                                onClick = {
                                    centerLat = userLat
                                    centerLng = userLng
                                    actionMessage = "已將地圖對齊 GPS 位置"
                                },
                                modifier = Modifier.size(28.dp).background(BusDarkSurface, RoundedCornerShape(6.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "Center on GPS",
                                    tint = BusLavenderPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            // Undo last node
                            IconButton(
                                onClick = {
                                    if (points.isNotEmpty()) {
                                        points.removeAt(points.size - 1)
                                        selectedNodeIndex = null
                                        actionMessage = "已復原節點"
                                    }
                                },
                                enabled = points.isNotEmpty(),
                                modifier = Modifier.size(28.dp).background(BusDarkSurface, RoundedCornerShape(6.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Undo,
                                    contentDescription = "Undo point",
                                    tint = if (points.isNotEmpty()) BusLavenderPrimary else BusTextMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            // Clear all nodes
                            IconButton(
                                onClick = {
                                    points.clear()
                                    selectedNodeIndex = null
                                    actionMessage = "已清除所有節點"
                                },
                                modifier = Modifier.size(28.dp).background(BusDarkSurface, RoundedCornerShape(6.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Clear points",
                                    tint = BusRoseAlert,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Interactive Map Canvas Container - Takes remaining height
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF14171E))
                            .border(1.dp, BusSubtleBorder, RoundedCornerShape(12.dp))
                            .testTag("interactive_map_polygon_container")
                    ) {
                        val density = LocalDensity.current
                        val containerWidthPx = with(density) { maxWidth.toPx() }
                        val containerHeightPx = with(density) { maxHeight.toPx() }

                        val currentZoom = zoomLevel.toDouble()
                        val tileSizePx = with(density) { 256.dp.toPx() }
                        val scaleFactor = tileSizePx / 256.0

                        // Calculate World coordinates of center
                        val centerWorldX = lngToWorldX(centerLng, currentZoom)
                        val centerWorldY = latToWorldY(centerLat, currentZoom)

                        // Determine visible tile range
                        val minWorldX = centerWorldX - (containerWidthPx / 2.0) / scaleFactor
                        val maxWorldX = centerWorldX + (containerWidthPx / 2.0) / scaleFactor
                        val minWorldY = centerWorldY - (containerHeightPx / 2.0) / scaleFactor
                        val maxWorldY = centerWorldY + (containerHeightPx / 2.0) / scaleFactor

                        val minTileX = (minWorldX / 256.0).toInt().coerceAtLeast(0)
                        val maxTileX = (maxWorldX / 256.0).toInt().coerceAtLeast(0)
                        val minTileY = (minWorldY / 256.0).toInt().coerceAtLeast(0)
                        val maxTileY = (maxWorldY / 256.0).toInt().coerceAtLeast(0)

                        // 1. Render Actual Background Map Tiles
                        Box(modifier = Modifier.fillMaxSize()) {
                            for (tx in minTileX..maxTileX) {
                                for (ty in minTileY..maxTileY) {
                                    val tileWorldX = tx * 256.0
                                    val tileWorldY = ty * 256.0

                                    val screenPx = (containerWidthPx / 2.0 + (tileWorldX - centerWorldX) * scaleFactor).toFloat()
                                    val screenPy = (containerHeightPx / 2.0 + (tileWorldY - centerWorldY) * scaleFactor).toFloat()

                                    val tileUrl = if (mapTheme == "dark") {
                                        "https://a.basemaps.cartocdn.com/dark_all/$zoomLevel/$tx/$ty@2x.png"
                                    } else {
                                        "https://a.basemaps.cartocdn.com/rastertiles/voyager/$zoomLevel/$tx/$ty@2x.png"
                                    }

                                    AsyncImage(
                                        model = tileUrl,
                                        contentDescription = "Map Tile",
                                        contentScale = ContentScale.FillBounds,
                                        modifier = Modifier
                                            .size(256.dp)
                                            .offset {
                                                IntOffset(screenPx.toInt(), screenPy.toInt())
                                            }
                                    )
                                }
                            }
                        }

                        // 2. Gesture Handling & Polygon Rendering Overlay
                        var draggedNodeIdx by remember { mutableStateOf<Int?>(null) }

                        // Gestures: Stable pointerInput without changing state keys to prevent gesture cancellation during continuous drag
                        val gestureModifier = if (interactionMode == MapInteractionMode.PAN_MAP) {
                            Modifier.pointerInput(interactionMode) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val zoom = zoomLevel.toDouble()
                                    val currCenterWx = lngToWorldX(centerLng, zoom)
                                    val currCenterWy = latToWorldY(centerLat, zoom)
                                    val nextCenterWx = currCenterWx - (dragAmount.x / scaleFactor)
                                    val nextCenterWy = currCenterWy - (dragAmount.y / scaleFactor)
                                    centerLng = worldXToLng(nextCenterWx, zoom)
                                    centerLat = worldYToLat(nextCenterWy, zoom)
                                }
                            }
                        } else {
                            // DRAW & EDIT MODE: Tap to add/select, Drag node to move, Drag canvas to pan
                            Modifier
                                .pointerInput(interactionMode) {
                                    detectTapGestures { tapOffset ->
                                        val zoom = zoomLevel.toDouble()
                                        val currCenterWx = lngToWorldX(centerLng, zoom)
                                        val currCenterWy = latToWorldY(centerLat, zoom)
                                        val tapX = tapOffset.x
                                        val tapY = tapOffset.y

                                        // Check if tapped near an existing node (tolerance radius = 42px)
                                        var clickedNodeIdx: Int? = null
                                        var minDst = 42f * 42f
                                        points.forEachIndexed { idx, pt ->
                                            val wx = lngToWorldX(pt.lng, zoom)
                                            val wy = latToWorldY(pt.lat, zoom)
                                            val px = (containerWidthPx / 2.0 + (wx - currCenterWx) * scaleFactor).toFloat()
                                            val py = (containerHeightPx / 2.0 + (wy - currCenterWy) * scaleFactor).toFloat()
                                            val dx = px - tapX
                                            val dy = py - tapY
                                            val distSq = dx * dx + dy * dy
                                            if (distSq < minDst) {
                                                minDst = distSq
                                                clickedNodeIdx = idx
                                            }
                                        }

                                        if (clickedNodeIdx != null) {
                                            selectedNodeIndex = clickedNodeIdx
                                            actionMessage = "已選取節點 #${clickedNodeIdx + 1}"
                                        } else {
                                            // Add new node at tap location
                                            val targetWorldX = currCenterWx + (tapX - containerWidthPx / 2.0) / scaleFactor
                                            val targetWorldY = currCenterWy + (tapY - containerHeightPx / 2.0) / scaleFactor
                                            val newLng = worldXToLng(targetWorldX, zoom)
                                            val newLat = worldYToLat(targetWorldY, zoom)

                                            points.add(GeoPoint(newLat, newLng))
                                            selectedNodeIndex = points.size - 1
                                            actionMessage = "新增節點 #${points.size}"
                                        }
                                    }
                                }
                                .pointerInput(interactionMode) {
                                    detectDragGestures(
                                        onDragStart = { startOffset ->
                                            val zoom = zoomLevel.toDouble()
                                            val currCenterWx = lngToWorldX(centerLng, zoom)
                                            val currCenterWy = latToWorldY(centerLat, zoom)
                                            var closestIdx: Int? = null
                                            var minDst = 48f * 48f
                                            points.forEachIndexed { idx, pt ->
                                                val wx = lngToWorldX(pt.lng, zoom)
                                                val wy = latToWorldY(pt.lat, zoom)
                                                val px = (containerWidthPx / 2.0 + (wx - currCenterWx) * scaleFactor).toFloat()
                                                val py = (containerHeightPx / 2.0 + (wy - currCenterWy) * scaleFactor).toFloat()
                                                val dx = px - startOffset.x
                                                val dy = py - startOffset.y
                                                val d2 = dx * dx + dy * dy
                                                if (d2 < minDst) {
                                                    minDst = d2
                                                    closestIdx = idx
                                                }
                                            }
                                            draggedNodeIdx = closestIdx
                                            if (closestIdx != null) {
                                                selectedNodeIndex = closestIdx
                                            }
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val zoom = zoomLevel.toDouble()
                                            val currCenterWx = lngToWorldX(centerLng, zoom)
                                            val currCenterWy = latToWorldY(centerLat, zoom)
                                            val idx = draggedNodeIdx
                                            if (idx != null && idx in points.indices) {
                                                val pt = points[idx]
                                                val currWx = lngToWorldX(pt.lng, zoom)
                                                val currWy = latToWorldY(pt.lat, zoom)
                                                val nextWx = currWx + (dragAmount.x / scaleFactor)
                                                val nextWy = currWy + (dragAmount.y / scaleFactor)
                                                val newLng = worldXToLng(nextWx, zoom)
                                                val newLat = worldYToLat(nextWy, zoom)
                                                points[idx] = GeoPoint(newLat, newLng)
                                            } else {
                                                // Pan map center
                                                val nextCenterWx = currCenterWx - (dragAmount.x / scaleFactor)
                                                val nextCenterWy = currCenterWy - (dragAmount.y / scaleFactor)
                                                centerLng = worldXToLng(nextCenterWx, zoom)
                                                centerLat = worldYToLat(nextCenterWy, zoom)
                                            }
                                        },
                                        onDragEnd = { draggedNodeIdx = null },
                                        onDragCancel = { draggedNodeIdx = null }
                                    )
                                }
                        }

                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(gestureModifier)
                        ) {
                            // 3. Draw Polygon Geometry
                            if (points.isNotEmpty()) {
                                val path = Path()
                                val offsets = points.map { pt ->
                                    val wx = lngToWorldX(pt.lng, currentZoom)
                                    val wy = latToWorldY(pt.lat, currentZoom)
                                    val px = (containerWidthPx / 2.0 + (wx - centerWorldX) * scaleFactor).toFloat()
                                    val py = (containerHeightPx / 2.0 + (wy - centerWorldY) * scaleFactor).toFloat()
                                    Offset(px, py)
                                }

                                path.moveTo(offsets[0].x, offsets[0].y)
                                for (i in 1 until offsets.size) {
                                    path.lineTo(offsets[i].x, offsets[i].y)
                                }
                                if (offsets.size >= 3) {
                                    path.close()
                                    // Semi-transparent area fill
                                    drawPath(
                                        path = path,
                                        color = Color(selectedColorHex).copy(alpha = 0.28f),
                                        style = Fill
                                    )
                                }

                                // Glowing polygon stroke
                                drawPath(
                                    path = path,
                                    color = Color(selectedColorHex),
                                    style = Stroke(width = 3.5.dp.toPx())
                                )

                                // Vertex markers
                                offsets.forEachIndexed { i, off ->
                                    val isSelected = selectedNodeIndex == i
                                    val isDragged = draggedNodeIdx == i

                                    if (isSelected || isDragged) {
                                        // Highlight ring around selected node
                                        drawCircle(
                                            color = Color.White.copy(alpha = 0.45f),
                                            radius = 18.dp.toPx(),
                                            center = off
                                        )
                                        drawCircle(
                                            color = BusRoseAlert,
                                            radius = 12.dp.toPx(),
                                            center = off
                                        )
                                        drawCircle(
                                            color = Color.White,
                                            radius = 4.5.dp.toPx(),
                                            center = off
                                        )
                                    } else {
                                        drawCircle(
                                            color = Color(selectedColorHex),
                                            radius = 9.dp.toPx(),
                                            center = off
                                        )
                                        drawCircle(
                                            color = Color.Black,
                                            radius = 3.5.dp.toPx(),
                                            center = off
                                        )
                                    }
                                }
                            }

                            // 4. Draw User GPS Location Marker
                            val userWx = lngToWorldX(userLng, currentZoom)
                            val userWy = latToWorldY(userLat, currentZoom)
                            val userPx = (containerWidthPx / 2.0 + (userWx - centerWorldX) * scaleFactor).toFloat()
                            val userPy = (containerHeightPx / 2.0 + (userWy - centerWorldY) * scaleFactor).toFloat()
                            val userOffset = Offset(userPx, userPy)

                            // Pulsing radius aura around user
                            drawCircle(
                                color = Color(0x332979FF),
                                radius = 24.dp.toPx(),
                                center = userOffset
                            )
                            drawCircle(
                                color = Color(0xFF2979FF),
                                radius = 7.dp.toPx(),
                                center = userOffset
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 3.dp.toPx(),
                                center = userOffset
                            )
                        }

                        // 5. Node Action Toolbar (When a node is selected -> can delete it)
                        val selIdx = selectedNodeIndex
                        if (selIdx != null && selIdx in points.indices) {
                            Card(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 8.dp)
                                    .testTag("selected_node_action_card"),
                                shape = RoundedCornerShape(100.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xEB1E202B)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BusRoseAlert.copy(alpha = 0.7f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(BusRoseAlert, CircleShape)
                                    )
                                    Text(
                                        text = "節點 #${selIdx + 1}",
                                        color = BusTextPrimary,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Button(
                                        onClick = {
                                            points.removeAt(selIdx)
                                            selectedNodeIndex = null
                                            actionMessage = "已刪除節點 #${selIdx + 1}"
                                        },
                                        shape = RoundedCornerShape(100.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = BusRoseAlert,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.height(26.dp).testTag("btn_delete_selected_node")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Node",
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("刪除節點", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                    }

                                    IconButton(
                                        onClick = { selectedNodeIndex = null },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Deselect",
                                            tint = BusTextSecondary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 6. Floating Zoom Controls
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Zoom In
                            IconButton(
                                onClick = { zoomLevel = (zoomLevel + 1).coerceAtMost(18) },
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(BusDarkSurface, CircleShape)
                                    .border(1.dp, BusSubtleBorder, CircleShape)
                                    .testTag("btn_zoom_in")
                            ) {
                                Text("+", color = BusLavenderPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            }

                            // Zoom Out
                            IconButton(
                                onClick = { zoomLevel = (zoomLevel - 1).coerceAtLeast(13) },
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(BusDarkSurface, CircleShape)
                                    .border(1.dp, BusSubtleBorder, CircleShape)
                                    .testTag("btn_zoom_out")
                            ) {
                                Text("-", color = BusLavenderPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            }
                        }

                        // 7. Mini Status Badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(6.dp)
                                .background(Color(0xD910121A), RoundedCornerShape(6.dp))
                                .border(1.dp, BusSubtleBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (actionMessage != null) actionMessage!! else if (interactionMode == MapInteractionMode.PAN_MAP) "✋ 滑動以移動地圖" else "✏️ 點擊新增 • 點節點可刪除",
                                color = if (actionMessage != null) BusLavenderPrimary else BusTextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Bottom Action Buttons - Guaranteed 100% visible on screen
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (initialArea != null && onDelete != null) {
                            OutlinedButton(
                                onClick = {
                                    onDelete(initialArea.id)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(100.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = BusRoseAlert),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .testTag("btn_delete_area")
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("刪除區域", fontSize = 12.5.sp)
                            }
                        } else {
                            OutlinedButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(100.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                            ) {
                                Text("取消", color = BusTextSecondary, fontSize = 12.5.sp)
                            }
                        }

                        Button(
                            onClick = {
                                if (points.size >= 3 && areaName.isNotBlank()) {
                                    onSave(areaName.trim(), points.toList(), selectedColorHex, initialArea?.id ?: 0L)
                                    onDismiss()
                                }
                            },
                            enabled = points.size >= 3 && areaName.isNotBlank(),
                            shape = RoundedCornerShape(100.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BusLavenderPrimary,
                                contentColor = BusLavenderOnPrimary
                            ),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(40.dp)
                                .testTag("btn_save_area")
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (initialArea == null) "儲存地理區域" else "更新區域",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

