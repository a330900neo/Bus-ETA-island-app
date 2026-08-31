package com.busetaisland.app.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import com.busetaisland.app.BusApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BusAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var overlayManager: OverlayWindowManager? = null

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val repository = (application as? BusApp)?.repository ?: return
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    repository.locationTracker.setScreenState(isScreenOn = false)
                }
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                    repository.locationTracker.setScreenState(isScreenOn = true)
                    serviceScope.launch {
                        repository.triggerImmediateRefresh()
                    }
                    if (OverlayStateHolder.config.value.autoExpandOnScreenOn) {
                        OverlayStateHolder.setCollapsed(false)
                    }
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        OverlayStateHolder.updateConfig { it.copy(isAccessibilityEnabled = true) }
        overlayManager = OverlayWindowManager(this, isAccessibility = true)

        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            registerReceiver(screenStateReceiver, filter)
        } catch (e: Exception) {
            // Ignore
        }

        val repository = (application as BusApp).repository

        serviceScope.launch {
            repository.allTrackedBusesState.collectLatest { buses ->
                OverlayStateHolder.updateAllTrackedBuses(buses)
            }
        }

        serviceScope.launch {
            OverlayStateHolder.config.collectLatest { config ->
                if (config.isOverlayEnabled) {
                    overlayManager?.showOverlay()
                } else {
                    overlayManager?.hideOverlay()
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Can monitor window state or lock screen appearance if needed
    }

    override fun onInterrupt() {
        overlayManager?.hideOverlay()
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (e: Exception) {
            // Ignore
        }
        OverlayStateHolder.updateConfig { it.copy(isAccessibilityEnabled = false) }
        overlayManager?.hideOverlay()
        overlayManager = null
        serviceScope.cancel()
    }

    companion object {
        var instance: BusAccessibilityService? = null
            private set
    }
}
