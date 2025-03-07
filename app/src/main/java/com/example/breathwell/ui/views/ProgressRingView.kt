package com.example.breathwell.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withRotation
import kotlin.math.min

class ProgressRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val backgroundPaint = Paint().apply {
        color = "#333333".toColorInt() // Dark gray
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(8f) // Thicker stroke
        isAntiAlias = true
        alpha = 60 // Lower opacity for background
    }

    private val progressPaint = Paint().apply {
        color = "#38E1FF".toColorInt() // Cyan
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(8f) // Thicker stroke
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    private val rect = RectF()

    var currentCycle: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    var totalCycles: Int = 5
        set(value) {
            field = value
            invalidate()
        }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val radius = min(width, height) / 2f - backgroundPaint.strokeWidth
        val centerX = width / 2f
        val centerY = height / 2f

        // Calculate angle for progress arc
        val progressAngle = 360f * (currentCycle.toFloat() / totalCycles.toFloat().coerceAtLeast(1f))

        // Draw background circle - slightly larger than HAL Circle
        rect.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )
        canvas.drawCircle(centerX, centerY, radius, backgroundPaint)

        // Draw progress arc
        canvas.withRotation(-90f, centerX, centerY) {
            drawArc(rect, 0f, progressAngle, false, progressPaint)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredSize = dpToPx(300f).toInt() // Larger default size

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> min(desiredSize, widthSize)
            else -> desiredSize
        }

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> min(desiredSize, heightSize)
            else -> desiredSize
        }

        // Keep it square
        val finalSize = min(width, height)
        setMeasuredDimension(finalSize, finalSize)
    }
}