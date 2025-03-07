package com.example.breathwell

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.breathwell.databinding.ActivityMainBinding
import com.example.breathwell.model.BreathingPattern
import com.example.breathwell.notification.ReminderNotificationHelper
import com.example.breathwell.ui.BreathingUIController
import com.example.breathwell.utils.AccessibilityUtils
import com.example.breathwell.utils.BatteryOptimizationUtils
import com.example.breathwell.utils.SoundEffectHelper
import com.example.breathwell.utils.VibrationHelper
import com.example.breathwell.viewmodel.BreathingViewModel
import com.example.breathwell.viewmodel.BreathingViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: BreathingViewModel
    private lateinit var binding: ActivityMainBinding
    private lateinit var breathingUIController: BreathingUIController
    private lateinit var soundEffectHelper: SoundEffectHelper
    private lateinit var vibrationHelper: VibrationHelper

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
        const val NO_FRAGMENT = 0
        const val SETTINGS_FRAGMENT = 1
        const val HABIT_TRACKER_FRAGMENT = 2
        const val REMINDER_SETTINGS_FRAGMENT = 3
    }

    // Track current active fragment
    private var activeFragment = NO_FRAGMENT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply edge-to-edge design
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Setup view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ViewModel
        viewModel = ViewModelProvider(
            this,
            BreathingViewModelFactory(application)
        )[BreathingViewModel::class.java]

        // Request needed permissions
        requestPermissions()

        // Initialize components
        initializeComponents()

        // Setup back press handling
        setupBackPressHandling()

        // Initialize reminders if enabled
        initializeReminders()

        // Check battery optimization
        lifecycleScope.launch {
            BatteryOptimizationUtils.requestDisableBatteryOptimization(this@MainActivity)
        }
    }

    private fun requestPermissions() {
        // Check for vibration permission
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.VIBRATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.VIBRATE),
                PERMISSION_REQUEST_CODE
            )
        }

        // For Android 13+ request notification permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun initializeComponents() {
        // Initialize feedback helpers
        soundEffectHelper = SoundEffectHelper()
        vibrationHelper = VibrationHelper(this)

        // Create UI controller
        breathingUIController = BreathingUIController(this, binding, viewModel)

        // Setup UI components
        breathingUIController.setupPatternSpinner()
        breathingUIController.setupCyclesControl()
        breathingUIController.setupActionButtons()
        breathingUIController.setupAccessibility()
        breathingUIController.observeViewModelState()

        // Observe feedback settings
        viewModel.vibrationEnabled.observe(this) { enabled ->
            vibrationHelper.setVibrationEnabled(enabled)
        }

        viewModel.soundEnabled.observe(this) { enabled ->
            soundEffectHelper.setSoundEnabled(enabled)
        }

        // Observe screen wake state
        viewModel.isRunning.observe(this) { isRunning ->
            updateScreenWakeState(isRunning)
        }

        // Observe phase transitions for feedback
        viewModel.phaseTransitionEvent.observe(this) { phase ->
            phase?.let {
                vibrationHelper.vibrateForPhase(it)
                soundEffectHelper.playPhaseTransitionSound(it)
            }
        }

        // Setup power saving mode
        viewModel.setPowerSavingMode(BatteryOptimizationUtils.adaptToPowerSaving(this))

        // Setup navigation buttons
        setupNavigationButtons()
    }

    private fun setupNavigationButtons() {
        binding.settingsButton.setOnClickListener {
            if (activeFragment == SETTINGS_FRAGMENT) {
                hideSettingsFragment()
            } else {
                showSettingsFragment()
            }
        }


        binding.habitTrackerButton.setOnClickListener {
            if (activeFragment == HABIT_TRACKER_FRAGMENT) {
                hideHabitTrackerFragment()
            } else {
                showHabitTrackerFragment()
            }
        }

        // Check if reset button exists in the layout before setting it up
        val resetButton = binding.root.findViewById<View>(R.id.resetButton)
        if (resetButton != null) {
            resetButton.setOnClickListener {
                resetApplication()
            }

            AccessibilityUtils.setupAccessibilityForButton(
                resetButton,
                getString(R.string.reset),
                getString(R.string.accessibility_reset)
            )

            val resetIcon = binding.root.findViewById<ImageView>(R.id.resetIcon)
            if (resetIcon != null) {
                AccessibilityUtils.setContentDescription(
                    resetIcon,
                    getString(R.string.reset)
                )
            }
        }
    }

    /**
     * Resets the application to its initial state
     * This should be called when the reset button is clicked
     */
    private fun resetApplication() {
        // Stop any ongoing session
        if (viewModel.isRunning.value == true) {
            viewModel.toggleBreathing()
        }

        // Reset breathing phase to READY
        viewModel.resetBreathingPhase()

        // Reset to default breathing pattern
        viewModel.setActivePattern(BreathingPattern.BOX_BREATHING)

        // Reset cycles to default value
        viewModel.setTotalCycles(5)

        // Reset current cycle count
        viewModel.resetCurrentCycle()

        // Ensure we're showing the main breathing content
        if (activeFragment != NO_FRAGMENT) {
            supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            activeFragment = NO_FRAGMENT
            showBreathingContent()

            // Reset navigation buttons
            binding.settingsIcon.setImageResource(R.drawable.ic_settings)
            binding.habitTrackerIcon.setImageResource(R.drawable.ic_calendar)
        }

        // Reset UI state
        breathingUIController.refreshUIState()
    }

    private fun setupBackPressHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (activeFragment) {
                    SETTINGS_FRAGMENT -> {
                        hideSettingsFragment()
                    }
                    HABIT_TRACKER_FRAGMENT -> {
                        hideHabitTrackerFragment()
                    }
                    REMINDER_SETTINGS_FRAGMENT -> {
                        supportFragmentManager.popBackStack()
                        activeFragment = NO_FRAGMENT
                    }
                    else -> {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })
    }

    private fun initializeReminders() {
        val sharedPrefs = getSharedPreferences("breathwell_prefs", Context.MODE_PRIVATE)
        val isEnabled = sharedPrefs.getBoolean("reminder_enabled", false)

        if (isEnabled) {
            val hour = sharedPrefs.getInt("reminder_hour", 20)
            val minute = sharedPrefs.getInt("reminder_minute", 0)
            ReminderNotificationHelper(this).scheduleDaily(hour, minute)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateLayoutForOrientation(newConfig.orientation)
        breathingUIController.refreshUIState()
    }

    private fun updateLayoutForOrientation(orientation: Int) {
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            binding.breathingContent.root.visibility = View.GONE
            binding.breathingContentLand.root.visibility = View.VISIBLE
        } else {
            binding.breathingContent.root.visibility = View.VISIBLE
            binding.breathingContentLand.root.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        breathingUIController.refreshUIState()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::soundEffectHelper.isInitialized) {
            soundEffectHelper.release()
        }
    }

    // Fragment management methods
    fun showSettingsFragment() {
        if (viewModel.isRunning.value == true) {
            viewModel.toggleBreathing() // Stop the session first
        }

        hideBreathingContent()

        val settingsFragment = SettingsFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.contentContainer, settingsFragment)
            .addToBackStack(null)
            .commit()

        binding.settingsIcon.setImageResource(R.drawable.ic_close)
        activeFragment = SETTINGS_FRAGMENT
    }

    fun hideSettingsFragment() {
        supportFragmentManager.popBackStack()
        showBreathingContent()
        binding.settingsIcon.setImageResource(R.drawable.ic_settings)
        activeFragment = NO_FRAGMENT
    }

    fun showHabitTrackerFragment() {
        if (viewModel.isRunning.value == true) {
            viewModel.toggleBreathing() // Stop the session first
        }

        hideBreathingContent()

        val habitTrackerFragment = HabitTrackerFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.contentContainer, habitTrackerFragment)
            .addToBackStack(null)
            .commit()

        binding.habitTrackerIcon.setImageResource(R.drawable.ic_close)
        activeFragment = HABIT_TRACKER_FRAGMENT
    }

    fun hideHabitTrackerFragment() {
        supportFragmentManager.popBackStack()
        showBreathingContent()
        binding.habitTrackerIcon.setImageResource(R.drawable.ic_calendar)
        activeFragment = NO_FRAGMENT
    }

    fun showReminderSettingsFragment() {
        if (viewModel.isRunning.value == true) {
            viewModel.toggleBreathing() // Stop the session first
        }

        hideBreathingContent()

        val reminderSettingsFragment = ReminderSettingsFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.contentContainer, reminderSettingsFragment)
            .addToBackStack(null)
            .commit()

        activeFragment = REMINDER_SETTINGS_FRAGMENT
    }

    fun updateScreenWakeState(keepAwake: Boolean) {
        if (keepAwake) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun hideBreathingContent() {
        binding.breathingContent.root.visibility = View.GONE
        binding.breathingContentLand.root.visibility = View.GONE
    }

    private fun showBreathingContent() {
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            binding.breathingContentLand.root.visibility = View.VISIBLE
        } else {
            binding.breathingContent.root.visibility = View.VISIBLE
        }
    }
}