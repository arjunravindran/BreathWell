package com.example.breathwell.data.entity

import java.time.Duration
import java.time.LocalDateTime

/**
 * Data class for detailed breathing session statistics
 * This is used for analytics and isn't stored directly in the database
 */
data class SessionStatistics(
    val patternName: String,
    val cycles: Int,
    val durationSeconds: Int,
    val expectedDurationSeconds: Int,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime
) {
    /**
     * Calculate completion percentage (how much of expected duration was completed)
     */
    fun getCompletionPercentage(): Int {
        if (expectedDurationSeconds <= 0) return 100
        return (durationSeconds * 100 / expectedDurationSeconds).coerceIn(0, 100)
    }

    /**
     * Calculate if the session was completed in expected time
     */
    fun wasCompletedInExpectedTime(): Boolean {
        // Allow a 10% margin of error
        val lowerBound = expectedDurationSeconds * 0.9
        val upperBound = expectedDurationSeconds * 1.1
        return durationSeconds in lowerBound.toInt()..upperBound.toInt()
    }

    /**
     * Get total duration in minutes (rounded)
     */
    fun getDurationMinutes(): Int {
        return (durationSeconds / 60.0).toInt()
    }

    /**
     * Get formatted duration string (e.g., "5m 23s")
     */
    fun getFormattedDuration(): String {
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60

        return if (minutes > 0) {
            "$minutes min ${seconds}s"
        } else {
            "${seconds}s"
        }
    }

    /**
     * Get comparison to expected duration
     */
    fun getDurationComparison(): DurationComparison {
        // Allow a 10% margin of error
        val lowerBound = expectedDurationSeconds * 0.9
        val upperBound = expectedDurationSeconds * 1.1

        return when {
            durationSeconds < lowerBound.toInt() -> DurationComparison.FASTER
            durationSeconds > upperBound.toInt() -> DurationComparison.SLOWER
            else -> DurationComparison.ON_TARGET
        }
    }

    /**
     * Enum for duration comparison results
     */
    enum class DurationComparison {
        FASTER,     // Session was completed faster than expected
        ON_TARGET,  // Session was completed within expected time (±10%)
        SLOWER      // Session took longer than expected
    }
}