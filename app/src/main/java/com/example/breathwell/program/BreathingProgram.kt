package com.example.breathwell.program

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.breathwell.model.BreathingPattern
import java.time.LocalDate

/**
 * Model class for guided breathing programs
 */
@Entity(tableName = "breathing_programs")
data class BreathingProgram(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val difficulty: Int,  // 1-3
    val days: Int,
    val sessionsPerDay: Int,
    val coverImageResId: Int,
    val isFeatured: Boolean = false,
    val isCompleted: Boolean = false,
    val currentDay: Int = 0,
    val currentSession: Int = 0,
    val dateStarted: LocalDate? = null,
    val dateCompleted: LocalDate? = null
) {
    /**
     * Calculate program progress as a percentage
     */
    fun calculateProgress(): Int {
        val totalSessions = days * sessionsPerDay
        val completedSessions = (currentDay * sessionsPerDay) + currentSession

        return if (totalSessions > 0) {
            (completedSessions * 100) / totalSessions
        } else {
            0
        }
    }

    /**
     * Check if the program is in progress
     */
    fun isInProgress(): Boolean {
        return currentDay > 0 || currentSession > 0
    }

    /**
     * Get the program status
     */
    fun getStatus(): ProgramStatus {
        return when {
            isCompleted -> ProgramStatus.COMPLETED
            isInProgress() -> ProgramStatus.IN_PROGRESS
            else -> ProgramStatus.NOT_STARTED
        }
    }

    /**
     * Get the next session in the program
     */
    fun getNextSession(): ProgramSession? {
        if (isCompleted) return null

        val sessionIndex = (currentDay * sessionsPerDay) + currentSession
        return sessions.getOrNull(sessionIndex)
    }

    /**
     * Check if a specific day is locked
     */
    fun isDayLocked(day: Int): Boolean {
        return day > currentDay && !isCompleted
    }

    /**
     * Session definitions for this program
     */
    val sessions: List<ProgramSession> = generateSessions()

    /**
     * Generate session definitions
     * This would normally be stored in a database, but for simplicity
     * we're generating them programmatically.
     */
    private fun generateSessions(): List<ProgramSession> {
        val programSessions = mutableListOf<ProgramSession>()

        // Different programs have different session progressions
        when (id) {
            "beginner" -> {
                // Beginner program: Simple progression
                for (day in 0 until days) {
                    for (session in 0 until sessionsPerDay) {
                        // For beginners, start with Diaphragmatic breathing and
                        // gradually introduce more complex patterns
                        val pattern = when {
                            day < 3 -> BreathingPattern.DIAPHRAGMATIC_BREATHING
                            day < 5 -> BreathingPattern.BOX_BREATHING
                            day < 7 -> BreathingPattern.RELAXING_BREATH
                            else -> BreathingPattern.COHERENT_BREATHING
                        }

                        // Gradually increase cycles
                        val cycles = 3 + (day / 2)

                        programSessions.add(
                            ProgramSession(
                                programId = id,
                                day = day,
                                session = session,
                                title = "Day ${day+1} - Session ${session+1}",
                                description = generateSessionDescription(day, session, pattern.name),
                                pattern = pattern,
                                cycles = cycles
                            )
                        )
                    }
                }
            }

            "stress_reduction" -> {
                // Stress reduction: Focus on relaxing patterns
                val stressPatterns = listOf(
                    BreathingPattern.RELAXING_BREATH,
                    BreathingPattern.CALMING_BREATH,
                    BreathingPattern.BOX_BREATHING
                )

                for (day in 0 until days) {
                    for (session in 0 until sessionsPerDay) {
                        // Cycle through patterns
                        val patternIndex = (day + session) % stressPatterns.size
                        val pattern = stressPatterns[patternIndex]

                        // Cycles increase gradually
                        val cycles = 4 + (day / 3)

                        programSessions.add(
                            ProgramSession(
                                programId = id,
                                day = day,
                                session = session,
                                title = "Day ${day+1} - Session ${session+1}",
                                description = generateSessionDescription(day, session, pattern.name),
                                pattern = pattern,
                                cycles = cycles
                            )
                        )
                    }
                }
            }

            "sleep_improvement" -> {
                // Sleep program: Focus on calming patterns with longer sessions at night
                for (day in 0 until days) {
                    for (session in 0 until sessionsPerDay) {
                        // Different patterns for morning vs evening
                        val pattern = if (session == 0) {
                            // Morning session
                            BreathingPattern.ENERGIZING_BREATH
                        } else {
                            // Evening session
                            BreathingPattern.SLEEP_BREATH
                        }

                        // Evening sessions are longer
                        val cycles = if (session == 0) 3 else 5 + (day / 3)

                        programSessions.add(
                            ProgramSession(
                                programId = id,
                                day = day,
                                session = session,
                                title = if (session == 0) "Morning Practice" else "Bedtime Practice",
                                description = generateSessionDescription(day, session, pattern.name),
                                pattern = pattern,
                                cycles = cycles
                            )
                        )
                    }
                }
            }

            "mindfulness" -> {
                // Mindfulness program: Diverse patterns with focus on awareness
                val mindfulnessPatterns = listOf(
                    BreathingPattern.DIAPHRAGMATIC_BREATHING,
                    BreathingPattern.ALTERNATE_NOSTRIL,
                    BreathingPattern.UJJAYI_BREATH,
                    BreathingPattern.BREATH_COUNTING
                )

                for (day in 0 until days) {
                    for (session in 0 until sessionsPerDay) {
                        // Cycle through patterns
                        val patternIndex = (day * sessionsPerDay + session) % mindfulnessPatterns.size
                        val pattern = mindfulnessPatterns[patternIndex]

                        // Consistent cycles for mindfulness
                        val cycles = 6

                        programSessions.add(
                            ProgramSession(
                                programId = id,
                                day = day,
                                session = session,
                                title = "Day ${day+1} - ${pattern.name}",
                                description = generateSessionDescription(day, session, pattern.name),
                                pattern = pattern,
                                cycles = cycles
                            )
                        )
                    }
                }
            }

            else -> {
                // Default program structure
                for (day in 0 until days) {
                    for (session in 0 until sessionsPerDay) {
                        val pattern = BreathingPattern.getAllPatterns()
                            .filter { !it.isCustom }
                            .random()

                        val cycles = 4 + (day / 2)

                        programSessions.add(
                            ProgramSession(
                                programId = id,
                                day = day,
                                session = session,
                                title = "Day ${day+1} - Session ${session+1}",
                                description = "Practice ${pattern.name} breathing",
                                pattern = pattern,
                                cycles = cycles
                            )
                        )
                    }
                }
            }
        }

        return programSessions
    }

    /**
     * Generate a description for a session based on day and session numbers
     */
    private fun generateSessionDescription(day: Int, session: Int, patternName: String): String {
        // Early in program
        if (day < 3) {
            return when (session) {
                0 -> "Start your day with a gentle $patternName practice to establish your routine."
                else -> "Evening practice using $patternName to wind down and reflect."
            }
        }

        // Mid program
        if (day < 7) {
            return when (session) {
                0 -> "Morning $patternName practice to energize your day."
                else -> "Continue building your habit with this evening $patternName session."
            }
        }

        // Late program
        return when (session) {
            0 -> "Deepen your morning practice with $patternName breathing."
            else -> "Evening $patternName practice to reinforce your breathing skills."
        }
    }

    /**
     * Status enum for program state
     */
    enum class ProgramStatus {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED
    }

    companion object {
        /**
         * Create the predefined programs
         */
        fun getDefaultPrograms(): List<BreathingProgram> {
            return listOf(
                BreathingProgram(
                    id = "beginner",
                    name = "Beginner's Journey",
                    description = "A 10-day introduction to breathing techniques for newcomers. Gradually builds from simple to more advanced patterns.",
                    difficulty = 1,
                    days = 10,
                    sessionsPerDay = 1,
                    coverImageResId = R.drawable.program_beginner,
                    isFeatured = true
                ),

                BreathingProgram(
                    id = "stress_reduction",
                    name = "Stress Reduction",
                    description = "14 days of breathing techniques specifically designed to activate your parasympathetic nervous system and reduce stress.",
                    difficulty = 2,
                    days = 14,
                    sessionsPerDay = 2,
                    coverImageResId = R.drawable.program_stress
                ),

                BreathingProgram(
                    id = "sleep_improvement",
                    name = "Better Sleep",
                    description = "A 7-day program with morning and evening sessions to regulate your body's rhythms and improve sleep quality.",
                    difficulty = 1,
                    days = 7,
                    sessionsPerDay = 2,
                    coverImageResId = R.drawable.program_sleep
                ),

                BreathingProgram(
                    id = "mindfulness",
                    name = "Mindfulness Foundation",
                    description = "Build mindfulness skills with 21 days of breathing practices that enhance present-moment awareness.",
                    difficulty = 2,
                    days = 21,
                    sessionsPerDay = 1,
                    coverImageResId = R.drawable.program_mindfulness
                )
            )
        }
    }
}