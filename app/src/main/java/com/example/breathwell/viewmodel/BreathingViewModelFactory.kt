package com.example.breathwell.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.breathwell.data.AppDatabase
import com.example.breathwell.data.repository.BreathingSessionRepository

class BreathingViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BreathingViewModel::class.java)) {
            val database = AppDatabase.getDatabase(application)
            val repository = BreathingSessionRepository(database.breathingSessionDao())

            @Suppress("UNCHECKED_CAST")
            return BreathingViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}