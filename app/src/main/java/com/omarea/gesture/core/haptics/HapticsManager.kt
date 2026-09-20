package com.omarea.gesture.core.haptics

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * 现代触觉反馈管理器
 * 适配 Android 10 ~ 16 现代线性马达（Rich Haptics / Haptic Feedback Primitives）
 */
class HapticsManager(private val context: Context) {

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
     * 针对 View 执行系统级触觉反馈
     */
    fun performHapticFeedback(view: View?, type: HapticType) {
        if (view == null) {
            vibrate(type)
            return
        }

        val feedbackConstant = when (type) {
            HapticType.CLICK -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.CONFIRM
                } else {
                    HapticFeedbackConstants.KEYBOARD_TAP
                }
            }
            HapticType.LONG_PRESS -> HapticFeedbackConstants.LONG_PRESS
            HapticType.TICK -> HapticFeedbackConstants.CLOCK_TICK
            HapticType.SUCCESS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.CONFIRM
                } else {
                    HapticFeedbackConstants.LONG_PRESS
                }
            }
            HapticType.WARNING -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.REJECT
                } else {
                    HapticFeedbackConstants.LONG_PRESS
                }
            }
        }

        val success = view.performHapticFeedback(feedbackConstant)
        if (!success) {
            vibrate(type)
        }
    }

    /**
     * 直接调用 Vibrator 进行高精度马达振动
     */
    fun vibrate(type: HapticType) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 支持丰富的 Primitive 组合触感
            val primitive = when (type) {
                HapticType.CLICK -> VibrationEffect.Composition.PRIMITIVE_CLICK
                HapticType.LONG_PRESS -> VibrationEffect.Composition.PRIMITIVE_TICK
                HapticType.TICK -> VibrationEffect.Composition.PRIMITIVE_LOW_TICK
                HapticType.SUCCESS -> VibrationEffect.Composition.PRIMITIVE_CLICK
                HapticType.WARNING -> VibrationEffect.Composition.PRIMITIVE_QUICK_FALL
            }

            if (v.areAllPrimitivesSupported(primitive)) {
                val composition = VibrationEffect.startComposition()
                when (type) {
                    HapticType.CLICK -> composition.addPrimitive(primitive, 1.0f)
                    HapticType.LONG_PRESS -> {
                        composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.7f)
                        composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 1.0f, 20)
                    }
                    HapticType.TICK -> composition.addPrimitive(primitive, 0.5f)
                    HapticType.SUCCESS -> {
                        composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.8f)
                        composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 1.0f, 30)
                    }
                    HapticType.WARNING -> composition.addPrimitive(primitive, 1.0f)
                }
                v.vibrate(composition.compose())
                return
            }
        }

        // Android 10+ 预设效果与单脉冲回退
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effectId = when (type) {
                HapticType.CLICK -> VibrationEffect.EFFECT_CLICK
                HapticType.LONG_PRESS -> VibrationEffect.EFFECT_HEAVY_CLICK
                HapticType.TICK -> VibrationEffect.EFFECT_TICK
                HapticType.SUCCESS -> VibrationEffect.EFFECT_CLICK
                HapticType.WARNING -> VibrationEffect.EFFECT_DOUBLE_CLICK
            }
            try {
                v.vibrate(VibrationEffect.createPredefined(effectId))
                return
            } catch (_: Exception) {
            }
        }

        // 传统单脉冲振动回退
        val (duration, amplitude) = when (type) {
            HapticType.CLICK -> 18L to 180
            HapticType.LONG_PRESS -> 35L to 255
            HapticType.TICK -> 10L to 100
            HapticType.SUCCESS -> 25L to 200
            HapticType.WARNING -> 40L to 230
        }
        v.vibrate(VibrationEffect.createOneShot(duration, amplitude))
    }
}
