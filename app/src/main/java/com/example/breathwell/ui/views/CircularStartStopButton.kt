package com.example.breathwell.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.breathwell.R
import kotlin.math.sqrt

class CircularStartStopButton @JvmOverloads constructor(
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
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val shadowPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.BLACK
        alpha = 40
    }

    private val rect = RectF()
    private var isPressed = false
    private var isPlaying = false
    private var onClickListener: (() -> Unit)? = null

    // Path for play icon
    private val playPath = Path()

    // Path for stop icon (square)
    private val stopPath = Path()

    fun setOnButtonClickListener(listener: () -> Unit) {
        onClickListener = listener
    }

    fun setPlaying(playing: Boolean) {
        isPlaying = playing
        invalidate()
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

        // Draw button background
        canvas.drawCircle(centerX, centerY, radius, buttonPaint)

        // Update icon based on state
        updateIcon(centerX, centerY, radius * 0.5f)

        // Draw appropriate icon
        iconPaint.color = Color.WHITE
        if (isPlaying) {
            canvas.drawPath(stopPath, iconPaint)
        } else {
            canvas.drawPath(playPath, iconPaint)
        }
    }

    private fun updateIcon(centerX: Float, centerY: Float, iconSize: Float) {
        // Create play icon (triangle)
        playPath.reset()
        playPath.moveTo(centerX - iconSize * 0.7f, centerY - iconSize)
        playPath.lineTo(centerX - iconSize * 0.7f, centerY + iconSize)
        playPath.lineTo(centerX + iconSize * 0.9f, centerY)
        playPath.close()

        // Create stop icon (square)
        stopPath.reset()
        val squareSize = iconSize * 1.3f
        stopPath.addRect(
            centerX - squareSize / 2f,
            centerY - squareSize / 2f,
            centerX + squareSize / 2f,
            centerY + squareSize / 2f,
            Path.Direction.CW
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) {
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