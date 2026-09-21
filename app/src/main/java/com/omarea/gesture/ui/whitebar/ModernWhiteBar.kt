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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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

    private var burnInJob: Job? = null
    private var burnInStep = 0
    private val burnInOffsets = listOf(
        -2.5f to 0f,
        -1.2f to 0.8f,
        1.2f to 0.8f,
        2.5f to 0f,
        1.2f to -0.8f,
        -1.2f to -0.8f,
        0f to 0f
    )

    private val longPressRunnable = Runnable {
        if (!isGestureCancelled && !isLongPressTriggered) {
            isLongPressTriggered = true
            val config = configRepository.whiteBarConfig.value
            hapticsManager.performHapticFeedback(barView, HapticsManager.HapticType.LONG_PRESS)
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
                setupBurnInProtection(config)
            } else {
                detachView()
            }
        }.launchIn(scope)
    }

    private fun setupBurnInProtection(config: WhiteBarConfig) {
        burnInJob?.cancel()
        if (!config.burnInProtection || !config.enabled) {
            barView?.setBurnInOffset(0f, 0f)
            return
        }

        burnInJob = scope.launch {
            val density = service.resources.displayMetrics.density
            while (isActive) {
                kotlinx.coroutines.delay(60_000L) // 每 60 秒平滑微偏移，防止 OLED 烧屏
                burnInStep = (burnInStep + 1) % burnInOffsets.size
                val (targetX, targetY) = burnInOffsets[burnInStep]
                barView?.let { v ->
                    val startX = v.burnInShiftX
                    val startY = v.burnInShiftY
                    val endX = targetX * density
                    val endY = targetY * density
                    ValueAnimator.ofFloat(0f, 1f).apply {
                        duration = 600
                        addUpdateListener { anim ->
                            val f = anim.animatedValue as Float
                            v.setBurnInOffset(startX + (endX - startX) * f, startY + (endY - startY) * f)
                        }
                        start()
                    }
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachOrUpdateView(config: WhiteBarConfig) {
        val density = service.resources.displayMetrics.density
        val widthPx = config.widthDp * density
        val heightPx = config.heightDp * density
        val radiusPx = config.radiusDp * density
        val bottomMarginPx = (config.bottomMarginDp * density).toInt()

        // 触控热区尺寸：支持用户独立调节热区宽度与热区高度
        val touchWidthPx = (config.touchWidthDp * density).toInt().coerceAtLeast(1)
        val touchHeightPx = (config.touchHeightDp * density).toInt().coerceAtLeast(1)
        val totalViewWidthPx = touchWidthPx
        val totalViewHeightPx = touchHeightPx

        if (barView == null) {
            instance = this
            barView = ModernWhiteBarView(service).apply {
                isHapticFeedbackEnabled = true
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
                totalViewWidthPx,
                totalViewHeightPx,
                type,
                flags,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                x = 0
                y = 0 // 贴在屏幕最底部
            }

            try {
                windowManager.addView(barView, layoutParams)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            barView?.isHapticFeedbackEnabled = true
            layoutParams?.let { lp ->
                lp.width = totalViewWidthPx
                lp.height = totalViewHeightPx
                lp.x = 0
                lp.y = 0 // 贴在屏幕最底部
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
            bottomMarginPx = bottomMarginPx.toFloat(),
            color = config.color.toInt(),
            alpha = config.alpha,
            batteryLevelEnabled = config.batteryLevelEnabled,
            batterySmoothGradient = config.batterySmoothGradient
        )
    }

    companion object {
        var instance: ModernWhiteBar? = null
            private set

        fun requestHighlight() {
            instance?.barView?.showHighlight(3000L)
        }
    }

    // 悬停状态
    private var isSwipeUpHoverTriggered = false
    private var isSwipeUpHoverScheduled = false
    private var swipeUpHoverRunnable: Runnable? = null

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
                isSwipeUpHoverTriggered = false
                isSwipeUpHoverScheduled = false

                // 按压缩小视觉反馈（平滑微缩至 0.90x）
                animateScale(view as? ModernWhiteBarView, (view as? ModernWhiteBarView)?.scaleRatio ?: 1f, 0.90f)

                // 启动长按检测计时器
                handler.postDelayed(longPressRunnable, longPressTimeout)

                swipeUpHoverRunnable = Runnable {
                    val dy = event.rawY - touchDownY
                    if (dy < -swipePx && !isSwipeUpHoverTriggered) {
                        isSwipeUpHoverTriggered = true
                        hapticsManager.performHapticFeedback(view, HapticsManager.HapticType.LONG_PRESS)
                        actionDispatcher.dispatch(config.swipeUpHoldAction, service)
                    }
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - touchDownX
                val dy = event.rawY - touchDownY

                // 物理阻尼衰减跟手位移：直接通过 WindowManager 更新窗口位置，确保完全不被裁切
                val maxOffsetX = 35f * density
                val maxOffsetY = 30f * density
                val dampedDx = (dx * 0.35f).coerceIn(-maxOffsetX, maxOffsetX)
                val dampedDy = (-dy * 0.35f).coerceIn(0f, maxOffsetY)
                layoutParams?.let { lp ->
                    lp.x = dampedDx.toInt()
                    lp.y = dampedDy.toInt()
                    try {
                        windowManager.updateViewLayout(view, lp)
                    } catch (_: Exception) {}
                }

                // 若位移超出阈值，取消长按判定
                if (abs(dx) > slopPx || abs(dy) > slopPx) {
                    handler.removeCallbacks(longPressRunnable)
                }

                // 上滑悬停检测
                if (dy < -swipePx && abs(dy) > abs(dx)) {
                    if (!isSwipeUpHoverScheduled && !isSwipeUpHoverTriggered) {
                        isSwipeUpHoverScheduled = true
                        swipeUpHoverRunnable?.let { handler.postDelayed(it, 300L) }
                    }
                } else {
                    if (isSwipeUpHoverScheduled) {
                        swipeUpHoverRunnable?.let { handler.removeCallbacks(it) }
                        isSwipeUpHoverScheduled = false
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(longPressRunnable)
                swipeUpHoverRunnable?.let { handler.removeCallbacks(it) }
                swipeUpHoverRunnable = null
                isSwipeUpHoverScheduled = false

                // 弹性回弹居中并恢复 1.0x 缩放
                animateReset(view as? ModernWhiteBarView)

                if (isLongPressTriggered || isSwipeUpHoverTriggered) {
                    // 长按或上滑悬停已执行，抬起时不触发任何动作
                    return true
                }

                val dx = event.rawX - touchDownX
                val dy = event.rawY - touchDownY
                val totalDist = abs(dx) + abs(dy)

                // 1. 判断是否为点击（彻底移除双击延迟，0ms 瞬时触发）
                if (totalDist < slopPx) {
                    hapticsManager.performHapticFeedback(view, HapticsManager.HapticType.CLICK)
                    actionDispatcher.dispatch(config.clickAction, service)
                    return true
                }

                // 2. 判断是否为滑动操作
                if (abs(dx) > abs(dy) && abs(dx) > swipePx) {
                    // 横向滑动
                    hapticsManager.performHapticFeedback(view, HapticsManager.HapticType.TICK)
                    if (dx > 0) {
                        actionDispatcher.dispatch(config.swipeRightAction, service)
                    } else {
                        actionDispatcher.dispatch(config.swipeLeftAction, service)
                    }
                    return true
                } else if (dy < -swipePx) {
                    // 向上轻扫
                    hapticsManager.performHapticFeedback(view, HapticsManager.HapticType.TICK)
                    actionDispatcher.dispatch(config.swipeUpAction, service)
                    return true
                }

                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPressRunnable)
                swipeUpHoverRunnable?.let { handler.removeCallbacks(it) }
                swipeUpHoverRunnable = null
                isSwipeUpHoverScheduled = false
                isSwipeUpHoverTriggered = false
                isGestureCancelled = true
                animateReset(view as? ModernWhiteBarView)
                return true
            }
        }
        return false
    }

    private fun animateReset(view: ModernWhiteBarView?) {
        if (view == null) return
        val startScale = view.scaleRatio
        val startX = layoutParams?.x ?: 0
        val startY = layoutParams?.y ?: 0
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 220
            interpolator = OvershootInterpolator(1.2f)
            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float
                view.scaleRatio = startScale + (1.0f - startScale) * fraction
                layoutParams?.let { lp ->
                    lp.x = (startX * (1f - fraction)).toInt()
                    lp.y = (startY * (1f - fraction)).toInt()
                    try {
                        windowManager.updateViewLayout(view, lp)
                    } catch (_: Exception) {}
                }
            }
            start()
        }
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
        burnInJob?.cancel()
        burnInJob = null
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

    fun refreshTestMode() {
        handler.post {
            barView?.invalidate()
        }
    }

    fun onDestroy() {
        instance = null
        configJob?.cancel()
        burnInJob?.cancel()
        detachView()
    }
}
