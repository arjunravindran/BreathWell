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
import com.example.breathwell.R
import com.example.breathwell.audio.AudioManager
import com.example.breathwell.audio.BreathingAudioGuide
import com.example.breathwell.data.AppDatabase
import com.example.breathwell.data.entity.BreathingSession
import com.example.breathwell.data.entity.SessionStatistics
import com.example.breathwell.data.repository.BreathingSessionRepository
import com.example.breathwell.model.BreathPhase
import com.example.breathwell.model.BreathingCategory
import com.example.breathwell.model.BreathingPattern
import com.example.breathwell.program.BreathingProgram
import com.example.breathwell.program.ProgramRepository
import com.example.breathwell.program.ProgramSession
import com.example.breathwell.utils.AnalyticsHelper
import com.example.breathwell.utils.CountdownController
import com.example.breathwell.utils.PowerSavingMode
import com.example.breathwell.widget.WidgetHelper
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * ViewModel for managing breathing exercises with expanded features:
 * - Multiple breathing techniques with categories
 * - Audio guidance including voice instructions
 * - Session statistics and analytics
 * - Integration with guided programs
 */
class BreathingViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
) : AndroidViewModel(application), CountdownController.CountdownListener {

    // Repository for session data
    private val repository: BreathingSessionRepository

    // Program repository
    private val programRepository: ProgramRepository = ProgramRepository.getInstance()

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
        const val KEY_AUDIO_GUIDANCE_ENABLED = "audio_guidance_enabled"
        const val KEY_VOICE_TYPE = "voice_type"
        const val KEY_SPEECH_RATE = "speech_rate"
        const val KEY_FAVORITE_PATTERNS = "favorite_patterns"
        const val KEY_SELECTED_CATEGORY = "selected_category"
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
            savedStateHandle[KEY_CUSTOM_HOLD2] ?: 2,
            "Your customized breathing pattern.",
            "Benefits depend on your chosen pattern ratios.",
            BreathingCategory.GENERAL,
            isCustom = true
        )
    )
    val customPattern = _customPattern

    // Expansion percentage for circle animation (0-100)
    private val _circleExpansion = MutableLiveData(
        savedStateHandle[KEY_EXPANSION] ?: 50f
    )
    val circleExpansion = _circleExpansion

    // Selected breathing category for filtering
    private val _selectedCategory = MutableLiveData<BreathingCategory>(
        savedStateHandle[KEY_SELECTED_CATEGORY] ?: BreathingCategory.GENERAL
    )
    val selectedCategory: LiveData<BreathingCategory> = _selectedCategory

    // Filtered patterns based on selected category
    private val _filteredPatterns = MutableLiveData<List<BreathingPattern>>(
        BreathingPattern.getAllPatterns()
    )
    val filteredPatterns: LiveData<List<BreathingPattern>> = _filteredPatterns

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

    // Audio guidance settings
    private val _audioGuidanceEnabled = MutableLiveData(
        savedStateHandle[KEY_AUDIO_GUIDANCE_ENABLED] ?: true
    )
    val audioGuidanceEnabled: LiveData<Boolean> = _audioGuidanceEnabled

    private val _voiceType = MutableLiveData(
        savedStateHandle[KEY_VOICE_TYPE] ?: BreathingAudioGuide.VoiceType.CALM.name
    )
    val voiceType: LiveData<String> = _voiceType

    private val _speechRate = MutableLiveData(
        savedStateHandle[KEY_SPEECH_RATE] ?: 0.85f
    )
    val speechRate: LiveData<Float> = _speechRate

    // Favorite patterns
    private val _favoritePatterns = MutableLiveData<Set<String>>(
        savedStateHandle[KEY_FAVORITE_PATTERNS] ?: setOf()
    )
    val favoritePatterns: LiveData<Set<String>> = _favoritePatterns

    // Phase transition event - used to notify for sound/vibration
    val phaseTransitionEvent = MutableLiveData<BreathPhase?>()

    // Current program session (if started from a program)
    private val _currentProgramSession = MutableLiveData<ProgramSession?>(null)
    val currentProgramSession: LiveData<ProgramSession?> = _currentProgramSession

    // Session statistics
    private val _sessionCompleted = MutableLiveData(false)
    val sessionCompleted: LiveData<Boolean> = _sessionCompleted

    private val _currentSessionStatistics = MutableLiveData<SessionStatistics?>(null)
    val currentSessionStatistics: LiveData<SessionStatistics?> = _currentSessionStatistics

    // For analytics
    val completedDates: LiveData<List<LocalDate>>
    val todaysSessions: LiveData<List<BreathingSession>>
    val allSessions: LiveData<List<BreathingSession>>

    // Audio manager for coordinating sounds
    private val audioManager: AudioManager

    // Countdown controller
    private val countdownController = CountdownController()

    // Circle animator
    private var circleAnimator: ValueAnimator? = null

    // Session start time for tracking
    private var sessionStartTime: LocalDateTime? = null

    /**
     * Initialize ViewModel and repositories
     */
    init {
        // Initialize repository
        val database = AppDatabase.getDatabase(application)
        repository = BreathingSessionRepository(database.breathingSessionDao())

        // Initialize LiveData for analytics
        completedDates = repository.getAllCompletedDates()
        todaysSessions = repository.getSessionForDate(LocalDate.now())
        allSessions = repository.getAllSessions()

        // Setup countdown controller
        countdownController.setListener(this)

        // Initialize audio manager
        audioManager = AudioManager(application)
        updateAudioManagerSettings()

        // Update filtered patterns based on selected category
        updateFilteredPatterns()
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
        _currentSessionStatistics.value = null
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
        _currentProgramSession.value = null

        // Trigger event for reset feedback
        phaseTransitionEvent.value = BreathPhase.READY
        phaseTransitionEvent.value = null
    }

    /**
     * Start or stop the breathing exercise
     */
    fun toggleBreathing() {
        if (_isRunning.value == true) {
            stopBreathing()
        } else {
            startBreathing()
        }
    }

    /**
     * Explicitly start the breathing session
     */
    fun startBreathing() {
        _isRunning.value = true
        savedStateHandle[KEY_IS_RUNNING] = true

        // Start session time tracking
        sessionStartTime = LocalDateTime.now()

        // Only reset phase if not resuming from config change or if we're complete
        if (_breathPhase.value == BreathPhase.READY || _breathPhase.value == BreathPhase.COMPLETE) {
            _breathPhase.value = BreathPhase.READY
            savedStateHandle[KEY_PHASE] = BreathPhase.READY
            _currentCycle.value = 0
            this.savedStateHandle[KEY_CURRENT_CYCLE] = 0
            _sessionCompleted.value = false
            _currentSessionStatistics.value = null
        }

        // Start background music
        audioManager.startBackgroundMusic()

        // Announce session start if first starting
        if (_breathPhase.value == BreathPhase.READY) {
            val pattern = _activePattern.value ?: BreathingPattern.BOX_BREATHING
            val cycles = _totalCycles.value ?: 5
            audioManager.announceSessionStart(pattern, cycles)
        }

        advanceToNextPhase()
    }

    /**
     * Stop the breathing session
     */
    private fun stopBreathing() {
        countdownController.cancelCountdown()
        circleAnimator?.cancel()
        circleAnimator = null

        // Save state before stopping
        saveAnimationState(_circleExpansion.value ?: 50f)

        _isRunning.value = false
        this.savedStateHandle[KEY_IS_RUNNING] = false
    }

    /**
     * Complete the breathing session
     */
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

        // Calculate session statistics
        calculateSessionStatistics()

        // Trigger phase transition event for completion sound/vibration/voice
        phaseTransitionEvent.value = BreathPhase.COMPLETE
        phaseTransitionEvent.value = null

        // Mark today's session as complete in the database
        saveSessionToDatabase()

        // Update session completed status
        _sessionCompleted.value = true

        // Complete program session if applicable
        _currentProgramSession.value?.let { session ->
            val programViewModel = ProgramViewModel()
            programViewModel.loadProgram(session.programId)
            programViewModel.completeCurrentSession()
        }

        // Update app widgets to reflect new streak
        updateWidgets(getApplication())
    }

    /**
     * Calculate statistics for the current session
     */
    private fun calculateSessionStatistics() {
        val pattern = _activePattern.value ?: BreathingPattern.BOX_BREATHING
        val cycles = _totalCycles.value ?: 5
        val startTime = sessionStartTime ?: LocalDateTime.now().minusMinutes(1)
        val endTime = LocalDateTime.now()

        val duration = java.time.Duration.between(startTime, endTime)
        val durationSeconds = duration.seconds.toInt()

        // Create session statistics
        val stats = SessionStatistics(
            patternName = pattern.name,
            cycles = cycles,
            durationSeconds = durationSeconds,
            expectedDurationSeconds = pattern.getSessionDuration(cycles),
            startTime = startTime,
            endTime = endTime
        )

        _currentSessionStatistics.value = stats
    }

    /**
     * Save the completed session to the database
     */
    private fun saveSessionToDatabase() {
        val activePattern = _activePattern.value ?: BreathingPattern.BOX_BREATHING
        val totalCycles = _totalCycles.value ?: 5
        val stats = _currentSessionStatistics.value

        // Calculate total session duration in seconds
        val durationSeconds = stats?.durationSeconds ?: run {
            val cycleDuration = (activePattern.inhale + activePattern.hold1 +
                    activePattern.exhale + activePattern.hold2)
            cycleDuration * totalCycles
        }

        viewModelScope.launch {
            val session = BreathingSession(
                date = LocalDate.now(),
                patternName = activePattern.name,
                cycles = totalCycles,
                durationSeconds = durationSeconds,
                completed = true
            )
            repository.insertSession(session)

            // Reset session start time for next session
            sessionStartTime = null
        }
    }

    /**
     * Updates the app widgets when a session is completed
     */
    private fun updateWidgets(context: Context) {
        WidgetHelper.updateWidgets(context)
    }

    /**
     * Advance to the next breathing phase
     */
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

            // Trigger phase transition event
            phaseTransitionEvent.value = it
            phaseTransitionEvent.value = null

            // Play sound effect for phase transition
            audioManager.playPhaseTransition(it)

            // Announce phase for audio guidance if not READY or COMPLETE
            // (those are handled separately)
            if (it != BreathPhase.READY && it != BreathPhase.COMPLETE) {
                val duration = when (it) {
                    BreathPhase.INHALE -> _activePattern.value?.inhale ?: 4
                    BreathPhase.HOLD1 -> _activePattern.value?.hold1 ?: 4
                    BreathPhase.EXHALE -> _activePattern.value?.exhale ?: 4
                    BreathPhase.HOLD2 -> _activePattern.value?.hold2 ?: 2
                    else -> 0
                }

                audioManager.announcePhase(it, duration)
            }
        }
    }

    /**
     * Advance to the next breathing cycle
     */
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

    /**
     * Start the inhale phase
     */
    private fun startInhalePhase() {
        val inhaleDuration = _activePattern.value?.inhale ?: 4
        countdownController.startCountdown(inhaleDuration)
        animateCircle(30f, 95f, inhaleDuration * 1000L)
    }

    /**
     * Start the first hold phase
     */
    private fun startHold1Phase() {
        val holdDuration = _activePattern.value?.hold1 ?: 0
        countdownController.startCountdown(holdDuration)
        // Keep expanded at 95%
        _circleExpansion.value = 95f
        savedStateHandle[KEY_EXPANSION] = 95f
    }

    /**
     * Start the exhale phase
     */
    private fun startExhalePhase() {
        val exhaleDuration = _activePattern.value?.exhale ?: 4
        countdownController.startCountdown(exhaleDuration)
        animateCircle(95f, 30f, exhaleDuration * 1000L)
    }

    /**
     * Start the second hold phase
     */
    private fun startHold2Phase() {
        val hold2Duration = _activePattern.value?.hold2 ?: 0
        countdownController.startCountdown(hold2Duration)
        // Keep contracted at 30%
        _circleExpansion.value = 30f
        savedStateHandle[KEY_EXPANSION] = 30f
    }

    /**
     * Animate the breathing circle between two sizes
     */
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

    /**
     * Set the active breathing pattern
     */
    fun setActivePattern(pattern: BreathingPattern) {
        if (_isRunning.value != true) {
            _activePattern.value = pattern
        }
    }

    /**
     * Set the total number of breathing cycles
     */
    fun setTotalCycles(cycles: Int) {
        if (_isRunning.value != true) {
            _totalCycles.value = cycles
        }
    }

    /**
     * Update the custom breathing pattern with new values
     */
    fun updateCustomPattern(inhale: Int, hold1: Int, exhale: Int, hold2: Int) {
        val updated = BreathingPattern(
            "Custom",
            inhale.coerceIn(1, 10),
            hold1.coerceIn(0, 10),
            exhale.coerceIn(1, 10),
            hold2.coerceIn(0, 10),
            "Your customized breathing pattern.",
            "Benefits depend on your chosen pattern ratios.",
            BreathingCategory.GENERAL,
            difficulty = 1,
            isCustom = true
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

    /**
     * Set the selected breathing category for filtering
     */
    fun setSelectedCategory(category: BreathingCategory) {
        _selectedCategory.value = category
        savedStateHandle[KEY_SELECTED_CATEGORY] = category
        updateFilteredPatterns()
    }

    /**
     * Update the filtered breathing patterns based on selected category
     */
    private fun updateFilteredPatterns() {
        val category = _selectedCategory.value ?: BreathingCategory.GENERAL
        val patterns = if (category == BreathingCategory.GENERAL) {
            BreathingPattern.getAllPatterns().filter { !it.isCustom || it == _customPattern.value }
        } else {
            BreathingPattern.getPatternsByCategory(category) +
                    if (_customPattern.value?.category == category) listOf(_customPattern.value!!) else emptyList()
        }
        _filteredPatterns.value = patterns
    }

    // Vibration and Sound Setting Methods

    /**
     * Toggle vibration feedback
     */
    fun toggleVibration() {
        _vibrationEnabled.value = _vibrationEnabled.value != true
        savedStateHandle[KEY_VIBRATION_ENABLED] = _vibrationEnabled.value

        // Update audio manager
        updateAudioManagerSettings()
    }

    /**
     * Toggle sound effects
     */
    fun toggleSound() {
        _soundEnabled.value = _soundEnabled.value != true
        savedStateHandle[KEY_SOUND_ENABLED] = this._soundEnabled.value

        // Update audio manager
        updateAudioManagerSettings()
    }

    /**
     * Toggle background music
     */
    fun toggleMusic() {
        _musicEnabled.value = _musicEnabled.value != true
        savedStateHandle[KEY_MUSIC_ENABLED] = _musicEnabled.value

        // Update audio manager
        updateAudioManagerSettings()
    }

    /**
     * Set background music volume
     */
    fun setMusicVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        _musicVolume.value = clampedVolume
        this.savedStateHandle[KEY_MUSIC_VOLUME] = clampedVolume

        // Update audio manager
        updateAudioManagerSettings()
    }

    // Audio Guidance Methods

    /**
     * Toggle audio guidance on/off
     */
    fun toggleAudioGuidance() {
        _audioGuidanceEnabled.value = _audioGuidanceEnabled.value != true
        savedStateHandle[KEY_AUDIO_GUIDANCE_ENABLED] = _audioGuidanceEnabled.value

        // Update audio manager
        updateAudioManagerSettings()
    }

    /**
     * Set the voice type for audio guidance
     */
    fun setVoiceType(type: BreathingAudioGuide.VoiceType) {
        _voiceType.value = type.name
        savedStateHandle[KEY_VOICE_TYPE] = type.name

        // Update audio manager
        updateAudioManagerSettings()
    }

    /**
     * Set the speech rate for audio guidance
     */
    fun setSpeechRate(rate: Float) {
        val clampedRate = rate.coerceIn(0.5f, 1.5f)
        _speechRate.value = clampedRate
        savedStateHandle[KEY_SPEECH_RATE] = clampedRate

        // Update audio manager
        updateAudioManagerSettings()
    }

    /**
     * Preview the current voice settings
     */
    fun previewVoice() {
        audioManager.previewVoice()
    }

    /**
     * Update all audio manager settings based on current values
     */
    private fun updateAudioManagerSettings() {
        audioManager.setAudioGuidanceEnabled(_audioGuidanceEnabled.value == true)
        audioManager.setSoundEffectsEnabled(_soundEnabled.value == true)
        audioManager.setBackgroundMusicEnabled(_musicEnabled.value == true)
        audioManager.setBackgroundMusicVolume(_musicVolume.value ?: 0.3f)

        // Set voice type
        _voiceType.value?.let { typeName ->
            try {
                val type = BreathingAudioGuide.VoiceType.valueOf(typeName)
                audioManager.setVoiceType(type)
            } catch (e: IllegalArgumentException) {
                // Use default if invalid
                audioManager.setVoiceType(BreathingAudioGuide.VoiceType.CALM)
            }
        }

        // Set speech rate
        audioManager.setSpeechRate(_speechRate.value ?: 0.85f)
    }

    // Favorite Patterns Methods

    /**
     * Toggle whether a pattern is in favorites
     */
    fun toggleFavoritePattern(patternName: String) {
        val currentFavorites = _favoritePatterns.value?.toMutableSet() ?: mutableSetOf()

        if (currentFavorites.contains(patternName)) {
            currentFavorites.remove(patternName)
        } else {
            currentFavorites.add(patternName)
        }

        _favoritePatterns.value = currentFavorites
        savedStateHandle[KEY_FAVORITE_PATTERNS] = currentFavorites
    }

    /**
     * Check if a pattern is in favorites
     */
    fun isFavoritePattern(patternName: String): Boolean {
        return _favoritePatterns.value?.contains(patternName) == true
    }

    /**
     * Get all favorite patterns
     */
    fun getFavoritePatterns(): List<BreathingPattern> {
        val favoriteNames = _favoritePatterns.value ?: setOf()
        return BreathingPattern.getAllPatterns().filter { pattern ->
            favoriteNames.contains(pattern.name)
        }
    }

    // Program Integration

    /**
     * Set the current program session when starting from a program
     */
    fun setProgramSession(session: ProgramSession?) {
        _currentProgramSession.value = session

        // Apply session settings if provided
        session?.let {
            setActivePattern(it.pattern)
            setTotalCycles(it.cycles)
        }
    }

    // Habit tracking and Analytics methods

    /**
     * Check if a specific date has completed sessions
     */
    fun isDateCompleted(date: LocalDate): LiveData<Boolean> {
        val sessions = repository.getSessionForDate(date)
        return sessions.map { it.isNotEmpty() && it.any { session -> session.completed } }
    }

    /**
     * Calculate current streak of consecutive days with completed sessions
     */
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

    /**
     * Get statistics for a specific day
     */
    fun getDateStatistics(date: LocalDate): LiveData<AnalyticsHelper.DayStatistics> {
        val sessions = repository.getSessionForDate(date)
        return sessions.map { AnalyticsHelper.calculateDayStatistics(it) }
    }

    /**
     * Get weekly statistics
     */
    fun getWeeklyStatistics(): LiveData<AnalyticsHelper.PeriodStatistics> {
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(6) // Last 7 days
        return repository.getSessionsForDateRange(startDate, endDate).map {
            AnalyticsHelper.calculatePeriodStatistics(it)
        }
    }

    /**
     * Get monthly statistics
     */
    fun getMonthlyStatistics(): LiveData<AnalyticsHelper.PeriodStatistics> {
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(29) // Last 30 days
        return repository.getSessionsForDateRange(startDate, endDate).map {
            AnalyticsHelper.calculatePeriodStatistics(it)
        }
    }

    /**
     * Get sessions by time of day
     */
    fun getSessionsByTimeOfDay(): LiveData<Map<AnalyticsHelper.TimeOfDay, Int>> {
        return allSessions.map { sessions ->
            val sessionsByTime = mutableMapOf(
                AnalyticsHelper.TimeOfDay.MORNING to 0,
                AnalyticsHelper.TimeOfDay.AFTERNOON to 0,
                AnalyticsHelper.TimeOfDay.EVENING to 0,
                AnalyticsHelper.TimeOfDay.NIGHT to 0
            )

            // Default time of day assignment
            val timeDistribution = listOf(
                AnalyticsHelper.TimeOfDay.MORNING,
                AnalyticsHelper.TimeOfDay.AFTERNOON,
                AnalyticsHelper.TimeOfDay.EVENING,
                AnalyticsHelper.TimeOfDay.NIGHT
            )

            // Assign sessions to time of day categories
            sessions.forEach { session ->
                // Use hash code to pseudo-randomly assign a time of day
                // In a real app, we would use actual timestamps
                val timeIndex = session.id.toInt() % timeDistribution.size
                val timeOfDay = timeDistribution[timeIndex]
                sessionsByTime[timeOfDay] = sessionsByTime[timeOfDay]!! + 1
            }

            sessionsByTime
        }
    }

    /**
     * Get most practiced technique
     */
    fun getMostPracticedTechnique(): LiveData<String?> {
        return allSessions.map { sessions ->
            if (sessions.isEmpty()) {
                null
            } else {
                sessions.groupBy { it.patternName }
                    .mapValues { it.value.size }
                    .maxByOrNull { it.value }
                    ?.key
            }
        }
    }

    /**
     * Get average session duration
     */
    fun getAverageSessionDuration(): LiveData<Int> {
        return allSessions.map { sessions ->
            if (sessions.isEmpty()) {
                0
            } else {
                sessions.sumOf { it.durationSeconds } / sessions.size
            }
        }
    }

    /**
     * Get total practice time
     */
    fun getTotalPracticeTime(): LiveData<Int> {
        return allSessions.map { sessions ->
            sessions.sumOf { it.durationSeconds }
        }
    }

    /**
     * Get best practicing time of day
     */
    fun getBestPracticingTimeOfDay(): LiveData<AnalyticsHelper.TimeOfDay?> {
        return getSessionsByTimeOfDay().map { sessionsByTime ->
            if (sessionsByTime.isEmpty() || sessionsByTime.values.all { it == 0 }) {
                null
            } else {
                sessionsByTime.maxByOrNull { it.value }?.key
            }
        }
    }

    // Color methods for UI

    /**
     * Get HAL circle color based on current breath phase
     */
    fun getBreathColor(): Int {
        return when (_breathPhase.value) {
            BreathPhase.INHALE -> "#38E1FF".toColorInt() // Cyan for inhale
            BreathPhase.HOLD1 -> "#38E1FF".toColorInt() // Cyan for hold after inhale
            BreathPhase.EXHALE -> "#00D084".toColorInt() // Bright green for exhale
            BreathPhase.HOLD2 -> "#00A66A".toColorInt() // Deeper green for hold after exhale
            else -> "#38E1FF".toColorInt() // Default blue
        }
    }

    /**
     * Get inner circle color (darker variation)
     */
    fun getInnerColor(): Int {
        return when (_breathPhase.value) {
            BreathPhase.INHALE -> "#00D4FF".toColorInt() // Darker cyan for inhale
            BreathPhase.HOLD1 -> "#00D4FF".toColorInt() // Darker cyan for hold after inhale
            BreathPhase.EXHALE -> "#00A066".toColorInt() // Darker green
            BreathPhase.HOLD2 -> "#007A4D".toColorInt() // Even darker green
            else -> "#00D4FF".toColorInt() // Default darker blue
        }
    }

    /**
     * Get color for a breathing category
     */
    fun getCategoryColor(category: BreathingCategory): Int {
        return when (category) {
            BreathingCategory.STRESS_REDUCTION -> "#00D084".toColorInt() // Green
            BreathingCategory.RELAXATION -> "#38E1FF".toColorInt() // Cyan
            BreathingCategory.ENERGY -> "#FFC107".toColorInt() // Yellow
            BreathingCategory.MINDFULNESS -> "#9C27B0".toColorInt() // Purple
            BreathingCategory.SLEEP -> "#3F51B5".toColorInt() // Indigo
            BreathingCategory.HEART_HEALTH -> "#E91E63".toColorInt() // Pink
            BreathingCategory.PERFORMANCE -> "#FF9800".toColorInt() // Orange
            else -> "#00D4FF".toColorInt() // Default blue
        }
    }

    /**
     * Get a background resource for a program based on its ID
     */
    fun getProgramBackgroundResource(programId: String): Int {
        return when (programId) {
            "beginner" -> R.drawable.program_beginner
            "stress_reduction" -> R.drawable.program_stress
            "sleep_improvement" -> R.drawable.program_sleep
            "mindfulness" -> R.drawable.program_mindfulness
            else -> R.drawable.program_default
        }
    }

    /**
     * Clean up resources
     */
    override fun onCleared() {
        super.onCleared()
        countdownController.cleanup()
        circleAnimator?.cancel()
        circleAnimator = null
        audioManager.release()
    }
}