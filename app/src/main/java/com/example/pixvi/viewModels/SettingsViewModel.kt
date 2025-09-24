package com.example.pixvi.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pixvi.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    val isDarkMode: StateFlow<Boolean> = settingsRepository.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
}


class SettingsViewModelFactory(
    private val repository: SettingsRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Check if the requested ViewModel is our SettingsViewModel
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            // If it is, create and return an instance of it,
            // passing the repository we have.
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository) as T
        }
        // If it's some other ViewModel, throw an error.
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
