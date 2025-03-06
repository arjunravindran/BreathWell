package com.example.breathwell.managers

import android.content.res.Configuration
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.breathwell.databinding.ActivityMainBinding
import com.example.breathwell.ui.BreathingUIController
import com.example.breathwell.viewmodel.BreathingViewModel

/**
 * Manages orientation changes and UI adaptations
 */
class OrientationManager(
    private val activity: AppCompatActivity,
    private val binding: ActivityMainBinding,
    private val viewModel: BreathingViewModel
) {
    // Track current orientation
    private var currentOrientation = activity.resources.configuration.orientation

    /**
     * Handle configuration changes such as orientation changes
     */
    fun handleConfigurationChange(newConfig: Configuration) {
        Log.d("OrientationManager", "Configuration changed. New orientation: ${newConfig.orientation}")

        // If orientation hasn't changed, no need to update UI
        if (newConfig.orientation == currentOrientation) {
            Log.d("OrientationManager", "Orientation unchanged, skipping update")
            return
        }

        currentOrientation = newConfig.orientation

        // Force update layout visibility based on orientation
        when (newConfig.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                Log.d("OrientationManager", "Switching to landscape layout")
                switchToLandscapeLayout()
            }
            else -> {
                Log.d("OrientationManager", "Switching to portrait layout")
                switchToPortraitLayout()
            }
        }

        // Refresh UI from ViewModel state
        refreshUIFromViewModel()
    }

    /**
     * Switch to landscape layout
     */
    private fun switchToLandscapeLayout() {
        binding.breathingContent.root.visibility = View.GONE
        binding.breathingContentLand.root.visibility = View.VISIBLE
    }

    /**
     * Switch to portrait layout
     */
    private fun switchToPortraitLayout() {
        binding.breathingContent.root.visibility = View.VISIBLE
        binding.breathingContentLand.root.visibility = View.GONE
    }

    /**
     * Refresh UI state from ViewModel
     * This ensures state is preserved across orientation changes
     */
    private fun refreshUIFromViewModel() {
        val breathingUIController = BreathingUIController(activity, binding, viewModel)

        // Re-setup UI components
        breathingUIController.setupPatternSpinner()
        breathingUIController.setupCyclesControl()
        breathingUIController.setupActionButtons()
        breathingUIController.setupAccessibility()

        // Apply current ViewModel state to UI
        breathingUIController.refreshUIState()
    }

    /**
     * Check if currently in landscape mode
     */
    fun isInLandscapeMode(): Boolean {
        return currentOrientation == Configuration.ORIENTATION_LANDSCAPE
    }

    /**
     * Get current orientation
     */
    fun getCurrentOrientation(): Int {
        return currentOrientation
    }
}