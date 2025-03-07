package com.example.breathwell.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.breathwell.model.BreathPhase

/**
 * Helper class for handling vibration effects during breathing exercises
 */
class VibrationHelper(private val context: Context) {

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    // Vibration enabled flag
    private var vibrationEnabled = true
    
    /**
     * Triggers vibration feedback based on the breathing phase
     * @param phase Current breathing phase
     */
    fun vibrateForPhase(phase: BreathPhase) {
        if (!vibrationEnabled || vibrator == null || !vibrator!!.hasVibrator()) return
        
        when (phase) {
            BreathPhase.INHALE -> vibrateMild()
            BreathPhase.EXHALE -> vibrateMild()
            BreathPhase.HOLD1, BreathPhase.HOLD2 -> vibrateShort()
            BreathPhase.COMPLETE -> vibrateCompletion()
            else -> {}
        }
    }
    
    /**
     * Mild vibration for standard phase changes
     */
    private fun vibrateMild() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(80)
        }
    }
    
    /**
     * Short vibration for hold phases
     */
    private fun vibrateShort() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(40)
        }
    }
    
    /**
     * Stronger vibration pattern for session completion
     */
    private fun vibrateCompletion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Pattern: 0ms delay, 100ms vibrate, 100ms off, 100ms vibrate, 100ms off, 200ms vibrate
            val pattern = longArrayOf(0, 100, 100, 100, 100, 200)
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 100, 100, 100, 100, 200), -1)
        }
    }
    
    /**
     * Set whether vibration feedback is enabled
     */
    fun setVibrationEnabled(enabled: Boolean) {
        vibrationEnabled = enabled
    }
}