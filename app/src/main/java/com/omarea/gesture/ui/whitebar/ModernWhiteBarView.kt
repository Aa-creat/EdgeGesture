package com.omarea.gesture.ui.whitebar

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/**
 * 现代 iOS 风格小白条轻量级绘制视图
 * 采用原生 Canvas 高频绘制，超低延迟与低内存占用
 */
class ModernWhiteBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x332196F3 // 半透明蓝色遮罩
    }

    private val highlightBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xFF2196F3.toInt()
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 28f
        textAlign = Paint.Align.CENTER
        setShadowLayer(4f, 0f, 2f, 0x88000000)
    }

    private val barRect = RectF()
    private val batteryRect = RectF()
    private val hotAreaRect = RectF()

    private var barWidthPx: Float = 0f
    private var barHeightPx: Float = 0f
    private var cornerRadiusPx: Float = 0f
    private var bottomMarginPx: Float = 0f
    private var barColor: Int = 0xFFFFFFFF.toInt()
    private var barAlpha: Float = 0.85f
    private var batteryLevelEnabled: Boolean = false
    private var batterySmoothGradient: Boolean = true

    private var isHighlighting: Boolean = false
    private val handler = Handler(Looper.getMainLooper())
    private val clearHighlightRunnable = Runnable {
        isHighlighting = false
        invalidate()
    }

    // 缩放动效（按压/回弹）
    var scaleRatio: Float = 1.0f
        set(value) {
            field = value
            invalidate()
        }

    // 物理跟手位移动效
    var barTranslationX: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    var barTranslationY: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    // 防烧屏微位移（像素偏移）
    var burnInShiftX: Float = 0f
        private set
    var burnInShiftY: Float = 0f
        private set

    fun setBurnInOffset(shiftX: Float, shiftY: Float) {
        if (burnInShiftX != shiftX || burnInShiftY != shiftY) {
            burnInShiftX = shiftX
            burnInShiftY = shiftY
            invalidate()
        }
    }

    fun updateStyle(
        widthPx: Float,
        heightPx: Float,
        radiusPx: Float,
        bottomMarginPx: Float,
        color: Int,
        alpha: Float,
        batteryLevelEnabled: Boolean = false,
        batterySmoothGradient: Boolean = true
    ) {
        this.barWidthPx = widthPx
        this.barHeightPx = heightPx
        this.cornerRadiusPx = radiusPx
        this.bottomMarginPx = bottomMarginPx
        this.barColor = color
        this.barAlpha = alpha
        this.batteryLevelEnabled = batteryLevelEnabled
        this.batterySmoothGradient = batterySmoothGradient
        invalidate()
    }

    /**
     * 在屏幕上高亮显示触控热区辅助线，展示指定时长后自动消失
     */
    fun showHighlight(durationMs: Long = 3000L) {
        isHighlighting = true
        handler.removeCallbacks(clearHighlightRunnable)
        handler.postDelayed(clearHighlightRunnable, durationMs)
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(w, h)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        // 1. 若处于高亮预览模式或设置测试模式，在屏幕实际位置绘制触控热区矩形边界及标注
        val density = context.resources.displayMetrics.density
        if (isHighlighting || com.omarea.gesture.util.GlobalState.testMode) {
            hotAreaRect.set(0f, 0f, w, h)
            canvas.drawRect(hotAreaRect, highlightPaint)
            canvas.drawRect(hotAreaRect, highlightBorderPaint)
            val touchWidthDp = (w / density).toInt()
            val touchHeightDp = (h / density).toInt()
            canvas.drawText("热区: ${touchWidthDp}dp × ${touchHeightDp}dp", w / 2f, h / 2f + 10f, textPaint)
        }

        // 若透明度为 0，则完全隐藏小白条本体（但保留手势触控与测试高亮）
        if (barAlpha <= 0f) return

        canvas.save()
        // 以小白条自身中心点进行微缩，并融入防烧屏微偏移
        val barCenterX = (w / 2f) + burnInShiftX
        val barCenterY = if (h > bottomMarginPx + barHeightPx) {
            h - bottomMarginPx - (barHeightPx / 2f) + burnInShiftY
        } else {
            h / 2f + burnInShiftY
        }
        canvas.scale(scaleRatio, scaleRatio, barCenterX, barCenterY)

        val left = (w - barWidthPx) / 2f + burnInShiftX
        val bottom = if (h > bottomMarginPx + barHeightPx) {
            h - bottomMarginPx + burnInShiftY
        } else {
            h - ((h - barHeightPx) / 2f).coerceAtLeast(0f) + burnInShiftY
        }
        val top = bottom - barHeightPx
        val right = left + barWidthPx

        barRect.set(left, top, right, bottom)

        // 计算有效圆角（如果设置半径很大，自动适配为完全圆角半圆胶囊）
        val maxRadius = barHeightPx / 2f
        val actualRadius = if (cornerRadiusPx >= maxRadius || cornerRadiusPx > 20f) maxRadius else cornerRadiusPx

        if (batteryLevelEnabled) {
            // 读取电池电量
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val capacity = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100

            // 1. 绘制底轨（半透明背景）
            paint.shader = null
            paint.color = barColor
            paint.alpha = (barAlpha * 0.35f * 255).toInt().coerceIn(0, 255)
            canvas.drawRoundRect(barRect, actualRadius, actualRadius, paint)

            // 2. 绘制电量进度条
            val fillWidth = barWidthPx * (capacity.coerceIn(0, 100) / 100f)
            batteryRect.set(left, top, left + fillWidth, bottom)

            if (batterySmoothGradient) {
                // 现代平滑动态渐变：警示赤红 -> 暖橙 -> 柠檬黄绿 -> 翠绿 -> 湖青 -> 青蓝 -> 科技蓝
                val colors = intArrayOf(
                    0xFFF9592F.toInt(), // <=20% 警示赤红
                    0xFFFC8A1B.toInt(), // 21%~35% 暖橙色
                    0xFF87CB00.toInt(), // 36%~45% 柠檬黄绿
                    0xFF02D98D.toInt(), // 46%~60% 翠绿色
                    0xFF00D5D9.toInt(), // 61%~75% 湖青色
                    0xFF00B9C2.toInt(), // 76%~85% 青蓝色
                    0xFF138ED6.toInt()  // >85% 科技蓝
                )
                val positions = floatArrayOf(0.0f, 0.20f, 0.35f, 0.50f, 0.65f, 0.80f, 1.0f)
                paint.shader = android.graphics.LinearGradient(
                    left, top, left + barWidthPx, top,
                    colors, positions,
                    android.graphics.Shader.TileMode.CLAMP
                )
                paint.alpha = (barAlpha * 255).toInt().coerceIn(0, 255)
                canvas.drawRoundRect(batteryRect, actualRadius, actualRadius, paint)
                paint.shader = null
            } else {
                // 原版 7 档阶梯色阶
                val batteryColor = when {
                    capacity > 85 -> 0xFF138ED6.toInt() // 科技蓝 (>85%)
                    capacity > 75 -> 0xFF00B9C2.toInt() // 青蓝色 (76%~85%)
                    capacity > 60 -> 0xFF00D5D9.toInt() // 湖青色 (61%~75%)
                    capacity > 45 -> 0xFF02D98D.toInt() // 翠绿色 (46%~60%)
                    capacity > 35 -> 0xFF87CB00.toInt() // 柠檬黄绿 (36%~45%)
                    capacity > 20 -> 0xFFFC8A1B.toInt() // 暖橙色 (21%~35%)
                    else -> 0xFFF9592F.toInt()          // 警示赤红 (<=20%)
                }
                paint.shader = null
                paint.color = batteryColor
                paint.alpha = (barAlpha * 255).toInt().coerceIn(0, 255)
                canvas.drawRoundRect(batteryRect, actualRadius, actualRadius, paint)
            }
        } else {
            // 绘制标准小白条主体
            paint.shader = null
            paint.color = barColor
            paint.alpha = (barAlpha * 255).toInt().coerceIn(0, 255)
            canvas.drawRoundRect(barRect, actualRadius, actualRadius, paint)
        }

        // 绘制细微半透明边缘阴影/描边，确保浅色背景下依然清晰可见
        strokePaint.color = 0x22000000
        strokePaint.alpha = (barAlpha * 0x22).toInt().coerceIn(0, 255)
        strokePaint.strokeWidth = 1.5f
        canvas.drawRoundRect(barRect, actualRadius, actualRadius, strokePaint)

        canvas.restore()
    }
}
