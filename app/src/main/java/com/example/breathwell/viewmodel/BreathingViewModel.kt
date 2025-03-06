package com.example.breathwell.viewmodel

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.CountDownTimer
import android.view.animation.LinearInterpolator
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.breathwell.data.entity.BreathingSession
import com.example.breathwell.data.repository.BreathingSessionRepository
import com.example.breathwell.model.BreathPhase
import com.example.breathwell.model.BreathingPattern
import kotlinx.coroutines.launch
import java.time.LocalDate

class BreathingViewModel(
    private val repository: BreathingSessionRepository
) : ViewModel() {

    // LiveData to observe in the UI
    private val _activePattern = MutableLiveData<BreathingPattern>(BreathingPattern.BOX_BREATHING)
    val activePattern: LiveData<BreathingPattern> = _activePattern

    private val _breathPhase = MutableLiveData<BreathPhase>(BreathPhase.READY)
    val breathPhase: LiveData<BreathPhase> = _breathPhase

    private val _counter = MutableLiveData<Int>(0)
    val counter: LiveData<Int> = _counter

    private val _isRunning = MutableLiveData<Boolean>(false)
    val isRunning: LiveData<Boolean> = _isRunning

    private val _totalCycles = MutableLiveData<Int>(5)
    val totalCycles: LiveData<Int> = _totalCycles

    private val _currentCycle = MutableLiveData<Int>(0)
    val currentCycle: LiveData<Int> = _currentCycle

    private val _customPattern = MutableLiveData<BreathingPattern>(BreathingPattern.CUSTOM)
    val customPattern: LiveData<BreathingPattern> = _customPattern

    // Expansion percentage for circle animation (0-100)
    private val _circleExpansion = MutableLiveData<Float>(50f)
    val circleExpansion: LiveData<Float> = _circleExpansion

    // New properties for habit tracking
    private val _sessionCompleted = MutableLiveData<Boolean>(false)
    val sessionCompleted: LiveData<Boolean> = _sessionCompleted

    val completedDates: LiveData<List<LocalDate>> = repository.getAllCompletedDates()

    // Countdown timer
    private var timer: CountDownTimer? = null

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
        _breathPhase.value = BreathPhase.READY
        _currentCycle.value = 0
        _sessionCompleted.value = false
        advanceToNextPhase()
    }

    private fun stopBreathing() {
        timer?.cancel()
        _isRunning.value = false
        _breathPhase.value = BreathPhase.READY
        _currentCycle.value = 0
        _circleExpansion.value = 50f
    }

    private fun completeSession() {
        timer?.cancel()
        _isRunning.value = false
        _breathPhase.value = BreathPhase.COMPLETE
        _currentCycle.value = _totalCycles.value ?: 0
        _circleExpansion.value = 50f

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
                if (_activePattern.value?.hold1 ?: 0 > 0) {
                    startHold1Phase()
                } else {
                    startExhalePhase()
                }
            }
            BreathPhase.HOLD1 -> startExhalePhase()
            BreathPhase.EXHALE -> {
                if (_activePattern.value?.hold2 ?: 0 > 0) {
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
            startInhalePhase()
        }
    }

    private fun startInhalePhase() {
        _breathPhase.value = BreathPhase.INHALE
        startCountdown(_activePattern.value?.inhale ?: 4)
        animateCircle(30f, 95f, _activePattern.value?.inhale?.times(1000L) ?: 4000L)
    }

    private fun startHold1Phase() {
        _breathPhase.value = BreathPhase.HOLD1
        startCountdown(_activePattern.value?.hold1 ?: 0)
        // Keep expanded at 95%
        _circleExpansion.value = 95f
    }

    private fun startExhalePhase() {
        _breathPhase.value = BreathPhase.EXHALE
        startCountdown(_activePattern.value?.exhale ?: 4)
        animateCircle(95f, 30f, _activePattern.value?.exhale?.times(1000L) ?: 4000L)
    }

    private fun startHold2Phase() {
        _breathPhase.value = BreathPhase.HOLD2
        startCountdown(_activePattern.value?.hold2 ?: 0)
        // Keep contracted at 30%
        _circleExpansion.value = 30f
    }

    private fun animateCircle(from: Float, to: Float, duration: Long) {
        // Animation logic to update _circleExpansion
        val animator = ValueAnimator.ofFloat(from, to).apply {
            this.duration = duration
            addUpdateListener { animation ->
                _circleExpansion.value = animation.animatedValue as Float
            }
            interpolator = LinearInterpolator()
            start()
        }
    }

    private fun startCountdown(seconds: Int) {
        _counter.value = seconds

        timer?.cancel()

        timer = object : CountDownTimer(seconds * 1000L, 1000) {
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
            BreathPhase.INHALE -> Color.parseColor("#00A6ED") // Bright blue for inhale
            BreathPhase.HOLD1 -> Color.parseColor("#0080B3") // Deeper blue for hold after inhale
            BreathPhase.EXHALE -> Color.parseColor("#00D084") // Bright green for exhale
            BreathPhase.HOLD2 -> Color.parseColor("#00A66A") // Deeper green for hold after exhale
            else -> Color.parseColor("#4682B4") // Default blue
        }
    }

    // Get inner circle color (darker variation)
    fun getInnerColor(): Int {
        return when (_breathPhase.value) {
            BreathPhase.INHALE -> Color.parseColor("#0076AD") // Darker blue
            BreathPhase.HOLD1 -> Color.parseColor("#005580") // Even darker blue
            BreathPhase.EXHALE -> Color.parseColor("#00A066") // Darker green
            BreathPhase.HOLD2 -> Color.parseColor("#007A4D") // Even darker green
            else -> Color.parseColor("#2C5984") // Default darker blue
        }
    }

    // Get background gradient colors based on current phase
    fun getBackgroundColors(): Pair<Int, Int> {
        return when (_breathPhase.value) {
            BreathPhase.INHALE, BreathPhase.HOLD1 -> {
                Pair(
                    Color.parseColor("#0A0A14"), // Very dark blue-black
                    Color.parseColor("#0A1A30")  // Dark blue tint
                )
            }
            BreathPhase.EXHALE, BreathPhase.HOLD2 -> {
                Pair(
                    Color.parseColor("#0A0A14"), // Very dark blue-black
                    Color.parseColor("#0A2414")  // Dark green tint
                )
            }
            else -> {
                Pair(
                    Color.parseColor("#0A0A14"), // Very dark blue-black
                    Color.parseColor("#121218")  // Slightly lighter dark
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timer?.cancel()
    }
}