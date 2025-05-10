package com.example.breathwell.program

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.breathwell.model.BreathingPattern
import java.time.LocalDateTime

/**
 * Model class for a session within a guided breathing program
 */
@Entity(tableName = "program_sessions")
data class ProgramSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val programId: String,
    val day: Int,
    val session: Int,
    val title: String,
    val description: String,
    val pattern: BreathingPattern,
    val cycles: Int,
    val isCompleted: Boolean = false,
    val completedDateTime: LocalDateTime? = null
) {
    /**
     * Get a unique identifier for this session within its program
     */
    fun getSessionIdentifier(): String {
        return "$programId-$day-$session"
    }

    /**
     * Check if this session is available to be played
     * (based on program progress)
     */
    fun isAvailable(currentDay: Int, currentSession: Int): Boolean {
        return if (day < currentDay) {
            // Previous days are always available
            true
        } else if (day == currentDay) {
            // Current day - session needs to be current or earlier
            session <= currentSession
        } else {
            // Future days are locked
            false
        }
    }

    /**
     * Get the status of this session
     */
    fun getStatus(currentDay: Int, currentSession: Int): SessionStatus {
        return when {
            isCompleted -> SessionStatus.COMPLETED
            isAvailable(currentDay, currentSession) -> SessionStatus.AVAILABLE
            else -> SessionStatus.LOCKED
        }
    }

    /**
     * Status enum for session state
     */
    enum class SessionStatus {
        LOCKED,
        AVAILABLE,
        COMPLETED
    }

    companion object {
        /**
         * Create a default blank session
         */
        fun createEmpty(programId: String): ProgramSession {
            return ProgramSession(
                programId = programId,
                day = 0,
                session = 0,
                title = "Empty Session",
                description = "Placeholder session",
                pattern = BreathingPattern.BOX_BREATHING,
                cycles = 5
            )
        }
    }
}