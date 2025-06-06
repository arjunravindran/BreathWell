package com.example.breathwell.viewmodel

import android.animation.ValueAnimator
import android.app.Application
import android.content.Context
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.graphics.toColorInt
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.breathwell.data.entity.BreathingSession
import com.example.breathwell.data.repository.BreathingSessionRepository
import com.example.breathwell.model.BreathPhase
import com.example.breathwell.model.BreathingPattern
import com.example.breathwell.utils.CountdownController
import com.example.breathwell.utils.PowerSavingMode
import com.example.breathwell.widget.WidgetHelper
import kotlinx.coroutines.launch
import java.time.LocalDate

class BreathingViewModel(
    application: Application,
    private val repository: BreathingSessionRepository,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
) : AndroidViewModel(application), CountdownController.CountdownListener {

    // Keys for saved state
    private companion object {
        const val KEY_EXPANSION = "circle_expansion"
        const val KEY_CURRENT_CYCLE = "current_cycle"
        const val KEY_IS_RUNNING = "is_running"
        const val KEY_PHASE = "breath_phase"
        const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        const val KEY_SOUND_ENABLED = "sound_enabled"
        const val KEY_CUSTOM_INHALE = "custom_inhale"
        const val KEY_CUSTOM_HOLD1 = "custom_hold1"
        const val KEY_CUSTOM_EXHALE = "custom_exhale"
        const val KEY_CUSTOM_HOLD2 = "custom_hold2"
        const val KEY_MUSIC_ENABLED = "music_enabled"
        const val KEY_MUSIC_VOLUME = "music_volume"
    }

    // LiveData to observe in the UI
    private val _activePattern = MutableLiveData(BreathingPattern.BOX_BREATHING)
    val activePattern: LiveData<BreathingPattern> = _activePattern

    private val _breathPhase: MutableLiveData<BreathPhase> = MutableLiveData(
        savedStateHandle[KEY_PHASE] ?: BreathPhase.READY
    )
    val breathPhase: LiveData<BreathPhase> = _breathPhase

    private val _counter = MutableLiveData(0)
    val counter: LiveData<Int> = _counter

    private val _isRunning = MutableLiveData(
        savedStateHandle[KEY_IS_RUNNING] ?: false
    )
    val isRunning: LiveData<Boolean> = _isRunning

    private val _totalCycles = MutableLiveData(5)
    val totalCycles: LiveData<Int> = _totalCycles

    private val _currentCycle = MutableLiveData(
        savedStateHandle[KEY_CURRENT_CYCLE] ?: 0
    )
    val currentCycle: LiveData<Int> = _currentCycle

    // Initialize custom pattern from saved state or default values
    private val _customPattern = MutableLiveData(
        BreathingPattern(
            "Custom",
            savedStateHandle[KEY_CUSTOM_INHALE] ?: 4,
            savedStateHandle[KEY_CUSTOM_HOLD1] ?: 4,
            savedStateHandle[KEY_CUSTOM_EXHALE] ?: 4,
            savedStateHandle[KEY_CUSTOM_HOLD2] ?: 2
        )
    )
    val customPattern = _customPattern

    // Expansion percentage for circle animation (0-100)
    private val _circleExpansion = MutableLiveData(
        savedStateHandle[KEY_EXPANSION] ?: 50f
    )
    val circleExpansion = _circleExpansion

    // Power saving mode
    private val _powerSavingMode = MutableLiveData(PowerSavingMode.NONE)

    // Haptic feedback settings
    private val _vibrationEnabled = MutableLiveData(
        savedStateHandle[KEY_VIBRATION_ENABLED] ?: true
    )
    val vibrationEnabled: LiveData<Boolean> = _vibrationEnabled

    // Sound feedback settings
    private val _soundEnabled = MutableLiveData(
        this.savedStateHandle[KEY_SOUND_ENABLED] ?: true
    )
    val soundEnabled = _soundEnabled

    // Background music settings
    private val _musicEnabled = MutableLiveData(
        savedStateHandle[KEY_MUSIC_ENABLED] ?: true
    )
    val musicEnabled = _musicEnabled

    private val _musicVolume = MutableLiveData(
        savedStateHandle[KEY_MUSIC_VOLUME] ?: 0.3f
    )
    val musicVolume = _musicVolume

    // Phase transition event - used to notify for sound/vibration
    val phaseTransitionEvent = MutableLiveData<BreathPhase?>()

    // New properties for habit tracking
    private val _sessionCompleted = MutableLiveData(false)

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
    private fun saveAnimationState(expansion: Float) {
        savedStateHandle[KEY_EXPANSION] = expansion
        savedStateHandle[KEY_CURRENT_CYCLE] = _currentCycle.value
        savedStateHandle[KEY_IS_RUNNING] = _isRunning.value
        savedStateHandle[KEY_PHASE] = _breathPhase.value
        savedStateHandle[KEY_VIBRATION_ENABLED] = _vibrationEnabled.value
        savedStateHandle[KEY_SOUND_ENABLED] = _soundEnabled.value
    }

    /**
     * Reset breathing phase to READY state
     */
    private fun resetBreathingPhase() {
        _breathPhase.value = BreathPhase.READY
        savedStateHandle[KEY_PHASE] = BreathPhase.READY
        _circleExpansion.value = 50f  // Reset circle to neutral position
        savedStateHandle[KEY_EXPANSION] = 50f
    }

    /**
     * Reset current cycle count to zero
     */
    private fun resetCurrentCycle() {
        _currentCycle.value = 0
        savedStateHandle[KEY_CURRENT_CYCLE] = 0
        _sessionCompleted.value = false
    }

    /**
     * Reset the entire breathing session to its initial state
     */
    fun resetBreathing() {
        // Cancel ongoing countdowns and animations
        countdownController.cancelCountdown()
        circleAnimator?.cancel()
        circleAnimator = null

        // Reset state
        _isRunning.value = false
        this.savedStateHandle[KEY_IS_RUNNING] = false
        resetBreathingPhase()
        resetCurrentCycle()
        _circleExpansion.value = 50f
        savedStateHandle[KEY_EXPANSION] = 50f
        _counter.value = 0

        // Trigger event for reset feedback
        phaseTransitionEvent.value = BreathPhase.READY
        phaseTransitionEvent.value = null
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
        savedStateHandle[KEY_VIBRATION_ENABLED] = _vibrationEnabled.value
    }

    // Toggle sound feedback
    fun toggleSound() {
        _soundEnabled.value = _soundEnabled.value != true
        savedStateHandle[KEY_SOUND_ENABLED] = this._soundEnabled.value
    }

    // Toggle background music on/off
    fun toggleMusic() {
        _musicEnabled.value = _musicEnabled.value != true
        savedStateHandle[KEY_MUSIC_ENABLED] = _musicEnabled.value
    }

    // Set background music volume
    fun setMusicVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        _musicVolume.value = clampedVolume
        this.savedStateHandle[KEY_MUSIC_VOLUME] = clampedVolume
    }

    private fun startBreathing() {
        _isRunning.value = true
        savedStateHandle[KEY_IS_RUNNING] = true

        // Only reset phase if not resuming from config change or if we're complete
        if (_breathPhase.value == BreathPhase.READY || _breathPhase.value == BreathPhase.COMPLETE) {
            _breathPhase.value = BreathPhase.READY
            savedStateHandle[KEY_PHASE] = BreathPhase.READY
            _currentCycle.value = 0
            this.savedStateHandle[KEY_CURRENT_CYCLE] = 0
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
        this.savedStateHandle[KEY_IS_RUNNING] = false
    }

    private fun completeSession() {
        countdownController.cancelCountdown()
        circleAnimator?.cancel()
        circleAnimator = null
        _isRunning.value = false
        savedStateHandle[KEY_IS_RUNNING] = false
        _breathPhase.value = BreathPhase.COMPLETE
        savedStateHandle[KEY_PHASE] = BreathPhase.COMPLETE
        _currentCycle.value = _totalCycles.value ?: 0
        savedStateHandle[KEY_CURRENT_CYCLE] = _currentCycle.value
        _circleExpansion.value = 50f
        savedStateHandle[KEY_EXPANSION] = 50f

        // Trigger phase transition event for completion sound/vibration
        phaseTransitionEvent.value = BreathPhase.COMPLETE
        phaseTransitionEvent.value = null

        // Mark today's session as complete in the database
        saveSessionToDatabase()

        // Update session completed status
        _sessionCompleted.value = true

        // Update app widgets to reflect new streak
        updateWidgets(getApplication())
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

    /**
     * Updates the app widgets when a session is completed
     */
    private fun updateWidgets(context: Context) {
        WidgetHelper.updateWidgets(context)
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
            this.savedStateHandle[KEY_PHASE] = it
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
            this.savedStateHandle[KEY_CURRENT_CYCLE] = _currentCycle.value
        }
    }

    private fun startInhalePhase() {
        val inhaleDuration = _activePattern.value?.inhale ?: 4
        countdownController.startCountdown(inhaleDuration)
        animateCircle(30f, 95f, inhaleDuration * 1000L)
    }

    private fun startHold1Phase() {
        val holdDuration = _activePattern.value?.hold1 ?: 0
        countdownController.startCountdown(holdDuration)
        // Keep expanded at 95%
        _circleExpansion.value = 95f
        savedStateHandle[KEY_EXPANSION] = 95f
    }

    private fun startExhalePhase() {
        val exhaleDuration = _activePattern.value?.exhale ?: 4
        countdownController.startCountdown(exhaleDuration)
        animateCircle(95f, 30f, exhaleDuration * 1000L)
    }

    private fun startHold2Phase() {
        val hold2Duration = _activePattern.value?.hold2 ?: 0
        countdownController.startCountdown(hold2Duration)
        // Keep contracted at 30%
        _circleExpansion.value = 30f
        savedStateHandle[KEY_EXPANSION] = 30f
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
                savedStateHandle[KEY_EXPANSION] = _circleExpansion.value
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

        // Save the custom pattern values to savedStateHandle
        savedStateHandle[KEY_CUSTOM_INHALE] = inhale.coerceIn(1, 10)
        savedStateHandle[KEY_CUSTOM_HOLD1] = hold1.coerceIn(0, 10)
        savedStateHandle[KEY_CUSTOM_EXHALE] = exhale.coerceIn(1, 10)
        savedStateHandle[KEY_CUSTOM_HOLD2] = hold2.coerceIn(0, 10)

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

    // Get HAL circle color based on current breath phase
    fun getBreathColor(): Int {
        return when (_breathPhase.value) {
            BreathPhase.INHALE -> "#38E1FF".toColorInt() // Cyan for inhale (was blue)
            BreathPhase.HOLD1 -> "#38E1FF".toColorInt() // Cyan for hold after inhale (was blue)
            BreathPhase.EXHALE -> "#00D084".toColorInt() // Bright green for exhale
            BreathPhase.HOLD2 -> "#00A66A".toColorInt() // Deeper green for hold after exhale
            else -> "#38E1FF".toColorInt() // Default blue
        }
    }

    // Get inner circle color (darker variation)
    fun getInnerColor(): Int {
        return when (_breathPhase.value) {
            BreathPhase.INHALE -> "#00D4FF".toColorInt() // Darker cyan for inhale (was darker blue)
            BreathPhase.HOLD1 -> "#00D4FF".toColorInt() // Darker cyan for hold after inhale (was darker blue)
            BreathPhase.EXHALE -> "#00A066".toColorInt() // Darker green
            BreathPhase.HOLD2 -> "#007A4D".toColorInt() // Even darker green
            else -> "#00D4FF".toColorInt() // Default darker blue
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownController.cleanup()
        circleAnimator?.cancel()
        circleAnimator = null
    }
}