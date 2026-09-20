package com.omarea.gesture.ui.whitebar

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import com.omarea.gesture.AccessibilityServiceGesture
import com.omarea.gesture.core.config.AppConfigRepository
import com.omarea.gesture.core.config.WhiteBarConfig
import com.omarea.gesture.core.dispatcher.ActionDispatcher
import com.omarea.gesture.core.haptics.HapticsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.abs

/**
 * 现代 iOS 风格小白条手势控制器
 * 负责悬浮窗挂载、触控检测（单击、长按、滑动）、线性马达反馈与动作分发
 */
class ModernWhiteBar(
    private val service: AccessibilityServiceGesture,
    private val configRepository: AppConfigRepository,
    private val actionDispatcher: ActionDispatcher,
    private val hapticsManager: HapticsManager
) {
    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main)

    private var barView: ModernWhiteBarView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var configJob: Job? = null

    // 手势状态
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var touchDownTime = 0L
    private var isLongPressTriggered = false
    private var isGestureCancelled = false

    private val longPressTimeout = 320L // 长按判定时间（毫秒）
    private val clickDistanceSlopDp = 12f // 判定为点击的最大位移阈值
    private val swipeDistanceThresholdDp = 35f // 判定为滑动的最小位移阈值

    private val longPressRunnable = Runnable {
        if (!isGestureCancelled && !isLongPressTriggered) {
            isLongPressTriggered = true
            val config = configRepository.whiteBarConfig.value
            if (config.hapticsEnabled) {
                hapticsManager.performHapticFeedback(barView, HapticsManager.HapticType.LONG_PRESS)
            }
            animateScale(barView, 1.15f, 1.0f)
            actionDispatcher.dispatch(config.longPressAction, service)
        }
    }

    init {
        observeConfig()
    }

    private fun observeConfig() {
        configJob = configRepository.whiteBarConfig.onEach { config ->
            if (config.enabled) {
                attachOrUpdateView(config)
            } else {
                detachView()
            }
        }.launchIn(scope)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachOrUpdateView(config: WhiteBarConfig) {
        val density = service.resources.displayMetrics.density
        val widthPx = config.widthDp * density
        val heightPx = config.heightDp * density
        val radiusPx = config.radiusDp * density
        val bottomMarginPx = (config.bottomMarginDp * density).toInt()

        // 触摸热区增加内边距，让小白条极易点击
        val touchPaddingVerticalPx = (14f * density).toInt()
        val totalViewHeightPx = (heightPx + touchPaddingVerticalPx * 2).toInt()

        if (barView == null) {
            barView = ModernWhiteBarView(service).apply {
                setOnTouchListener { v, event -> handleTouchEvent(v, event) }
            }

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

            layoutParams = WindowManager.LayoutParams(
                widthPx.toInt(),
                totalViewHeightPx,
                type,
                flags,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                y = bottomMarginPx
            }

            try {
                windowManager.addView(barView, layoutParams)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            layoutParams?.let { lp ->
                lp.width = widthPx.toInt()
                lp.height = totalViewHeightPx
                lp.y = bottomMarginPx
                try {
                    windowManager.updateViewLayout(barView, lp)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        barView?.updateStyle(
            widthPx = widthPx,
            heightPx = heightPx,
            radiusPx = radiusPx,
            color = config.color.toInt(),
            alpha = config.alpha
        )
    }

    private fun handleTouchEvent(view: View, event: MotionEvent): Boolean {
        val density = service.resources.displayMetrics.density
        val slopPx = clickDistanceSlopDp * density
        val swipePx = swipeDistanceThresholdDp * density
        val config = configRepository.whiteBarConfig.value

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = event.rawX
                touchDownY = event.rawY
                touchDownTime = System.currentTimeMillis()
                isLongPressTriggered = false
                isGestureCancelled = false

                // 按压缩小视觉反馈
                (view as? ModernWhiteBarView)?.scaleRatio = 0.92f

                // 启动长按检测计时器
                handler.postDelayed(longPressRunnable, longPressTimeout)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - touchDownX
                val dy = event.rawY - touchDownY

                // 若位移超出阈值，取消长按判定
                if (abs(dx) > slopPx || abs(dy) > slopPx) {
                    handler.removeCallbacks(longPressRunnable)
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(longPressRunnable)
                // 恢复缩放
                animateScale(view as? ModernWhiteBarView, (view as? ModernWhiteBarView)?.scaleRatio ?: 1f, 1f)

                if (isLongPressTriggered) {
                    // 长按已执行，抬起时不触发任何动作
                    return true
                }

                val dx = event.rawX - touchDownX
                val dy = event.rawY - touchDownY
                val totalDist = abs(dx) + abs(dy)

                // 1. 判断是否为单击 (Click / Short Touch)
                if (totalDist < slopPx) {
                    if (config.hapticsEnabled) {
                        hapticsManager.performHapticFeedback(view, HapticsManager.HapticType.CLICK)
                    }
                    actionDispatcher.dispatch(config.clickAction, service)
                    return true
                }

                // 2. 判断是否为滑动操作
                if (abs(dx) > abs(dy) && abs(dx) > swipePx) {
                    // 横向滑动
                    if (config.hapticsEnabled) {
                        hapticsManager.performHapticFeedback(view, HapticsManager.HapticType.TICK)
                    }
                    if (dx > 0) {
                        actionDispatcher.dispatch(config.swipeRightAction, service)
                    } else {
                        actionDispatcher.dispatch(config.swipeLeftAction, service)
                    }
                    return true
                } else if (dy < -swipePx) {
                    // 向上轻扫
                    if (config.hapticsEnabled) {
                        hapticsManager.performHapticFeedback(view, HapticsManager.HapticType.TICK)
                    }
                    actionDispatcher.dispatch(config.swipeUpAction, service)
                    return true
                }

                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPressRunnable)
                isGestureCancelled = true
                animateScale(view as? ModernWhiteBarView, (view as? ModernWhiteBarView)?.scaleRatio ?: 1f, 1f)
                return true
            }
        }
        return false
    }

    private fun animateScale(view: ModernWhiteBarView?, start: Float, end: Float) {
        if (view == null) return
        ValueAnimator.ofFloat(start, end).apply {
            duration = 180
            interpolator = OvershootInterpolator(1.5f)
            addUpdateListener { animator ->
                view.scaleRatio = animator.animatedValue as Float
            }
            start()
        }
    }

    fun detachView() {
        handler.removeCallbacksAndMessages(null)
        barView?.let { v ->
            try {
                windowManager.removeView(v)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        barView = null
        layoutParams = null
    }

    fun onDestroy() {
        configJob?.cancel()
        detachView()
    }
}
