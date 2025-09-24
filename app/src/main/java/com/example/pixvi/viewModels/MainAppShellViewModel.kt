package com.example.pixvi.viewModels

import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.pixvi.network.api.PixivApiService
import com.example.pixvi.network.response.AppLoading.CurrentAccountManager
import com.example.pixvi.repo.BatterySaverThemeRepository
import com.example.pixvi.settings.SettingsRepository

class MainAppShellViewModel(
    private val pixivApiService: PixivApiService,
    val settingsRepository: SettingsRepository,
    isBatterySaver: BatterySaverThemeRepository
) : ViewModel() {

    // UI State
    private val _showProfileMenu = MutableStateFlow(false)
    val showProfileMenu: StateFlow<Boolean> = _showProfileMenu.asStateFlow()

    private val _showHomeDropdownMenu = MutableStateFlow(false)
    val showHomeDropdownMenu: StateFlow<Boolean> = _showHomeDropdownMenu.asStateFlow()

    private val _navigatingToLogin = MutableStateFlow(false)
    val navigatingToLogin: StateFlow<Boolean> = _navigatingToLogin.asStateFlow()

    // Search State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchFieldFocused = MutableStateFlow(false)
    val isSearchFieldFocused: StateFlow<Boolean> = _isSearchFieldFocused.asStateFlow()

    val isPowerSaverTheme: StateFlow<Boolean> = isBatterySaver.batterSaver

    init {
        initializeUserState()
    }

    fun initializeUserState() {
        viewModelScope.launch {
            CurrentAccountManager.setLoadingState(true)
            try {
                val userState = pixivApiService.getUserState()
                userState.body()?.profile?.let {
                    CurrentAccountManager.loginAccount(it)
                }
            } catch (e: Exception) {
                CurrentAccountManager.setErrorState("Network error: ${e.message}")
                CurrentAccountManager.logoutAccount()
            }
        }
    }

    fun changeUserBatteryTheme(choice: Boolean) {
        viewModelScope.launch {
            try{
                settingsRepository.setBatterySaverFlag(choice)
                Log.e("Setting Repo, Battery flag","The new value of battery saver: ${choice}")
            } catch (e: Exception){
                Log.e("Setting Repo, Battery flag",e.message?:"Empty")
            }
        }
    }
    // UI State Management
    fun setShowProfileMenu(show: Boolean) {
        _showProfileMenu.value = show
    }

    fun setShowHomeDropdownMenu(show: Boolean) {
        _showHomeDropdownMenu.value = show
    }

    fun setNavigatingToLogin(navigating: Boolean) {
        _navigatingToLogin.value = navigating
    }

    // Search Management
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query

        viewModelScope.launch {
            if (query.isNotBlank()) {
                // TODO: Implement autosuggest endpoint logic
                handleAutoSuggest(query)
            }
        }
    }

    fun setSearchFieldFocused(focused: Boolean) {
        _isSearchFieldFocused.value = focused
    }

    fun performSearch() {
        val query = _searchQuery.value
        if (query.isNotBlank()) {
            // TODO: Implement search results loading
            viewModelScope.launch {
                handleSearch(query)
            }
        }
    }

    fun clearSearchFocus() {
        _isSearchFieldFocused.value = false
    }

    // Private helper functions
    private suspend fun handleAutoSuggest(query: String) {
        // TODO: Implement autosuggest with latency
        // This would typically call a search suggestion API
    }

    private suspend fun handleSearch(query: String) {
        // TODO: Implement search endpoint call
        // This would navigate to search results or update search state
    }

    // Navigation helpers
    fun handleDrawerItemClick(action: () -> Unit) {
        setShowProfileMenu(false)
        setShowHomeDropdownMenu(false)
        action()
    }

    fun handleProfileMenuAction(action: () -> Unit) {
        setShowProfileMenu(false)
        action()
    }

    fun handleHomeDropdownAction(action: () -> Unit) {
        setShowHomeDropdownMenu(false)
        action()
    }
}