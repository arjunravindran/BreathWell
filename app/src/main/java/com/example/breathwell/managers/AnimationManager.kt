package com.example.breathwell.managers

import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.TransitionDrawable
import com.example.breathwell.databinding.ActivityMainBinding
import com.example.breathwell.model.BreathPhase
import com.example.breathwell.viewmodel.BreathingViewModel
import androidx.lifecycle.Observer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.core.view.isVisible

/**
 * Manages animations and visual effects in the application
 * Implements DefaultLifecycleObserver to properly handle lifecycle events
 */
class AnimationManager(
    private val binding: ActivityMainBinding,
    private val viewModel: BreathingViewModel
) : DefaultLifecycleObserver {

    // Observers for animated properties
    private val phaseObserver = Observer<BreathPhase> { phase ->
        updatePhaseUI(phase)
    }

    /**
     * Initialize animation manager
     */
    init {
        // Observe breath phase changes to update animations
        viewModel.breathPhase.observeForever(phaseObserver)
    }

    /**
     * Restore UI state based on current ViewModel state
     * Called after configuration changes to ensure consistent UI
     */
    fun restoreAnimationState() {
        // Get current phase and update UI
        viewModel.breathPhase.value?.let { updatePhaseUI(it) }

        // Restore circle expansion
        viewModel.circleExpansion.value?.let { expansion ->
            getCurrentHALCircleView().expansion = expansion
        }

        // Update background gradient based on current state
        updateBackgroundGradient()
    }

    /**
     * Update UI based on current breathing phase
     */
    private fun updatePhaseUI(phase: BreathPhase?) {
        if (phase == null) return

        // Update breath colors
        getCurrentHALCircleView().breathColor = viewModel.getBreathColor()
        getCurrentHALCircleView().innerColor = viewModel.getInnerColor()

        // Update background gradient
        updateBackgroundGradient()

        // Only show pulse animation during inhale and exhale phases
        getCurrentHALCircleView().showPulseEffect =
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
        binding.breathingContent.root.isVisible ->
            binding.breathingContent.halCircleView
        else -> binding.breathingContentLand.halCircleView
    }

    /**
     * Handle animation pausing/resuming on lifecycle pause events
     */
    override fun onPause(owner: LifecycleOwner) {
        // Pause any active animations when app goes to background
        // Store state in ViewModel for later restoration
        val halCircleView = getCurrentHALCircleView()
        viewModel.saveAnimationState(
            halCircleView.expansion,
            halCircleView.showPulseEffect
        )

        // Pause animations
        halCircleView.pauseAnimations()
    }

    /**
     * Resume animations when app comes back to foreground
     */
    override fun onResume(owner: LifecycleOwner) {
        // Restore animation state from ViewModel
        restoreAnimationState()

        // Resume animations if needed
        if (viewModel.isRunning.value == true) {
            val halCircleView = getCurrentHALCircleView()
            halCircleView.resumeAnimations()
        }
    }

    /**
     * Clean up resources to prevent leaks
     */
    fun cleanup() {
        // Remove observers
        viewModel.breathPhase.removeObserver(phaseObserver)
    }
}