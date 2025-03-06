package com.example.breathwell.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.widget.OverScroller
import androidx.core.graphics.withTranslation
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CyclesPicker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val cycles = (1..10).map { it.toString() }
    private var selectedCycleIndex = 4 // Default to 5 cycles (index 4)
    
    private val textPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        color = Color.WHITE
    }
    
    private val selectedTextPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        color = Color.WHITE
    }
    
    private val backgroundPaint = Paint().apply {
        isAntiAlias = true
        color = Color.parseColor("#33000000") // Semi-transparent background
    }
    
    private val selectedBackgroundPaint = Paint().apply {
        isAntiAlias = true
        color = Color.parseColor("#44FFFFFF") // Slightly lighter for selected item
    }
    
    private val scroller = OverScroller(context, DecelerateInterpolator())
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var lastMotionX = 0f
    private var isDragging = false
    
    private var itemWidth = 0f
    private var currentScrollX = 0f
    private var cycleChangeListener: ((Int) -> Unit)? = null

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }
    
    fun setOnCycleChangeListener(listener: (Int) -> Unit) {
        cycleChangeListener = listener
    }
    
    fun setSelectedCycle(cycle: Int) {
        if (cycle in 1..10) {
            selectedCycleIndex = cycle - 1
            currentScrollX = selectedCycleIndex * itemWidth
            invalidate()
            cycleChangeListener?.invoke(cycle)
        }
    }
    
    fun getSelectedCycle(): Int = selectedCycleIndex + 1

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = suggestedMinimumWidth + paddingLeft + paddingRight
        val desiredHeight = 150 // Fixed height
        
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        
        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> min(desiredWidth, widthSize)
            else -> desiredWidth
        }
        
        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> min(desiredHeight, heightSize)
            else -> desiredHeight
        }
        
        setMeasuredDimension(width, height)
        
        // Calculate item width based on view width
        itemWidth = width / 3f
        
        // Update text size based on view size
        val textSize = height * 0.25f
        textPaint.textSize = textSize
        selectedTextPaint.textSize = textSize * 1.2f
        
        // Initialize scroll position to selected cycle
        currentScrollX = selectedCycleIndex * itemWidth
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val centerY = height / 2f
        val selectedItemX = width / 2f
        
        // Calculate the offset to ensure the selected item is centered
        val offset = selectedItemX - currentScrollX - itemWidth / 2
        
        // Draw visible cycles
        val startIndex = max(0, (currentScrollX / itemWidth).toInt() - 1)
        val endIndex = min(cycles.size - 1, (currentScrollX / itemWidth).toInt() + 3)
        
        for (i in startIndex..endIndex) {
            val itemCenterX = i * itemWidth + itemWidth / 2 + offset
            
            // Draw only if the item is visible
            if (itemCenterX + itemWidth / 2 >= 0 && itemCenterX - itemWidth / 2 <= width) {
                val isSelected = i == selectedCycleIndex
                
                // Calculate position and size for the cycle item
                val rect = RectF(
                    itemCenterX - itemWidth * 0.45f,
                    centerY - height * 0.3f,
                    itemCenterX + itemWidth * 0.45f,
                    centerY + height * 0.3f
                )
                
                // Draw background
                canvas.drawRoundRect(
                    rect,
                    20f, 20f,
                    if (isSelected) selectedBackgroundPaint else backgroundPaint
                )
                
                // Draw cycle number
                canvas.withTranslation(
                    x = itemCenterX,
                    y = centerY + (if (isSelected) selectedTextPaint else textPaint).textSize / 3
                ) {
                    drawText(
                        cycles[i],
                        0f, 0f,
                        if (isSelected) selectedTextPaint else textPaint
                    )
                }
            }
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) {
            return false
        }
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!scroller.isFinished) {
                    scroller.abortAnimation()
                }
                lastMotionX = event.x
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                val x = event.x
                val deltaX = lastMotionX - x
                
                if (!isDragging && abs(deltaX) > touchSlop) {
                    isDragging = true
                }
                
                if (isDragging) {
                    currentScrollX = (currentScrollX + deltaX).coerceIn(0f, (cycles.size - 1) * itemWidth)
                    lastMotionX = x
                    invalidate()
                }
                return true
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    
                    // Calculate the closest cycle to snap to
                    val nearestCycleIndex = (currentScrollX / itemWidth).toInt().coerceIn(0, cycles.size - 1)
                    val nearestCycleScrollX = nearestCycleIndex * itemWidth
                    
                    // Apply fling if there's sufficient velocity
                    if (abs(currentScrollX - nearestCycleScrollX) > 0.1f) {
                        scroller.startScroll(
                            currentScrollX.toInt(),
                            0,
                            (nearestCycleScrollX - currentScrollX).toInt(),
                            0,
                            300
                        )
                        postInvalidateOnAnimation()
                    }
                    
                    // Update selected cycle
                    if (selectedCycleIndex != nearestCycleIndex) {
                        selectedCycleIndex = nearestCycleIndex
                        cycleChangeListener?.invoke(getSelectedCycle())
                    }
                } else {
                    // Handle tap (select cycle without dragging)
                    val x = event.x
                    val selectedX = x - (width / 2f - currentScrollX - itemWidth / 2)
                    val tappedIndex = (selectedX / itemWidth).toInt().coerceIn(0, cycles.size - 1)
                    
                    if (selectedCycleIndex != tappedIndex) {
                        setSelectedCycle(tappedIndex + 1)
                    }
                }
                return true
            }
        }
        
        return super.onTouchEvent(event)
    }
    
    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            currentScrollX = scroller.currX.toFloat()
            invalidate()
        }
    }
}