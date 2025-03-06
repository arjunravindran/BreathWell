package com.example.breathwell

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.TransitionDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.example.breathwell.databinding.ActivityMainBinding
import com.example.breathwell.model.BreathPhase
import com.example.breathwell.model.BreathingPattern
import com.example.breathwell.notification.ReminderNotificationHelper
import com.example.breathwell.ui.views.HALCircleView
import com.example.breathwell.ui.views.ProgressRingView
import com.example.breathwell.viewmodel.BreathingViewModel
import com.example.breathwell.viewmodel.BreathingViewModelFactory

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: BreathingViewModel
    private lateinit var binding: ActivityMainBinding

    private var settingsFragment: SettingsFragment? = null
    private var habitTrackerFragment: HabitTrackerFragment? = null
    private var reminderSettingsFragment: ReminderSettingsFragment? = null

    private val breathingPatternAdapter by lazy { BreathingPatternAdapter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply our premium theme with edge-to-edge design
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Setup view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup view model with factory
        viewModel = ViewModelProvider(
            this,
            BreathingViewModelFactory(application)
        )[BreathingViewModel::class.java]

        // Setup UI components
        setupPatternSpinner()
        setupCyclesSeekBar()
        setupButtons()

        // Observe view model state
        observeViewModelState()

        // Setup back press handling with the new API
        setupBackPressHandling()

        // Initialize daily reminders if enabled
        initializeReminders()
    }

    private fun initializeReminders() {
        val sharedPrefs = getSharedPreferences("breathwell_prefs", Context.MODE_PRIVATE)
        val isEnabled = sharedPrefs.getBoolean("reminder_enabled", false)

        if (isEnabled) {
            val hour = sharedPrefs.getInt("reminder_hour", 20)
            val minute = sharedPrefs.getInt("reminder_minute", 0)

            val reminderHelper = ReminderNotificationHelper(this)
            reminderHelper.scheduleDaily(hour, minute)
        }
    }

    private fun setupBackPressHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    settingsFragment != null && settingsFragment!!.isVisible -> {
                        hideSettingsFragment()
                    }
                    habitTrackerFragment != null && habitTrackerFragment!!.isVisible -> {
                        hideHabitTrackerFragment()
                    }
                    reminderSettingsFragment != null && reminderSettingsFragment!!.isVisible -> {
                        supportFragmentManager.popBackStack()
                        reminderSettingsFragment = null
                    }
                    else -> {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })
    }

    private fun setupPatternSpinner() {
        val spinner = binding.breathingContent.patternSpinnerView
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

    private fun setupCyclesSeekBar() {
        val seekBar = binding.breathingContent.cyclesSeekBar
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val cycles = progress + 1
                binding.breathingContent.cyclesValue.text = cycles.toString()
                if (fromUser) {
                    viewModel.setTotalCycles(cycles)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // Do nothing
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // Do nothing
            }
        })
    }

    private fun setupButtons() {
        // Action button (Start/Stop)
        binding.breathingContent.actionButton.setOnClickListener {
            viewModel.toggleBreathing()
        }

        // Settings button
        binding.settingsButton.setOnClickListener {
            if (settingsFragment == null || !settingsFragment!!.isVisible) {
                showSettingsFragment()
            } else {
                hideSettingsFragment()
            }
        }

        // Habit Tracker button
        binding.habitTrackerButton.setOnClickListener {
            if (habitTrackerFragment == null || !habitTrackerFragment!!.isVisible) {
                showHabitTrackerFragment()
            } else {
                hideHabitTrackerFragment()
            }
        }

        // Reminder settings button
        binding.reminderButton.setOnClickListener {
            showReminderSettingsFragment()
        }
    }

    private fun observeViewModelState() {
        // Update pattern spinner selection
        viewModel.activePattern.observe(this) { pattern ->
            val position = breathingPatternAdapter.getPosition(pattern)
            if (position >= 0) {
                binding.breathingContent.patternSpinnerView.setSelection(position)
            }
        }

        // Update breath phase, animation, and instructions
        viewModel.breathPhase.observe(this) { phase ->
            // Update UI based on phase
            val instructionText = when (phase) {
                BreathPhase.INHALE -> getString(R.string.inhale)
                BreathPhase.HOLD1, BreathPhase.HOLD2 -> getString(R.string.hold)
                BreathPhase.EXHALE -> getString(R.string.exhale)
                BreathPhase.READY -> getString(R.string.ready)
                BreathPhase.COMPLETE -> getString(R.string.complete)
                null -> getString(R.string.ready) // Handle null case
            }

            binding.breathingContent.halCircleView.instruction = instructionText

            // Only show pulse animation during inhale and exhale phases
            binding.breathingContent.halCircleView.showPulseEffect =
                phase == BreathPhase.INHALE || phase == BreathPhase.EXHALE

            // Update breath colors
            binding.breathingContent.halCircleView.breathColor = viewModel.getBreathColor()
            binding.breathingContent.halCircleView.innerColor = viewModel.getInnerColor()

            // Update background gradient
            updateBackgroundGradient()
        }

        // Update counter display
        viewModel.counter.observe(this) { count ->
            binding.breathingContent.halCircleView.counter = count
        }

        // Update circle expansion
        viewModel.circleExpansion.observe(this) { expansion ->
            binding.breathingContent.halCircleView.expansion = expansion
        }

        // Update running state and control screen wakelock
        viewModel.isRunning.observe(this) { isRunning ->
            val button = binding.breathingContent.actionButton
            val spinner = binding.breathingContent.patternSpinnerView
            val seekBar = binding.breathingContent.cyclesSeekBar

            // Keep screen on during active breathing sessions
            if (isRunning) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                button.text = getString(R.string.stop)
                button.backgroundTintList = ColorStateList.valueOf(
                    resources.getColor(R.color.red_500, theme)
                )
                spinner.isEnabled = false
                seekBar.isEnabled = false
            } else {
                // Allow screen to turn off when not in a session
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                if (viewModel.breathPhase.value == BreathPhase.COMPLETE) {
                    button.text = getString(R.string.start_new_session)
                } else {
                    button.text = getString(R.string.start)
                }
                button.backgroundTintList = ColorStateList.valueOf(
                    resources.getColor(R.color.cyan_gradient_start, theme)
                )
                spinner.isEnabled = true
                seekBar.isEnabled = true
            }
        }

        // Update cycle counts
        viewModel.totalCycles.observe(this) { cycles ->
            binding.breathingContent.cyclesSeekBar.progress = cycles - 1
            binding.breathingContent.cyclesValue.text = cycles.toString()
            binding.progressRing.totalCycles = cycles
        }

        viewModel.currentCycle.observe(this) { cycle ->
            binding.progressRing.currentCycle = cycle
        }

        // Observe session completion for habit tracking
        viewModel.sessionCompleted.observe(this) { completed ->
            if (completed) {
                // Could show a completion message or animation
                // Optionally show the habit tracker
                // showHabitTrackerFragment()
            }
        }
    }

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

    private fun showSettingsFragment() {
        if (viewModel.isRunning.value == true) {
            viewModel.toggleBreathing() // Stop the session first
        }

        // Hide other fragments if visible
        if (habitTrackerFragment != null && habitTrackerFragment!!.isVisible) {
            hideHabitTrackerFragment()
        }

        if (reminderSettingsFragment != null && reminderSettingsFragment!!.isVisible) {
            supportFragmentManager.popBackStack()
            reminderSettingsFragment = null
        }

        // Create and show the settings fragment
        settingsFragment = SettingsFragment()

        supportFragmentManager.beginTransaction()
            .replace(R.id.contentContainer, settingsFragment!!)
            .addToBackStack(null)
            .commit()

        // Update settings icon
        binding.settingsIcon.setImageResource(R.drawable.ic_close)
    }

    private fun hideSettingsFragment() {
        supportFragmentManager.popBackStack()
        settingsFragment = null

        // Update settings icon
        binding.settingsIcon.setImageResource(R.drawable.ic_settings)
    }

    private fun showHabitTrackerFragment() {
        if (viewModel.isRunning.value == true) {
            viewModel.toggleBreathing() // Stop the session first
        }

        // Hide settings if visible
        if (settingsFragment != null && settingsFragment!!.isVisible) {
            hideSettingsFragment()
        }

        if (reminderSettingsFragment != null && reminderSettingsFragment!!.isVisible) {
            supportFragmentManager.popBackStack()
            reminderSettingsFragment = null
        }

        // Create and show the habit tracker fragment
        habitTrackerFragment = HabitTrackerFragment()

        supportFragmentManager.beginTransaction()
            .replace(R.id.contentContainer, habitTrackerFragment!!)
            .addToBackStack(null)
            .commit()

        // Update header icon
        binding.habitTrackerIcon.setImageResource(R.drawable.ic_close)
    }

    private fun hideHabitTrackerFragment() {
        supportFragmentManager.popBackStack()
        habitTrackerFragment = null

        // Update icon
        binding.habitTrackerIcon.setImageResource(R.drawable.ic_calendar)
    }

    private fun showReminderSettingsFragment() {
        if (viewModel.isRunning.value == true) {
            viewModel.toggleBreathing() // Stop the session first
        }

        // Hide other fragments if visible
        if (settingsFragment != null && settingsFragment!!.isVisible) {
            hideSettingsFragment()
        }

        if (habitTrackerFragment != null && habitTrackerFragment!!.isVisible) {
            hideHabitTrackerFragment()
        }

        // Create and show the reminder settings fragment
        reminderSettingsFragment = ReminderSettingsFragment()

        supportFragmentManager.beginTransaction()
            .replace(R.id.contentContainer, reminderSettingsFragment!!)
            .addToBackStack(null)
            .commit()
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
            val view = convertView ?: layoutInflater.inflate(R.layout.item_spinner, parent, false)
            val pattern = getItem(position)
            view.findViewById<TextView>(R.id.spinnerText).text = pattern?.name
            return view
        }

        private fun createDropDownItemView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.item_spinner_dropdown, parent, false)
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