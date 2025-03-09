package com.example.breathwell

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.breathwell.databinding.FragmentReminderSettingsBinding
import com.example.breathwell.notification.ReminderNotificationHelper

class ReminderSettingsFragment : Fragment() {

    private var _binding: FragmentReminderSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var reminderHelper: ReminderNotificationHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReminderSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        reminderHelper = ReminderNotificationHelper(requireContext())

        // Load current settings
        loadCurrentSettings()

        // Setup toggle switch
        binding.reminderSwitch.setOnCheckedChangeListener { _, isChecked ->
            binding.timePickerCard.visibility = if (isChecked) View.VISIBLE else View.GONE

            if (!isChecked) {
                // Cancel reminders if toggled off
                reminderHelper.cancelReminder()
                saveReminderSettings(false, 0, 0)
            }
        }

        // Setup save button
        binding.saveButton.setOnClickListener {
            val isEnabled = binding.reminderSwitch.isChecked
            val hour = binding.timePicker.hour
            val minute = binding.timePicker.minute

            // Save settings
            saveReminderSettings(isEnabled, hour, minute)

            // Schedule or cancel reminder
            if (isEnabled) {
                reminderHelper.scheduleDaily(hour, minute)
            } else {
                reminderHelper.cancelReminder()
            }

            // Navigate back properly with respect to fragment stack
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private fun loadCurrentSettings() {
        val sharedPrefs = requireActivity().getSharedPreferences("breathwell_prefs", Context.MODE_PRIVATE)
        val isEnabled = sharedPrefs.getBoolean("reminder_enabled", false)
        val hour = sharedPrefs.getInt("reminder_hour", 20) // Default: 8 PM
        val minute = sharedPrefs.getInt("reminder_minute", 0)

        // Set UI state
        binding.reminderSwitch.isChecked = isEnabled
        binding.timePickerCard.visibility = if (isEnabled) View.VISIBLE else View.GONE

        // Set time picker
        binding.timePicker.hour = hour
        binding.timePicker.minute = minute
    }

    private fun saveReminderSettings(isEnabled: Boolean, hour: Int, minute: Int) {
        val sharedPrefs = requireActivity().getSharedPreferences("breathwell_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putBoolean("reminder_enabled", isEnabled)
            putInt("reminder_hour", hour)
            putInt("reminder_minute", minute)
            apply()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}