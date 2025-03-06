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

    @Query("SELECT COUNT(*) FROM breathing_sessions WHERE completed = 1 AND date BETWEEN :startDate AND :endDate")
    fun getCompletionCountForRange(startDate: LocalDate, endDate: LocalDate): LiveData<Int>
}