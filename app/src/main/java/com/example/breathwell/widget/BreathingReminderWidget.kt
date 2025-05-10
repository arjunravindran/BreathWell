package com.example.breathwell.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.RemoteViews
import com.example.breathwell.MainActivity
import com.example.breathwell.R
import com.example.breathwell.data.AppDatabase
import java.time.LocalDate
import java.util.Random

/**
 * Implementation of App Widget that shows breathing reminders and streak information
 * Styled like the Duolingo widget to encourage users to maintain their breathing practice
 */
class BreathingReminderWidget : AppWidgetProvider() {

    companion object {
        // Reminder messages when user hasn't practiced today
        private val REMINDER_MESSAGES = arrayOf(
            "Get started early!",
            "Keep your streak going!",
            "Time for mindful breathing!",
            "Don't miss your practice today!",
            "Take a breathing break!",
            "Breathe and be present!",
            "Maintain your %d day streak!",
            "A moment of calm awaits!"
        )

        // Congratulation messages when user has completed today's practice
        private val CONGRATULATION_MESSAGES = arrayOf(
            "Great job! %d day streak!",
            "You're on fire! %d days!",
            "Streak: %d days! Amazing!",
            "%d days of mindfulness!"
        )

        /**
         * Updates all breathing widgets
         * Called when a breathing session is completed
         */
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, BreathingReminderWidget::class.java)
            )

            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, BreathingReminderWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Use a thread for database access
        Thread {
            try {
                // Access database directly
                val dao = AppDatabase.getDatabase(context).breathingSessionDao()
                val completedDates = dao.getAllCompletedDatesSync()

                // Calculate streak
                val streak = calculateStreak(completedDates)
                val hasDoneToday = completedDates.contains(LocalDate.now())

                // Update UI on main thread
                Handler(Looper.getMainLooper()).post {
                    for (appWidgetId in appWidgetIds) {
                        updateAppWidget(context, appWidgetManager, appWidgetId, streak, hasDoneToday)
                    }
                }
            } catch (e: Exception) {
                // If there's an error, update with default values
                Handler(Looper.getMainLooper()).post {
                    for (appWidgetId in appWidgetIds) {
                        updateAppWidget(context, appWidgetManager, appWidgetId, 0, false)
                    }
                }
            }
        }.start()
    }

    private fun calculateStreak(dates: List<LocalDate>): Int {
        var streak = 0
        var currentDate = LocalDate.now()

        // Check yesterday if not completed today
        if (!dates.contains(currentDate)) {
            currentDate = currentDate.minusDays(1)
        }

        // Count consecutive days
        while (dates.contains(currentDate)) {
            streak++
            currentDate = currentDate.minusDays(1)
        }

        return streak
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        streak: Int,
        hasDoneToday: Boolean
    ) {
        // Create RemoteViews
        val views = RemoteViews(context.packageName, R.layout.widget_breathing_reminder)

        // Prepare content based on status
        if (hasDoneToday) {
            // Congratulations mode
            val randomMessageIdx = Random().nextInt(CONGRATULATION_MESSAGES.size)
            val message = String.format(CONGRATULATION_MESSAGES[randomMessageIdx], streak)

            views.setTextViewText(R.id.widget_message, message)
            views.setTextViewText(R.id.widget_streak_count, streak.toString())
            views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_rounded_bg_success)
        } else {
            // Reminder mode
            val randomMessageIdx = Random().nextInt(REMINDER_MESSAGES.size)
            var message = REMINDER_MESSAGES[randomMessageIdx]
            if (message.contains("%d")) {
                message = String.format(message, streak)
            }

            views.setTextViewText(R.id.widget_message, message)
            views.setTextViewText(R.id.widget_streak_count, streak.toString())
            views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_rounded_bg)
        }

        // Set up intent to open app when widget is clicked
        val intent = Intent(context, MainActivity::class.java).apply {
            // If not done today, add action to start breathing session immediately
            if (!hasDoneToday) {
                action = "com.example.breathwell.START_BREATHING"
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        // Update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    override fun onEnabled(context: Context) {
        // Called when the first widget is created
        super.onEnabled(context)
        // Force an update when widget is first added
        updateAllWidgets(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        // Handle custom actions if needed
        if (intent.action == "com.example.breathwell.UPDATE_WIDGET") {
            updateAllWidgets(context)
        }
    }
}