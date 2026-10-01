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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import android.widget.Toast
import com.busetaisland.app.ui.components.FlightTrackingProgressRow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.busetaisland.app.ui.theme.BusCardBorder
import com.busetaisland.app.ui.theme.BusDarkBackground
import com.busetaisland.app.ui.theme.BusDarkSurface
import com.busetaisland.app.ui.theme.BusDarkSurfaceElevated
import com.busetaisland.app.ui.theme.BusDarkSurfaceVariant
import com.busetaisland.app.ui.theme.BusEmeraldGreen
import com.busetaisland.app.ui.theme.BusLavenderOnPrimary
import com.busetaisland.app.ui.theme.BusLavenderPrimary
import com.busetaisland.app.ui.theme.BusSubtleBorder
import com.busetaisland.app.ui.theme.BusTextMuted
import com.busetaisland.app.ui.theme.BusTextPrimary
import com.busetaisland.app.ui.theme.BusTextSecondary
import com.busetaisland.app.ui.viewmodel.BusViewModel

@Composable
fun SettingsScreen(
    viewModel: BusViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val overlayConfig by viewModel.overlayConfig.collectAsState()
    val trackedFlight by viewModel.trackedFlight.collectAsState()
    var flightInputText by remember(overlayConfig.trackedFlightQuery) { mutableStateOf(overlayConfig.trackedFlightQuery) }
    var flightEmailInput by remember(overlayConfig.flightdataEmail) { mutableStateOf(overlayConfig.flightdataEmail) }
    var flightPasswordInput by remember(overlayConfig.flightdataPassword) { mutableStateOf(overlayConfig.flightdataPassword) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoggingIn by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BusDarkBackground)
            .padding(horizontal = 16.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Settings & Overlay",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = BusTextPrimary,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Configure Dynamic Island physics, morphing animation & permissions",
                style = MaterialTheme.typography.bodySmall,
                color = BusTextSecondary
            )
        }

        // Dynamic Island Spatial & Physics Customization Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BusSubtleBorder, RoundedCornerShape(22.dp))
                    .testTag("settings_island_physics_card"),
                colors = CardDefaults.cardColors(containerColor = BusDarkSurface),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(BusDarkSurfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Animation,
                                contentDescription = "Animation & Spatial",
                                tint = BusLavenderPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "動態島展開與物理動畫設定",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = BusTextPrimary
                            )
                            Text(
                                text = "Parametric Morphing & Snap Physics",
                                fontSize = 11.sp,
                                color = BusLavenderPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Auto Expand on Screen On
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "亮屏自動展開靈動島 (Auto Expand on Wake)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = BusTextPrimary
                            )
                            Text(
                                text = if (overlayConfig.autoExpandOnScreenOn) "開啟：點亮螢幕時靈動島自動展開為完整 ETA 膠囊" else "關閉：點亮螢幕時維持當前收合狀態 (不自動展開)",
                                fontSize = 11.5.sp,
                                color = BusTextSecondary
                            )
                        }
                        Switch(
                            checked = overlayConfig.autoExpandOnScreenOn,
                            onCheckedChange = { viewModel.setAutoExpandOnScreenOn(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BusLavenderOnPrimary,
                                checkedTrackColor = BusLavenderPrimary
                            ),
                            modifier = Modifier.testTag("toggle_auto_expand_screen_on")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Auto-Center on Expand
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "展開時自動置中 (Auto Center X)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = BusTextPrimary
                            )
                            Text(
                                text = if (overlayConfig.autoCenterOnExpand) "開啟：展開時平滑移動至螢幕水平正中" else "關閉：展開時維持拖曳時的 X 軸水平位置",
                                fontSize = 11.5.sp,
                                color = BusTextSecondary
                            )
                        }
                        Switch(
                            checked = overlayConfig.autoCenterOnExpand,
                            onCheckedChange = { viewModel.setAutoCenterOnExpand(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BusLavenderOnPrimary,
                                checkedTrackColor = BusLavenderPrimary
                            ),
                            modifier = Modifier.testTag("toggle_auto_center")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Lock Top Position / Snap to Default on Expand
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "展開時靠頂/鎖定位置 (Lock Top Y)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = BusTextPrimary
                            )
                            Text(
                                text = if (overlayConfig.lockTopPositionOnExpand) "開啟：展開時自動吸附至頂部預設靈動島位置" else "關閉：展開時原地向周圍形變展開，不強制跳至頂部",
                                fontSize = 11.5.sp,
                                color = BusTextSecondary
                            )
                        }
                        Switch(
                            checked = overlayConfig.lockTopPositionOnExpand,
                            onCheckedChange = { viewModel.setLockTopPositionOnExpand(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BusLavenderOnPrimary,
                                checkedTrackColor = BusLavenderPrimary
                            ),
                            modifier = Modifier.testTag("toggle_lock_top")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Vertical Offset / Move Area Top Limit
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "頂部垂直偏移 / 拖曳上限 (Top Limit)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = BusTextPrimary
                            )
                            Text(
                                text = "${overlayConfig.posY} px",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = BusLavenderPrimary
                            )
                        }
                        Text(
                            text = "限制圓形與島無法穿出螢幕頂端邊界，並作為展開時預設高度。",
                            fontSize = 11.5.sp,
                            color = BusTextSecondary
                        )
                        Slider(
                            value = overlayConfig.posY.toFloat(),
                            onValueChange = { viewModel.setVerticalOffset(it.toInt()) },
                            valueRange = 0f..160f,
                            steps = 31,
                            colors = SliderDefaults.colors(
                                thumbColor = BusLavenderPrimary,
                                activeTrackColor = BusLavenderPrimary,
                                inactiveTrackColor = BusDarkSurfaceVariant
                            ),
                            modifier = Modifier.testTag("slider_vertical_offset")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Expand Animation Duration
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "展開動畫時間 (Expand Duration)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = BusTextPrimary
                            )
                            Text(
                                text = "${overlayConfig.expandDurationMs} ms",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = BusLavenderPrimary
                            )
                        }
                        Slider(
                            value = overlayConfig.expandDurationMs.toFloat(),
                            onValueChange = {
                                viewModel.setAnimationDurations(
                                    expandMs = it.toInt(),
                                    collapseMs = overlayConfig.collapseDurationMs
                                )
                            },
                            valueRange = 120f..600f,
                            steps = 15,
                            colors = SliderDefaults.colors(
                                thumbColor = BusLavenderPrimary,
                                activeTrackColor = BusLavenderPrimary,
                                inactiveTrackColor = BusDarkSurfaceVariant
                            ),
                            modifier = Modifier.testTag("slider_expand_duration")
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Collapse Animation Duration
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "收合動畫時間 (Collapse Duration)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = BusTextPrimary
                            )
                            Text(
                                text = "${overlayConfig.collapseDurationMs} ms",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = BusLavenderPrimary
                            )
                        }
                        Slider(
                            value = overlayConfig.collapseDurationMs.toFloat(),
                            onValueChange = {
                                viewModel.setAnimationDurations(
                                    expandMs = overlayConfig.expandDurationMs,
                                    collapseMs = it.toInt()
                                )
                            },
                            valueRange = 100f..500f,
                            steps = 15,
                            colors = SliderDefaults.colors(
                                thumbColor = BusLavenderPrimary,
                                activeTrackColor = BusLavenderPrimary,
                                inactiveTrackColor = BusDarkSurfaceVariant
                            ),
                            modifier = Modifier.testTag("slider_collapse_duration")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Collapsed Circle Radius Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "收合時圓形半徑 / 大小 (Circle Radius)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = BusTextPrimary
                            )
                            Text(
                                text = "${overlayConfig.collapsedRadiusDp} dp (直徑 ${overlayConfig.collapsedRadiusDp * 2} dp)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = BusLavenderPrimary
                            )
                        }
                        Text(
                            text = "自訂靈動島收合為圓形浮球時的直徑與大小。",
                            fontSize = 11.5.sp,
                            color = BusTextSecondary
                        )
                        Slider(
                            value = overlayConfig.collapsedRadiusDp.toFloat(),
                            onValueChange = { viewModel.setCollapsedRadiusDp(it.toInt()) },
                            valueRange = 14f..48f,
                            steps = 16,
                            colors = SliderDefaults.colors(
                                thumbColor = BusLavenderPrimary,
                                activeTrackColor = BusLavenderPrimary,
                                inactiveTrackColor = BusDarkSurfaceVariant
                            ),
                            modifier = Modifier.testTag("slider_circle_radius")
                        )
                    }
                }
            }
        }

        // ETA Display Format & Battery Optimization Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BusSubtleBorder, RoundedCornerShape(22.dp))
                    .testTag("settings_eta_format_card"),
                colors = CardDefaults.cardColors(containerColor = BusDarkSurface),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ETA 顯示模式 (分鐘 / 具體時間)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = BusTextPrimary
                            )
                            Text(
                                text = if (overlayConfig.etaUnit == com.busetaisland.app.data.model.EtaDisplayUnit.MINUTES) "目前: 分鐘倒數 (例如 4m / 4分)" else "目前: 具體時間 (例如 15:42)",
                                fontSize = 12.sp,
                                color = BusLavenderPrimary
                            )
                        }

                        Button(
                            onClick = { viewModel.toggleEtaUnit() },
                            shape = RoundedCornerShape(100.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BusLavenderPrimary,
                                contentColor = BusLavenderOnPrimary
                            )
                        ) {
                            Text("切換模式", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BusDarkSurfaceVariant, RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "⚡ 背景智慧省電與更新機制:",
                                color = BusTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "• 熄屏省電：熄屏黑屏時 GPS 降頻至每 5 分鐘檢測一次，極致延長電池續航。",
                                color = BusTextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "• 亮屏喚醒：點亮螢幕時立即同步最新 GPS 及 30 秒 ETA，並自動展開動態島。",
                                color = BusTextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "• 手動更新：在動態島上長按即可隨時手動刷新即時 GPS 與到站時間。",
                                color = BusTextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "• 班次狀態：若為原定班次 (rmk_tc) 路線文字自動呈現灰色，即時班次則呈現高亮。",
                                color = BusTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Upcoming Bus Horizontal Timeline Card (Default OFF)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x44D0BCFF), RoundedCornerShape(22.dp))
                    .testTag("settings_bus_timeline_card"),
                colors = CardDefaults.cardColors(containerColor = BusDarkSurface),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0x22D0BCFF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = "Bus Timeline",
                                    tint = BusLavenderPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "動態島橫向到站時間軸 (Timeline)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BusTextPrimary,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "於動態島所有路線下方顯示 0 - 30 分鐘橫向視覺化時間軸，一眼掌握各班次間隔與進站順序",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BusTextSecondary,
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        Switch(
                            checked = overlayConfig.showTimelineBar,
                            onCheckedChange = { viewModel.setTimelineBarVisible(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BusLavenderPrimary,
                                checkedTrackColor = Color(0x33D0BCFF),
                                uncheckedThumbColor = BusTextMuted,
                                uncheckedTrackColor = BusDarkSurfaceVariant
                            ),
                            modifier = Modifier.testTag("switch_bus_timeline")
                        )
                    }

                    if (overlayConfig.showTimelineBar) {
                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "時間軸時間視窗上限 (用戶自訂):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BusTextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(15, 30, 45, 60).forEach { window ->
                                val isSelected = overlayConfig.timelineWindowMinutes == window
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setTimelineWindowMinutes(window) },
                                    label = {
                                        Text(
                                            text = "${window}分鐘",
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BusLavenderPrimary,
                                        selectedLabelColor = BusLavenderOnPrimary,
                                        containerColor = BusDarkSurfaceVariant,
                                        labelColor = BusTextSecondary
                                    ),
                                    modifier = Modifier.weight(1f).testTag("chip_timeline_window_${window}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BusDarkSurfaceVariant, RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "💡 點狀時間軸 (Point Timeline) 特色:",
                                    color = BusTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "• 極簡圓點標記：將各班次呈現為軌道上的極簡圓點 (不佔用字體空間，零崩潰)。",
                                    color = BusTextSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "• 3分鐘內進站高亮：即將到達之班次自動呈現綠色發光護目光環。",
                                    color = BusTextSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "• 橫向時間視窗：自訂 15 至 60 分鐘座標軸上限，秒懂班次進站間隔。",
                                    color = BusTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Flight Tracking Configuration Card (Optional feature based on pyflightdata / FlightRadar24)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x4438BDF8), RoundedCornerShape(22.dp))
                    .testTag("settings_flight_tracking_card"),
                colors = CardDefaults.cardColors(containerColor = BusDarkSurface),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0x2238BDF8), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AirplanemodeActive,
                                    contentDescription = "Flight Tracking",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Chaquopy Python 航班追蹤 (pyflightdata)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BusTextPrimary,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "原生運行 pyflightdata Python 套件，於靈動島顯示航班進度、STD/ATD、STA/ETA及機身號",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BusTextSecondary,
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        Switch(
                            checked = overlayConfig.isFlightTrackingEnabled,
                            onCheckedChange = { viewModel.setFlightTrackingEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF38BDF8),
                                checkedTrackColor = Color(0x3338BDF8),
                                uncheckedThumbColor = BusTextMuted,
                                uncheckedTrackColor = BusDarkSurfaceVariant
                            ),
                            modifier = Modifier.testTag("switch_flight_tracking")
                        )
                    }

                    if (overlayConfig.isFlightTrackingEnabled) {
                        Spacer(modifier = Modifier.height(14.dp))

                        // pyflightdata Account Authentication (FlightData(email=None, password=None))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A), RoundedCornerShape(16.dp))
                                .border(1.dp, Color(0x4438BDF8), RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Key,
                                            contentDescription = "pyflightdata auth",
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "pyflightdata 帳號登入 (FlightData)",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF38BDF8)
                                        )
                                    }

                                    // Status tag
                                    val isConnected = overlayConfig.flightdataAuthToken.isNotBlank() || overlayConfig.flightdataLoginStatus.contains("Connected") || overlayConfig.flightdataLoginStatus.contains("已成功") || overlayConfig.flightdataLoginStatus.contains("Ready")
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isConnected) Color(0x3310B981) else Color(0x3364748B))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = overlayConfig.flightdataLoginStatus,
                                            color = if (isConnected) Color(0xFF34D399) else Color(0xFF94A3B8),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Text(
                                    text = "支援 pyflightdata 帳號驗證 class FlightData(email=None, password=None)。如無帳號可留空以訪客模式使用。",
                                    fontSize = 11.sp,
                                    color = BusTextSecondary
                                )

                                OutlinedTextField(
                                    value = flightEmailInput,
                                    onValueChange = {
                                        flightEmailInput = it
                                        viewModel.updateFlightdataCredentials(it, flightPasswordInput)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_flightdata_email"),
                                    placeholder = { Text("帳號電郵 (Email, 可選)", fontSize = 12.sp, color = BusTextMuted) },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Default.Email, contentDescription = "Email", tint = BusTextMuted, modifier = Modifier.size(16.dp))
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF38BDF8),
                                        unfocusedBorderColor = Color(0x3338BDF8),
                                        focusedContainerColor = Color(0xFF1E293B),
                                        unfocusedContainerColor = Color(0xFF1E293B),
                                        focusedTextColor = Color(0xFFFFFFFF),
                                        unfocusedTextColor = Color(0xFFFFFFFF)
                                    )
                                )

                                OutlinedTextField(
                                    value = flightPasswordInput,
                                    onValueChange = {
                                        flightPasswordInput = it
                                        viewModel.updateFlightdataCredentials(flightEmailInput, it)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_flightdata_password"),
                                    placeholder = { Text("密碼 (Password, 可選)", fontSize = 12.sp, color = BusTextMuted) },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Password", tint = BusTextMuted, modifier = Modifier.size(16.dp))
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                            Icon(
                                                imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = "Toggle password",
                                                tint = BusTextMuted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    },
                                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF38BDF8),
                                        unfocusedBorderColor = Color(0x3338BDF8),
                                        focusedContainerColor = Color(0xFF1E293B),
                                        unfocusedContainerColor = Color(0xFF1E293B),
                                        focusedTextColor = Color(0xFFFFFFFF),
                                        unfocusedTextColor = Color(0xFFFFFFFF)
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (flightEmailInput.isNotBlank() && flightPasswordInput.isNotBlank()) {
                                                isLoggingIn = true
                                                viewModel.loginFlightdata(flightEmailInput, flightPasswordInput) { success, msg ->
                                                    isLoggingIn = false
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                Toast.makeText(context, "請輸入帳號與密碼", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("btn_flightdata_login"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF0284C7),
                                            contentColor = Color.White
                                        ),
                                        enabled = !isLoggingIn
                                    ) {
                                        Text(if (isLoggingIn) "驗證中..." else "登入 / 驗證 pyflightdata", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }

                                    if (flightEmailInput.isNotBlank() || flightPasswordInput.isNotBlank()) {
                                        FilledTonalButton(
                                            onClick = {
                                                flightEmailInput = ""
                                                flightPasswordInput = ""
                                                viewModel.logoutFlightdata()
                                                Toast.makeText(context, "已清除帳號 (訪客模式)", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = Color(0xFF334155),
                                                contentColor = Color(0xFFE2E8F0)
                                            ),
                                            modifier = Modifier.testTag("btn_flightdata_logout")
                                        ) {
                                            Text("清除", fontSize = 11.5.sp)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Flight input & search row
                        Text(
                            text = "✈️ 輸入機身註冊編號 (Aircraft Registration Only):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = BusTextPrimary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x2238BDF8), RoundedCornerShape(8.dp))
                                .border(0.5.dp, Color(0x4438BDF8), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "💡 提示：pyflightdata 實時雷達需要輸入【機身註冊編號 (Registration)】以取得精確起降機場 (IATA 代碼)、即時飛行進度及 ETA/STA 時間。請勿單獨輸入航班號，以確保資料準確。",
                                fontSize = 10.5.sp,
                                color = Color(0xFFBAE6FD),
                                lineHeight = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = flightInputText,
                                onValueChange = { flightInputText = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_flight_query"),
                                placeholder = { Text("例: B-LRA, B-LNJ, B-HNX, B-KPP", fontSize = 11.5.sp, color = BusTextMuted) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0x3338BDF8),
                                    focusedContainerColor = Color(0xFF131C2E),
                                    unfocusedContainerColor = Color(0xFF131C2E),
                                    focusedTextColor = Color(0xFFFFFFFF),
                                    unfocusedTextColor = Color(0xFFFFFFFF)
                                )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    if (flightInputText.isNotBlank()) {
                                        viewModel.setTrackedFlightQuery(flightInputText.trim().uppercase())
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF38BDF8),
                                    contentColor = Color(0xFF0F172A)
                                ),
                                modifier = Modifier.testTag("apply_flight_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("套用", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Preset quick chips (Aircraft Registrations)
                        Text(
                            text = "快速選擇熱門機身註冊編號 (Registration):",
                            fontSize = 11.sp,
                            color = BusTextSecondary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        val presetFlights = listOf(
                            "B-LRA" to "國泰 A350-900",
                            "B-LNJ" to "快運 A321neo",
                            "B-HNX" to "國泰 B777-300",
                            "B-KPP" to "國泰 B777-300ER",
                            "B-HPB" to "港航 A330-300",
                            "B-LQC" to "快運 A320neo"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            presetFlights.take(3).forEach { (code, label) ->
                                val isSelected = overlayConfig.trackedFlightQuery.equals(code, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E293B))
                                        .border(0.5.dp, if (isSelected) Color(0xFF38BDF8) else Color(0x3338BDF8), RoundedCornerShape(8.dp))
                                        .clickable {
                                            flightInputText = code
                                            viewModel.setTrackedFlightQuery(code)
                                        }
                                        .padding(vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = code,
                                        color = if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            presetFlights.drop(3).forEach { (code, label) ->
                                val isSelected = overlayConfig.trackedFlightQuery.equals(code, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E293B))
                                        .border(0.5.dp, if (isSelected) Color(0xFF38BDF8) else Color(0x3338BDF8), RoundedCornerShape(8.dp))
                                        .clickable {
                                            flightInputText = code
                                            viewModel.setTrackedFlightQuery(code)
                                        }
                                        .padding(vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = code,
                                        color = if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Real-time Preview in Settings
                        if (trackedFlight != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "即時預覽 (Live Preview in Island):",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            FlightTrackingProgressRow(flight = trackedFlight!!)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Info box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                                .border(0.5.dp, Color(0x3338BDF8), RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "✈️ Chaquopy & pyflightdata 運行規格:",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "• Python 運行環境：透過 Chaquopy 17.0 於 Android 原生運行 Python 3.10 直譯器與 pyflightdata 套件。",
                                    color = BusTextSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "• 核心函數：支援 pyflightdata.FlightData(email, password)、get_flight_data(flight) 及 get_flight_for_aircraft(reg)。",
                                    color = BusTextSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "• 數據指標：即時計算並顯示 STD (預定起飛)、ATD (實際/估計起飛)、STA (預定降落)、ETA (估計降落)、呼號、機身註冊號及動態航程進度條。",
                                    color = BusTextSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "• 自動輪詢：啟用時每 30 秒自動更新航程進度與實際起降時間。",
                                    color = BusTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Rain Nowcast Radar Map Configuration Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(22.dp))
                    .testTag("settings_nowcast_card"),
                colors = CardDefaults.cardColors(containerColor = BusDarkSurface),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0x2200E5FF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = "Rain Nowcast",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "降雨臨近預報地圖 (Rain Nowcast)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = BusTextPrimary
                            )
                            Text(
                                text = "HKO Gridded Rainfall Nowcast & Radar",
                                fontSize = 11.sp,
                                color = Color(0xFF00E5FF)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // GPS Center Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "地圖以 GPS 定位置中 (Center with GPS)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = BusTextPrimary
                                )
                            }
                            Text(
                                text = if (overlayConfig.nowcastCenterGps) "開啟：以即時 GPS 座標為雷達中心顯示週邊雨量" else "關閉：以全香港區域全景為中心顯示",
                                fontSize = 11.5.sp,
                                color = BusTextSecondary
                            )
                        }
                        Switch(
                            checked = overlayConfig.nowcastCenterGps,
                            onCheckedChange = { viewModel.setNowcastCenterGps(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF0F172A),
                                checkedTrackColor = Color(0xFF00E5FF)
                            ),
                            modifier = Modifier.testTag("toggle_nowcast_gps_center")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Animation Cycle Duration Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "單次循環動畫時長 (Loop Duration)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = BusTextPrimary
                            )
                            Text(
                                text = String.format(java.util.Locale.US, "%.1f 秒 / 循環", overlayConfig.nowcastCycleDurationSec),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF00E5FF)
                            )
                        }
                        Text(
                            text = "調整雷達雲圖從第 1 幀播至最後 1 幀的循環速度。",
                            fontSize = 11.5.sp,
                            color = BusTextSecondary
                        )
                        Slider(
                            value = overlayConfig.nowcastCycleDurationSec,
                            onValueChange = { viewModel.setNowcastCycleDurationSec(it) },
                            valueRange = 1.0f..8.0f,
                            steps = 13,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00E5FF),
                                activeTrackColor = Color(0xFF00E5FF),
                                inactiveTrackColor = BusDarkSurfaceVariant
                            ),
                            modifier = Modifier.testTag("slider_nowcast_cycle_duration")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Auto-Update Cooldown & Custom Refresh Setting
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "定時自動更新 / 僅手動模式",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = BusTextPrimary
                            )
                            Text(
                                text = if (overlayConfig.nowcastCooldownMinutes == 0) "僅手動更新 (0分)" else "${overlayConfig.nowcastCooldownMinutes} 分鐘/次",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (overlayConfig.nowcastCooldownMinutes == 0) BusTextMuted else Color(0xFF00E5FF)
                            )
                        }
                        Text(
                            text = "可選擇預設時間、自訂冷卻分鐘數，或設為「0分 (僅手動更新)」。",
                            fontSize = 11.5.sp,
                            color = BusTextSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick Preset Options
                        val cooldownOptions = listOf(0, 1, 3, 5, 10, 15, 30, 60)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            cooldownOptions.forEach { mins ->
                                val isSelected = overlayConfig.nowcastCooldownMinutes == mins
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) Color(0xFF00E5FF) else BusDarkSurfaceVariant
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) Color(0xFF00E5FF) else Color(0x22FFFFFF),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { viewModel.setNowcastCooldownMinutes(mins) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (mins == 0) "手動" else "${mins}分",
                                        color = if (isSelected) Color(0xFF0F172A) else BusTextSecondary,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Custom Minute Stepper Adjustment
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                                .border(0.5.dp, Color(0x3338BDF8), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "自訂冷卻分鐘數 (Custom Interval)",
                                fontSize = 12.sp,
                                color = BusTextSecondary,
                                fontWeight = FontWeight.Medium
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(BusDarkSurfaceVariant)
                                        .clickable {
                                            viewModel.setNowcastCooldownMinutes(
                                                (overlayConfig.nowcastCooldownMinutes - 1).coerceAtLeast(0)
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("-", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }

                                Text(
                                    text = "${overlayConfig.nowcastCooldownMinutes} 分",
                                    color = Color(0xFF00E5FF),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )

                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(BusDarkSurfaceVariant)
                                        .clickable {
                                            viewModel.setNowcastCooldownMinutes(
                                                (overlayConfig.nowcastCooldownMinutes + 1).coerceAtMost(180)
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("+", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // API Key & Refresh Info Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                            .border(0.5.dp, Color(0x3338BDF8), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "ℹ️ 降雨預報地圖說明與規則:",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "• 僅手動模式 (0 分鐘)：後台不會自動抓取雷達數據，完全由手動觸發。",
                                color = BusTextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "• 靈動島獨立手動刷新：長按靈動島降雨地圖 1 秒，即可單獨刷新雷達數據，不刷新整體巴士 ETA。",
                                color = BusTextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "• 自訂更新時間：可透過按鈕 +/- 或快選晶片設定任意分鐘 (1-180分鐘) 自訂自動更新間隔。",
                                color = BusTextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "• OpenStreetMap 底圖與方形網格：雷達雨網格以高解析像素方塊精準對應天文台坐標。",
                                color = BusTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.refreshNowcastData() },
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E5FF),
                            contentColor = Color(0xFF0F172A)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("manual_nowcast_refresh_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("立即手動更新臨近預報數據", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Accessibility Service Info & Action Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BusSubtleBorder, RoundedCornerShape(22.dp))
                    .testTag("settings_accessibility_card"),
                colors = CardDefaults.cardColors(containerColor = BusDarkSurface),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(BusDarkSurfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Accessibility,
                                contentDescription = "Accessibility",
                                tint = BusLavenderPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "無障礙服務 (Accessibility Service)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = BusTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "僅需啟用無障礙服務即可顯示動態島懸浮窗與鎖屏顯示，無需授予額外的應用層疊權限，避免雙圈衝突。",
                        fontSize = 13.sp,
                        color = BusTextSecondary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
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
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BusLavenderPrimary,
                            contentColor = BusLavenderOnPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("open_accessibility_btn")
                    ) {
                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("前往無障礙設定開啟服務", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Lockscreen Overlay Guide
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BusSubtleBorder, RoundedCornerShape(22.dp))
                    .testTag("settings_lockscreen_card"),
                colors = CardDefaults.cardColors(containerColor = BusDarkSurface),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(BusDarkSurfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lockscreen",
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Lockscreen Visibility",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = BusTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "The overlay is configured with FLAG_SHOW_WHEN_LOCKED to remain visible on the lock screen as you walk toward your bus stop.",
                        fontSize = 13.sp,
                        color = BusTextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // KMB Open Data API Info
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BusSubtleBorder, RoundedCornerShape(22.dp))
                    .testTag("settings_api_card"),
                colors = CardDefaults.cardColors(containerColor = BusDarkSurface),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "API Info",
                            tint = BusTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Kowloon Motor Bus (KMB) Open API v1.02",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = BusTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Real-time ETA data provided by The Kowloon Motor Bus Company (1933) Limited via DATA.GOV.HK Open Data API.",
                        fontSize = 12.sp,
                        color = BusTextMuted
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

