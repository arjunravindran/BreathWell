package com.example.breathwell.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import kotlin.math.min

class ProgressRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val backgroundPaint = Paint().apply {
        color = Color.parseColor("#333333") // Dark gray
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(3f)
        isAntiAlias = true
    }

    private val progressPaint = Paint().apply {
        color = Color.parseColor("#38E1FF") // Cyan
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(3f)
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = spToPx(12f)
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
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

    private fun spToPx(sp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sp,
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

        // Draw background circle
        rect.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )
        canvas.drawCircle(centerX, centerY, radius, backgroundPaint)

        // Draw progress arc
        canvas.save()
        canvas.rotate(-90f, centerX, centerY) // Start from top
        canvas.drawArc(rect, 0f, progressAngle, false, progressPaint)
        canvas.restore()

        // Draw text
        val text = "${currentCycle + 1}/$totalCycles"
        canvas.drawText(text, centerX, centerY + textPaint.textSize / 3, textPaint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredSize = dpToPx(48f).toInt()

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