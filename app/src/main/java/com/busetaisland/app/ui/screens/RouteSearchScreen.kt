package com.busetaisland.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.busetaisland.app.data.location.LocationTracker
import com.busetaisland.app.data.model.FormattedEtaItem
import com.busetaisland.app.data.model.KmbRouteData
import com.busetaisland.app.data.model.KmbRouteStopData
import com.busetaisland.app.data.model.KmbStopDetail
import com.busetaisland.app.data.model.MtrRegistry
import com.busetaisland.app.ui.theme.BusCardBorder
import com.busetaisland.app.ui.theme.BusDarkBackground
import com.busetaisland.app.ui.theme.BusDarkSurface
import com.busetaisland.app.ui.theme.BusDarkSurfaceElevated
import com.busetaisland.app.ui.theme.BusDarkSurfaceVariant
import com.busetaisland.app.ui.theme.BusEmeraldGreen
import com.busetaisland.app.ui.theme.BusLavenderContainer
import com.busetaisland.app.ui.theme.BusLavenderOnContainer
import com.busetaisland.app.ui.theme.BusLavenderOnPrimary
import com.busetaisland.app.ui.theme.BusLavenderPrimary
import com.busetaisland.app.ui.theme.BusSubtleBorder
import com.busetaisland.app.ui.theme.BusTextMuted
import com.busetaisland.app.ui.theme.BusTextPrimary
import com.busetaisland.app.ui.theme.BusTextSecondary
import com.busetaisland.app.ui.viewmodel.BusViewModel

