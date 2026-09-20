package com.omarea.gesture.ui.whitebar

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

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

    private val barRect = RectF()
    private var barWidthPx: Float = 0f
    private var barHeightPx: Float = 0f
    private var cornerRadiusPx: Float = 0f
    private var barColor: Int = 0xFFFFFFFF.toInt()
    private var barAlpha: Float = 0.85f

    // 缩放动效（按压/回弹）
    var scaleRatio: Float = 1.0f
        set(value) {
            field = value
            invalidate()
        }

    fun updateStyle(
        widthPx: Float,
        heightPx: Float,
        radiusPx: Float,
        color: Int,
        alpha: Float
    ) {
        this.barWidthPx = widthPx
        this.barHeightPx = heightPx
        this.cornerRadiusPx = radiusPx
        this.barColor = color
        this.barAlpha = alpha
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = if (barWidthPx > 0) barWidthPx.toInt() else MeasureSpec.getSize(widthMeasureSpec)
        val h = if (barHeightPx > 0) barHeightPx.toInt() else MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(w, h)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        canvas.save()
        // 以中心点进行物理按压缩放
        canvas.scale(scaleRatio, scaleRatio, w / 2f, h / 2f)

        // 绘制小白条主体
        paint.color = barColor
        paint.alpha = (barAlpha * 255).toInt().coerceIn(0, 255)

        val left = (w - barWidthPx) / 2f
        val top = (h - barHeightPx) / 2f
        val right = left + barWidthPx
        val bottom = top + barHeightPx

        barRect.set(left, top, right, bottom)
        canvas.drawRoundRect(barRect, cornerRadiusPx, cornerRadiusPx, paint)

        // 绘制细微半透明边缘阴影/描边，确保浅色背景下依然可见
        strokePaint.color = 0x22000000
        strokePaint.strokeWidth = 1.5f
        canvas.drawRoundRect(barRect, cornerRadiusPx, cornerRadiusPx, strokePaint)

        canvas.restore()
    }
}
