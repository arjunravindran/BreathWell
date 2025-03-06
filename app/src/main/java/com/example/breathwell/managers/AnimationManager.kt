package com.example.breathwell.managers

import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.TransitionDrawable
import com.example.breathwell.databinding.ActivityMainBinding
import com.example.breathwell.model.BreathPhase
import com.example.breathwell.viewmodel.BreathingViewModel
import androidx.lifecycle.Observer

/**
 * Manages animations and visual effects in the application
 */
class AnimationManager(
    private val binding: ActivityMainBinding,
    private val viewModel: BreathingViewModel
) {
    // Observers for animated properties
    private val phaseObserver = Observer<BreathPhase> { phase ->
        updatePhaseUI(phase)
    }
    
    /**
     * Initialize animation manager and set up observers
     */
    init {
        // Observe breath phase changes to update animations
        viewModel.breathPhase.observeForever(phaseObserver)
    }
    
    /**
     * Update UI based on current breathing phase
     */
    private fun updatePhaseUI(phase: BreathPhase?) {
        // Update breath colors
        getCurrentHALCircleView()?.breathColor = viewModel.getBreathColor()
        getCurrentHALCircleView()?.innerColor = viewModel.getInnerColor()

        // Update background gradient
        updateBackgroundGradient()
        
        // Only show pulse animation during inhale and exhale phases
        getCurrentHALCircleView()?.showPulseEffect =
            phase == BreathPhase.INHALE || phase == BreathPhase.EXHALE
    }
    
    /**
     * Update background gradient based on current phase
     */
    private fun updateBackgroundGradient() {
        val (startColor, endColor) = viewModel.getBackgroundColors()

        val gradient = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(startColor, endColor)
        )

        // Apply gradient with animation
        val oldDrawable = binding.rootLayout.background
        val transition = TransitionDrawable(arrayOf(oldDrawable, gradient))
        binding.rootLayout.background = transition
        transition.startTransition(400)
    }
    
    /**
     * Get the currently visible HAL circle view
     */
    private fun getCurrentHALCircleView() = when {
        binding.breathingContent.root.visibility == android.view.View.VISIBLE -> 
            binding.breathingContent.halCircleView
        else -> binding.breathingContentLand.halCircleView
    }
    
    /**
     * Clean up resources to prevent leaks
     */
    fun cleanup() {
        // Remove observers
        viewModel.breathPhase.removeObserver(phaseObserver)
    }
}
