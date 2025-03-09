package com.example.breathwell.utils

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.util.Log
import com.example.breathwell.R
import com.example.breathwell.model.BreathPhase

/**
 * Helper class for playing sound effects during breathing exercises
 * Uses both Android's built-in system tones and custom sound files
 */
class SoundEffectHelper(private val context: Context) {

    // ToneGenerator for system sounds
    private var toneGenerator: ToneGenerator? = null

    // MediaPlayer for custom sounds
    private var phaseTransitionPlayer: MediaPlayer? = null
    private var sessionCompletePlayer: MediaPlayer? = null

    // Sound enabled flag
    private var soundEnabled = true

    init {
        // Initialize tone generator and media players
        setupToneGenerator()
        setupMediaPlayers()
    }

    /**
     * Set up the tone generator
     */
    private fun setupToneGenerator() {
        try {
            // Create with 70% volume (0-100 scale)
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
            Log.d("SoundEffectHelper", "ToneGenerator initialized successfully")
        } catch (e: Exception) {
            Log.e("SoundEffectHelper", "Failed to create tone generator: ${e.message}")
        }
    }

    /**
     * Set up media players for custom sounds
     */
    private fun setupMediaPlayers() {
        try {
            // Phase transition sound
            phaseTransitionPlayer = MediaPlayer.create(context, R.raw.phase_transition)
            phaseTransitionPlayer?.setVolume(0.7f, 0.7f)
            phaseTransitionPlayer?.setOnCompletionListener {
                it.reset()
                it.setDataSource(context.resources.openRawResourceFd(R.raw.phase_transition))
                it.prepare()
            }

            // Session complete sound
            sessionCompletePlayer = MediaPlayer.create(context, R.raw.session_complete)
            sessionCompletePlayer?.setVolume(0.7f, 0.7f)
            sessionCompletePlayer?.setOnCompletionListener {
                it.reset()
                it.setDataSource(context.resources.openRawResourceFd(R.raw.session_complete))
                it.prepare()
            }

            Log.d("SoundEffectHelper", "Media players initialized successfully")
        } catch (e: Exception) {
            Log.e("SoundEffectHelper", "Failed to create media players: ${e.message}")
        }
    }

    /**
     * Play sound for phase transition
     * @param phase Current breathing phase
     */
    fun playPhaseTransitionSound(phase: BreathPhase) {
        if (!soundEnabled) return

        try {
            when (phase) {
                BreathPhase.INHALE, BreathPhase.EXHALE, BreathPhase.HOLD1, BreathPhase.HOLD2 -> {
                    // Play the phase transition sound for all breathing phases
                    if (phaseTransitionPlayer?.isPlaying == true) {
                        phaseTransitionPlayer?.stop()
                        phaseTransitionPlayer?.reset()
                        phaseTransitionPlayer?.setDataSource(context.resources.openRawResourceFd(R.raw.phase_transition))
                        phaseTransitionPlayer?.prepare()
                    }
                    phaseTransitionPlayer?.start()
                }
                BreathPhase.COMPLETE -> {
                    // Play completion sound
                    sessionCompletePlayer?.start()
                }
                else -> {
                    // No sound for READY phase
                }
            }
        } catch (e: Exception) {
            Log.e("SoundEffectHelper", "Error playing sound: ${e.message}")

            // If sound playback failed, try to recreate resources
            setupToneGenerator()
            setupMediaPlayers()
        }
    }

    /**
     * Set whether sound effects are enabled
     */
    fun setSoundEnabled(enabled: Boolean) {
        soundEnabled = enabled
    }

    /**
     * Clean up resources
     */
    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null

            phaseTransitionPlayer?.release()
            phaseTransitionPlayer = null

            sessionCompletePlayer?.release()
            sessionCompletePlayer = null
        } catch (e: Exception) {
            Log.e("SoundEffectHelper", "Error releasing resources: ${e.message}")
        }
    }
}