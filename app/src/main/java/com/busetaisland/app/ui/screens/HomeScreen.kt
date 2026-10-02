package com.busetaisland.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.busetaisland.app.data.model.MtrRegistry
import com.busetaisland.app.ui.overlay.DynamicIslandOverlayContent
import com.busetaisland.app.ui.theme.BusAmberWarning
import com.busetaisland.app.ui.theme.BusCardBorder
import com.busetaisland.app.ui.theme.BusDarkBackground
import com.busetaisland.app.ui.theme.BusDarkSurface
import com.busetaisland.app.ui.theme.BusDarkSurfaceElevated
import com.busetaisland.app.ui.theme.BusDarkSurfaceVariant
import com.busetaisland.app.ui.theme.BusEmeraldGreen
import com.busetaisland.app.ui.theme.BusIslandBlack
import com.busetaisland.app.ui.theme.BusLavenderContainer
import com.busetaisland.app.ui.theme.BusLavenderOnContainer
import com.busetaisland.app.ui.theme.BusLavenderOnPrimary
import com.busetaisland.app.ui.theme.BusLavenderPrimary
import com.busetaisland.app.ui.theme.BusRoseAlert
import com.busetaisland.app.ui.theme.BusRoseContainer
import com.busetaisland.app.ui.theme.BusSubtleBorder
import com.busetaisland.app.ui.theme.BusTextMuted
import com.busetaisland.app.ui.theme.BusTextPrimary
import com.busetaisland.app.ui.theme.BusTextSecondary
import com.busetaisland.app.ui.viewmodel.BusViewModel

@Composable
fun HomeScreen(
    viewModel: BusViewModel,
    onNavigateToSearch: () -> Unit,
    onNavigateToPinned: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val trackedBus by viewModel.trackedBus.collectAsState()
    val allTrackedBuses by viewModel.allTrackedBuses.collectAsState()
    val activeBusIndex by viewModel.activeBusIndex.collectAsState()
    val overlayConfig by viewModel.overlayConfig.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BusDarkBackground)
            .padding(horizontal = 16.dp)
            .testTag("home_screen_lazy_column"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            // Top Header & Status Banner matching Sophisticated Dark
            HomeTopHeader()
        }

        // Live Dynamic Island Master Card & Interactive Live Preview
        item {
            IslandMasterCard(
                trackedBus = trackedBus,
                isOverlayEnabled = overlayConfig.isOverlayEnabled,
                isCollapsed = overlayConfig.isCollapsed,
                onToggleOverlay = { viewModel.setOverlayEnabled(!overlayConfig.isOverlayEnabled) },
                onToggleCollapse = { viewModel.toggleOverlayCollapsed() },
                onRefreshEta = { viewModel.refreshActiveEta() }
            )
        }

        // Multi-Bus Stops Tracking Card (New: Switch between multiple stops)
        item {
            MultiBusStopsTrackingCard(
                allTrackedBuses = allTrackedBuses,
                activeBusIndex = activeBusIndex,
                onSelectBus = { index -> viewModel.selectBusIndex(index) },
                onNavigateToSearch = onNavigateToSearch,
                onNavigateToPinned = onNavigateToPinned
            )
        }

        // Vertical Offset (Notch Avoidance) Card
        item {
            VerticalOffsetControlCard(
                currentPosY = overlayConfig.posY,
                onOffsetYChanged = { viewModel.setVerticalOffset(it) }
            )
        }

        // The Bus ETA One-Line Summary Card (User's specific core requirement)
        item {
            OneLineEtaFeatureCard(
                trackedBus = trackedBus,
                etaUnit = overlayConfig.etaUnit,
                onToggleUnit = { viewModel.toggleEtaUnit() }
            )
        }

        // Stop & Geofencing Radius Control Card
        item {
            GeofenceRadiusCard(
                trackedBus = trackedBus,
                userLocation = userLocation,
                onRadiusChanged = { radius -> viewModel.updateRadius(radius, true) },
                onToggleGeofence = { enabled ->
                    viewModel.updateRadius(trackedBus?.radiusMeters ?: 300f, enabled)
                },
                onOpenSearch = onNavigateToSearch
            )
        }

        // Interactive Distance Simulation for Testing Geofence
        item {
            LocationSimulationCard(
                isSimulated = userLocation.isSimulated,
                trackedBus = trackedBus,
                onSimulateDistance = { dist -> viewModel.simulateDistance(dist) },
                onResetGps = { viewModel.resetSimulationToRealGPS() }
            )
        }

        // Permission & Lock Screen Overlay Setup Helper Card
        item {
            AccessibilityAndOverlayCard(
                context = context,
                isOverlayEnabled = overlayConfig.isOverlayEnabled
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HomeTopHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Transit Overlay",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = BusTextPrimary,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Lockscreen & Dynamic Island Overlay",
                style = MaterialTheme.typography.bodySmall,
                color = BusTextSecondary
            )
        }

        Box(
            modifier = Modifier
                .background(
                    color = BusDarkSurface,
                    shape = RoundedCornerShape(20.dp)
                )
                .border(
                    width = 1.dp,
                    color = BusSubtleBorder,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(BusEmeraldGreen, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "KMB LIVE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BusLavenderPrimary
                )
            }
        }
    }
}

