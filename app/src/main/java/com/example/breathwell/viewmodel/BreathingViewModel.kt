package com.example.breathwell.viewmodel

import android.animation.ValueAnimator
import android.view.animation.AccelerateDecelerateInterpolator
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
import com.example.breathwell.utils.CountdownController
import com.example.breathwell.utils.PowerSavingMode
import kotlinx.coroutines.launch
import java.time.LocalDate

class BreathingViewModel(
    private val repository: BreathingSessionRepository,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
) : ViewModel(), CountdownController.CountdownListener {

    // Keys for saved state
    private companion object {
        const val KEY_EXPANSION = "circle_expansion"
        const val KEY_CURRENT_CYCLE = "current_cycle"
        const val KEY_IS_RUNNING = "is_running"
        const val KEY_PHASE = "breath_phase"
        const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        const val KEY_SOUND_ENABLED = "sound_enabled"
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

    // Haptic feedback settings
    private val _vibrationEnabled = MutableLiveData<Boolean>(
        savedStateHandle.get(KEY_VIBRATION_ENABLED) ?: true
    )
    val vibrationEnabled: LiveData<Boolean> = _vibrationEnabled

    // Sound feedback settings
    private val _soundEnabled = MutableLiveData<Boolean>(
        savedStateHandle.get(KEY_SOUND_ENABLED) ?: true
    )
    val soundEnabled: LiveData<Boolean> = _soundEnabled

    // Phase transition event - used to notify for sound/vibration
    val phaseTransitionEvent = MutableLiveData<BreathPhase?>()

    // New properties for habit tracking
    private val _sessionCompleted = MutableLiveData<Boolean>(false)
    val sessionCompleted: LiveData<Boolean> = _sessionCompleted

    val completedDates: LiveData<List<LocalDate>> = repository.getAllCompletedDates()

    // Countdown controller
    private val countdownController = CountdownController()

    // Circle animator
    private var circleAnimator: ValueAnimator? = null

    init {
        // Setup countdown controller
        countdownController.setListener(this)
    }

    /**
     * Callback when countdown ticks
     */
    override fun onTick(secondsRemaining: Int) {
        _counter.value = secondsRemaining
    }

    /**
     * Callback when countdown phase completes
     */
    override fun onPhaseComplete() {
        advanceToNextPhase()
    }

    /**
     * Save animation state for restoration after config changes
     */
    fun saveAnimationState(expansion: Float) {
        savedStateHandle.set(KEY_EXPANSION, expansion)
        savedStateHandle.set(KEY_CURRENT_CYCLE, _currentCycle.value)
        savedStateHandle.set(KEY_IS_RUNNING, _isRunning.value)
        savedStateHandle.set(KEY_PHASE, _breathPhase.value)
        savedStateHandle.set(KEY_VIBRATION_ENABLED, _vibrationEnabled.value)
        savedStateHandle.set(KEY_SOUND_ENABLED, _soundEnabled.value)
    }

    /**
     * Reset breathing phase to READY state
     */
    fun resetBreathingPhase() {
        _breathPhase.value = BreathPhase.READY
        savedStateHandle.set(KEY_PHASE, BreathPhase.READY)
        _circleExpansion.value = 50f  // Reset circle to neutral position
        savedStateHandle.set(KEY_EXPANSION, 50f)
    }

    /**
     * Reset current cycle count to zero
     */
    fun resetCurrentCycle() {
        _currentCycle.value = 0
        savedStateHandle.set(KEY_CURRENT_CYCLE, 0)
        _sessionCompleted.value = false
    }

    // Set power saving mode
    fun setPowerSavingMode(mode: PowerSavingMode) {
        if (_powerSavingMode.value != mode) {
            _powerSavingMode.value = mode
            countdownController.setPowerSavingMode(mode)
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

    // Toggle vibration feedback
    fun toggleVibration() {
        _vibrationEnabled.value = _vibrationEnabled.value != true
        savedStateHandle.set(KEY_VIBRATION_ENABLED, _vibrationEnabled.value)
    }

    // Toggle sound feedback
    fun toggleSound() {
        _soundEnabled.value = _soundEnabled.value != true
        savedStateHandle.set(KEY_SOUND_ENABLED, _soundEnabled.value)
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
        countdownController.cancelCountdown()
        circleAnimator?.cancel()
        circleAnimator = null

        // Save state before stopping
        saveAnimationState(_circleExpansion.value ?: 50f)

        _isRunning.value = false
        savedStateHandle.set(KEY_IS_RUNNING, false)
    }

    private fun completeSession() {
        countdownController.cancelCountdown()
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

        // Trigger phase transition event for completion sound/vibration
        phaseTransitionEvent.value = BreathPhase.COMPLETE
        phaseTransitionEvent.value = null

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
        val currentPhase = _breathPhase.value ?: BreathPhase.READY
        var nextPhase: BreathPhase? = null

        when (currentPhase) {
            BreathPhase.READY -> {
                nextPhase = BreathPhase.INHALE
                startInhalePhase()
            }
            BreathPhase.INHALE -> {
                if ((_activePattern.value?.hold1 ?: 0) > 0) {
                    nextPhase = BreathPhase.HOLD1
                    startHold1Phase()
                } else {
                    nextPhase = BreathPhase.EXHALE
                    startExhalePhase()
                }
            }
            BreathPhase.HOLD1 -> {
                nextPhase = BreathPhase.EXHALE
                startExhalePhase()
            }
            BreathPhase.EXHALE -> {
                if ((_activePattern.value?.hold2 ?: 0) > 0) {
                    nextPhase = BreathPhase.HOLD2
                    startHold2Phase()
                } else {
                    advanceCycle()
                    if (_currentCycle.value == _totalCycles.value) {
                        nextPhase = BreathPhase.COMPLETE
                    } else {
                        nextPhase = BreathPhase.INHALE
                        startInhalePhase()
                    }
                }
            }
            BreathPhase.HOLD2 -> {
                advanceCycle()
                if (_currentCycle.value == _totalCycles.value) {
                    nextPhase = BreathPhase.COMPLETE
                } else {
                    nextPhase = BreathPhase.INHALE
                    startInhalePhase()
                }
            }
            BreathPhase.COMPLETE -> {
                // No action for COMPLETE
            }
        }

        // Trigger phase transition event for sound/vibration
        nextPhase?.let {
            _breathPhase.value = it
            savedStateHandle.set(KEY_PHASE, it)
            phaseTransitionEvent.value = it
            phaseTransitionEvent.value = null
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
        }
    }

    private fun startInhalePhase() {
        val inhaleDuration = _activePattern.value?.inhale ?: 4
        countdownController.startCountdown(inhaleDuration)
        animateCircle(30f, 95f, _activePattern.value?.inhale?.times(1000L) ?: 4000L)
    }

    private fun startHold1Phase() {
        val holdDuration = _activePattern.value?.hold1 ?: 0
        countdownController.startCountdown(holdDuration)
        // Keep expanded at 95%
        _circleExpansion.value = 95f
        savedStateHandle.set(KEY_EXPANSION, 95f)
    }

    private fun startExhalePhase() {
        val exhaleDuration = _activePattern.value?.exhale ?: 4
        countdownController.startCountdown(exhaleDuration)
        animateCircle(95f, 30f, _activePattern.value?.exhale?.times(1000L) ?: 4000L)
    }

    private fun startHold2Phase() {
        val hold2Duration = _activePattern.value?.hold2 ?: 0
        countdownController.startCountdown(hold2Duration)
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
        countdownController.cleanup()
        circleAnimator?.cancel()
        circleAnimator = null
    }
}