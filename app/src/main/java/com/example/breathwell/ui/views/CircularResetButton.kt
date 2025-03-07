package com.example.breathwell.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.breathwell.R
import kotlin.math.sqrt

/**
 * A circular button used for resetting a breathing session.
 * This is styled to match the CircularStartStopButton but with a reset icon.
 */
class CircularResetButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val buttonPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.cyan_400)
    }

    private val iconPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = Color.WHITE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val shadowPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.BLACK
        alpha = 40
    }

    private var isPressed = false
    private var onClickListener: (() -> Unit)? = null
    private var isButtonEnabled = true

    // Path for reset icon
    private val resetPath = Path()

    fun setOnButtonClickListener(listener: () -> Unit) {
        onClickListener = listener
    }

    override fun setEnabled(enabled: Boolean) {
        isButtonEnabled = enabled
        super.setEnabled(enabled)
        invalidate() // Redraw to show disabled state
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = width.coerceAtMost(height) / 2f * 0.85f

        // Draw shadow with slight offset
        canvas.drawCircle(centerX + 2f, centerY + 2f, radius, shadowPaint)

        // Draw button background - use cyan color for reset button
        buttonPaint.color = if (isButtonEnabled) {
            ContextCompat.getColor(context, R.color.cyan_400)
        } else {
            // Use a muted color when disabled
            ContextCompat.getColor(context, R.color.gray_600)
        }
        canvas.drawCircle(centerX, centerY, radius, buttonPaint)

        // Update reset icon
        updateResetIcon(centerX, centerY, radius * 0.4f)

        // Draw the reset icon with appropriate alpha for enabled/disabled state
        iconPaint.alpha = if (isButtonEnabled) 255 else 128
        canvas.drawPath(resetPath, iconPaint)
    }

    private fun updateResetIcon(centerX: Float, centerY: Float, iconSize: Float) {
        // Create reset icon (circular arrow)
        resetPath.reset()

        // Draw arrow (simplified refresh icon)
        val arrowPath = Path()

        // Start at the 12 o'clock position
        resetPath.moveTo(centerX, centerY - iconSize)

        // Draw the circle (counterclockwise, leaving a gap at the start)
        resetPath.addArc(
            centerX - iconSize,
            centerY - iconSize,
            centerX + iconSize,
            centerY + iconSize,
            -60f, 300f
        )

        // Draw the arrow head
        val arrowSize = iconSize * 0.3f
        resetPath.moveTo(centerX, centerY - iconSize)
        resetPath.lineTo(centerX - arrowSize, centerY - iconSize + arrowSize)
        resetPath.moveTo(centerX, centerY - iconSize)
        resetPath.lineTo(centerX + arrowSize, centerY - iconSize + arrowSize)

        // Set stroke width proportional to the icon size
        iconPaint.strokeWidth = iconSize * 0.15f
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isButtonEnabled) {
            return false
        }

        val x = event.x
        val y = event.y
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = width.coerceAtMost(height) / 2f * 0.85f

        // Check if touch is within button circle
        val distanceFromCenter = sqrt((x - centerX) * (x - centerX) + (y - centerY) * (y - centerY))

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (distanceFromCenter <= radius) {
                    isPressed = true
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                if (isPressed && distanceFromCenter <= radius) {
                    performClick()
                    isPressed = false
                    invalidate()
                    return true
                }
                isPressed = false
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                isPressed = false
                invalidate()
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        onClickListener?.invoke()
        return true
    }
}