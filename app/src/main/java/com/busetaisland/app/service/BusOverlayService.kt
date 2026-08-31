package com.busetaisland.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.busetaisland.app.BusApp
import com.busetaisland.app.MainActivity
import com.busetaisland.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import android.content.BroadcastReceiver
import android.content.IntentFilter
import kotlinx.coroutines.delay

class BusOverlayService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val repository = (application as? BusApp)?.repository ?: return
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    // Screen turned off (blackscreen): throttle GPS to 5 minutes to optimize battery
                    repository.locationTracker.setScreenState(isScreenOn = false)
                }
                Intent.ACTION_SCREEN_ON -> {
                    // Screen lit up: restore active GPS tracking, trigger immediate ETA/GPS refresh, auto-expand island
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

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Bus ETA Island Active", "Live Bus ETA tracking on screen"))

        OverlayStateHolder.updateConfig { it.copy(isServiceRunning = true) }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenStateReceiver, filter)

        val repository = (application as BusApp).repository
        repository.locationTracker.startTracking(isScreenOff = false)

        serviceScope.launch {
            repository.allTrackedBusesState.collectLatest { buses ->
                OverlayStateHolder.updateAllTrackedBuses(buses)
                val inRangeBuses = buses.filter { !it.isGeofenceEnabled || it.isInRange }
                val active = inRangeBuses.firstOrNull() ?: OverlayStateHolder.getActiveBus() ?: buses.firstOrNull()
                if (active != null) {
                    if (inRangeBuses.isEmpty() && buses.any { it.isGeofenceEnabled && !it.isInRange }) {
                        updateNotification("KMB Island", "已超出地理圍欄範圍 (待機中)")
                    } else {
                        val summary = active.toOneLineCompact(OverlayStateHolder.config.value.etaUnit)
                        updateNotification(active.route, summary)
                    }
                }
            }
        }

        // Listen for manual refresh requests from Island hold gesture
        serviceScope.launch {
            OverlayStateHolder.refreshRequests.collectLatest {
                OverlayStateHolder.setRefreshing(true)
                repository.triggerImmediateRefresh()
                delay(800L)
                OverlayStateHolder.setRefreshing(false)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_OVERLAY -> {
                OverlayStateHolder.updateConfig { it.copy(isOverlayEnabled = !it.isOverlayEnabled) }
            }
            ACTION_TOGGLE_COLLAPSE -> {
                OverlayStateHolder.toggleCollapsed()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (e: Exception) {
            // Ignore
        }
        OverlayStateHolder.updateConfig { it.copy(isServiceRunning = false) }
        val repository = (application as BusApp).repository
        repository.locationTracker.stopTracking()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bus ETA Live Island",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active bus ETA tracking island"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val toggleIntent = Intent(this, BusOverlayService::class.java).apply {
            action = ACTION_TOGGLE_COLLAPSE
        }
        val pendingToggle = PendingIntent.getService(
            this, 1, toggleIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_rotate, "Toggle Island Size", pendingToggle)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(route: String, content: String) {
        val notification = buildNotification("Bus ETA: $route", content)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "bus_eta_overlay_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_SERVICE = "com.busetaisland.app.ACTION_STOP_SERVICE"
        const val ACTION_TOGGLE_OVERLAY = "com.busetaisland.app.ACTION_TOGGLE_OVERLAY"
        const val ACTION_TOGGLE_COLLAPSE = "com.busetaisland.app.ACTION_TOGGLE_COLLAPSE"

        fun start(context: Context) {
            val intent = Intent(context, BusOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, BusOverlayService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}
