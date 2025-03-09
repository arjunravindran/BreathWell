package com.example.breathwell

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.breathwell.databinding.FragmentSettingsBinding
import com.example.breathwell.notification.ReminderNotificationHelper
import com.example.breathwell.viewmodel.BreathingViewModel

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: BreathingViewModel
    private lateinit var reminderHelper: ReminderNotificationHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[BreathingViewModel::class.java]
        reminderHelper = ReminderNotificationHelper(requireContext())

        setupBreathingSettings()
        setupFeedbackSettings()
        setupMusicSettings()
        setupReminderSettings()
        setupApplyButton()
    }

    private fun setupBreathingSettings() {
        val customPattern = viewModel.customPattern.value ?: return

        // Set current custom values in input fields
        binding.inhaleInput.setText(customPattern.inhale.toString())
        binding.hold1Input.setText(customPattern.hold1.toString())
        binding.exhaleInput.setText(customPattern.exhale.toString())
        binding.hold2Input.setText(customPattern.hold2.toString())
    }

    private fun setupFeedbackSettings() {
        // Set initial switch states from ViewModel
        binding.soundSwitch.isChecked = viewModel.soundEnabled.value ?: true
        binding.vibrationSwitch.isChecked = viewModel.vibrationEnabled.value ?: true

        // Setup switch change listeners
        binding.soundSwitch.setOnCheckedChangeListener { _, _ ->
            viewModel.toggleSound()
        }

        binding.vibrationSwitch.setOnCheckedChangeListener { _, _ ->
            viewModel.toggleVibration()
        }

        // Observe changes from ViewModel to keep UI in sync
        viewModel.soundEnabled.observe(viewLifecycleOwner) { enabled ->
            if (binding.soundSwitch.isChecked != enabled) {
                binding.soundSwitch.isChecked = enabled
            }
        }

        viewModel.vibrationEnabled.observe(viewLifecycleOwner) { enabled ->
            if (binding.vibrationSwitch.isChecked != enabled) {
                binding.vibrationSwitch.isChecked = enabled
            }
        }
    }

    private fun setupMusicSettings() {
        // Set initial states from ViewModel
        binding.musicSwitch.isChecked = viewModel.musicEnabled.value ?: true
        binding.volumeSlider.value = viewModel.musicVolume.value ?: 0.3f

        // Show/hide volume slider based on initial music enabled state
        binding.volumeSettingsLayout.visibility = if (viewModel.musicEnabled.value == true) View.VISIBLE else View.GONE

        // Setup music switch listener
        binding.musicSwitch.setOnCheckedChangeListener { _, _ ->
            viewModel.toggleMusic()
        }

        // Setup volume slider listener
        binding.volumeSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                viewModel.setMusicVolume(value)
            }
        }

        // Observe changes from ViewModel
        viewModel.musicEnabled.observe(viewLifecycleOwner) { enabled ->
            if (binding.musicSwitch.isChecked != enabled) {
                binding.musicSwitch.isChecked = enabled
            }
            // Show/hide volume slider based on music enabled state
            binding.volumeSettingsLayout.visibility = if (enabled) View.VISIBLE else View.GONE
        }

        viewModel.musicVolume.observe(viewLifecycleOwner) { volume ->
            if (binding.volumeSlider.value != volume) {
                binding.volumeSlider.value = volume
            }
        }
    }

    private fun setupReminderSettings() {
        // Load current settings
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

        // Setup toggle switch
        binding.reminderSwitch.setOnCheckedChangeListener { _, isChecked ->
            binding.timePickerCard.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
    }

    private fun setupApplyButton() {
        binding.applyButton.setOnClickListener {
            // Get values from input fields
            val inhaleText = binding.inhaleInput.text.toString()
            val hold1Text = binding.hold1Input.text.toString()
            val exhaleText = binding.exhaleInput.text.toString()
            val hold2Text = binding.hold2Input.text.toString()

            // Parse values with default fallbacks if inputs are invalid
            val inhale = inhaleText.toIntOrNull() ?: 4
            val hold1 = hold1Text.toIntOrNull() ?: 4
            val exhale = exhaleText.toIntOrNull() ?: 4
            val hold2 = hold2Text.toIntOrNull() ?: 2

            // First update the custom pattern with the new values
            viewModel.updateCustomPattern(inhale, hold1, exhale, hold2)

            // Then set it as the active pattern
            viewModel.setActivePattern(viewModel.customPattern.value!!)

            // Apply reminder settings
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

            // Return to main screen
            (requireActivity() as MainActivity).hideAllFragments()
        }
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