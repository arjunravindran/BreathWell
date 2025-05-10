package com.example.breathwell.utils

import com.example.breathwell.data.entity.BreathingSession
import java.time.LocalDate
import java.time.LocalTime

/**
 * Helper class for analyzing breathing session data and generating statistics
 */
object AnalyticsHelper {

    /**
     * Calculates statistics for a single day's sessions
     */
    fun calculateDayStatistics(sessions: List<BreathingSession>): DayStatistics {
        if (sessions.isEmpty()) {
            return DayStatistics(
                totalSessions = 0,
                totalDuration = 0,
                techniquesUsed = emptyList(),
                hasCompletedSession = false,
                date = LocalDate.now()
            )
        }

        // Filter for completed sessions only
        val completedSessions = sessions.filter { it.completed }

        // Basic statistics
        val totalSessions = completedSessions.size
        val totalDuration = completedSessions.sumOf { it.durationSeconds }
        val hasCompletedSession = completedSessions.isNotEmpty()
        val date = sessions.first().date

        // Count techniques used
        val techniqueCount = mutableMapOf<String, Int>()
        for (session in completedSessions) {
            techniqueCount[session.patternName] = techniqueCount.getOrDefault(session.patternName, 0) + 1
        }

        // Create TechniqueUsage objects
        val techniquesUsed = techniqueCount.map { (name, count) ->
            TechniqueUsage(name, count)
        }.sortedByDescending { it.count }

        return DayStatistics(
            totalSessions = totalSessions,
            totalDuration = totalDuration,
            techniquesUsed = techniquesUsed,
            hasCompletedSession = hasCompletedSession,
            date = date
        )
    }

    /**
     * Calculates statistics for a period (week/month)
     */
    fun calculatePeriodStatistics(sessions: List<BreathingSession>): PeriodStatistics {
        if (sessions.isEmpty()) {
            return PeriodStatistics(
                totalSessions = 0,
                totalDuration = 0,
                totalDays = 0,
                averageDurationPerSession = 0,
                completedDays = 0,
                mostUsedTechnique = null,
                sessionsByTechnique = emptyList(),
                sessionsByTimeOfDay = mapOf(
                    TimeOfDay.MORNING to 0,
                    TimeOfDay.AFTERNOON to 0,
                    TimeOfDay.EVENING to 0,
                    TimeOfDay.NIGHT to 0
                )
            )
        }

        // Filter for completed sessions only
        val completedSessions = sessions.filter { it.completed }

        // Basic metrics
        val totalSessions = completedSessions.size
        val totalDuration = completedSessions.sumOf { it.durationSeconds }
        val averageDurationPerSession = if (totalSessions > 0) totalDuration / totalSessions else 0

        // Days analysis
        val daysSet = sessions.map { it.date }.toSet()
        val totalDays = daysSet.size
        val daysWithCompletedSessions = completedSessions.map { it.date }.toSet()
        val completedDays = daysWithCompletedSessions.size

        // Technique analysis
        val techniqueCount = mutableMapOf<String, Int>()
        for (session in completedSessions) {
            techniqueCount[session.patternName] = techniqueCount.getOrDefault(session.patternName, 0) + 1
        }

        // Find most used technique
        val mostUsedTechnique = techniqueCount.entries.maxByOrNull { it.value }?.let { (name, count) ->
            TechniqueUsage(name, count)
        }

        // Create technique usage list
        val sessionsByTechnique = techniqueCount.map { (name, count) ->
            TechniqueUsage(name, count)
        }.sortedByDescending { it.count }

        // Time of day analysis
        val sessionsByTimeOfDay = mutableMapOf(
            TimeOfDay.MORNING to 0,
            TimeOfDay.AFTERNOON to 0,
            TimeOfDay.EVENING to 0,
            TimeOfDay.NIGHT to 0
        )

        // This is a placeholder since we don't have session time in data model yet
        // In a real implementation, you'd use the actual session time
        for (session in completedSessions) {
            // We'll use a random time of day for demonstration
            val timeOfDay = TimeOfDay.values().random()
            sessionsByTimeOfDay[timeOfDay] = sessionsByTimeOfDay[timeOfDay]!! + 1
        }

        return PeriodStatistics(
            totalSessions = totalSessions,
            totalDuration = totalDuration,
            totalDays = totalDays,
            averageDurationPerSession = averageDurationPerSession,
            completedDays = completedDays,
            mostUsedTechnique = mostUsedTechnique,
            sessionsByTechnique = sessionsByTechnique,
            sessionsByTimeOfDay = sessionsByTimeOfDay
        )
    }

    /**
     * Data class for daily statistics
     */
    data class DayStatistics(
        val totalSessions: Int,
        val totalDuration: Int,
        val techniquesUsed: List<TechniqueUsage>,
        val hasCompletedSession: Boolean,
        val date: LocalDate
    )

    /**
     * Data class for period statistics (week/month)
     */
    data class PeriodStatistics(
        val totalSessions: Int,
        val totalDuration: Int,
        val totalDays: Int,
        val averageDurationPerSession: Int,
        val completedDays: Int,
        val mostUsedTechnique: TechniqueUsage?,
        val sessionsByTechnique: List<TechniqueUsage>,
        val sessionsByTimeOfDay: Map<TimeOfDay, Int>
    )

    /**
     * Data class for technique usage statistics
     */
    data class TechniqueUsage(
        val techniqueName: String,
        val count: Int
    )

    /**
     * Enum for time of day categories
     */
    enum class TimeOfDay {
        MORNING,     // 5am - 12pm
        AFTERNOON,   // 12pm - 5pm
        EVENING,     // 5pm - 10pm
        NIGHT        // 10pm - 5am
    }

    /**
     * Helper function to determine time of day from a LocalTime
     */
    fun getTimeOfDay(time: LocalTime): TimeOfDay {
        return when {
            time.isAfter(LocalTime.of(5, 0)) && !time.isAfter(LocalTime.of(12, 0)) -> TimeOfDay.MORNING
            time.isAfter(LocalTime.of(12, 0)) && !time.isAfter(LocalTime.of(17, 0)) -> TimeOfDay.AFTERNOON
            time.isAfter(LocalTime.of(17, 0)) && !time.isAfter(LocalTime.of(22, 0)) -> TimeOfDay.EVENING
            else -> TimeOfDay.NIGHT
        }
    }
}