package com.example.breathwell.ui.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.min

class HALCircleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Paint objects
    private val circlePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.parseColor("#00A6ED") // Default blue
    }

    private val outerRingPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(2f)
        color = Color.WHITE
        alpha = 80 // 30% opacity
    }

    private val innerCirclePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.parseColor("#0076AD") // Darker blue
    }

    private val glowPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val timerPaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = spToPx(48f)
        typeface = Typeface.DEFAULT_BOLD
        alpha = 230 // 90% opacity
    }

    private val instructionPaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = spToPx(24f)
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        letterSpacing = 0.05f
        alpha = 230 // 90% opacity
    }

    // Animation properties
    private var pulseAnimator: ValueAnimator? = null
    private var pulseScale = 1.0f

    var expansion: Float = 50f
        set(value) {
            field = value
            invalidate()
        }

    var breathColor: Int = Color.parseColor("#00A6ED")
        set(value) {
            field = value
            circlePaint.color = value
            invalidate()
        }

    var innerColor: Int = Color.parseColor("#0076AD")
        set(value) {
            field = value
            innerCirclePaint.color = value
            invalidate()
        }

    var counter: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    var instruction: String = "READY"
        set(value) {
            field = value
            invalidate()
        }

    var showPulseEffect: Boolean = false
        set(value) {
            field = value
            if (value) {
                startPulseAnimation()
            } else {
                pulseAnimator?.cancel()
                pulseScale = 1.0f
            }
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

    private fun startPulseAnimation() {
        pulseAnimator?.cancel()

        pulseAnimator = ValueAnimator.ofFloat(1.0f, 1.15f).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()

            addUpdateListener { animator ->
                pulseScale = animator.animatedValue as Float
                invalidate()
            }

            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val centerX = width / 2f
        val centerY = height / 2f

        // Calculate circle dimensions
        val minDimension = min(width, height)
        val baseRadius = minDimension * 0.245f // 0.35f × 0.7f = 0.245f (30% smaller)
        val outerRadius = baseRadius * (1.0f + (expansion - 50f) / 150f)
        val innerRadius = outerRadius * 0.6f

        // Apply pulse animation
        val animatedOuterRadius = outerRadius * pulseScale

        // Create radial gradient for the glow effect
        val gradient = RadialGradient(
            centerX,
            centerY,
            animatedOuterRadius * 1.3f,
            intArrayOf(breathColor, Color.TRANSPARENT),
            floatArrayOf(0.7f, 1.0f),
            Shader.TileMode.CLAMP
        )
        glowPaint.shader = gradient

        // Draw outer glow
        canvas.drawCircle(centerX, centerY, animatedOuterRadius * 1.3f, glowPaint)

        // Draw main circle
        canvas.drawCircle(centerX, centerY, animatedOuterRadius, circlePaint)

        // Draw outer ring
        canvas.drawCircle(centerX, centerY, animatedOuterRadius, outerRingPaint)

        // Draw inner circle (HAL's "eye")
        canvas.drawCircle(centerX, centerY, innerRadius, innerCirclePaint)

        // Draw counter text
        if (counter > 0) {
            canvas.drawText(counter.toString(), centerX, centerY + timerPaint.textSize / 3, timerPaint)
        }

        // Draw instruction text
        canvas.drawText(instruction, centerX, height - instructionPaint.textSize * 1.5f, instructionPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pulseAnimator?.cancel()
    }
}