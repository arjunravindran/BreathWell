package com.example.breathwell.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.breathwell.program.BreathingProgram
import com.example.breathwell.program.ProgramRepository
import com.example.breathwell.program.ProgramSession
import kotlinx.coroutines.launch

/**
 * ViewModel for guided breathing programs
 */
class ProgramViewModel : ViewModel() {

    private val repository = ProgramRepository.getInstance()

    // Current program and session
    private val _currentProgram = MutableLiveData<BreathingProgram?>()
    val currentProgram: LiveData<BreathingProgram?> = _currentProgram

    private val _currentSession = MutableLiveData<ProgramSession?>()
    val currentSession: LiveData<ProgramSession?> = _currentSession

    // Program filter (for program list screen)
    private val _programFilter = MutableLiveData(ProgramFilter.ALL)
    val programFilter: LiveData<ProgramFilter> = _programFilter

    // Filtered programs
    val filteredPrograms = MediatorLiveData<List<BreathingProgram>>()

    init {
        // Set up program filtering
        val allPrograms = repository.getAllPrograms()
        val featuredPrograms = repository.getFeaturedPrograms()
        val inProgressPrograms = repository.getInProgressPrograms()

        filteredPrograms.addSource(allPrograms) { updateFilteredPrograms() }
        filteredPrograms.addSource(featuredPrograms) { updateFilteredPrograms() }
        filteredPrograms.addSource(inProgressPrograms) { updateFilteredPrograms() }
        filteredPrograms.addSource(_programFilter) { updateFilteredPrograms() }
    }

    /**
     * Update the filtered programs based on the current filter
     */
    private fun updateFilteredPrograms() {
        viewModelScope.launch {
            val filter = _programFilter.value ?: ProgramFilter.ALL

            val programs = when (filter) {
                ProgramFilter.ALL -> repository.getAllPrograms().value ?: emptyList()
                ProgramFilter.FEATURED -> repository.getFeaturedPrograms().value ?: emptyList()
                ProgramFilter.IN_PROGRESS -> repository.getInProgressPrograms().value ?: emptyList()
            }

            filteredPrograms.value = programs
        }
    }

    /**
     * Set the program filter
     */
    fun setFilter(filter: ProgramFilter) {
        _programFilter.value = filter
    }

    /**
     * Load a program by ID
     */
    fun loadProgram(programId: String) {
        viewModelScope.launch {
            val programLiveData = repository.getProgramById(programId)

            _currentProgram.value = programLiveData.value

            // If program has a next session, load it
            _currentProgram.value?.let { program ->
                _currentSession.value = program.getNextSession()
            }
        }
    }

    /**
     * Start a program
     */
    fun startProgram(programId: String) {
        repository.startProgram(programId)
        loadProgram(programId)
    }

    /**
     * Complete current session
     */
    fun completeCurrentSession() {
        val program = _currentProgram.value ?: return
        val session = _currentSession.value ?: return

        repository.completeSession(program.id, session.day, session.session)
        loadProgram(program.id)
    }

    /**
     * Reset a program
     */
    fun resetProgram(programId: String) {
        repository.resetProgram(programId)
        loadProgram(programId)
    }

    /**
     * Get sessions for a specific day
     */
    fun getSessionsForDay(day: Int): LiveData<List<ProgramSession>> {
        return currentProgram.map { program ->
            program?.sessions?.filter { it.day == day } ?: emptyList()
        }
    }

    /**
     * Check if a day is locked
     */
    fun isDayLocked(day: Int): LiveData<Boolean> {
        return currentProgram.map { program ->
            program?.isDayLocked(day) ?: true
        }
    }

    /**
     * Get a specific session
     */
    fun getSession(day: Int, session: Int): LiveData<ProgramSession?> {
        return currentProgram.map { program ->
            program?.sessions?.find { it.day == day && it.session == session }
        }
    }

    /**
     * Set current session to specific day and session
     */
    fun setCurrentSession(day: Int, session: Int) {
        val program = _currentProgram.value ?: return
        _currentSession.value = program.sessions.find {
            it.day == day && it.session == session
        }
    }

    /**
     * Enum for program filtering
     */
    enum class ProgramFilter {
        ALL,
        FEATURED,
        IN_PROGRESS
    }
}