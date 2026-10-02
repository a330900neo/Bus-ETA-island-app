package com.busetaisland.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.busetaisland.app.ui.screens.HomeScreen
import com.busetaisland.app.ui.screens.PinnedStopsScreen
import com.busetaisland.app.ui.screens.RouteSearchScreen
import com.busetaisland.app.ui.screens.SettingsScreen
import com.busetaisland.app.ui.theme.BusDarkBackground
import com.busetaisland.app.ui.theme.BusDarkSurfaceElevated
import com.busetaisland.app.ui.theme.BusEtaTheme
import com.busetaisland.app.ui.theme.BusLavenderOnPrimary
import com.busetaisland.app.ui.theme.BusLavenderPrimary
import com.busetaisland.app.ui.theme.BusTextMuted
import com.busetaisland.app.ui.viewmodel.BusViewModel

enum class MainNavTab(val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector, val tag: String) {
    HOME("Island ETA", Icons.Default.DirectionsBus, Icons.Outlined.DirectionsBus, "nav_tab_home"),
    SEARCH("Routes", Icons.Default.Search, Icons.Outlined.Search, "nav_tab_search"),
    PINNED("Saved", Icons.Default.Bookmark, Icons.Outlined.BookmarkBorder, "nav_tab_pinned"),
    SETTINGS("Settings", Icons.Default.Settings, Icons.Outlined.Settings, "nav_tab_settings")
}

class MainActivity : ComponentActivity() {

    private val viewModel: BusViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (com.busetaisland.app.service.OverlayStateHolder.config.value.isPaused) {
            com.busetaisland.app.service.OverlayStateHolder.resumeTrackingAndOverlay(this)
        }

        setContent {
            val overlayConfig by viewModel.overlayConfig.collectAsState()
            val isSystemDark = isSystemInDarkTheme()
            val isDarkTheme = when (overlayConfig.appTheme) {
                "light" -> false
                "dark" -> true
                "system" -> isSystemDark
                else -> true
            }

            LaunchedEffect(isDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    }
                )
            }

            val customBusColors = if (overlayConfig.appTheme == "custom") {
                val bg = Color(overlayConfig.islandCustomBgColorHex)
                val text = Color(overlayConfig.islandCustomTextColorHex)
                val secText = Color(overlayConfig.islandCustomSecondaryTextColorHex)
                val border = Color(overlayConfig.islandCustomBorderColorHex)
                val accent = Color(overlayConfig.islandCustomAccentColorHex)
                com.busetaisland.app.ui.theme.BusColors(
                    background = com.busetaisland.app.ui.theme.DarkBusColors.background,
                    surface = com.busetaisland.app.ui.theme.DarkBusColors.surface,
                    surfaceElevated = com.busetaisland.app.ui.theme.DarkBusColors.surfaceElevated,
                    surfaceVariant = bg.copy(alpha = 0.35f),
                    textPrimary = text,
                    textSecondary = secText,
                    textMuted = secText,
                    cardBorder = border,
                    subtleBorder = border,
                    primary = accent,
                    onPrimary = bg,
                    primaryContainer = accent.copy(alpha = 0.25f),
                    onPrimaryContainer = accent,
                    emeraldGreen = Color(0xFF86F8B6),
                    emeraldContainer = Color(0xFF005234),
                    roseAlert = Color(0xFFFFB4AB),
                    roseContainer = Color(0xFF690005),
                    amberWarning = Color(0xFFFFD56B),
                    islandBlack = bg
                )
            } else null

            BusEtaTheme(
                darkTheme = isDarkTheme,
                appTheme = overlayConfig.appTheme,
                customColors = customBusColors
            ) {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (com.busetaisland.app.service.OverlayStateHolder.config.value.isPaused) {
            com.busetaisland.app.service.OverlayStateHolder.resumeTrackingAndOverlay(this)
        }
    }
}

@Composable
fun MainAppScreen(viewModel: BusViewModel) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    // Request runtime location and notification permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle result
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(BusDarkBackground),
        containerColor = BusDarkBackground,
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .border(
                        width = 1.dp,
                        color = Color(0x1AFFFFFF)
                    )
                    .testTag("main_bottom_nav_bar"),
                containerColor = BusDarkSurfaceElevated,
                contentColor = BusLavenderPrimary
            ) {
                MainNavTab.values().forEachIndexed { index, tab ->
                    val isSelected = selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BusLavenderOnPrimary,
                            selectedTextColor = BusLavenderPrimary,
                            indicatorColor = BusLavenderPrimary,
                            unselectedIconColor = BusTextMuted,
                            unselectedTextColor = BusTextMuted
                        ),
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToSearch = { selectedTab = 1 },
                    onNavigateToPinned = { selectedTab = 2 }
                )
                1 -> RouteSearchScreen(
                    viewModel = viewModel,
                    onStopTracked = { selectedTab = 0 }
                )
                2 -> PinnedStopsScreen(
                    viewModel = viewModel,
                    onNavigateToSearch = { selectedTab = 1 }
                )
                3 -> SettingsScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}
