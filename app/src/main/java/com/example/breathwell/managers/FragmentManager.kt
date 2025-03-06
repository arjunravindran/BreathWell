package com.example.breathwell.managers

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.breathwell.HabitTrackerFragment
import com.example.breathwell.R
import com.example.breathwell.ReminderSettingsFragment
import com.example.breathwell.SettingsFragment
import com.example.breathwell.databinding.ActivityMainBinding
import com.example.breathwell.viewmodel.BreathingViewModel

/**
 * Manages fragment navigation and handling in the application
 */
class FragmentManager(
    private val activity: AppCompatActivity,
    private val binding: ActivityMainBinding,
    private val viewModel: BreathingViewModel
) {
    // Reference to active fragments
    private var settingsFragment: SettingsFragment? = null
    private var habitTrackerFragment: HabitTrackerFragment? = null
    private var reminderSettingsFragment: ReminderSettingsFragment? = null

    /**
     * Handle back button press based on visible fragment
     * @return true if back press was handled, false otherwise
     */
    fun handleBackPress(): Boolean {
        return when {
            settingsFragment != null && settingsFragment!!.isVisible -> {
                hideSettingsFragment()
                true
            }
            habitTrackerFragment != null && habitTrackerFragment!!.isVisible -> {
                hideHabitTrackerFragment()
                true
            }
            reminderSettingsFragment != null && reminderSettingsFragment!!.isVisible -> {
                activity.supportFragmentManager.popBackStack()
                reminderSettingsFragment = null
                true
            }
            else -> false
        }
    }

    /**
     * Shows settings fragment
     */
    fun showSettingsFragment() {
        if (viewModel.isRunning.value == true) {
            viewModel.toggleBreathing() // Stop the session first
        }

        // Hide other fragments if visible
        hideOtherFragments(FragmentType.SETTINGS)

        // Hide the breathing content
        hideBreathingContent()

        // Create and show the settings fragment
        settingsFragment = SettingsFragment()

        activity.supportFragmentManager.beginTransaction()
            .replace(R.id.contentContainer, settingsFragment!!)
            .addToBackStack(null)
            .commit()

        // Update settings icon
        binding.settingsIcon.setImageResource(R.drawable.ic_close)

        // Add click listener to close button
        binding.settingsButton.setOnClickListener {
            hideSettingsFragment()
        }
    }

    /**
     * Hides settings fragment
     */
    fun hideSettingsFragment() {
        activity.supportFragmentManager.popBackStack()
        settingsFragment = null

        // Show the main content again
        showBreathingContent()

        // Update settings icon
        binding.settingsIcon.setImageResource(R.drawable.ic_settings)

        // Reset click listener to open settings
        binding.settingsButton.setOnClickListener {
            if (activity is com.example.breathwell.MainActivity) {
                activity.showSettingsFragment()
            }
        }
    }

    /**
     * Shows habit tracker fragment
     */
    fun showHabitTrackerFragment() {
        if (viewModel.isRunning.value == true) {
            viewModel.toggleBreathing() // Stop the session first
        }

        // Hide other fragments if visible
        hideOtherFragments(FragmentType.HABIT_TRACKER)

        // Hide the breathing content
        hideBreathingContent()

        // Create and show the habit tracker fragment
        habitTrackerFragment = HabitTrackerFragment()

        activity.supportFragmentManager.beginTransaction()
            .replace(R.id.contentContainer, habitTrackerFragment!!)
            .addToBackStack(null)
            .commit()

        // Update header icon
        binding.habitTrackerIcon.setImageResource(R.drawable.ic_close)

        // Add click listener to close button
        binding.habitTrackerButton.setOnClickListener {
            hideHabitTrackerFragment()
        }
    }

    /**
     * Hides habit tracker fragment
     */
    fun hideHabitTrackerFragment() {
        activity.supportFragmentManager.popBackStack()
        habitTrackerFragment = null

        // Show the main content again
        showBreathingContent()

        // Update icon
        binding.habitTrackerIcon.setImageResource(R.drawable.ic_calendar)

        // Reset click listener to open habit tracker
        binding.habitTrackerButton.setOnClickListener {
            if (activity is com.example.breathwell.MainActivity) {
                activity.showHabitTrackerFragment()
            }
        }
    }

    /**
     * Shows reminder settings fragment
     */
    fun showReminderSettingsFragment() {
        if (viewModel.isRunning.value == true) {
            viewModel.toggleBreathing() // Stop the session first
        }

        // Hide other fragments if visible
        hideOtherFragments(FragmentType.REMINDER_SETTINGS)

        // Hide the breathing content
        hideBreathingContent()

        // Create and show the reminder settings fragment
        reminderSettingsFragment = ReminderSettingsFragment()

        activity.supportFragmentManager.beginTransaction()
            .replace(R.id.contentContainer, reminderSettingsFragment!!)
            .addToBackStack(null)
            .commit()
    }

    /**
     * Hide fragments that aren't of the specified type
     */
    private fun hideOtherFragments(showingType: FragmentType) {
        if (showingType != FragmentType.SETTINGS && settingsFragment != null && settingsFragment!!.isVisible) {
            hideSettingsFragment()
        }

        if (showingType != FragmentType.HABIT_TRACKER && habitTrackerFragment != null && habitTrackerFragment!!.isVisible) {
            hideHabitTrackerFragment()
        }

        if (showingType != FragmentType.REMINDER_SETTINGS && reminderSettingsFragment != null && reminderSettingsFragment!!.isVisible) {
            activity.supportFragmentManager.popBackStack()
            reminderSettingsFragment = null
        }
    }

    /**
     * Hide breathing content
     */
    private fun hideBreathingContent() {
        binding.breathingContent.root.visibility = View.GONE
        binding.breathingContentLand.root.visibility = View.GONE
    }

    /**
     * Show breathing content based on current orientation
     */
    private fun showBreathingContent() {
        binding.breathingContent.root.visibility = View.VISIBLE
        if (activity.resources.configuration.orientation ==
            android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            binding.breathingContentLand.root.visibility = View.VISIBLE
        }
    }

    /**
     * Fragment types in the application
     */
    enum class FragmentType {
        SETTINGS,
        HABIT_TRACKER,
        REMINDER_SETTINGS
    }
}