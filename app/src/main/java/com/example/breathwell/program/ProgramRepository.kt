package com.example.breathwell.program

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.breathwell.model.BreathingPattern
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Repository for accessing breathing programs
 */
class ProgramRepository {

    // For a real app, we would use Room database here
    // For simplicity, we're using in-memory storage
    private val programs = MutableLiveData<List<BreathingProgram>>()

    init {
        // Initialize with default programs
        programs.value = BreathingProgram.getDefaultPrograms()
    }

    /**
     * Get all available programs
     */
    fun getAllPrograms(): LiveData<List<BreathingProgram>> {
        return programs
    }

    /**
     * Get a specific program by ID
     */
    fun getProgramById(programId: String): LiveData<BreathingProgram?> {
        val result = MutableLiveData<BreathingProgram?>()

        // Find the program in our list
        val program = programs.value?.find { it.id == programId }
        result.value = program

        return result
    }

    /**
     * Get featured programs
     */
    fun getFeaturedPrograms(): LiveData<List<BreathingProgram>> {
        val result = MutableLiveData<List<BreathingProgram>>()

        // Filter for featured programs
        result.value = programs.value?.filter { it.isFeatured } ?: emptyList()

        return result
    }

    /**
     * Get in-progress programs
     */
    fun getInProgressPrograms(): LiveData<List<BreathingProgram>> {
        val result = MutableLiveData<List<BreathingProgram>>()

        // Filter for in-progress programs
        result.value = programs.value?.filter {
            !it.isCompleted && it.isInProgress()
        } ?: emptyList()

        return result
    }

    /**
     * Start a program
     */
    fun startProgram(programId: String) {
        updateProgram(programId) { program ->
            program.copy(
                currentDay = 0,
                currentSession = 0,
                dateStarted = LocalDate.now(),
                isCompleted = false
            )
        }
    }

    /**
     * Complete a program session
     */
    fun completeSession(programId: String, day: Int, session: Int) {
        val currentPrograms = programs.value ?: return
        val programIndex = currentPrograms.indexOfFirst { it.id == programId }

        if (programIndex == -1) return

        val program = currentPrograms[programIndex]

        // Calculate next session/day
        var nextDay = day
        var nextSession = session + 1

        // If we've completed all sessions for this day, move to next day
        if (nextSession >= program.sessionsPerDay) {
            nextDay++
            nextSession = 0
        }

        // Check if we've completed the entire program
        val isProgCompleted = nextDay >= program.days

        // Create updated program
        val updatedProgram = program.copy(
            currentDay = if (isProgCompleted) program.days else nextDay,
            currentSession = if (isProgCompleted) 0 else nextSession,
            isCompleted = isProgCompleted,
            dateCompleted = if (isProgCompleted) LocalDate.now() else null
        )

        // Update program in list
        val updatedPrograms = currentPrograms.toMutableList()
        updatedPrograms[programIndex] = updatedProgram
        programs.value = updatedPrograms
    }

    /**
     * Reset a program
     */
    fun resetProgram(programId: String) {
        updateProgram(programId) { program ->
            program.copy(
                currentDay = 0,
                currentSession = 0,
                dateStarted = null,
                dateCompleted = null,
                isCompleted = false
            )
        }
    }

    /**
     * Helper method to update a program
     */
    private fun updateProgram(programId: String, updateFunction: (BreathingProgram) -> BreathingProgram) {
        val currentPrograms = programs.value ?: return
        val programIndex = currentPrograms.indexOfFirst { it.id == programId }

        if (programIndex == -1) return

        val program = currentPrograms[programIndex]
        val updatedProgram = updateFunction(program)

        // Update program in list
        val updatedPrograms = currentPrograms.toMutableList()
        updatedPrograms[programIndex] = updatedProgram
        programs.value = updatedPrograms
    }

    companion object {
        // Singleton pattern
        @Volatile
        private var instance: ProgramRepository? = null

        fun getInstance(): ProgramRepository {
            return instance ?: synchronized(this) {
                instance ?: ProgramRepository().also { instance = it }
            }
        }
    }
}