package com.example.pixvi.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.pixvi.network.api.PixivApiService
import com.example.pixvi.network.response.AppLoading.CurrentAccountManager

class MainAppShellViewModel(
    private val pixivApiService: PixivApiService
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