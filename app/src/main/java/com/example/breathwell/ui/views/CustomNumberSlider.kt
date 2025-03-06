package com.example.breathwell.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import androidx.core.content.ContextCompat
import com.example.breathwell.R
import com.google.android.material.slider.Slider

/**
 * Custom slider that shows the current value inside a custom large thumb
 */
class CustomNumberSlider @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.sliderStyle
) : Slider(context, attrs, defStyleAttr) {

    private val thumbPaint = Paint().apply {
        isAntiAlias = true
        color = ContextCompat.getColor(context, R.color.cyan_400)
        style = Paint.Style.FILL
    }

    private val thumbTextPaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = spToPx(16f)
        typeface = Typeface.DEFAULT_BOLD
    }

    private val thumbSize = dpToPx(36f)
    private var displayValue: String = "0"
    private var thumbPositionX: Float = 0f

    init {
        // Hide the default thumb by setting its radius very small
        thumbRadius = 1

        // Set listener for value changes
        addOnChangeListener { _, value, _ ->
            displayValue = value.toInt().toString()
            invalidate()
        }

        // Set initial value
        displayValue = value.toInt().toString()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Calculate thumb position
        thumbPositionX = trackSidePadding + (trackWidth * (value - valueFrom) / (valueTo - valueFrom))

        // Draw custom larger thumb
        canvas.drawCircle(thumbPositionX, height / 2f, thumbSize / 2f, thumbPaint)

        // Draw text centered in the thumb
        val textX = thumbPositionX
        val textY = height / 2f + (thumbTextPaint.textSize / 3f) // Adjust for text baseline
        canvas.drawText(displayValue, textX, textY, thumbTextPaint)
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
}