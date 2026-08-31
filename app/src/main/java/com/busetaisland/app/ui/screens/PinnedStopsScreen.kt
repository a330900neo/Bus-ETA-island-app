package com.busetaisland.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.busetaisland.app.data.local.GeoPoint
import com.busetaisland.app.data.local.GeofenceAreaEntity
import com.busetaisland.app.data.local.PinnedStopEntity
import com.busetaisland.app.data.location.LocationTracker
import com.busetaisland.app.data.model.MtrRegistry
import com.busetaisland.app.ui.components.GeofenceAreaEditorDialog
import com.busetaisland.app.ui.theme.BusCardBorder
import com.busetaisland.app.ui.theme.BusDarkBackground
import com.busetaisland.app.ui.theme.BusDarkSurface
import com.busetaisland.app.ui.theme.BusDarkSurfaceElevated
import com.busetaisland.app.ui.theme.BusDarkSurfaceVariant
import com.busetaisland.app.ui.theme.BusEmeraldContainer
import com.busetaisland.app.ui.theme.BusEmeraldGreen
import com.busetaisland.app.ui.theme.BusLavenderContainer
import com.busetaisland.app.ui.theme.BusLavenderOnContainer
import com.busetaisland.app.ui.theme.BusLavenderOnPrimary
import com.busetaisland.app.ui.theme.BusLavenderPrimary
import com.busetaisland.app.ui.theme.BusRoseAlert
import com.busetaisland.app.ui.theme.BusSubtleBorder
import com.busetaisland.app.ui.theme.BusTextMuted
import com.busetaisland.app.ui.theme.BusTextPrimary
import com.busetaisland.app.ui.theme.BusTextSecondary
import com.busetaisland.app.ui.viewmodel.BusViewModel

