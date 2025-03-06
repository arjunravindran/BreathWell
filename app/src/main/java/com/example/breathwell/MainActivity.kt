package com.example.breathwell

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.breathwell.databinding.ActivityMainBinding
import com.example.breathwell.managers.AnimationManager
import com.example.breathwell.managers.FragmentManager
import com.example.breathwell.managers.OrientationManager
import com.example.breathwell.managers.PowerSavingManager
import com.example.breathwell.notification.ReminderNotificationHelper
import com.example.breathwell.ui.BreathingUIController
import com.example.breathwell.utils.BatteryOptimizationUtils
import com.example.breathwell.viewmodel.BreathingViewModel
import com.example.breathwell.viewmodel.BreathingViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: BreathingViewModel
    private lateinit var binding: ActivityMainBinding

    // Managers
    private lateinit var orientationManager: OrientationManager
    private lateinit var fragmentManager: FragmentManager
    private lateinit var powerSavingManager: PowerSavingManager
    private lateinit var animationManager: AnimationManager

    // UI Controllers
    private lateinit var breathingUIController: BreathingUIController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply edge-to-edge design
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Setup view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup ViewModel
        setupViewModel()

        // Initialize managers
        initializeManagers()

        // Setup UI controllers
        setupUIControllers()

        // Setup back press handling
        setupBackPressHandling()

        // Check battery optimization
        checkBatteryOptimization()

        // Initialize reminders if enabled
        initializeReminders()
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(
            this,
            BreathingViewModelFactory(application)
        )[BreathingViewModel::class.java]
    }

    private fun initializeManagers() {
        // Create and initialize all managers
        orientationManager = OrientationManager(this, binding, viewModel)
        fragmentManager = FragmentManager(this, binding, viewModel)
        powerSavingManager = PowerSavingManager(this, viewModel)
        animationManager = AnimationManager(binding, viewModel)

        // Set up power saving mode adaptations
        powerSavingManager.setupPowerSavingMode()
    }

    private fun setupUIControllers() {
        // Create and initialize UI controllers
        breathingUIController = BreathingUIController(this, binding, viewModel)

        // Setup UI components and observers
        breathingUIController.setupPatternSpinner()
        breathingUIController.setupCyclesControl()
        breathingUIController.setupActionButtons()
        breathingUIController.setupAccessibility()
        breathingUIController.observeViewModelState()
    }

    private fun checkBatteryOptimization() {
        lifecycleScope.launch {
            BatteryOptimizationUtils.requestDisableBatteryOptimization(this@MainActivity)
        }
    }

    private fun setupBackPressHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (fragmentManager.handleBackPress()) {
                    // Fragment manager handled the back press
                    return
                }

                // Normal back behavior
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        })
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        orientationManager.handleConfigurationChange(newConfig)
    }

    // Public methods for fragment interaction
    fun showSettingsFragment() {
        fragmentManager.showSettingsFragment()
    }

    fun hideSettingsFragment() {
        fragmentManager.hideSettingsFragment()
    }

    fun showHabitTrackerFragment() {
        fragmentManager.showHabitTrackerFragment()
    }

    fun hideHabitTrackerFragment() {
        fragmentManager.hideHabitTrackerFragment()
    }

    fun showReminderSettingsFragment() {
        fragmentManager.showReminderSettingsFragment()
    }

    fun updateScreenWakeState(keepAwake: Boolean) {
        if (keepAwake) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}