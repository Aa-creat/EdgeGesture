package com.omarea.gesture.ui.gesture

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.omarea.gesture.AccessibilityServiceGesture
import com.omarea.gesture.core.config.AppConfigRepository
import com.omarea.gesture.core.config.SideGestureConfig
import com.omarea.gesture.core.dispatcher.ActionDispatcher
import com.omarea.gesture.core.haptics.HapticsManager
import com.omarea.gesture.core.model.Action
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.abs

/**
 * 现代两侧边缘手势控制器
 * 负责屏幕左侧、右侧触控条挂载、手势捕获（内滑返回、内滑悬停切换应用）、触感反馈及 Android 10+ 手势排除区域适配
 */
class ModernSideGestureBar(
    private val service: AccessibilityServiceGesture,
    private val configRepository: AppConfigRepository,
    private val actionDispatcher: ActionDispatcher,
    private val hapticsManager: HapticsManager
) {
    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val scope = CoroutineScope(Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())

    private var leftView: View? = null
    private var rightView: View? = null
    private var configJob: Job? = null

    private val hoverTimeout = 320L
    private val slideDistanceThresholdDp = 30f

    init {
        observeConfig()
    }

    private fun observeConfig() {
        configJob = configRepository.sideGestureConfig.onEach { config ->
            updateBars(config)
        }.launchIn(scope)
    }

    private fun getScreenBounds(): Rect {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds
        } else {
            val dm = service.resources.displayMetrics
            Rect(0, 0, dm.widthPixels, dm.heightPixels)
        }
    }

    private fun updateBars(config: SideGestureConfig) {
        val screenBounds = getScreenBounds()
        val screenHeight = screenBounds.height()
        val density = service.resources.displayMetrics.density

        // 1. 左侧边缘手势条
        if (config.leftEnabled) {
            val widthPx = (config.leftWidthDp * density).toInt().coerceAtLeast(1)
            val heightPx = (screenHeight * config.leftHeightPercent).toInt().coerceAtLeast(100)
            val yOffsetPx = (screenHeight * config.leftYOffsetPercent).toInt()

            attachOrUpdateSideBar(
                isLeft = true,
                widthPx = widthPx,
                heightPx = heightPx,
                yOffsetPx = yOffsetPx,
                slideAction = config.leftSlideAction,
                hoverAction = config.leftHoverAction
            )
        } else {
            removeSideBar(isLeft = true)
        }

        // 2. 右侧边缘手势条
        if (config.rightEnabled) {
            val widthPx = (config.rightWidthDp * density).toInt().coerceAtLeast(1)
            val heightPx = (screenHeight * config.rightHeightPercent).toInt().coerceAtLeast(100)
            val yOffsetPx = (screenHeight * config.rightYOffsetPercent).toInt()

            attachOrUpdateSideBar(
                isLeft = false,
                widthPx = widthPx,
                heightPx = heightPx,
                yOffsetPx = yOffsetPx,
                slideAction = config.rightSlideAction,
                hoverAction = config.rightHoverAction
            )
        } else {
            removeSideBar(isLeft = false)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachOrUpdateSideBar(
        isLeft: Boolean,
        widthPx: Int,
        heightPx: Int,
        yOffsetPx: Int,
        slideAction: Action,
        hoverAction: Action
    ) {
        val existingView = if (isLeft) leftView else rightView

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        val flags = (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        val gravity = (if (isLeft) Gravity.START else Gravity.END) or Gravity.TOP

        val lp = WindowManager.LayoutParams(
            widthPx,
            heightPx,
            type,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            this.gravity = gravity
            this.x = 0
            this.y = yOffsetPx
        }

        if (existingView == null) {
            val newView = View(service).apply {
                setBackgroundColor(Color.TRANSPARENT)
                setOnTouchListener { v, event ->
                    handleSideTouch(v, event, isLeft, slideAction, hoverAction)
                }
            }

            try {
                windowManager.addView(newView, lp)
                if (isLeft) leftView = newView else rightView = newView
                applySystemGestureExclusion(newView, widthPx, heightPx)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            try {
                windowManager.updateViewLayout(existingView, lp)
                applySystemGestureExclusion(existingView, widthPx, heightPx)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 适配 Android 10+ 系统手势排除区域，防止与原生侧滑返回冲突
     */
    private fun applySystemGestureExclusion(view: View, widthPx: Int, heightPx: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            view.post {
                try {
                    val rect = Rect(0, 0, widthPx, heightPx)
                    view.systemGestureExclusionRects = listOf(rect)
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun removeSideBar(isLeft: Boolean) {
        val view = if (isLeft) leftView else rightView
        if (view != null) {
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (isLeft) leftView = null else rightView = null
        }
    }

    // 手势识别逻辑
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var isHoverTriggered = false
    private var hoverRunnable: Runnable? = null

    private fun handleSideTouch(
        view: View,
        event: MotionEvent,
        isLeft: Boolean,
        slideAction: Action,
        hoverAction: Action
    ): Boolean {
        val density = service.resources.displayMetrics.density
        val thresholdPx = slideDistanceThresholdDp * density

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = event.rawX
                touchDownY = event.rawY
                isHoverTriggered = false

                hoverRunnable = Runnable {
                    val dx = event.rawX - touchDownX
                    val validInward = if (isLeft) dx > thresholdPx else dx < -thresholdPx
                    if (validInward && !isHoverTriggered) {
                        isHoverTriggered = true
                        hapticsManager.performHapticFeedback(view, HapticsManager.HapticType.LONG_PRESS)
                        actionDispatcher.dispatch(hoverAction, service)
                    }
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - touchDownX
                val validInward = if (isLeft) dx > thresholdPx else dx < -thresholdPx

                if (validInward && hoverRunnable != null && !isHoverTriggered) {
                    handler.postDelayed(hoverRunnable!!, hoverTimeout)
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                hoverRunnable?.let { handler.removeCallbacks(it) }
                hoverRunnable = null

                if (isHoverTriggered) {
                    // 悬停动作已触发，抬起时不重复触发
                    return true
                }

                val dx = event.rawX - touchDownX
                val dy = event.rawY - touchDownY
                val validInward = if (isLeft) dx > thresholdPx else dx < -thresholdPx

                // 只有内向滑动且纵向位移不大于横向位移时认定为有效侧滑
                if (validInward && abs(dx) > abs(dy)) {
                    hapticsManager.performHapticFeedback(view, HapticsManager.HapticType.CLICK)
                    actionDispatcher.dispatch(slideAction, service)
                    return true
                }
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                hoverRunnable?.let { handler.removeCallbacks(it) }
                hoverRunnable = null
                isHoverTriggered = false
                return true
            }
        }
        return false
    }

    fun onConfigurationChanged() {
        val config = configRepository.sideGestureConfig.value
        updateBars(config)
    }

    fun onDestroy() {
        configJob?.cancel()
        removeSideBar(isLeft = true)
        removeSideBar(isLeft = false)
    }
}
