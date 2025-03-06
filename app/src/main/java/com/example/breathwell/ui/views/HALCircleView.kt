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
import com.example.breathwell.utils.AnimationQuality
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
    private var animationQuality = AnimationQuality.FULL
    private var animationDuration = 1500L // Default animation duration

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

    // Set animation quality based on power saving mode
    fun setAnimationQuality(quality: AnimationQuality) {
        if (animationQuality == quality) return

        animationQuality = quality

        // Adjust animation parameters based on quality
        when (quality) {
            AnimationQuality.FULL -> {
                animationDuration = 1500L
                instructionPaint.alpha = 230
                timerPaint.alpha = 230
            }
            AnimationQuality.REDUCED -> {
                animationDuration = 2000L
                instructionPaint.alpha = 220
                timerPaint.alpha = 220
            }
            AnimationQuality.MINIMAL -> {
                animationDuration = 2500L
                instructionPaint.alpha = 210
                timerPaint.alpha = 210
            }
        }

        // Update ongoing animation if needed
        if (showPulseEffect) {
            pulseAnimator?.cancel()
            startPulseAnimation()
        }
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

        // Adjust animation based on quality
        val amplitude = when (animationQuality) {
            AnimationQuality.FULL -> 0.15f
            AnimationQuality.REDUCED -> 0.10f
            AnimationQuality.MINIMAL -> 0.05f
        }

        pulseAnimator = ValueAnimator.ofFloat(1.0f, 1.0f + amplitude).apply {
            duration = animationDuration
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
        // Increase expansion effect by using a larger base radius
        val baseRadius = minDimension * 0.3f // Changed from 0.245f for more dramatic expansion
        // Make expansion range wider
        val outerRadius = baseRadius * (1.0f + (expansion - 50f) / 120f) // Changed from 150f for wider range
        val innerRadius = outerRadius * 0.6f

        // Apply pulse animation
        val animatedOuterRadius = outerRadius * pulseScale

        // Only draw glow in FULL and REDUCED quality
        if (animationQuality != AnimationQuality.MINIMAL) {
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
        }

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