@Composable
private fun IslandMasterCard(
    trackedBus: com.busetaisland.app.data.model.TrackedBusInfo?,
    isOverlayEnabled: Boolean,
    isCollapsed: Boolean,
    onToggleOverlay: () -> Unit,
    onToggleCollapse: () -> Unit,
    onRefreshEta: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(28.dp))
            .border(
                1.dp,
                BusSubtleBorder,
                RoundedCornerShape(28.dp)
            )
            .testTag("island_master_card"),
        colors = CardDefaults.cardColors(containerColor = BusDarkSurface),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(BusLavenderContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Dynamic Island",
                            tint = BusLavenderPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Dynamic Island Overlay",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = BusTextPrimary
                        )
                        Text(
                            text = if (isOverlayEnabled) "Floating & active on Lockscreen" else "Overlay disabled",
                            fontSize = 12.sp,
                            color = if (isOverlayEnabled) BusEmeraldGreen else BusTextMuted
                        )
                    }
                }

                Switch(
                    checked = isOverlayEnabled,
                    onCheckedChange = { onToggleOverlay() },
                    modifier = Modifier.testTag("overlay_master_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BusLavenderOnPrimary,
                        checkedTrackColor = BusLavenderPrimary,
                        uncheckedThumbColor = BusTextMuted,
                        uncheckedTrackColor = BusDarkSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Live Visual Preview Container on Island Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BusIslandBlack, RoundedCornerShape(20.dp))
                    .border(1.dp, BusSubtleBorder, RoundedCornerShape(20.dp))
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                DynamicIslandOverlayContent(
                    onExpandToggle = onToggleCollapse,
                    onClose = onToggleOverlay
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onToggleCollapse,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("toggle_island_size_btn"),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BusDarkSurfaceVariant,
                        contentColor = BusTextPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (isCollapsed) Icons.Default.UnfoldMore else Icons.Default.UnfoldLess,
                        contentDescription = "Size",
                        modifier = Modifier.size(16.dp),
                        tint = BusLavenderPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isCollapsed) "Expand Pill" else "Collapse Circle",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(
                    onClick = onRefreshEta,
                    modifier = Modifier
                        .background(BusLavenderPrimary, CircleShape)
                        .size(44.dp)
                        .testTag("refresh_eta_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = BusLavenderOnPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OneLineEtaFeatureCard(
    trackedBus: com.busetaisland.app.data.model.TrackedBusInfo?,
    etaUnit: com.busetaisland.app.data.model.EtaDisplayUnit,
    onToggleUnit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BusSubtleBorder, RoundedCornerShape(24.dp))
            .testTag("one_line_eta_card"),
        colors = CardDefaults.cardColors(containerColor = BusDarkSurface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "單行動態島到站顯示",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BusLavenderPrimary,
                    letterSpacing = 0.5.sp
                )
                // Unit Switcher: Minutes vs Exact Time
                Button(
                    onClick = onToggleUnit,
                    shape = RoundedCornerShape(100.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BusLavenderPrimary.copy(alpha = 0.2f),
                        contentColor = BusLavenderPrimary
                    )
                ) {
                    Text(
                        text = if (etaUnit == com.busetaisland.app.data.model.EtaDisplayUnit.MINUTES) "切換至 具體時間 (xx:xx)" else "切換至 分鐘倒數",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // User requirement: "the bus eta one line should be route number, destination and 1st and 2nd eta"
            val oneLineText = if (trackedBus != null) {
                "${trackedBus.route} ➔ ${trackedBus.chineseDestination} | 1st: ${trackedBus.formattedEta1(etaUnit)} | 2nd: ${trackedBus.formattedEta2(etaUnit)}"
            } else {
                "1A ➔ 尖沙咀碼頭 | 1st: 4m (15:42) | 2nd: 12m (15:50)"
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF000000), RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text(
                    text = oneLineText,
                    color = BusLavenderPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun GeofenceRadiusCard(
    trackedBus: com.busetaisland.app.data.model.TrackedBusInfo?,
    userLocation: com.busetaisland.app.data.location.UserLocationState,
    onRadiusChanged: (Float) -> Unit,
    onToggleGeofence: (Boolean) -> Unit,
    onOpenSearch: () -> Unit
) {
    val radius = trackedBus?.radiusMeters ?: 300f
    val isGeofenceEnabled = trackedBus?.isGeofenceEnabled ?: true
    val distance = trackedBus?.distanceMeters?.toInt()
    val isInRange = trackedBus?.isInRange ?: true

    var textInput by remember(radius) { mutableStateOf(radius.toInt().toString()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BusSubtleBorder, RoundedCornerShape(28.dp))
            .testTag("geofence_radius_card"),
        colors = CardDefaults.cardColors(containerColor = BusDarkSurfaceElevated),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                if (isInRange) Color(0x1F86F8B6) else Color(0x1FFFB4AB),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = "GPS Geofence",
                            tint = if (isInRange) BusEmeraldGreen else BusRoseAlert,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "GPS 偵測半徑 (自訂數值)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = BusTextPrimary
                        )
                        Text(
                            text = "${radius.toInt()} 公尺",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BusLavenderPrimary
                        )
                    }
                }

                // Freely type int input field
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    androidx.compose.material3.OutlinedTextField(
                        value = textInput,
                        onValueChange = { newVal ->
                            if (newVal.all { it.isDigit() } && newVal.length <= 5) {
                                textInput = newVal
                                val parsed = newVal.toFloatOrNull()
                                if (parsed != null && parsed > 0f) {
                                    onRadiusChanged(parsed)
                                }
                            }
                        },
                        modifier = Modifier
                            .width(80.dp)
                            .height(44.dp),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = BusTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        ),
                        shape = RoundedCornerShape(10.dp),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BusLavenderPrimary,
                            unfocusedBorderColor = BusSubtleBorder,
                            focusedContainerColor = BusDarkSurface,
                            unfocusedContainerColor = BusDarkSurface
                        )
                    )
                    Text("m", color = BusTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "進入此半徑範圍時動態島將自動喚醒，遠離時則自動暫停以節省電量。",
                fontSize = 12.sp,
                color = BusTextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Current Distance Status Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BusDarkSurface, RoundedCornerShape(14.dp))
                    .border(1.dp, BusSubtleBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = "Distance",
                        tint = if (isInRange) BusEmeraldGreen else BusRoseAlert,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (distance != null) "距離巴士站 $distance 公尺" else "GPS 定位中...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isInRange) BusEmeraldGreen else BusRoseAlert
                    )
                }

                Text(
                    text = if (isInRange) "範圍內 ✓" else "超出半徑 ✕",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isInRange) BusEmeraldGreen else BusRoseAlert
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Radius Slider with Sophisticated Styling
            Slider(
                value = radius.coerceIn(30f, 1000f),
                onValueChange = {
                    onRadiusChanged(it)
                    textInput = it.toInt().toString()
                },
                valueRange = 30f..1000f,
                steps = 19,
                modifier = Modifier.testTag("radius_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = BusLavenderPrimary,
                    activeTrackColor = BusLavenderPrimary,
                    inactiveTrackColor = BusDarkSurfaceVariant
                )
            )

            // Preset Quick Radius Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(50f, 100f, 200f, 300f, 500f).forEach { r ->
                    FilterChip(
                        selected = radius.toInt() == r.toInt(),
                        onClick = {
                            onRadiusChanged(r)
                            textInput = r.toInt().toString()
                        },
                        label = { Text("${r.toInt()}m", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag("radius_chip_${r.toInt()}"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BusLavenderPrimary,
                            selectedLabelColor = BusLavenderOnPrimary,
                            containerColor = BusDarkSurface,
                            labelColor = BusTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = radius.toInt() == r.toInt(),
                            borderColor = BusSubtleBorder
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Switch Stop button
            Button(
                onClick = onOpenSearch,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("search_change_stop_btn"),
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BusLavenderPrimary,
                    contentColor = BusLavenderOnPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("搜尋及切換巴士路線 / 車站", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LocationSimulationCard(
    isSimulated: Boolean,
    trackedBus: com.busetaisland.app.data.model.TrackedBusInfo?,
    onSimulateDistance: (Float) -> Unit,
    onResetGps: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BusSubtleBorder, RoundedCornerShape(24.dp))
            .testTag("location_simulation_card"),
        colors = CardDefaults.cardColors(containerColor = BusDarkSurface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Simulate",
                        tint = BusAmberWarning,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "GPS Geofence Test Simulator",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BusTextPrimary
                    )
                }

                if (isSimulated) {
                    Text(
                        text = "Sim Active",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = BusAmberWarning
                    )
                }
            }

            Text(
                text = "Simulate your distance to verify Dynamic Island showing or pausing according to radius.",
                fontSize = 12.sp,
                color = BusTextSecondary,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { onSimulateDistance(25f) },
                    modifier = Modifier.weight(1f).testTag("sim_at_stop_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF005234),
                        contentColor = BusEmeraldGreen
                    )
                ) {
                    Text("At Stop (25m)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = { onSimulateDistance(180f) },
                    modifier = Modifier.weight(1f).testTag("sim_nearby_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = BusLavenderContainer,
                        contentColor = BusLavenderOnContainer
                    )
                ) {
                    Text("Near (180m)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = { onSimulateDistance(750f) },
                    modifier = Modifier.weight(1f).testTag("sim_far_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = BusRoseContainer,
                        contentColor = BusRoseAlert
                    )
                ) {
                    Text("Far (750m)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (isSimulated) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onResetGps,
                    modifier = Modifier.fillMaxWidth().testTag("reset_gps_btn"),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BusLavenderPrimary)
                ) {
                    Text("Reset to Device Real GPS Location", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun AccessibilityAndOverlayCard(
    context: android.content.Context,
    isOverlayEnabled: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BusSubtleBorder, RoundedCornerShape(24.dp))
            .testTag("permissions_setup_card"),
        colors = CardDefaults.cardColors(containerColor = BusDarkSurface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Permissions",
                    tint = BusLavenderPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lockscreen & Accessibility Setup",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = BusTextPrimary
                )
            }

            Text(
                text = "For showing over Lockscreen and all apps, ensure Overlay and Accessibility permissions are granted in System Settings.",
                fontSize = 12.sp,
                color = BusTextSecondary,
                modifier = Modifier.padding(vertical = 6.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("open_accessibility_settings_btn"),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = BusDarkSurfaceVariant,
                        contentColor = BusTextPrimary
                    )
                ) {
                    Text("Accessibility", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                FilledTonalButton(
                    onClick = {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                ).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            }
                        } catch (e: Exception) {
                            // Fallback
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("open_overlay_settings_btn"),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = BusDarkSurfaceVariant,
                        contentColor = BusTextPrimary
                    )
                ) {
                    Text("Overlay Window", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun VerticalOffsetControlCard(
    currentPosY: Int,
    onOffsetYChanged: (Int) -> Unit
) {
    var textInput by remember(currentPosY) { mutableStateOf(currentPosY.toString()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BusSubtleBorder, RoundedCornerShape(24.dp))
            .testTag("vertical_offset_card"),
        colors = CardDefaults.cardColors(containerColor = BusDarkSurface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(BusLavenderContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Vertical Offset",
                            tint = BusLavenderPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "動態島垂直偏移量 (Notch 避讓)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = BusTextPrimary
                        )
                        Text(
                            text = "Y 軸偏移: $currentPosY px",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BusLavenderPrimary
                        )
                    }
                }

                // Direct number input
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    androidx.compose.material3.OutlinedTextField(
                        value = textInput,
                        onValueChange = { newVal ->
                            if (newVal.all { it.isDigit() } && newVal.length <= 4) {
                                textInput = newVal
                                val parsed = newVal.toIntOrNull()
                                if (parsed != null && parsed >= 0) {
                                    onOffsetYChanged(parsed)
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
                        shape = RoundedCornerShape(10.dp),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BusLavenderPrimary,
                            unfocusedBorderColor = BusSubtleBorder,
                            focusedContainerColor = BusDarkSurfaceElevated,
                            unfocusedContainerColor = BusDarkSurfaceElevated
                        )
                    )
                    Text("px", color = BusTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "可依據手機螢幕上方相機挖孔、劉海 (Notch) 或狀態欄位置，自由調整動態島垂直高度。",
                fontSize = 12.sp,
                color = BusTextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Slider from 0 to 200px
            Slider(
                value = currentPosY.toFloat().coerceIn(0f, 200f),
                onValueChange = {
                    val intVal = it.toInt()
                    onOffsetYChanged(intVal)
                    textInput = intVal.toString()
                },
                valueRange = 0f..200f,
                modifier = Modifier.testTag("vertical_offset_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = BusLavenderPrimary,
                    activeTrackColor = BusLavenderPrimary,
                    inactiveTrackColor = BusDarkSurfaceVariant
                )
            )

            // Preset Quick Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    0 to "頂部 0px",
                    25 to "標準 25px",
                    45 to "挖孔 45px",
                    70 to "劉海 70px",
                    100 to "低位 100px"
                ).forEach { (offset, label) ->
                    FilterChip(
                        selected = currentPosY == offset,
                        onClick = {
                            onOffsetYChanged(offset)
                            textInput = offset.toString()
                        },
                        label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BusLavenderPrimary,
                            selectedLabelColor = BusLavenderOnPrimary,
                            containerColor = BusDarkSurfaceElevated,
                            labelColor = BusTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = currentPosY == offset,
                            borderColor = BusSubtleBorder
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun MultiBusStopsTrackingCard(
    allTrackedBuses: List<com.busetaisland.app.data.model.TrackedBusInfo>,
    activeBusIndex: Int,
    onSelectBus: (Int) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToPinned: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BusSubtleBorder, RoundedCornerShape(24.dp))
            .testTag("multi_bus_tracking_card"),
        colors = CardDefaults.cardColors(containerColor = BusDarkSurfaceElevated),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(BusLavenderContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBus,
                            contentDescription = "Multi-Stop",
                            tint = BusLavenderPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "多巴士站同時追蹤 (${allTrackedBuses.size} 個站點)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = BusTextPrimary
                        )
                        Text(
                            text = "動態島展開時將縱向同時顯示所有巴士站，一目了然",
                            fontSize = 11.sp,
                            color = BusLavenderPrimary
                        )
                    }
                }

                IconButton(
                    onClick = onNavigateToPinned,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Manage Pinned",
                        tint = BusLavenderPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (allTrackedBuses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BusDarkSurface, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "尚未儲存或追蹤多個巴士站",
                            fontSize = 12.sp,
                            color = BusTextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onNavigateToSearch,
                            shape = RoundedCornerShape(100.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BusLavenderPrimary,
                                contentColor = BusLavenderOnPrimary
                            ),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("新增/追蹤巴士站", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    allTrackedBuses.forEachIndexed { index, bus ->
                        val isSelected = index == activeBusIndex
                        val isScheduled = bus.isFirstEtaScheduled

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) Color(0xFF262330) else BusDarkSurface)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) BusLavenderPrimary else BusSubtleBorder,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { onSelectBus(index) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                val isMtr = bus.co.equals("MTR", ignoreCase = true)
                                val pillBg = if (isMtr) {
                                    MtrRegistry.getRouteColor(bus.co, bus.route)
                                } else if (isSelected) {
                                    BusLavenderPrimary
                                } else {
                                    BusDarkSurfaceVariant
                                }
                                val pillTextColor = if (isMtr) {
                                    MtrRegistry.getRouteTextColor(bus.co, bus.route)
                                } else if (isSelected) {
                                    BusLavenderOnPrimary
                                } else if (isScheduled) {
                                    Color(0xFF8E8E93)
                                } else {
                                    Color(0xFFD0BCFF)
                                }

                                Box(
                                    modifier = Modifier
                                        .background(
                                            pillBg,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = bus.route,
                                        color = pillTextColor,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "➔ ${bus.chineseDestination}",
                                        color = BusTextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "@ ${bus.stopNameTc.ifBlank { bus.stopNameEn }}",
                                        color = BusTextSecondary,
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val eta1 = bus.formattedEta1(com.busetaisland.app.data.model.EtaDisplayUnit.MINUTES)
                                Text(
                                    text = eta1,
                                    color = if (isScheduled) Color(0xFFAAAAAA) else BusEmeraldGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )

                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Active",
                                        tint = BusEmeraldGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

