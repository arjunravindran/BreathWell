package com.example.breathwell.viewmodel

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.CountDownTimer
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.core.graphics.toColorInt
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.breathwell.data.entity.BreathingSession
import com.example.breathwell.data.repository.BreathingSessionRepository
import com.example.breathwell.model.BreathPhase
import com.example.breathwell.model.BreathingPattern
import com.example.breathwell.utils.PowerSavingMode
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.pow

class BreathingViewModel(
    private val repository: BreathingSessionRepository,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
) : ViewModel() {

    // Keys for saved state
    private companion object {
        const val KEY_EXPANSION = "circle_expansion"
        const val KEY_PULSE_EFFECT = "show_pulse_effect"
        const val KEY_CIRCLE_STATE = "circle_state"
        const val KEY_CURRENT_CYCLE = "current_cycle"
        const val KEY_IS_RUNNING = "is_running"
        const val KEY_PHASE = "breath_phase"
    }

    // LiveData to observe in the UI
    private val _activePattern = MutableLiveData<BreathingPattern>(BreathingPattern.BOX_BREATHING)
    val activePattern: LiveData<BreathingPattern> = _activePattern

    private val _breathPhase = MutableLiveData<BreathPhase>(
        savedStateHandle.get(KEY_PHASE) ?: BreathPhase.READY
    )
    val breathPhase: LiveData<BreathPhase> = _breathPhase

    private val _counter = MutableLiveData<Int>(0)
    val counter: LiveData<Int> = _counter

    private val _isRunning = MutableLiveData<Boolean>(
        savedStateHandle.get(KEY_IS_RUNNING) ?: false
    )
    val isRunning: LiveData<Boolean> = _isRunning

    private val _totalCycles = MutableLiveData<Int>(5)
    val totalCycles: LiveData<Int> = _totalCycles

    private val _currentCycle = MutableLiveData<Int>(
        savedStateHandle.get(KEY_CURRENT_CYCLE) ?: 0
    )
    val currentCycle: LiveData<Int> = _currentCycle

    private val _customPattern = MutableLiveData<BreathingPattern>(BreathingPattern.CUSTOM)
    val customPattern: LiveData<BreathingPattern> = _customPattern

    // Expansion percentage for circle animation (0-100)
    private val _circleExpansion = MutableLiveData<Float>(
        savedStateHandle.get(KEY_EXPANSION) ?: 50f
    )
    val circleExpansion: LiveData<Float> = _circleExpansion

    // Power saving mode
    private val _powerSavingMode = MutableLiveData<PowerSavingMode>(PowerSavingMode.NONE)
    val powerSavingMode: LiveData<PowerSavingMode> = _powerSavingMode

    // Animation state properties for restoration
    private var wasAnimatingBeforeStop = false
    private var lastAnimationTimestamp = 0L

    // New properties for habit tracking
    private val _sessionCompleted = MutableLiveData<Boolean>(false)
    val sessionCompleted: LiveData<Boolean> = _sessionCompleted

    val completedDates: LiveData<List<LocalDate>> = repository.getAllCompletedDates()

    // Countdown timer
    private var timer: CountDownTimer? = null

    // Circle animator
    private var circleAnimator: ValueAnimator? = null

    /**
     * Save animation state for restoration after config changes
     */
    fun saveAnimationState(expansion: Float, showPulseEffect: Boolean) {
        savedStateHandle.set(KEY_EXPANSION, expansion)
        savedStateHandle.set(KEY_PULSE_EFFECT, showPulseEffect)
        savedStateHandle.set(KEY_CIRCLE_STATE, pulseScale)
        savedStateHandle.set(KEY_CURRENT_CYCLE, _currentCycle.value)
        savedStateHandle.set(KEY_IS_RUNNING, _isRunning.value)
        savedStateHandle.set(KEY_PHASE, _breathPhase.value)
    }

    /**
     * Animation state for restoring after config changes
     */
    private var pulseScale: Float = 1.0f

    // Set power saving mode
    fun setPowerSavingMode(mode: PowerSavingMode) {
        if (_powerSavingMode.value != mode) {
            _powerSavingMode.value = mode
        }
    }

    // Start/stop the breathing exercise
    fun toggleBreathing() {
        if (_isRunning.value == true) {
            stopBreathing()
        } else {
            startBreathing()
        }
    }

    private fun startBreathing() {
        _isRunning.value = true
        savedStateHandle.set(KEY_IS_RUNNING, true)

        // Only reset phase if not resuming from config change or if we're complete
        if (_breathPhase.value == BreathPhase.READY || _breathPhase.value == BreathPhase.COMPLETE) {
            _breathPhase.value = BreathPhase.READY
            savedStateHandle.set(KEY_PHASE, BreathPhase.READY)
            _currentCycle.value = 0
            savedStateHandle.set(KEY_CURRENT_CYCLE, 0)
            _sessionCompleted.value = false
        }

        advanceToNextPhase()
    }

    private fun stopBreathing() {
        wasAnimatingBeforeStop = timer != null || circleAnimator != null
        timer?.cancel()
        timer = null
        circleAnimator?.cancel()
        circleAnimator = null

        // Save state before stopping
        if (wasAnimatingBeforeStop) {
            lastAnimationTimestamp = System.currentTimeMillis()
            saveAnimationState(_circleExpansion.value ?: 50f,
                _breathPhase.value == BreathPhase.INHALE || _breathPhase.value == BreathPhase.EXHALE)
        }

        _isRunning.value = false
        savedStateHandle.set(KEY_IS_RUNNING, false)
    }

    private fun completeSession() {
        timer?.cancel()
        timer = null
        circleAnimator?.cancel()
        circleAnimator = null
        _isRunning.value = false
        savedStateHandle.set(KEY_IS_RUNNING, false)
        _breathPhase.value = BreathPhase.COMPLETE
        savedStateHandle.set(KEY_PHASE, BreathPhase.COMPLETE)
        _currentCycle.value = _totalCycles.value ?: 0
        savedStateHandle.set(KEY_CURRENT_CYCLE, _currentCycle.value)
        _circleExpansion.value = 50f
        savedStateHandle.set(KEY_EXPANSION, 50f)

        // Mark today's session as complete in the database
        saveSessionToDatabase()

        // Update session completed status
        _sessionCompleted.value = true
    }

    private fun saveSessionToDatabase() {
        val activePattern = _activePattern.value ?: BreathingPattern.BOX_BREATHING
        val totalCycles = _totalCycles.value ?: 5

        // Calculate total session duration in seconds
        val cycleDuration = (activePattern.inhale + activePattern.hold1 +
                activePattern.exhale + activePattern.hold2)
        val totalDuration = cycleDuration * totalCycles

        viewModelScope.launch {
            val session = BreathingSession(
                date = LocalDate.now(),
                patternName = activePattern.name,
                cycles = totalCycles,
                durationSeconds = totalDuration,
                completed = true
            )
            repository.insertSession(session)
        }
    }

    private fun advanceToNextPhase() {
        when (_breathPhase.value) {
            BreathPhase.READY -> startInhalePhase()
            BreathPhase.INHALE -> {
                if ((_activePattern.value?.hold1 ?: 0) > 0) {
                    startHold1Phase()
                } else {
                    startExhalePhase()
                }
            }
            BreathPhase.HOLD1 -> startExhalePhase()
            BreathPhase.EXHALE -> {
                if ((_activePattern.value?.hold2 ?: 0) > 0) {
                    startHold2Phase()
                } else {
                    advanceCycle()
                }
            }
            BreathPhase.HOLD2 -> advanceCycle()
            else -> {}  // No action for COMPLETE
        }
    }

    private fun advanceCycle() {
        val currentCycleValue = _currentCycle.value ?: 0
        val totalCyclesValue = _totalCycles.value ?: 5

        if (currentCycleValue >= totalCyclesValue - 1) {
            completeSession()
        } else {
            _currentCycle.value = currentCycleValue + 1
            savedStateHandle.set(KEY_CURRENT_CYCLE, _currentCycle.value)
            startInhalePhase()
        }
    }

    private fun startInhalePhase() {
        _breathPhase.value = BreathPhase.INHALE
        savedStateHandle.set(KEY_PHASE, BreathPhase.INHALE)
        startCountdown(_activePattern.value?.inhale ?: 4)
        animateCircle(30f, 95f, _activePattern.value?.inhale?.times(1000L) ?: 4000L)
    }

    private fun startHold1Phase() {
        _breathPhase.value = BreathPhase.HOLD1
        savedStateHandle.set(KEY_PHASE, BreathPhase.HOLD1)
        startCountdown(_activePattern.value?.hold1 ?: 0)
        // Keep expanded at 95%
        _circleExpansion.value = 95f
        savedStateHandle.set(KEY_EXPANSION, 95f)
    }

    private fun startExhalePhase() {
        _breathPhase.value = BreathPhase.EXHALE
        savedStateHandle.set(KEY_PHASE, BreathPhase.EXHALE)
        startCountdown(_activePattern.value?.exhale ?: 4)
        animateCircle(95f, 30f, _activePattern.value?.exhale?.times(1000L) ?: 4000L)
    }

    private fun startHold2Phase() {
        _breathPhase.value = BreathPhase.HOLD2
        savedStateHandle.set(KEY_PHASE, BreathPhase.HOLD2)
        startCountdown(_activePattern.value?.hold2 ?: 0)
        // Keep contracted at 30%
        _circleExpansion.value = 30f
        savedStateHandle.set(KEY_EXPANSION, 30f)
    }

    private fun animateCircle(from: Float, to: Float, duration: Long) {
        // Cancel any running animation
        circleAnimator?.cancel()
        circleAnimator = null

        // Make the range wider for more dramatic effect
        val adjustedFrom = if (from < 50f) from * 0.8f else from  // Inhale starts from smaller size
        val adjustedTo = if (to > 50f) to * 1.1f else to  // Exhale goes to larger size

        // Adjust animation duration based on power saving mode
        val adjustedDuration = when (_powerSavingMode.value) {
            PowerSavingMode.HIGH -> duration * 1.5
            PowerSavingMode.MEDIUM -> duration * 1.2
            else -> duration
        }

        // Animation logic to update _circleExpansion with smoother easing
        circleAnimator = ValueAnimator.ofFloat(adjustedFrom, adjustedTo).apply {
            this.duration = adjustedDuration.toLong()
            addUpdateListener { animation ->
                _circleExpansion.value = animation.animatedValue as Float
                savedStateHandle.set(KEY_EXPANSION, _circleExpansion.value)
            }

            // Use AccelerateDecelerateInterpolator for smoother start/stop
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun startCountdown(seconds: Int) {
        _counter.value = seconds

        timer?.cancel()
        timer = null

        // Adjust tick frequency based on power saving mode
        val tickInterval = when (_powerSavingMode.value) {
            PowerSavingMode.HIGH -> 1000L
            PowerSavingMode.MEDIUM -> 500L
            else -> 200L
        }

        timer = object : CountDownTimer(seconds * 1000L, tickInterval) {
            override fun onTick(millisUntilFinished: Long) {
                _counter.value = (millisUntilFinished / 1000).toInt() + 1
            }

            override fun onFinish() {
                advanceToNextPhase()
            }
        }.start()
    }

    // Settings methods
    fun setActivePattern(pattern: BreathingPattern) {
        if (_isRunning.value != true) {
            _activePattern.value = pattern
        }
    }

    fun setTotalCycles(cycles: Int) {
        if (_isRunning.value != true) {
            _totalCycles.value = cycles
        }
    }

    fun updateCustomPattern(inhale: Int, hold1: Int, exhale: Int, hold2: Int) {
        val updated = BreathingPattern(
            "Custom",
            inhale.coerceIn(1, 10),
            hold1.coerceIn(0, 10),
            exhale.coerceIn(1, 10),
            hold2.coerceIn(0, 10)
        )
        _customPattern.value = updated

        // If custom pattern is currently selected, update the active pattern too
        if (_activePattern.value?.name == "Custom") {
            _activePattern.value = updated
        }
    }

    // Habit tracking methods
    fun isDateCompleted(date: LocalDate): LiveData<Boolean> {
        val sessions = repository.getSessionForDate(date)
        return sessions.map { it.isNotEmpty() && it.any { session -> session.completed } }
    }

    fun getCurrentStreak(): LiveData<Int> {
        // Calculate consecutive completed days up to today
        return completedDates.map { dates ->
            var streak = 0
            var currentDate = LocalDate.now()

            while (dates.contains(currentDate)) {
                streak++
                currentDate = currentDate.minusDays(1)
            }

            streak
        }
    }

    fun getCompletionCountForRange(startDate: LocalDate, endDate: LocalDate): LiveData<Int> {
        return repository.getCompletionCountForRange(startDate, endDate)
    }

    // Get HAL circle color based on current breath phase
    fun getBreathColor(): Int {
        return when (_breathPhase.value) {
            BreathPhase.INHALE -> "#00A6ED".toColorInt() // Bright blue for inhale
            BreathPhase.HOLD1 -> "#0080B3".toColorInt() // Deeper blue for hold after inhale
            BreathPhase.EXHALE -> "#00D084".toColorInt() // Bright green for exhale
            BreathPhase.HOLD2 -> "#00A66A".toColorInt() // Deeper green for hold after exhale
            else -> "#4682B4".toColorInt() // Default blue
        }
    }

    // Get inner circle color (darker variation)
    fun getInnerColor(): Int {
        return when (_breathPhase.value) {
            BreathPhase.INHALE -> "#0076AD".toColorInt() // Darker blue
            BreathPhase.HOLD1 -> "#005580".toColorInt() // Even darker blue
            BreathPhase.EXHALE -> "#00A066".toColorInt() // Darker green
            BreathPhase.HOLD2 -> "#007A4D".toColorInt() // Even darker green
            else -> "#2C5984".toColorInt() // Default darker blue
        }
    }

    // Get background gradient colors based on current phase
    fun getBackgroundColors(): Pair<Int, Int> {
        return when (_breathPhase.value) {
            BreathPhase.INHALE, BreathPhase.HOLD1 -> {
                Pair(
                    "#0A0A14".toColorInt(), // Very dark blue-black
                    "#0A1A30".toColorInt()  // Dark blue tint
                )
            }
            BreathPhase.EXHALE, BreathPhase.HOLD2 -> {
                Pair(
                    "#0A0A14".toColorInt(), // Very dark blue-black
                    "#0A2414".toColorInt()  // Dark green tint
                )
            }
            else -> {
                Pair(
                    "#0A0A14".toColorInt(), // Very dark blue-black
                    "#121218".toColorInt()  // Slightly lighter dark
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timer?.cancel()
        timer = null
        circleAnimator?.cancel()
        circleAnimator = null
    }
}