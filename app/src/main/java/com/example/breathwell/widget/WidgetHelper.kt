package com.example.breathwell.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * Helper class to update widgets from different parts of the application
 */
object WidgetHelper {
    /**
     * Updates all breathing reminder widgets
     * Call this method when a breathing session is completed
     */
    fun updateWidgets(context: Context) {
        val intent = Intent(context, BreathingReminderWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, BreathingReminderWidget::class.java))
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        context.sendBroadcast(intent)
    }
}