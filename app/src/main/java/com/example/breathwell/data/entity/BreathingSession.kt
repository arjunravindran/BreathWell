package com.example.breathwell.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "breathing_sessions")
data class BreathingSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: LocalDate,
    val patternName: String,
    val cycles: Int,
    val durationSeconds: Int,
    val completed: Boolean = true
)