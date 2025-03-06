package com.example.breathwell.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AlertDialog

// Enums defined at top of file
enum class PowerSavingMode {
    NONE,       // No power saving
    MEDIUM,     // Medium power saving (app is optimized but not in power save mode)
    HIGH        // High power saving (device in power save mode)
}

enum class AnimationQuality {
    FULL,       // Full animations
    REDUCED,    // Reduced animations
    MINIMAL     // Minimal animations
}

/**
 * Utility class for managing battery optimization settings
 * This class provides functions to optimize battery usage while keeping critical features functional
 */
object BatteryOptimizationUtils {

    /**
     * Checks if the app is exempt from battery optimizations
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val packageName = context.packageName
        return powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    /**
     * Requests the user to disable battery optimization for the app
     * This is needed for reliable alarm/reminder functionality
     */
    fun requestDisableBatteryOptimization(activity: Activity) {
        if (isIgnoringBatteryOptimizations(activity)) return

        AlertDialog.Builder(activity)
            .setTitle("Battery Optimization")
            .setMessage("For reliable reminders, please disable battery optimization")
            .setPositiveButton("Disable Optimization") { _, _ ->
                val intent = Intent().apply {
                    action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    data = Uri.parse("package:${activity.packageName}")
                }
                activity.startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Low-power mode detection
     */
    fun isInPowerSaveMode(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isPowerSaveMode
    }

    /**
     * Adjust app behavior based on power state
     */
    fun adaptToPowerSaving(context: Context): PowerSavingMode {
        return when {
            isInPowerSaveMode(context) -> PowerSavingMode.HIGH
            !isIgnoringBatteryOptimizations(context) -> PowerSavingMode.MEDIUM
            else -> PowerSavingMode.NONE
        }
    }

    /**
     * Adjusts animation quality based on power saving mode
     */
    fun getAnimationQuality(powerSavingMode: PowerSavingMode): AnimationQuality {
        return when (powerSavingMode) {
            PowerSavingMode.HIGH -> AnimationQuality.MINIMAL
            PowerSavingMode.MEDIUM -> AnimationQuality.REDUCED
            PowerSavingMode.NONE -> AnimationQuality.FULL
        }
    }

    /**
     * Gets recommended update interval for UI components in milliseconds
     */
    fun getRecommendedUpdateInterval(powerSavingMode: PowerSavingMode): Long {
        return when (powerSavingMode) {
            PowerSavingMode.HIGH -> 1000    // 1 second update in high power saving
            PowerSavingMode.MEDIUM -> 500   // 0.5 second in medium power saving
            PowerSavingMode.NONE -> 100     // 0.1 second in normal mode
        }
    }
}