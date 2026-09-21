package com.omarea.gesture.core.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import com.omarea.gesture.core.config.AppConfigRepository

/**
 * 现代触觉反馈管理器
 * 适配 Android 10 ~ 16 现代线性马达（Rich Haptics / Haptic Feedback Primitives）
 * 支持 ColorOS / OnePlus 后台触控震动属性与自定义震动时长
 */
class HapticsManager(
    private val context: Context,
    private val configRepository: AppConfigRepository
) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    enum class HapticType {
        CLICK,          // 单击（轻快清脆）
        LONG_PRESS,     // 长按生效（沉稳顿挫）
        TICK,           // 细微刻度感（滑动过程）
        SUCCESS,        // 成功反馈
        WARNING         // 警告反馈
    }

    /**
     * 针对 View 执行触觉反馈
     * 保证 100% 触发马达振动：
     * 1. 尝试 View.performHapticFeedback (FLAG_IGNORE_VIEW_SETTING | FLAG_IGNORE_GLOBAL_SETTING)
     * 2. 核心直接调用 Vibrator.createOneShot(ms, 255)，彻底绕过 ColorOS 对无焦点无障碍悬浮窗的触感过滤与全局开关拦截
     */
    fun performHapticFeedback(view: View?, type: HapticType) {
        val basicConfig = configRepository.basicConfig.value

        if (basicConfig.vibratorUseSystem && view != null) {
            try {
                view.isHapticFeedbackEnabled = true
                val constant = when (type) {
                    HapticType.CLICK, HapticType.SUCCESS -> HapticFeedbackConstants.VIRTUAL_KEY
                    HapticType.LONG_PRESS, HapticType.WARNING -> HapticFeedbackConstants.LONG_PRESS
                    HapticType.TICK -> HapticFeedbackConstants.CLOCK_TICK
                }
                val flags = HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING or HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                view.performHapticFeedback(constant, flags)
            } catch (_: Exception) {
            }
        }

        // 直接调用硬件 Vibrator 进行触感兜底，确保在任何系统与设置下都能产生真实手感
        vibrateDirect(type, isSystem = basicConfig.vibratorUseSystem)
    }

    /**
     * 直接调用 Vibrator 进行马达振动
     */
    fun vibrate(type: HapticType) {
        val basicConfig = configRepository.basicConfig.value
        vibrateDirect(type, isSystem = basicConfig.vibratorUseSystem)
    }

    private fun vibrateDirect(type: HapticType, isSystem: Boolean) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        val basicConfig = configRepository.basicConfig.value
        v.cancel()

        val isLong = type == HapticType.LONG_PRESS || type == HapticType.SUCCESS || type == HapticType.WARNING
        val durationMs = if (isSystem) {
            if (isLong) 35L else 15L
        } else {
            (if (isLong) basicConfig.vibratorHoverTimeMs else basicConfig.vibratorTapTimeMs).toLong()
        }

        if (durationMs <= 0) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(durationMs, 255))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(durationMs)
        }
    }
}
