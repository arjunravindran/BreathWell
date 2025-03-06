package com.example.breathwell.managers

import android.content.Context
import androidx.lifecycle.Observer
import com.example.breathwell.utils.AnimationQuality
import com.example.breathwell.utils.BatteryOptimizationUtils
import com.example.breathwell.utils.PowerSavingMode
import com.example.breathwell.viewmodel.BreathingViewModel

/**
 * Manages power-saving adaptations and battery optimizations
 */
class PowerSavingManager(
    private val context: Context,
    private val viewModel: BreathingViewModel
) {
    // Current power saving state
    private var currentPowerSavingMode = PowerSavingMode.NONE
    private var currentAnimationQuality = AnimationQuality.FULL
    
    // Observer for power saving mode changes
    private val powerSavingObserver = Observer<PowerSavingMode> { mode ->
        onPowerSavingModeChanged(mode)
    }

    /**
     * Initialize power saving mode adaptations
     */
    fun setupPowerSavingMode() {
        // Determine initial power saving mode
        currentPowerSavingMode = BatteryOptimizationUtils.adaptToPowerSaving(context)
        currentAnimationQuality = BatteryOptimizationUtils.getAnimationQuality(currentPowerSavingMode)

        // Pass power saving status to view model
        viewModel.setPowerSavingMode(currentPowerSavingMode)
        
        // Observe future changes to power saving mode
        viewModel.powerSavingMode.observeForever(powerSavingObserver)
    }
    
    /**
     * Called when power saving mode changes
     */
    private fun onPowerSavingModeChanged(mode: PowerSavingMode) {
        if (currentPowerSavingMode == mode) return
        
        currentPowerSavingMode = mode
        currentAnimationQuality = BatteryOptimizationUtils.getAnimationQuality(mode)
        
        // Apply new settings to animations, timers etc.
        applyPowerSavingSettings()
    }
    
    /**
     * Apply power saving settings to various components
     */
    private fun applyPowerSavingSettings() {
        // Update animation refresh rates
        val updateInterval = BatteryOptimizationUtils.getRecommendedUpdateInterval(currentPowerSavingMode)
        
        // We don't directly access UI components - the ViewModel will handle that
        // when observing the powerSavingMode LiveData
    }
    
    /**
     * Get current animation quality setting
     */
    fun getCurrentAnimationQuality(): AnimationQuality {
        return currentAnimationQuality
    }
    
    /**
     * Check if device is in power saving mode
     */
    fun isInPowerSavingMode(): Boolean {
        return currentPowerSavingMode != PowerSavingMode.NONE
    }
    
    /**
     * Force update power saving mode
     * Should be called when device power saving mode changes
     */
    fun updatePowerSavingMode() {
        val newMode = BatteryOptimizationUtils.adaptToPowerSaving(context)
        if (newMode != currentPowerSavingMode) {
            viewModel.setPowerSavingMode(newMode)
        }
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        viewModel.powerSavingMode.removeObserver(powerSavingObserver)
    }
}
