package com.example.breathwell

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import com.example.breathwell.utils.SoundEffectHelper
import com.example.breathwell.utils.VibrationHelper
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

    // Feedback helpers
    private lateinit var soundEffectHelper: SoundEffectHelper
    private lateinit var vibrationHelper: VibrationHelper

    // UI Controllers
    private lateinit var breathingUIController: BreathingUIController

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply edge-to-edge design
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Setup view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup ViewModel
        setupViewModel()

        // Request permissions needed for the app
        requestPermissions()

        // Initialize managers
        initializeManagers()

        // Setup UI controllers
        setupUIControllers()

        // Setup back press handling
        setupBackPressHandling()

        // Initialize reminders if enabled
        initializeReminders()
    }

    private fun requestPermissions() {
        // For vibration
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.VIBRATE
            ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.VIBRATE),
                PERMISSION_REQUEST_CODE
            )
        }

        // For Android 13+ we need to request notification permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
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

        // Initialize feedback helpers
        soundEffectHelper = SoundEffectHelper()
        vibrationHelper = VibrationHelper(this)

        // Add animation manager as lifecycle observer for proper state handling
        lifecycle.addObserver(animationManager)

        // Set up power saving mode adaptations
        powerSavingManager.setupPowerSavingMode()

        // Set up phase transition observer for feedback
        viewModel.phaseTransitionEvent.observe(this) { phase ->
            phase?.let {
                // Provide haptic and sound feedback at each phase transition
                vibrationHelper.vibrateForPhase(it)
                soundEffectHelper.playPhaseTransitionSound(it)
            }
        }

        // Observe feedback settings
        viewModel.vibrationEnabled.observe(this) { enabled ->
            vibrationHelper.setVibrationEnabled(enabled)
        }

        viewModel.soundEnabled.observe(this) { enabled ->
            soundEffectHelper.setSoundEnabled(enabled)
        }
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Reinitialize helpers after permission results
            if (::vibrationHelper.isInitialized && ::soundEffectHelper.isInitialized) {
                // Nothing to do - the helpers will check permissions internally
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        orientationManager.handleConfigurationChange(newConfig)

        // Restore animation state after configuration change
        animationManager.restoreAnimationState()
    }

    override fun onResume() {
        super.onResume()
        // Refresh UI state when returning to the app
        breathingUIController.refreshUIState()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Clean up resources
        if (::animationManager.isInitialized) {
            lifecycle.removeObserver(animationManager)
            animationManager.cleanup()
        }

        if (::powerSavingManager.isInitialized) {
            powerSavingManager.cleanup()
        }

        // Clean up sound resources
        if (::soundEffectHelper.isInitialized) {
            soundEffectHelper.release()
        }
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