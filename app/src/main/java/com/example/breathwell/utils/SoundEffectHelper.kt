package com.example.breathwell.utils

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import com.example.breathwell.model.BreathPhase

/**
 * Helper class for playing sound effects during breathing exercises
 * Uses Android's built-in system tones
 */
class SoundEffectHelper(private val context: Context) {

    // ToneGenerator for system sounds
    private var toneGenerator: ToneGenerator? = null

    // Sound enabled flag
    private var soundEnabled = true

    init {
        // Initialize tone generator
        setupToneGenerator()
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
     * Play sound for phase transition
     * @param phase Current breathing phase
     */
    fun playPhaseTransitionSound(phase: BreathPhase) {
        if (!soundEnabled) return

        try {
            when (phase) {
                BreathPhase.INHALE -> {
                    // Gentle sound for inhale
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                }
                BreathPhase.EXHALE -> {
                    // Different sound for exhale
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 100)
                }
                BreathPhase.HOLD1, BreathPhase.HOLD2 -> {
                    // Subtle sound for hold phases
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 80)
                }
                BreathPhase.COMPLETE -> {
                    // More noticeable completion sound
                    toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 300)
                }
                else -> {
                    // No sound for READY phase
                }
            }
        } catch (e: Exception) {
            Log.e("SoundEffectHelper", "Error playing tone: ${e.message}")

            // If tone generator failed, try to recreate it
            setupToneGenerator()
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
        } catch (e: Exception) {
            Log.e("SoundEffectHelper", "Error releasing ToneGenerator: ${e.message}")
        }
    }
}