package com.example.breathwell.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.SavedStateHandle

/**
 * Factory for creating BreathingViewModel with dependencies
 */
class BreathingViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(BreathingViewModel::class.java)) {
            // Get the SavedStateHandle
            val savedStateHandle = extras[ViewModelProvider.NewInstanceFactory.VIEW_MODEL_KEY] as? SavedStateHandle
                ?: SavedStateHandle()

            return BreathingViewModel(application, savedStateHandle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BreathingViewModel::class.java)) {
            return BreathingViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}