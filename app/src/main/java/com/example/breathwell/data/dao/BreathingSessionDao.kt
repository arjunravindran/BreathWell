package com.example.breathwell.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.breathwell.data.entity.BreathingSession
import java.time.LocalDate

@Dao
interface BreathingSessionDao {
    @Insert
    suspend fun insertSession(session: BreathingSession): Long

    @Query("SELECT * FROM breathing_sessions WHERE date = :date")
    fun getSessionForDate(date: LocalDate): LiveData<List<BreathingSession>>

    @Query("SELECT * FROM breathing_sessions ORDER BY date DESC")
    fun getAllSessions(): LiveData<List<BreathingSession>>

    @Query("SELECT DISTINCT date FROM breathing_sessions WHERE completed = 1")
    fun getAllCompletedDates(): LiveData<List<LocalDate>>

    // Add this new method for the widget to use
    @Query("SELECT DISTINCT date FROM breathing_sessions WHERE completed = 1")
    fun getAllCompletedDatesSync(): List<LocalDate>

    @Query("SELECT COUNT(*) FROM breathing_sessions WHERE completed = 1 AND date BETWEEN :startDate AND :endDate")
    fun getCompletionCountForRange(startDate: LocalDate, endDate: LocalDate): LiveData<Int>

    // New methods for analytics

    @Query("SELECT * FROM breathing_sessions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getSessionsForDateRange(startDate: LocalDate, endDate: LocalDate): LiveData<List<BreathingSession>>

    @Query("SELECT COUNT(DISTINCT date) FROM breathing_sessions WHERE completed = 1")
    fun getTotalDaysWithSessions(): LiveData<Int>

    @Query("SELECT COUNT(*) FROM breathing_sessions WHERE completed = 1")
    fun getTotalCompletedSessions(): LiveData<Int>

    @Query("SELECT SUM(durationSeconds) FROM breathing_sessions WHERE completed = 1")
    fun getTotalDuration(): LiveData<Int>

    @Query("SELECT patternName, COUNT(*) as count FROM breathing_sessions WHERE completed = 1 GROUP BY patternName ORDER BY count DESC LIMIT 1")
    fun getMostUsedPattern(): LiveData<PatternUsage>

    @Query("SELECT patternName, COUNT(*) as count FROM breathing_sessions WHERE completed = 1 GROUP BY patternName ORDER BY count DESC")
    fun getPatternUsage(): LiveData<List<PatternUsage>>

    @Query("SELECT MAX(streak) FROM (SELECT date, " +
            "(SELECT COUNT(*) FROM breathing_sessions s2 WHERE " +
            "s2.date <= s1.date AND s2.date > date(s1.date, '-' || rowid || ' day') " +
            "AND s2.completed = 1) as streak " +
            "FROM breathing_sessions s1 WHERE s1.completed = 1)")
    fun getLongestStreak(): LiveData<Int>

    /**
     * Data class for pattern usage statistics
     */
    data class PatternUsage(
        val patternName: String,
        val count: Int
    )
}