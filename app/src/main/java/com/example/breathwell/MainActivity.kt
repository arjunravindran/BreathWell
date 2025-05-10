package com.example.breathwell

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.example.breathwell.databinding.ActivityMainBinding
import com.example.breathwell.notification.ReminderNotificationHelper
import com.example.breathwell.ui.BreathingUIController
import com.example.breathwell.utils.BackgroundMusicHelper
import com.example.breathwell.utils.SoundEffectHelper
import com.example.breathwell.utils.VibrationHelper
import com.example.breathwell.viewmodel.BreathingViewModel
import com.example.breathwell.viewmodel.BreathingViewModelFactory

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: BreathingViewModel
    private lateinit var binding: ActivityMainBinding
    private lateinit var breathingUIController: BreathingUIController
    private lateinit var soundEffectHelper: SoundEffectHelper
    private lateinit var vibrationHelper: VibrationHelper
    private lateinit var backgroundMusicHelper: BackgroundMusicHelper

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
        const val NO_FRAGMENT = 0
        const val SETTINGS_FRAGMENT = 1
        const val HABIT_TRACKER_FRAGMENT = 2
        const val REMINDER_SETTINGS_FRAGMENT = 3
        const val BREATHING_TECHNIQUES_FRAGMENT = 4
        const val BREATHING_DETAIL_FRAGMENT = 5
        const val ANALYTICS_FRAGMENT = 6
        const val PROGRAMS_FRAGMENT = 7
        const val PROGRAM_DETAIL_FRAGMENT = 8
    }

    // Track current active fragment
    var activeFragment = NO_FRAGMENT

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

        // Initialize background music
        backgroundMusicHelper = BackgroundMusicHelper(this)
        lifecycle.addObserver(backgroundMusicHelper)
        backgroundMusicHelper.initialize(R.raw.background_music)

        // Set up observers for music settings
        viewModel.musicEnabled.observe(this) { enabled ->
            backgroundMusicHelper.setMusicEnabled(enabled)
        }

        viewModel.musicVolume.observe(this) { volume ->
            backgroundMusicHelper.setVolume(volume)
        }

        // Restore fragment state if needed
        if (savedInstanceState != null) {
            activeFragment = savedInstanceState.getInt("activeFragment", NO_FRAGMENT)
            updateUIForActiveFragment()
        }

        // Check if this activity was started from widget to start breathing
        if (intent?.action == "com.example.breathwell.START_BREATHING") {
            // Make sure we're on main screen
            hideAllFragments()

            /* Use a slight delay to ensure UI is ready before starting */
            Handler(Looper.getMainLooper()).postDelayed({
                // Start breathing session
                viewModel.toggleBreathing()
            }, 500)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("activeFragment", activeFragment)
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
        soundEffectHelper = SoundEffectHelper(this)
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

        // Setup navigation buttons
        setupNavigationButtons()
    }

    private fun setupNavigationButtons() {
        binding.settingsButton.setOnClickListener {
            if (activeFragment == SETTINGS_FRAGMENT) {
                hideAllFragments()
            } else {
                showSettingsFragment()
            }
        }

        binding.habitTrackerButton.setOnClickListener {
            if (activeFragment == HABIT_TRACKER_FRAGMENT) {
                hideAllFragments()
            } else {
                showHabitTrackerFragment()
            }
        }
    }

    private fun setupBackPressHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (activeFragment) {
                    SETTINGS_FRAGMENT, HABIT_TRACKER_FRAGMENT -> {
                        hideAllFragments()
                    }
                    REMINDER_SETTINGS_FRAGMENT -> {
                        supportFragmentManager.popBackStack()
                        // Check what the current top fragment is
                        activeFragment = if (supportFragmentManager.backStackEntryCount > 0) {
                            val lastEntry = supportFragmentManager.getBackStackEntryAt(supportFragmentManager.backStackEntryCount - 1)
                            when (lastEntry.name) {
                                "settings" -> SETTINGS_FRAGMENT
                                "habit_tracker" -> HABIT_TRACKER_FRAGMENT
                                else -> NO_FRAGMENT
                            }
                        } else {
                            NO_FRAGMENT
                        }
                        updateUIForActiveFragment()
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
        updateUIForActiveFragment()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(backgroundMusicHelper)
        if (::soundEffectHelper.isInitialized) {
            soundEffectHelper.release()
        }
        if (::backgroundMusicHelper.isInitialized) {
            backgroundMusicHelper.release()
        }
    }

    // Fragment management methods
    private fun showSettingsFragment() {
        if (viewModel.isRunning.value == true) {
            viewModel.toggleBreathing() // Stop the session first
        }

        // Clear backstack and hide any existing fragments
        clearBackStackAndFragments()

        hideBreathingContent()

        val settingsFragment = SettingsFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.contentContainer, settingsFragment)
            .addToBackStack("settings")
            .commit()

        binding.settingsIcon.setImageResource(R.drawable.ic_close)
        binding.habitTrackerIcon.setImageResource(R.drawable.ic_calendar)
        activeFragment = SETTINGS_FRAGMENT
    }

    private fun showHabitTrackerFragment() {
        if (viewModel.isRunning.value == true) {
            viewModel.toggleBreathing() // Stop the session first
        }

        // Clear backstack and hide any existing fragments
        clearBackStackAndFragments()

        hideBreathingContent()

        val habitTrackerFragment = HabitTrackerFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.contentContainer, habitTrackerFragment)
            .addToBackStack("habit_tracker")
            .commit()

        binding.habitTrackerIcon.setImageResource(R.drawable.ic_close)
        binding.settingsIcon.setImageResource(R.drawable.ic_settings)
        activeFragment = HABIT_TRACKER_FRAGMENT
    }

    private fun updateScreenWakeState(keepAwake: Boolean) {
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

    // This method is public so it can be called from fragments
    fun hideAllFragments() {
        clearBackStackAndFragments()
        showBreathingContent()
        binding.settingsIcon.setImageResource(R.drawable.ic_settings)
        binding.habitTrackerIcon.setImageResource(R.drawable.ic_calendar)
        activeFragment = NO_FRAGMENT
    }

    private fun clearBackStackAndFragments() {
        // Clear the entire back stack
        supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)

        // Find and remove any fragments still in the container
        val currentFragment = supportFragmentManager.findFragmentById(R.id.contentContainer)
        if (currentFragment != null) {
            supportFragmentManager.beginTransaction()
                .remove(currentFragment)
                .commit()
        }
    }

    private fun updateUIForActiveFragment() {
        when (activeFragment) {
            NO_FRAGMENT -> {
                showBreathingContent()
                binding.settingsIcon.setImageResource(R.drawable.ic_settings)
                binding.habitTrackerIcon.setImageResource(R.drawable.ic_calendar)
            }
            SETTINGS_FRAGMENT -> {
                hideBreathingContent()
                binding.settingsIcon.setImageResource(R.drawable.ic_close)
                binding.habitTrackerIcon.setImageResource(R.drawable.ic_calendar)
            }
            HABIT_TRACKER_FRAGMENT -> {
                hideBreathingContent()
                binding.settingsIcon.setImageResource(R.drawable.ic_settings)
                binding.habitTrackerIcon.setImageResource(R.drawable.ic_close)
            }
            REMINDER_SETTINGS_FRAGMENT -> {
                hideBreathingContent()
            }
        }
    }
}