@Composable
fun RouteSearchScreen(
    viewModel: BusViewModel,
    onStopTracked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCompanyFilter by viewModel.selectedCompanyFilter.collectAsState()
    val filteredRoutes by viewModel.filteredRoutes.collectAsState()
    val allRoutes by viewModel.allRoutes.collectAsState()
    val selectedRoute by viewModel.selectedRoute.collectAsState()
    val selectedBound by viewModel.selectedBound.collectAsState()
    val routeStops by viewModel.routeStops.collectAsState()
    val isLoadingStops by viewModel.isLoadingStops.collectAsState()
    val stopPreviewEtas by viewModel.stopPreviewEtas.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()

    var trackingRadius by remember { mutableStateOf(300f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BusDarkBackground)
            .padding(horizontal = 16.dp)
            .testTag("route_search_screen")
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        if (selectedRoute == null) {
            // Route Search & List View
            Text(
                text = "路線搜尋 / Search Routes",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = BusTextPrimary,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "支援 港鐵 MTR、九巴 KMB、城巴 CTB、專線小巴 GMB",
                style = MaterialTheme.typography.bodySmall,
                color = BusTextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("route_search_input"),
                placeholder = { Text("搜尋路線 (例: 荃灣綫, 1A, 74B, 中環, 旺角...)", color = BusTextMuted, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = BusLavenderPrimary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = BusTextSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(100.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BusLavenderPrimary,
                    unfocusedBorderColor = BusSubtleBorder,
                    focusedContainerColor = BusDarkSurface,
                    unfocusedContainerColor = BusDarkSurface,
                    focusedTextColor = BusTextPrimary,
                    unfocusedTextColor = BusTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Transport Mode / Company Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val filterOptions = listOf(
                    "ALL" to "全部 All",
                    "MTR" to "港鐵 MTR",
                    "KMB" to "九巴 KMB",
                    "CTB" to "城巴 CTB",
                    "GMB" to "小巴 GMB"
                )
                filterOptions.forEach { (coKey, label) ->
                    val isSelected = selectedCompanyFilter == coKey
                    val chipColor = when (coKey) {
                        "MTR" -> Color(0xFFED1D24)
                        "KMB" -> BusLavenderPrimary
                        "CTB" -> Color(0xFFFFD600)
                        "GMB" -> Color(0xFF00C853)
                        else -> BusLavenderPrimary
                    }
                    val textColor = when {
                        !isSelected -> BusTextSecondary
                        coKey == "CTB" || coKey == "GMB" -> Color.Black
                        else -> Color.White
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setCompanyFilter(coKey) },
                        label = { Text(label, fontSize = 11.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                        modifier = Modifier.testTag("filter_chip_$coKey"),
                        shape = RoundedCornerShape(100.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = chipColor,
                            selectedLabelColor = textColor,
                            containerColor = BusDarkSurface,
                            labelColor = BusTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) chipColor else BusSubtleBorder
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick route shortcuts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val quickList = if (selectedCompanyFilter == "MTR") {
                    listOf("荃灣綫", "觀塘綫", "港島綫", "東鐵綫", "屯馬綫", "機場快綫")
                } else {
                    listOf("荃灣綫", "觀塘綫", "1A", "74B", "960", "290A")
                }
                quickList.forEach { r ->
                    FilterChip(
                        selected = searchQuery.equals(r, ignoreCase = true),
                        onClick = { viewModel.onSearchQueryChanged(r) },
                        label = { Text(r, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag("quick_route_$r"),
                        shape = RoundedCornerShape(100.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BusLavenderPrimary,
                            selectedLabelColor = BusLavenderOnPrimary,
                            containerColor = BusDarkSurfaceVariant,
                            labelColor = BusTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = searchQuery.equals(r, ignoreCase = true),
                            borderColor = BusSubtleBorder
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filtered Routes LazyColumn
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("routes_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(
                    items = filteredRoutes,
                    key = { index, routeItem -> "${routeItem.co}_${routeItem.route}_${routeItem.bound}_${routeItem.serviceType}_$index" }
                ) { _, routeItem ->
                    RouteItemCard(
                        routeItem = routeItem,
                        onClick = { viewModel.selectRoute(routeItem) }
                    )
                }
            }
        } else {
            // Stop List for Selected Route View
            val currentRoute = selectedRoute ?: return
            val isMtr = currentRoute.co.equals("MTR", ignoreCase = true)
            val headerColor = MtrRegistry.getRouteColor(currentRoute.co, currentRoute.route)
            val headerTextColor = MtrRegistry.getRouteTextColor(currentRoute.co, currentRoute.route)

            // Resolve proper outbound ("O") and inbound ("I") endpoints from allRoutes
            val outboundRoute = remember(currentRoute, allRoutes) {
                allRoutes.firstOrNull {
                    it.co.equals(currentRoute.co, ignoreCase = true) &&
                    it.route.equals(currentRoute.route, ignoreCase = true) &&
                    (it.bound.equals("O", ignoreCase = true) || it.bound.equals("outbound", ignoreCase = true) || it.bound == "1")
                }
            }
            val inboundRoute = remember(currentRoute, allRoutes) {
                allRoutes.firstOrNull {
                    it.co.equals(currentRoute.co, ignoreCase = true) &&
                    it.route.equals(currentRoute.route, ignoreCase = true) &&
                    (it.bound.equals("I", ignoreCase = true) || it.bound.equals("inbound", ignoreCase = true) || it.bound == "2")
                }
            }

            val outboundDestTc = when {
                outboundRoute != null && outboundRoute.destTc.isNotBlank() -> outboundRoute.destTc
                outboundRoute != null && outboundRoute.destEn.isNotBlank() -> outboundRoute.destEn
                currentRoute.bound.equals("O", ignoreCase = true) -> currentRoute.destTc.ifBlank { currentRoute.destEn }
                currentRoute.origTc.isNotBlank() -> currentRoute.origTc
                else -> currentRoute.destTc.ifBlank { currentRoute.destEn }
            }
            val outboundDestEn = when {
                outboundRoute != null && outboundRoute.destEn.isNotBlank() -> outboundRoute.destEn
                outboundRoute != null && outboundRoute.destTc.isNotBlank() -> outboundRoute.destTc
                currentRoute.bound.equals("O", ignoreCase = true) -> currentRoute.destEn.ifBlank { currentRoute.destTc }
                currentRoute.origEn.isNotBlank() -> currentRoute.origEn
                else -> currentRoute.destEn.ifBlank { currentRoute.destTc }
            }

            val inboundDestTc = when {
                inboundRoute != null && inboundRoute.destTc.isNotBlank() -> inboundRoute.destTc
                inboundRoute != null && inboundRoute.destEn.isNotBlank() -> inboundRoute.destEn
                currentRoute.bound.equals("I", ignoreCase = true) -> currentRoute.destTc.ifBlank { currentRoute.destEn }
                currentRoute.origTc.isNotBlank() -> currentRoute.origTc
                else -> currentRoute.destTc.ifBlank { currentRoute.destEn }
            }
            val inboundDestEn = when {
                inboundRoute != null && inboundRoute.destEn.isNotBlank() -> inboundRoute.destEn
                inboundRoute != null && inboundRoute.destTc.isNotBlank() -> inboundRoute.destTc
                currentRoute.bound.equals("I", ignoreCase = true) -> currentRoute.destEn.ifBlank { currentRoute.destTc }
                currentRoute.origEn.isNotBlank() -> currentRoute.origEn
                else -> currentRoute.destEn.ifBlank { currentRoute.destTc }
            }

            val currentDestTc = if (selectedBound == "O") outboundDestTc else inboundDestTc
            val currentDestEn = if (selectedBound == "O") outboundDestEn else inboundDestEn

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.clearSelectedRoute() },
                    modifier = Modifier
                        .size(40.dp)
                        .background(BusDarkSurface, CircleShape)
                        .testTag("back_to_routes_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = BusLavenderPrimary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(headerColor, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = currentRoute.route,
                                color = headerTextColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "➔ $currentDestTc",
                            color = BusTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = if (isMtr) "選擇車站並查看即時到站班次" else "選擇站點並釘選至動態島即時追蹤",
                        fontSize = 11.sp,
                        color = BusTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Inbound / Outbound Direction Selector Tabs
            TabRow(
                selectedTabIndex = if (selectedBound == "O") 0 else 1,
                containerColor = BusDarkSurface,
                contentColor = if (isMtr) headerColor else BusLavenderPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[if (selectedBound == "O") 0 else 1]),
                        color = if (isMtr) headerColor else BusLavenderPrimary
                    )
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, BusSubtleBorder, RoundedCornerShape(16.dp))
            ) {
                Tab(
                    selected = selectedBound == "O",
                    onClick = { viewModel.toggleRouteBound("O") },
                    text = {
                        Text(
                            text = "往 $outboundDestTc",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 12.sp,
                            fontWeight = if (selectedBound == "O") FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("tab_outbound")
                )
                Tab(
                    selected = selectedBound == "I",
                    onClick = { viewModel.toggleRouteBound("I") },
                    text = {
                        Text(
                            text = "往 $inboundDestTc",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 12.sp,
                            fontWeight = if (selectedBound == "I") FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("tab_inbound")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Radius configuration bar for this stop selection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BusDarkSurface, RoundedCornerShape(14.dp))
                    .border(1.dp, BusSubtleBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "預設追蹤半徑:",
                    color = BusTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    androidx.compose.material3.OutlinedTextField(
                        value = trackingRadius.toInt().toString(),
                        onValueChange = { newVal ->
                            if (newVal.all { it.isDigit() } && newVal.length <= 5) {
                                val parsed = newVal.toFloatOrNull()
                                if (parsed != null && parsed > 0f) {
                                    trackingRadius = parsed
                                }
                            }
                        },
                        modifier = Modifier
                            .width(72.dp)
                            .height(40.dp),
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
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BusLavenderPrimary,
                            unfocusedBorderColor = BusSubtleBorder,
                            focusedContainerColor = BusDarkSurfaceVariant,
                            unfocusedContainerColor = BusDarkSurfaceVariant
                        )
                    )
                    Text("m", color = BusTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isLoadingStops) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = if (isMtr) headerColor else BusLavenderPrimary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("route_stops_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(
                        items = routeStops,
                        key = { index, item -> "${item.first.seq}_${item.first.stop}_$index" }
                    ) { _, (stopData, stopDetail) ->
                        val stopLat = stopDetail?.latitude ?: 0.0
                        val stopLng = stopDetail?.longitude ?: 0.0
                        val dist = if (stopLat != 0.0) {
                            LocationTracker.calculateDistanceMeters(
                                userLocation.latitude,
                                userLocation.longitude,
                                stopLat,
                                stopLng
                            ).toInt()
                        } else null

                        val previewEtas = stopPreviewEtas[stopData.stop]

                        RouteStopCard(
                            co = currentRoute.co,
                            route = currentRoute.route,
                            seq = stopData.seq,
                            stopNameEn = stopDetail?.nameEn ?: "Stop #${stopData.seq}",
                            stopNameTc = stopDetail?.nameTc ?: "",
                            distanceMeters = dist,
                            previewEtas = previewEtas,
                            onFetchEta = {
                                viewModel.fetchEtaPreviewForStop(
                                    currentRoute.co,
                                    stopData.stop,
                                    currentRoute.route,
                                    currentRoute.serviceType,
                                    stopData.bound ?: selectedBound,
                                    stopData.seq
                                )
                            },
                            onTrackOnIsland = {
                                viewModel.trackStopOnIsland(
                                    co = currentRoute.co,
                                    route = currentRoute.route,
                                    bound = selectedBound,
                                    serviceType = currentRoute.serviceType,
                                    stopId = stopData.stop,
                                    stopNameEn = stopDetail?.nameEn ?: "Stop #${stopData.seq}",
                                    stopNameTc = stopDetail?.nameTc ?: "",
                                    destEn = currentDestEn,
                                    destTc = currentDestTc,
                                    seq = stopData.seq,
                                    stopLat = stopLat,
                                    stopLng = stopLng,
                                    radiusMeters = trackingRadius
                                )
                                onStopTracked()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteItemCard(
    routeItem: KmbRouteData,
    onClick: () -> Unit
) {
    val isMtr = routeItem.co.equals("MTR", ignoreCase = true)
    val isGmb = routeItem.co == "GMB"
    val pillBg = MtrRegistry.getRouteColor(routeItem.co, routeItem.route)
    val pillTextColor = MtrRegistry.getRouteTextColor(routeItem.co, routeItem.route)

    val companyLabel = when (routeItem.co) {
        "MTR" -> "MTR 港鐵"
        "GMB" -> "GMB 小巴"
        "CTB" -> "CTB 城巴"
        "NWFB" -> "NWFB 新巴"
        else -> "KMB 九巴"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, BusSubtleBorder, RoundedCornerShape(20.dp))
            .testTag("route_card_${routeItem.route}_${routeItem.bound}"),
        colors = CardDefaults.cardColors(containerColor = BusDarkSurface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Route Name Box with Follow Route Color
                Box(
                    modifier = Modifier
                        .background(
                            color = pillBg,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = routeItem.route,
                        color = pillTextColor,
                        fontWeight = FontWeight.Black,
                        fontSize = if (isMtr) 14.sp else 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = companyLabel,
                    color = when (routeItem.co) {
                        "MTR" -> pillBg
                        "GMB" -> Color(0xFF00E676)
                        "CTB" -> Color(0xFFFFD600)
                        else -> Color.Gray
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${routeItem.origEn} ➔ ${routeItem.destEn}",
                        color = BusTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${routeItem.origTc} ➔ ${routeItem.destTc}",
                        color = BusTextSecondary,
                        fontSize = 13.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Select Route",
                tint = BusTextSecondary
            )
        }
    }
}

@Composable
private fun RouteStopCard(
    co: String = "KMB",
    route: String = "",
    seq: Int,
    stopNameEn: String,
    stopNameTc: String,
    distanceMeters: Int?,
    previewEtas: List<FormattedEtaItem>?,
    onFetchEta: () -> Unit,
    onTrackOnIsland: () -> Unit
) {
    val isMtr = co.equals("MTR", ignoreCase = true)
    val accentColor = if (isMtr) MtrRegistry.getRouteColor(co, route) else BusLavenderPrimary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BusSubtleBorder, RoundedCornerShape(16.dp))
            .testTag("route_stop_card_$seq"),
        colors = CardDefaults.cardColors(containerColor = BusDarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                // Seq Badge
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(color = BusDarkBackground, shape = CircleShape)
                        .border(1.dp, if (isMtr) accentColor.copy(alpha = 0.5f) else BusSubtleBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$seq",
                        color = if (isMtr) accentColor else BusTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stopNameTc.ifBlank { stopNameEn },
                        color = BusTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stopNameEn,
                        color = BusTextSecondary,
                        fontSize = 13.5.sp
                    )
                    
                    if (distanceMeters != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NearMe,
                                contentDescription = "Distance",
                                tint = accentColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "距離 $distanceMeters m",
                                color = accentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // ETA Preview Row
            if (previewEtas == null) {
                Button(
                    onClick = onFetchEta,
                    colors = ButtonDefaults.buttonColors(containerColor = BusDarkBackground),
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("預覽即時到站時間", color = accentColor, fontSize = 14.sp)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (previewEtas.isEmpty()) {
                            Text("暫無即時班次", color = BusTextSecondary, fontSize = 13.sp)
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                previewEtas.take(2).forEach { eta ->
                                    val timeText = if (eta.minutesLeft <= 0) "即將抵達" else "${eta.minutesLeft}分鐘"
                                    val isScheduled = eta.isScheduled
                                    val color = if (isScheduled) BusTextSecondary else if (isMtr) accentColor else BusLavenderPrimary
                                    Column {
                                        Text(
                                            text = timeText,
                                            color = color,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (eta.remark.isNotBlank()) {
                                            Text(
                                                text = eta.remark,
                                                color = BusTextSecondary,
                                                fontSize = 10.5.sp,
                                                maxLines = 1
                                            )
                                        } else if (isScheduled) {
                                            Text(
                                                text = if (co.equals("GMB", ignoreCase = true)) "未開出" else "原定班次",
                                                color = BusTextSecondary,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onTrackOnIsland,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Track",
                            tint = if (isMtr && !MtrRegistry.findLine(route)?.isLightContent!!) Color.Black else BusLavenderOnPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "追蹤",
                            color = if (isMtr && !(MtrRegistry.findLine(route)?.isLightContent ?: true)) Color.Black else BusLavenderOnPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
