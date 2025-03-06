package com.example.breathwell.utils

import android.content.Context
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.widget.ImageView
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.example.breathwell.R
import com.example.breathwell.model.BreathPhase

/**
 * Utility class for accessibility-related functions
 * This manages all accessibility features including TalkBack support
 */
object AccessibilityUtils {

    /**
     * Announces the current breathing phase to screen readers
     */
    fun announceForAccessibility(context: Context, view: View, breathPhase: BreathPhase, counter: Int) {
        if (!isAccessibilityEnabled(context)) return

        val announcementText = when (breathPhase) {
            BreathPhase.INHALE -> context.getString(R.string.accessibility_inhale_announcement, counter)
            BreathPhase.EXHALE -> context.getString(R.string.accessibility_exhale_announcement, counter)
            BreathPhase.HOLD1, BreathPhase.HOLD2 -> context.getString(R.string.accessibility_hold_announcement, counter)
            BreathPhase.READY -> context.getString(R.string.accessibility_ready_announcement)
            BreathPhase.COMPLETE -> context.getString(R.string.accessibility_complete_announcement)
        }

        view.announceForAccessibility(announcementText)
    }

    /**
     * Sets up accessibility for UI controls
     */
    fun setupAccessibilityForButton(view: View, contentDescription: String, actionDescription: String) {
        ViewCompat.setAccessibilityDelegate(view, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.contentDescription = contentDescription
                info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    AccessibilityNodeInfoCompat.ACTION_CLICK, actionDescription))
            }
        })
    }

    /**
     * Add content descriptions to ImageViews
     */
    fun setContentDescription(imageView: ImageView, description: String) {
        imageView.contentDescription = description
    }

    /**
     * Check if accessibility services are enabled
     */
    private fun isAccessibilityEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return am.isEnabled
    }

    /**
     * Configure accessibility focus events for important state changes
     */
    fun sendAccessibilityFocusEvent(view: View) {
        view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
    }
}