@Composable
fun PinnedStopsScreen(
    viewModel: BusViewModel,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pinnedStops by viewModel.allPinnedStops.collectAsState()
    val geofenceAreas by viewModel.allGeofenceAreas.collectAsState()
    val trackedBus by viewModel.trackedBus.collectAsState()
    val userLoc by viewModel.userLocation.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Pinned Stops, 1 = Geofence Areas
    var editingArea by remember { mutableStateOf<GeofenceAreaEntity?>(null) }
    var isCreatingArea by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BusDarkBackground)
            .padding(horizontal = 16.dp)
            .testTag("pinned_stops_screen")
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Saved Stops & Geofences",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = BusTextPrimary,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Manage trigger zones (Radius or Polygon Areas) for stops",
                    style = MaterialTheme.typography.bodySmall,
                    color = BusTextSecondary
                )
            }

            if (selectedTab == 0) {
                IconButton(
                    onClick = onNavigateToSearch,
                    modifier = Modifier
                        .size(42.dp)
                        .background(BusDarkSurface, CircleShape)
                        .testTag("add_pinned_stop_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search and Pin",
                        tint = BusLavenderPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = { isCreatingArea = true },
                    modifier = Modifier
                        .size(42.dp)
                        .background(BusDarkSurface, CircleShape)
                        .testTag("create_geofence_area_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Area",
                        tint = BusLavenderPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = BusDarkSurface,
            contentColor = BusLavenderPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = BusLavenderPrimary,
                    height = 3.dp
                )
            },
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, BusSubtleBorder, RoundedCornerShape(14.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("已釘選巴士站 (${pinnedStops.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                },
                selectedContentColor = BusLavenderPrimary,
                unselectedContentColor = BusTextSecondary
            )

            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("多邊形區域 (${geofenceAreas.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                },
                selectedContentColor = BusLavenderPrimary,
                unselectedContentColor = BusTextSecondary
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedTab == 0) {
            // Pinned Stops List
            if (pinnedStops.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(BusDarkSurface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = "No Pinned",
                                tint = BusLavenderPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "未有已儲存的巴士站",
                            fontWeight = FontWeight.Bold,
                            color = BusTextPrimary,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "搜尋巴士路線並點擊「追蹤」，即可在此自訂半徑或多邊形觸發區域。",
                            color = BusTextSecondary,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onNavigateToSearch,
                            shape = RoundedCornerShape(100.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BusLavenderPrimary,
                                contentColor = BusLavenderOnPrimary
                            ),
                            modifier = Modifier.testTag("empty_state_search_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("搜尋巴士路線", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("pinned_stops_list"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(pinnedStops, key = { it.id }) { item ->
                        val isCurrentlyActive = trackedBus?.stopId == item.stopId && trackedBus?.route == item.route

                        PinnedStopCardWithGeofence(
                            pinned = item,
                            isActive = isCurrentlyActive,
                            areas = geofenceAreas,
                            onSelect = { viewModel.selectPinnedStopToTrack(item) },
                            onDelete = { viewModel.deletePinned(item.id) },
                            onTriggerTypeChange = { newType, areaId ->
                                viewModel.updateStopTriggerMode(
                                    id = item.id,
                                    triggerType = newType,
                                    areaId = areaId,
                                    radiusMeters = item.radiusMeters
                                )
                            },
                            onRadiusChange = { newRadius ->
                                viewModel.updateStopTriggerMode(
                                    id = item.id,
                                    triggerType = item.triggerType,
                                    areaId = item.areaId,
                                    radiusMeters = newRadius
                                )
                            },
                            onCreateArea = { isCreatingArea = true }
                        )
                    }
                }
            }
        } else {
            // Geofence Areas Management List
            if (geofenceAreas.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(BusDarkSurface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "No Areas",
                                tint = BusLavenderPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "尚未建立任何多邊形區域",
                            fontWeight = FontWeight.Bold,
                            color = BusTextPrimary,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "您可以自訂特定形狀的地理區域（如屋苑範圍、公司周邊、大型轉車站），多個巴士站可綁定同一區域！",
                            color = BusTextSecondary,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { isCreatingArea = true },
                            shape = RoundedCornerShape(100.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BusLavenderPrimary,
                                contentColor = BusLavenderOnPrimary
                            ),
                            modifier = Modifier.testTag("create_first_area_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("建立新地理區域", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("geofence_areas_list"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Button(
                            onClick = { isCreatingArea = true },
                            modifier = Modifier.fillMaxWidth().testTag("add_new_area_btn"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BusLavenderPrimary,
                                contentColor = BusLavenderOnPrimary
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("在地圖繪製新多邊形區域", fontWeight = FontWeight.Bold)
                        }
                    }

                    items(geofenceAreas, key = { it.id }) { area ->
                        val boundStopsCount = pinnedStops.count { it.triggerType == "AREA" && it.areaId == area.id }
                        val isInside = LocationTracker.isPointInPolygon(userLoc.latitude, userLoc.longitude, area.getPoints())

                        GeofenceAreaCard(
                            area = area,
                            boundStopsCount = boundStopsCount,
                            isUserInside = isInside,
                            onEdit = { editingArea = area },
                            onDelete = { viewModel.deleteGeofenceArea(area.id) }
                        )
                    }
                }
            }
        }
    }

    // Dialog for Creating/Editing Geofence Area
    if (isCreatingArea) {
        GeofenceAreaEditorDialog(
            initialArea = null,
            userLat = userLoc.latitude,
            userLng = userLoc.longitude,
            onDismiss = { isCreatingArea = false },
            onSave = { name, points, colorHex, id ->
                viewModel.saveGeofenceArea(name, points, colorHex, id)
                isCreatingArea = false
            }
        )
    }

    if (editingArea != null) {
        GeofenceAreaEditorDialog(
            initialArea = editingArea,
            userLat = userLoc.latitude,
            userLng = userLoc.longitude,
            onDismiss = { editingArea = null },
            onSave = { name, points, colorHex, id ->
                viewModel.saveGeofenceArea(name, points, colorHex, id)
                editingArea = null
            },
            onDelete = { id ->
                viewModel.deleteGeofenceArea(id)
                editingArea = null
            }
        )
    }
}

@Composable
private fun PinnedStopCardWithGeofence(
    pinned: PinnedStopEntity,
    isActive: Boolean,
    areas: List<GeofenceAreaEntity>,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onTriggerTypeChange: (String, Long?) -> Unit,
    onRadiusChange: (Float) -> Unit,
    onCreateArea: () -> Unit
) {
    var radiusInput by remember(pinned.radiusMeters) { mutableStateOf(pinned.radiusMeters.toInt().toString()) }
    var showAreaDropdown by remember { mutableStateOf(false) }

    val isAreaMode = pinned.triggerType.equals("AREA", ignoreCase = true)
    val selectedArea = areas.find { it.id == pinned.areaId }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isActive) BusLavenderPrimary else BusSubtleBorder,
                RoundedCornerShape(22.dp)
            )
            .testTag("pinned_card_${pinned.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) BusDarkSurfaceElevated else BusDarkSurface
        ),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Route, Destination, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val isMtr = pinned.co.equals("MTR", ignoreCase = true)
                    val routeBgColor = if (isMtr) {
                        MtrRegistry.getRouteColor(pinned.co, pinned.route)
                    } else if (isActive) {
                        BusLavenderPrimary
                    } else {
                        BusDarkSurfaceVariant
                    }
                    val routeTextColor = if (isMtr) {
                        MtrRegistry.getRouteTextColor(pinned.co, pinned.route)
                    } else if (isActive) {
                        BusLavenderOnPrimary
                    } else {
                        BusTextPrimary
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                color = routeBgColor,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = pinned.route,
                            color = routeTextColor,
                            fontWeight = FontWeight.Black,
                            fontSize = if (isMtr) 13.5.sp else 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "➔ ${pinned.destTc.ifBlank { pinned.destEn }}",
                            color = BusTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "@ ${pinned.stopNameTc.ifBlank { pinned.stopNameEn }}",
                            color = BusTextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(34.dp).testTag("delete_pinned_${pinned.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = BusRoseAlert.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Trigger Mode Switcher: [ 半徑 Radius ] vs [ 多邊形區域 Area ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BusDarkSurfaceVariant, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!isAreaMode) BusLavenderPrimary else Color.Transparent)
                        .clickable {
                            onTriggerTypeChange("RADIUS", null)
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = null,
                            tint = if (!isAreaMode) BusLavenderOnPrimary else BusTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "圓形半徑 (Radius)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!isAreaMode) BusLavenderOnPrimary else BusTextSecondary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isAreaMode) BusLavenderPrimary else Color.Transparent)
                        .clickable {
                            val defaultAreaId = areas.firstOrNull()?.id
                            onTriggerTypeChange("AREA", defaultAreaId)
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = if (isAreaMode) BusLavenderOnPrimary else BusTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "多邊形區域 (Area)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAreaMode) BusLavenderOnPrimary else BusTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Trigger Config Detail (Radius Input vs Area Selector)
            if (!isAreaMode) {
                // Radius Mode Editor
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BusDarkSurfaceVariant, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Radius",
                            tint = BusLavenderPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "個別半徑:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BusTextSecondary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val newR = (pinned.radiusMeters - 50f).coerceAtLeast(30f)
                                radiusInput = newR.toInt().toString()
                                onRadiusChange(newR)
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Text("-", color = BusLavenderPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        OutlinedTextField(
                            value = radiusInput,
                            onValueChange = { newVal ->
                                if (newVal.all { it.isDigit() } && newVal.length <= 5) {
                                    radiusInput = newVal
                                    val parsed = newVal.toFloatOrNull()
                                    if (parsed != null && parsed > 0f) {
                                        onRadiusChange(parsed)
                                    }
                                }
                            },
                            modifier = Modifier
                                .width(72.dp)
                                .height(42.dp),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = BusTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            ),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BusLavenderPrimary,
                                unfocusedBorderColor = BusSubtleBorder,
                                focusedContainerColor = BusDarkSurface,
                                unfocusedContainerColor = BusDarkSurface
                            )
                        )

                        Text("m", color = BusTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                        IconButton(
                            onClick = {
                                val newR = (pinned.radiusMeters + 50f).coerceAtMost(3000f)
                                radiusInput = newR.toInt().toString()
                                onRadiusChange(newR)
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Text("+", color = BusLavenderPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            } else {
                // Polygon Area Selection Mode
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BusDarkSurfaceVariant, RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "綁定的多邊形區域 (多個巴士站可共用同一區域):",
                        fontSize = 11.sp,
                        color = BusTextSecondary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(BusDarkSurface)
                                .border(1.dp, BusSubtleBorder, RoundedCornerShape(8.dp))
                                .clickable { showAreaDropdown = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (selectedArea != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(Color(selectedArea.colorHex), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = selectedArea.name,
                                        color = BusTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                } else {
                                    Text(
                                        text = "點擊選擇或新增地理區域...",
                                        color = BusTextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Text("切換 ▼", color = BusLavenderPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        DropdownMenu(
                            expanded = showAreaDropdown,
                            onDismissRequest = { showAreaDropdown = false },
                            modifier = Modifier.background(BusDarkSurfaceElevated)
                        ) {
                            areas.forEach { area ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .background(Color(area.colorHex), CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(area.name, color = BusTextPrimary, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("(${area.getPoints().size} 節點)", color = BusTextMuted, fontSize = 11.sp)
                                        }
                                    },
                                    onClick = {
                                        onTriggerTypeChange("AREA", area.id)
                                        showAreaDropdown = false
                                    }
                                )
                            }

                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = BusLavenderPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("+ 建立全新地理區域...", color = BusLavenderPrimary, fontWeight = FontWeight.Bold)
                                    }
                                },
                                onClick = {
                                    showAreaDropdown = false
                                    onCreateArea()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Active Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .background(Color(0x1F86F8B6), RoundedCornerShape(100.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Active",
                                tint = BusEmeraldGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "動態島正在追蹤",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BusEmeraldGreen
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onSelect,
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BusLavenderPrimary,
                            contentColor = BusLavenderOnPrimary
                        ),
                        modifier = Modifier.height(34.dp).testTag("activate_pinned_${pinned.id}")
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("切換至動態島", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun GeofenceAreaCard(
    area: GeofenceAreaEntity,
    boundStopsCount: Int,
    isUserInside: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val points = remember(area.pointsJson) { area.getPoints() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BusSubtleBorder, RoundedCornerShape(18.dp))
            .testTag("geofence_area_card_${area.id}"),
        colors = CardDefaults.cardColors(containerColor = BusDarkSurface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Mini Polygon Canvas Preview
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BusDarkSurfaceElevated)
                    .border(1.dp, BusSubtleBorder, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                    if (points.isNotEmpty()) {
                        val minLat = points.minOf { it.lat }
                        val maxLat = points.maxOf { it.lat }
                        val minLng = points.minOf { it.lng }
                        val maxLng = points.maxOf { it.lng }

                        val spanLat = (maxLat - minLat).coerceAtLeast(0.0001)
                        val spanLng = (maxLng - minLng).coerceAtLeast(0.0001)

                        val path = Path()
                        val w = size.width
                        val h = size.height

                        val offsets = points.map { pt ->
                            val x = (((pt.lng - minLng) / spanLng) * w).toFloat()
                            val y = (h - ((pt.lat - minLat) / spanLat) * h).toFloat()
                            Offset(x, y)
                        }

                        path.moveTo(offsets[0].x, offsets[0].y)
                        for (i in 1 until offsets.size) {
                            path.lineTo(offsets[i].x, offsets[i].y)
                        }
                        path.close()

                        drawPath(path = path, color = Color(area.colorHex).copy(alpha = 0.35f), style = Fill)
                        drawPath(path = path, color = Color(area.colorHex), style = Stroke(width = 2.dp.toPx()))
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(area.colorHex), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = area.name,
                        fontWeight = FontWeight.Bold,
                        color = BusTextPrimary,
                        fontSize = 14.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${points.size} 個頂點",
                        color = BusTextSecondary,
                        fontSize = 11.5.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• $boundStopsCount 個巴士站綁定",
                        color = BusLavenderPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isUserInside) "● 您目前在此區域內 (動態島顯示)" else "○ 您目前在此區域外 (動態島隱藏)",
                    color = if (isUserInside) BusEmeraldGreen else BusTextMuted,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(34.dp).testTag("edit_area_${area.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Area",
                        tint = BusLavenderPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(34.dp).testTag("delete_area_${area.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Area",
                        tint = BusRoseAlert.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
