package com.busetaisland.app.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.busetaisland.app.ui.overlay.DynamicIslandOverlayContent
import com.busetaisland.app.ui.theme.BusEtaTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OverlayWindowManager(private val context: Context, private val isAccessibility: Boolean = false) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    private var isDragging = false
    private var posJob: Job? = null
    private var animJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Saved position when collapsed
    private var collapsedX = 0
    private var collapsedY = 24

    private fun getDisplayMetrics() = context.resources.displayMetrics

    fun showOverlay() {
        if (overlayView != null) return

        try {
            val owner = OverlayLifecycleOwner()
            owner.performRestore(null)
            owner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
            owner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            lifecycleOwner = owner

            val type = if (isAccessibility) {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            }

            val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED

            val initialConfig = OverlayStateHolder.config.value
            collapsedX = initialConfig.circlePosX
            collapsedY = initialConfig.circlePosY

            val isInitialCollapsed = initialConfig.isCollapsed
            val startX = if (isInitialCollapsed) {
                collapsedX
            } else {
                if (initialConfig.autoCenterOnExpand) 0 else collapsedX
            }
            val startY = if (isInitialCollapsed) {
                collapsedY
            } else {
                if (initialConfig.lockTopPositionOnExpand) initialConfig.topVerticalOffset else collapsedY
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                flags,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                x = startX
                y = startY
            }
            layoutParams = params

            val composeView = ComposeView(context).apply {
                setViewTreeLifecycleOwner(owner)
                setViewTreeViewModelStoreOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)

                setContent {
                    BusEtaTheme(darkTheme = true) {
                        DynamicIslandOverlayContent(
                            onExpandToggle = {
                                OverlayStateHolder.toggleCollapsed()
                            },
                            onDrag = { dx, dy ->
                                isDragging = true
                                moveBy(dx, dy)
                            },
                            onDragEnd = {
                                isDragging = false
                                onDragFinished()
                            },
                            onClose = {
                                hideOverlay()
                                OverlayStateHolder.setOverlayEnabled(false)
                            }
                        )
                    }
                }
            }

            overlayView = composeView
            windowManager.addView(composeView, params)
            OverlayStateHolder.updateConfig { it.copy(isOverlayEnabled = true, isWindowAtTop = !isInitialCollapsed) }

            // Observe collapsed state and offset changes to smoothly animate WindowManager in sync with morph
            posJob?.cancel()
            posJob = scope.launch {
                var lastCollapsed = OverlayStateHolder.config.value.isCollapsed
                var lastVerticalOffset = OverlayStateHolder.config.value.topVerticalOffset
                OverlayStateHolder.config.collect { conf ->
                    if (!isDragging) {
                        if (conf.isCollapsed != lastCollapsed) {
                            lastCollapsed = conf.isCollapsed
                            if (!conf.isCollapsed) {
                                // Opening Island: Smoothly animate from circle position to target
                                val metrics = getDisplayMetrics()
                                val density = metrics.density
                                val screenWidth = metrics.widthPixels
                                val islandHalfWidthPx = (336f / 2f) * density
                                val maxAllowedIslandX = ((screenWidth / 2f) - islandHalfWidthPx - (8f * density)).coerceAtLeast(0f)

                                val targetX = if (conf.autoCenterOnExpand) {
                                    0
                                } else {
                                    // Scale away from the edge side so the island never clips the screen wall
                                    conf.circlePosX.toFloat().coerceIn(-maxAllowedIslandX, maxAllowedIslandX).toInt()
                                }
                                val targetY = if (conf.lockTopPositionOnExpand) conf.topVerticalOffset else conf.circlePosY
                                animateTo(targetX, targetY, conf.expandDurationMs.toLong())
                            } else {
                                // Collapsing Island: Smoothly animate back to custom circle position
                                animateTo(conf.circlePosX, conf.circlePosY, conf.collapseDurationMs.toLong())
                            }
                        } else if (conf.topVerticalOffset != lastVerticalOffset) {
                            lastVerticalOffset = conf.topVerticalOffset
                            if (!conf.isCollapsed && conf.lockTopPositionOnExpand) {
                                animateTo(0, conf.topVerticalOffset, 150L)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                overlayView?.let { windowManager.removeView(it) }
            } catch (ignored: Exception) {}
            overlayView = null
            try {
                lifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            } catch (ignored: Exception) {}
            lifecycleOwner = null
            posJob?.cancel()
            OverlayStateHolder.updateConfig { it.copy(isOverlayEnabled = false) }
        }
    }

    private var animator: android.animation.ValueAnimator? = null

    private fun animateTo(targetX: Int, targetY: Int, durationMs: Long) {
        val p = layoutParams ?: return
        val v = overlayView ?: return
        animator?.cancel()

        val startX = p.x.toFloat()
        val startY = p.y.toFloat()
        val endX = targetX.toFloat()
        val endY = targetY.toFloat()

        if (startX == endX && startY == endY) return

        val duration = durationMs.coerceIn(80L, 1000L)
        val interpolator = androidx.core.view.animation.PathInterpolatorCompat.create(0.4f, 0.0f, 0.2f, 1.0f)

        animator = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            this.interpolator = interpolator
            addUpdateListener { anim ->
                val f = anim.animatedFraction

                // Direct continuous trajectory without out-of-screen clamping during animation to eliminate jitter
                val curX = startX + (endX - startX) * f
                val curY = startY + (endY - startY) * f

                p.x = curX.toInt()
                p.y = curY.toInt()

                try {
                    windowManager.updateViewLayout(v, p)
                } catch (e: Exception) {
                    // Ignore transient layout updates
                }
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    p.x = targetX
                    p.y = targetY.coerceAtLeast(0)
                    try {
                        windowManager.updateViewLayout(v, p)
                    } catch (e: Exception) {}
                }
            })
            start()
        }
    }

    private fun setWindowPosition(targetX: Int, targetY: Int) {
        val p = layoutParams ?: return
        val v = overlayView ?: return
        if (p.x == targetX && p.y == targetY) return

        p.x = targetX
        p.y = targetY.coerceAtLeast(0)
        try {
            windowManager.updateViewLayout(v, p)
        } catch (e: Exception) {
            // Ignore layout errors during rapid movement
        }
    }

    fun moveBy(dx: Float, dy: Float) {
        val p = layoutParams ?: return
        val metrics = getDisplayMetrics()
        val density = metrics.density
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels

        // Dynamic boundaries: circle width depends on collapsedRadiusDp, stay safely inside screen
        val radiusDp = OverlayStateHolder.config.value.collapsedRadiusDp.toFloat()
        val maxOffsetX = ((screenWidth / 2f) - (radiusDp * density)).toInt().coerceAtLeast(0)
        val topLimit = OverlayStateHolder.config.value.topVerticalOffset.coerceAtLeast(0)
        val bottomLimit = (screenHeight - ((radiusDp * 2 + 30f) * density)).toInt().coerceAtLeast(topLimit)

        p.x = (p.x + dx.toInt()).coerceIn(-maxOffsetX, maxOffsetX)
        p.y = (p.y + dy.toInt()).coerceIn(topLimit, bottomLimit)

        try {
            overlayView?.let { windowManager.updateViewLayout(it, p) }
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun onDragFinished() {
        val p = layoutParams ?: return
        collapsedX = p.x
        collapsedY = p.y
        OverlayStateHolder.updateCirclePosition(p.x, p.y)
    }

    fun hideOverlay() {
        posJob?.cancel()
        posJob = null
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                // View might already be removed
            }
            overlayView = null
        }
        lifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        lifecycleOwner = null
    }

    private class OverlayLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateRegistryController = SavedStateRegistryController.create(this)
        private val store = ViewModelStore()

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
        override val viewModelStore: ViewModelStore get() = store

        fun performRestore(savedState: Bundle?) {
            savedStateRegistryController.performRestore(savedState)
        }

        fun handleLifecycleEvent(event: Lifecycle.Event) {
            lifecycleRegistry.handleLifecycleEvent(event)
        }
    }
}
