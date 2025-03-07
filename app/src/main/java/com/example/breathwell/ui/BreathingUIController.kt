package com.example.breathwell.ui

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.breathwell.MainActivity
import com.example.breathwell.R
import com.example.breathwell.databinding.ActivityMainBinding
import com.example.breathwell.model.BreathPhase
import com.example.breathwell.model.BreathingPattern
import com.example.breathwell.ui.views.CustomNumberSlider
import com.example.breathwell.utils.AccessibilityUtils
import com.example.breathwell.viewmodel.BreathingViewModel

/**
 * Controls the UI for the breathing interface
 */
class BreathingUIController(
    private val activity: AppCompatActivity,
    private val binding: ActivityMainBinding,
    private val viewModel: BreathingViewModel
) {
    // Adapter for breathing pattern selection
    private val breathingPatternAdapter by lazy { BreathingPatternAdapter(activity) }

    /**
     * Set up the breathing pattern spinner
     */
    fun setupPatternSpinner() {
        val spinner = getCurrentPatternSpinner() ?: return
        spinner.adapter = breathingPatternAdapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val pattern = breathingPatternAdapter.getItem(position)
                pattern?.let { viewModel.setActivePattern(it) }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }
    }

    /**
     * Set up the cycles slider control
     */
    fun setupCyclesControl() {
        // Get reference to the slider
        val cyclesSlider = getCurrentCyclesSlider() ?: return

        // Set initial value
        cyclesSlider.value = viewModel.totalCycles.value?.toFloat() ?: 5f

        // Setup slider change listener
        cyclesSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val cycles = value.toInt()
                viewModel.setTotalCycles(cycles)
            }
        }
    }

    /**
     * Set up action buttons
     */
    fun setupActionButtons() {
        // Original button (invisibly handling accessibility)
        getCurrentActionButton()?.setOnClickListener {
            viewModel.toggleBreathing()
        }

        // Circular action button (Start/Stop)
        getCurrentCircularActionButton()?.setOnButtonClickListener {
            viewModel.toggleBreathing()
        }

        // Settings button
        binding.settingsButton.setOnClickListener {
            if (activity is MainActivity) {
                activity.showSettingsFragment()
            }
        }

        // Habit Tracker button
        binding.habitTrackerButton.setOnClickListener {
            if (activity is MainActivity) {
                activity.showHabitTrackerFragment()
            }
        }
    }

    /**
     * Set up accessibility features
     */
    fun setupAccessibility() {
        // Set content descriptions for buttons
        AccessibilityUtils.setupAccessibilityForButton(
            binding.settingsButton,
            activity.getString(R.string.settings),
            activity.getString(R.string.accessibility_open_settings)
        )

        AccessibilityUtils.setupAccessibilityForButton(
            binding.habitTrackerButton,
            activity.getString(R.string.habit_tracker),
            activity.getString(R.string.accessibility_open_habit_tracker)
        )

        // Set content descriptions for icons
        AccessibilityUtils.setContentDescription(
            binding.settingsIcon,
            activity.getString(R.string.settings)
        )

        AccessibilityUtils.setContentDescription(
            binding.habitTrackerIcon,
            activity.getString(R.string.habit_tracker)
        )

        // Setup accessibility for action buttons
        getCurrentActionButton()?.let { button ->
            AccessibilityUtils.setupAccessibilityForButton(
                button,
                activity.getString(if (viewModel.isRunning.value == true) R.string.stop else R.string.start),
                activity.getString(if (viewModel.isRunning.value == true) R.string.accessibility_stop_session else R.string.accessibility_start_session)
            )
        }

        // Also setup accessibility for circular action button
        getCurrentCircularActionButton()?.let { button ->
            AccessibilityUtils.setupAccessibilityForButton(
                button,
                activity.getString(if (viewModel.isRunning.value == true) R.string.stop else R.string.start),
                activity.getString(if (viewModel.isRunning.value == true) R.string.accessibility_stop_session else R.string.accessibility_start_session)
            )
        }

        // Setup accessibility for the slider
        getCurrentCyclesSlider()?.let { slider ->
            AccessibilityUtils.setupAccessibilityForButton(
                slider,
                activity.getString(R.string.cycles),
                activity.getString(R.string.accessibility_cycles_slider)
            )
        }
    }

    /**
     * Observe ViewModel state
     */
    fun observeViewModelState() {
        // Update pattern spinner selection
        viewModel.activePattern.observe(activity) { pattern ->
            val position = breathingPatternAdapter.getPosition(pattern)
            if (position >= 0) {
                getCurrentPatternSpinner()?.setSelection(position)
            }
        viewModel.isRunning.observe(activity) { isRunning ->
            // Only show timer when a session is running
            getCurrentHALCircleView().showTimer = isRunning

            // Rest of your existing isRunning observer code...
        }
        }

        // Update breath phase, animation, and instructions
        viewModel.breathPhase.observe(activity) { phase ->
            updateInstructionText(phase)

            // Announce phase change for accessibility
            phase?.let {
                AccessibilityUtils.announceForAccessibility(
                    activity,
                    getCurrentHALCircleView(),
                    it,
                    viewModel.counter.value ?: 0
                )
            }
        }

        // Update counter display
        viewModel.counter.observe(activity) { count ->
            getCurrentHALCircleView().counter = count
        }

        // Update circle expansion
        viewModel.circleExpansion.observe(activity) { expansion ->
            getCurrentHALCircleView().expansion = expansion
        }

        // Update running state and control screen wakelock
        viewModel.isRunning.observe(activity) { isRunning ->
            updateActionButtonState(isRunning)

            // Update screen wake state
            if (activity is MainActivity) {
                activity.updateScreenWakeState(isRunning)
            }
        }

        // Update cycle counts
        viewModel.totalCycles.observe(activity) { cycles ->
            getCurrentCyclesSlider()?.value = cycles.toFloat()
            getCurrentProgressRing()?.totalCycles = cycles
        }

        viewModel.currentCycle.observe(activity) { cycle ->
            getCurrentProgressRing()?.currentCycle = cycle
        }
    }

    /**
     * Update instruction text based on breath phase
     */
    private fun updateInstructionText(phase: BreathPhase?) {
        // Update the HAL circle view instruction
        val instructionText = when (phase) {
            BreathPhase.INHALE -> activity.getString(R.string.inhale)
            BreathPhase.HOLD1, BreathPhase.HOLD2 -> activity.getString(R.string.hold)
            BreathPhase.EXHALE -> activity.getString(R.string.exhale)
            BreathPhase.READY -> activity.getString(R.string.ready)
            BreathPhase.COMPLETE -> activity.getString(R.string.complete)
            null -> activity.getString(R.string.ready)
        }

        getCurrentHALCircleView().instruction = instructionText

        // Update phase description text for landscape orientation
        if (activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val descriptionRes = when (phase) {
                BreathPhase.INHALE -> R.string.inhale_description
                BreathPhase.HOLD1 -> R.string.hold_after_inhale_description
                BreathPhase.EXHALE -> R.string.exhale_description
                BreathPhase.HOLD2 -> R.string.hold_after_exhale_description
                BreathPhase.READY -> R.string.ready_description
                BreathPhase.COMPLETE -> R.string.complete_description
                null -> R.string.ready_description
            }
            binding.breathingContentLand.phaseDescription.setText(descriptionRes)
        }
    }

    /**
     * Update action button state based on running state
     */
    private fun updateActionButtonState(isRunning: Boolean) {
        val button = getCurrentActionButton() ?: return
        val circularButton = getCurrentCircularActionButton() ?: return
        val spinner = getCurrentPatternSpinner() ?: return
        val cyclesSlider = getCurrentCyclesSlider() ?: return

        if (isRunning) {
            // Original button styling (still needed for accessibility)
            button.text = activity.getString(R.string.stop)
            button.backgroundTintList = ColorStateList.valueOf(
                activity.resources.getColor(R.color.red_500, activity.theme)
            )

            // Set circular button state
            circularButton.setPlaying(true)

            // Disable controls during session
            spinner.isEnabled = false
            cyclesSlider.isEnabled = false

            // Update accessibility
            AccessibilityUtils.setupAccessibilityForButton(
                button,
                activity.getString(R.string.stop),
                activity.getString(R.string.accessibility_stop_session)
            )

            AccessibilityUtils.setupAccessibilityForButton(
                circularButton,
                activity.getString(R.string.stop),
                activity.getString(R.string.accessibility_stop_session)
            )
        } else {
            // Original button styling
            if (viewModel.breathPhase.value == BreathPhase.COMPLETE) {
                button.text = activity.getString(R.string.start_new_session)
            } else {
                button.text = activity.getString(R.string.start)
            }
            button.backgroundTintList = ColorStateList.valueOf(
                activity.resources.getColor(R.color.cyan_gradient_start, activity.theme)
            )

            // Set circular button state
            circularButton.setPlaying(false)

            // Enable controls when not in session
            spinner.isEnabled = true
            cyclesSlider.isEnabled = true

            // Update accessibility
            AccessibilityUtils.setupAccessibilityForButton(
                button,
                activity.getString(R.string.start),
                activity.getString(R.string.accessibility_start_session)
            )

            AccessibilityUtils.setupAccessibilityForButton(
                circularButton,
                activity.getString(R.string.start),
                activity.getString(R.string.accessibility_start_session)
            )
        }
    }

    /**
     * Refresh UI state from ViewModel
     * This should be called after orientation changes or resuming the app
     */
    fun refreshUIState() {
        // Apply current ViewModel state to UI
        viewModel.activePattern.value?.let { pattern ->
            val position = breathingPatternAdapter.getPosition(pattern)
            if (position >= 0) {
                getCurrentPatternSpinner()?.setSelection(position)
            }
        }

        viewModel.totalCycles.value?.let { cycles ->
            getCurrentCyclesSlider()?.value = cycles.toFloat()
        }

        viewModel.breathPhase.value?.let { phase ->
            updateInstructionText(phase)
        }

        viewModel.isRunning.value?.let { isRunning ->
            updateActionButtonState(isRunning)
        }
    }

    // Helper methods to get the appropriate UI component based on current layout

    private fun getCurrentPatternSpinner() = when {
        binding.breathingContent.root.visibility == View.VISIBLE -> binding.breathingContent.patternSpinnerView
        else -> binding.breathingContentLand.patternSpinnerView
    }

    private fun getCurrentCyclesSlider() = when {
        binding.breathingContent.root.visibility == View.VISIBLE -> binding.breathingContent.cyclesSlider as? CustomNumberSlider
        else -> binding.breathingContentLand.cyclesSlider as? CustomNumberSlider
    }

    private fun getCurrentActionButton() = when {
        binding.breathingContent.root.visibility == View.VISIBLE -> binding.breathingContent.actionButton
        else -> binding.breathingContentLand.actionButton
    }

    private fun getCurrentCircularActionButton() = when {
        binding.breathingContent.root.visibility == View.VISIBLE -> binding.breathingContent.circularActionButton
        else -> binding.breathingContentLand.circularActionButton
    }

    private fun getCurrentHALCircleView() = when {
        binding.breathingContent.root.visibility == View.VISIBLE -> binding.breathingContent.halCircleView
        else -> binding.breathingContentLand.halCircleView
    }

    private fun getCurrentProgressRing() = when {
        binding.breathingContent.root.visibility == View.VISIBLE -> binding.breathingContent.progressRing
        else -> binding.breathingContentLand.progressRing
    }

    /**
     * Adapter for the breathing pattern spinner
     */
    private inner class BreathingPatternAdapter(context: Context) :
        ArrayAdapter<BreathingPattern>(context, R.layout.item_spinner, mutableListOf()) {

        init {
            addAll(BreathingPattern.getAllPatterns())
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            return createItemView(position, convertView, parent)
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            return createDropDownItemView(position, convertView, parent)
        }

        private fun createItemView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_spinner, parent, false)
            val pattern = getItem(position)
            view.findViewById<TextView>(R.id.spinnerText).text = pattern?.name
            return view
        }

        private fun createDropDownItemView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_spinner_dropdown, parent, false)
            val pattern = getItem(position)
            view.findViewById<TextView>(R.id.spinnerText).text = pattern?.name
            return view
        }

        fun getPosition(pattern: BreathingPattern): Int {
            for (i in 0 until count) {
                if (getItem(i)?.name == pattern.name) {
                    return i
                }
            }
            return -1
        }
    }
}