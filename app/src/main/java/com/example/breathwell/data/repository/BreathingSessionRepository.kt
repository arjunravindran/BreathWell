package com.example.breathwell.data.repository

import androidx.lifecycle.LiveData
import com.example.breathwell.data.dao.BreathingSessionDao
import com.example.breathwell.data.entity.BreathingSession
import java.time.LocalDate

class BreathingSessionRepository(private val breathingSessionDao: BreathingSessionDao) {

    fun getAllSessions(): LiveData<List<BreathingSession>> {
        return breathingSessionDao.getAllSessions()
    }

    fun getSessionForDate(date: LocalDate): LiveData<List<BreathingSession>> {
        return breathingSessionDao.getSessionForDate(date)
    }

    fun getAllCompletedDates(): LiveData<List<LocalDate>> {
        return breathingSessionDao.getAllCompletedDates()
    }

    fun getCompletionCountForRange(startDate: LocalDate, endDate: LocalDate): LiveData<Int> {
        return breathingSessionDao.getCompletionCountForRange(startDate, endDate)
    }

    suspend fun insertSession(session: BreathingSession): Long {
        return breathingSessionDao.insertSession(session)
    }

    // New analytics methods

    fun getSessionsForDateRange(startDate: LocalDate, endDate: LocalDate): LiveData<List<BreathingSession>> {
        return breathingSessionDao.getSessionsForDateRange(startDate, endDate)
    }

    fun getTotalDaysWithSessions(): LiveData<Int> {
        return breathingSessionDao.getTotalDaysWithSessions()
    }

    fun getTotalCompletedSessions(): LiveData<Int> {
        return breathingSessionDao.getTotalCompletedSessions()
    }

    fun getTotalDuration(): LiveData<Int> {
        return breathingSessionDao.getTotalDuration()
    }

    fun getMostUsedPattern(): LiveData<BreathingSessionDao.PatternUsage> {
        return breathingSessionDao.getMostUsedPattern()
    }

    fun getPatternUsage(): LiveData<List<BreathingSessionDao.PatternUsage>> {
        return breathingSessionDao.getPatternUsage()
    }

    fun getLongestStreak(): LiveData<Int> {
        return breathingSessionDao.getLongestStreak()
    }
}