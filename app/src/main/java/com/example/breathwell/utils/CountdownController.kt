package com.example.breathwell.utils

import android.os.CountDownTimer
import android.util.Log

/**
 * Controller class to handle countdown timing for breathing exercises
 */
class CountdownController {

    // Callback interface for phase changes
    interface CountdownListener {
        fun onTick(secondsRemaining: Int)
        fun onPhaseComplete()
    }

    private var countDownTimer: CountDownTimer? = null
    private var listener: CountdownListener? = null
    private var powerSavingMode = PowerSavingMode.NONE

    /**
     * Start a countdown for the specified duration
     * @param seconds Duration in seconds
     */
    fun startCountdown(seconds: Int) {
        // Cancel any existing timer
        countDownTimer?.cancel()

        // Determine tick interval based on power saving mode
        val tickInterval = when (powerSavingMode) {
            PowerSavingMode.HIGH -> 1000L  // Update once per second in high power saving
            PowerSavingMode.MEDIUM -> 500L // Update twice per second in medium power saving
            PowerSavingMode.NONE -> 200L   // Update 5 times per second normally
        }

        try {
            // Create and start a new timer
            countDownTimer = object : CountDownTimer(seconds * 1000L, tickInterval) {
                override fun onTick(millisUntilFinished: Long) {
                    val secondsRemaining = (millisUntilFinished / 1000).toInt() + 1
                    listener?.onTick(secondsRemaining)
                }

                override fun onFinish() {
                    listener?.onPhaseComplete()
                }
            }.start()
        } catch (e: Exception) {
            Log.e("CountdownController", "Error starting countdown: ${e.message}")
            // Fail gracefully
        }
    }

    /**
     * Cancel the countdown
     */
    fun cancelCountdown() {
        try {
            countDownTimer?.cancel()
            countDownTimer = null
        } catch (e: Exception) {
            Log.e("CountdownController", "Error canceling countdown: ${e.message}")
        }
    }

    /**
     * Set the countdown listener
     */
    fun setListener(listener: CountdownListener) {
        this.listener = listener
    }

    /**
     * Set power saving mode
     */
    fun setPowerSavingMode(mode: PowerSavingMode) {
        powerSavingMode = mode
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        countDownTimer?.cancel()
        countDownTimer = null
        listener = null
    }
}