package com.example.breathwell.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.breathwell.model.BreathPhase
import com.example.breathwell.model.BreathingPattern
import com.example.breathwell.utils.BackgroundMusicHelper
import com.example.breathwell.utils.SoundEffectHelper

/**
 * Coordinates audio components for the breathing experience:
 * - Voice guidance (TTS)
 * - Background music
 * - Sound effects
 *
 * Handles audio focus management and ducking of background audio during speech.
 */
class AudioManager(private val context: Context) : DefaultLifecycleObserver {

    // Audio components
    private val breathingAudioGuide = BreathingAudioGuide(context)
    private val backgroundMusicHelper = BackgroundMusicHelper(context)
    private val soundEffectHelper = SoundEffectHelper(context)

    // Android audio manager
    private val androidAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Audio focus handling
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false

    // Settings state
    private var isAudioGuidanceEnabled = true
    private var isBackgroundMusicEnabled = true
    private var isSoundEffectsEnabled = true

    // Saved audio settings for restoring after speech
    private var savedMusicVolume = 0.3f

    init {
        setupAudioFocusRequest()
        setupAudioGuidanceListener()

        // Initialize background music
        backgroundMusicHelper.initialize(com.example.breathwell.R.raw.background_music)
    }

    /**
     * Set up audio focus request for modern Android versions
     */
    private fun setupAudioFocusRequest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { focusChange ->
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_GAIN -> {
                            Log.d(TAG, "Audio focus gained")
                            hasAudioFocus = true
                        }
                        AudioManager.AUDIOFOCUS_LOSS,
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                            Log.d(TAG, "Audio focus lost")
                            hasAudioFocus = false
                            // Pause audio guidance
                            breathingAudioGuide.stopSpeaking()
                        }
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                            // Lower volume of background music during speech
                            if (isBackgroundMusicEnabled) {
                                backgroundMusicHelper.setVolume(0.1f)
                            }
                        }
                    }
                }
                .build()
        }
    }

    /**
     * Set up listener for TTS completion to restore background music volume
     */
    private fun setupAudioGuidanceListener() {
        breathingAudioGuide.audioCompletionListener = {
            // Restore background music volume after speech completes
            if (isBackgroundMusicEnabled) {
                backgroundMusicHelper.setVolume(savedMusicVolume)
            }

            // Abandon audio focus
            abandonAudioFocus()
        }
    }

    /**
     * Request audio focus for TTS
     */
    private fun requestAudioFocus(): Boolean {
        // Save current music volume for restoring later
        savedMusicVolume = backgroundMusicHelper.getVolume()

        // Lower volume of background music during speech
        if (isBackgroundMusicEnabled) {
            backgroundMusicHelper.setVolume(0.1f)
        }

        // Request audio focus
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { request ->
                val result = androidAudioManager.requestAudioFocus(request)
                hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                return hasAudioFocus
            }
        } else {
            @Suppress("DEPRECATION")
            val result = androidAudioManager.requestAudioFocus(
                { focusChange ->
                    // Handle focus changes for older devices
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                        breathingAudioGuide.stopSpeaking()
                    }
                },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
            hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            return hasAudioFocus
        }

        return false
    }

    /**
     * Abandon audio focus after TTS completes
     */
    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { request ->
                androidAudioManager.abandonAudioFocusRequest(request)
            }
        } else {
            @Suppress("DEPRECATION")
            androidAudioManager.abandonAudioFocus(null)
        }

        hasAudioFocus = false
    }

    /**
     * Announce the start of a breathing session
     */
    fun announceSessionStart(pattern: BreathingPattern, cycles: Int) {
        if (!isAudioGuidanceEnabled) return

        requestAudioFocus()
        breathingAudioGuide.announceSessionStart(pattern.name, cycles)
    }

    /**
     * Announce phase change with duration
     */
    fun announcePhase(phase: BreathPhase, duration: Int) {
        if (!isAudioGuidanceEnabled) return

        requestAudioFocus()
        breathingAudioGuide.announcePhase(phase, duration)
    }

    /**
     * Play phase transition sound and vibration
     */
    fun playPhaseTransition(phase: BreathPhase) {
        if (isSoundEffectsEnabled) {
            soundEffectHelper.playPhaseTransitionSound(phase)
        }
    }

    /**
     * Start or resume background music playback
     */
    fun startBackgroundMusic() {
        if (isBackgroundMusicEnabled) {
            backgroundMusicHelper.start()
        }
    }

    /**
     * Pause background music playback
     */
    fun pauseBackgroundMusic() {
        backgroundMusicHelper.pause()
    }

    // Settings methods

    /**
     * Enable or disable audio guidance
     */
    fun setAudioGuidanceEnabled(enabled: Boolean) {
        isAudioGuidanceEnabled = enabled

        // Stop any ongoing TTS if disabled
        if (!enabled) {
            breathingAudioGuide.stopSpeaking()
        }
    }

    /**
     * Enable or disable background music
     */
    fun setBackgroundMusicEnabled(enabled: Boolean) {
        isBackgroundMusicEnabled = enabled
        backgroundMusicHelper.setMusicEnabled(enabled)
    }

    /**
     * Enable or disable sound effects
     */
    fun setSoundEffectsEnabled(enabled: Boolean) {
        isSoundEffectsEnabled = enabled
        soundEffectHelper.setSoundEnabled(enabled)
    }

    /**
     * Set background music volume
     */
    fun setBackgroundMusicVolume(volume: Float) {
        savedMusicVolume = volume
        backgroundMusicHelper.setVolume(volume)
    }

    /**
     * Set voice type for audio guidance
     */
    fun setVoiceType(voiceType: BreathingAudioGuide.VoiceType) {
        breathingAudioGuide.setVoiceType(voiceType)
    }

    /**
     * Set speech rate for audio guidance
     */
    fun setSpeechRate(rate: Float) {
        breathingAudioGuide.setSpeechRate(rate)
    }

    /**
     * Preview current voice settings
     */
    fun previewVoice() {
        requestAudioFocus()
        breathingAudioGuide.speakPreview()
    }

    /**
     * Release all audio resources
     */
    fun release() {
        breathingAudioGuide.release()
        backgroundMusicHelper.release()
        soundEffectHelper.release()
        abandonAudioFocus()
    }

    // Lifecycle methods
    override fun onResume(owner: LifecycleOwner) {
        if (isBackgroundMusicEnabled) {
            backgroundMusicHelper.start()
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        backgroundMusicHelper.pause()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        release()
    }

    companion object {
        private const val TAG = "AudioManager"
    }
}