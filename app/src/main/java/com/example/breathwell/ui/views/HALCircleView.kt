package com.example.breathwell.ui.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.min
import androidx.core.graphics.toColorInt

class HALCircleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Paint objects for various components
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = "#38E1FF".toColorInt() // Cyan color
    }

    private val outerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(2f)
        color = Color.WHITE
        alpha = 80 // 30% opacity
    }

    private val innerCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = "#00D4FF".toColorInt() // Darker cyan
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val timerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = spToPx(64f) // Larger text size for better visibility
        typeface = Typeface.DEFAULT_BOLD
    }

    private val instructionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
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
    private var animationDuration = 1500L // Default animation duration
    private var wasAnimating = false

    // Public properties
    var expansion: Float = 50f
        set(value) {
            field = value
            invalidate()
        }

    var breathColor: Int = "#38E1FF".toColorInt() // Cyan
        set(value) {
            field = value
            circlePaint.color = value
            invalidate()
        }

    var innerColor: Int = "#00D4FF".toColorInt() // Darker cyan
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
            if (field != value) {
                field = value
                if (value) {
                    startPulseAnimation()
                } else {
                    pulseAnimator?.cancel()
                    pulseScale = 1.0f
                }
                invalidate()
            }
        }

    // Flag to control if timer is visible (only during active sessions)
    var showTimer: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    /**
     * Pause all ongoing animations
     */
    fun pauseAnimations() {
        wasAnimating = pulseAnimator?.isRunning ?: false
        pulseAnimator?.pause()
    }

    /**
     * Resume animations if they were running before
     */
    fun resumeAnimations() {
        if (wasAnimating) {
            pulseAnimator?.resume()
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

        pulseAnimator = ValueAnimator.ofFloat(1.0f, 1.15f).apply {
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
        // Adjust base radius to be smaller (75% of original)
        val baseRadius = minDimension * 0.25f

        // More conservative expansion range to prevent overflow
        // Map expansion from 0-100 to a smaller range
        val expansionFactor = (expansion - 50f) / 150f // Less dramatic expansion
        val outerRadius = baseRadius * (1.0f + expansionFactor.coerceIn(-0.3f, 0.3f))
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

        // Draw counter text only if showTimer is true and counter > 0
        if (showTimer && counter > 0) {
            // Draw shadow behind text for better contrast
            val shadowPaint = Paint(timerPaint)
            shadowPaint.setShadowLayer(15f, 0f, 0f, Color.BLACK)
            canvas.drawText(counter.toString(), centerX, centerY + timerPaint.textSize / 3, shadowPaint)

            // Draw the actual counter text
            canvas.drawText(counter.toString(), centerX, centerY + timerPaint.textSize / 3, timerPaint)
        }

        // Draw instruction text
        canvas.drawText(instruction, centerX, height - instructionPaint.textSize * 1.5f, instructionPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pulseAnimator?.cancel()
        pulseAnimator = null
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val savedState = SavedState(superState)
        savedState.expansion = expansion
        savedState.breathColor = breathColor
        savedState.innerColor = innerColor
        savedState.counter = counter
        savedState.instruction = instruction
        savedState.showPulseEffect = showPulseEffect
        savedState.pulseScale = pulseScale
        savedState.showTimer = showTimer
        return savedState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        when (state) {
            is SavedState -> {
                super.onRestoreInstanceState(state.superState)
                expansion = state.expansion
                breathColor = state.breathColor
                innerColor = state.innerColor
                counter = state.counter
                instruction = state.instruction
                pulseScale = state.pulseScale
                showTimer = state.showTimer
                // Don't immediately start animation, will be handled by showPulseEffect setter
                this.showPulseEffect = state.showPulseEffect
            }
            else -> super.onRestoreInstanceState(state)
        }
    }

    private class SavedState : BaseSavedState {
        var expansion: Float = 50f
        var breathColor: Int = "#38E1FF".toColorInt() // Cyan
        var innerColor: Int = "#00D4FF".toColorInt() // Darker cyan
        var counter: Int = 0
        var instruction: String = "READY"
        var showPulseEffect: Boolean = false
        var pulseScale: Float = 1.0f
        var showTimer: Boolean = false

        constructor(superState: Parcelable?) : super(superState)

        constructor(parcel: Parcel) : super(parcel) {
            expansion = parcel.readFloat()
            breathColor = parcel.readInt()
            innerColor = parcel.readInt()
            counter = parcel.readInt()
            instruction = parcel.readString() ?: "READY"
            showPulseEffect = parcel.readInt() == 1
            pulseScale = parcel.readFloat()
            showTimer = parcel.readInt() == 1
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeFloat(expansion)
            out.writeInt(breathColor)
            out.writeInt(innerColor)
            out.writeInt(counter)
            out.writeString(instruction)
            out.writeInt(if (showPulseEffect) 1 else 0)
            out.writeFloat(pulseScale)
            out.writeInt(if (showTimer) 1 else 0)
        }

        companion object {
            @JvmField
            val CREATOR = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(source: Parcel): SavedState = SavedState(source)
                override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
            }
        }
    }
}