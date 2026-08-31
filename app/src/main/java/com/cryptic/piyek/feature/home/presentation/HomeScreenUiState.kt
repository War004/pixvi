package com.cryptic.piyek.feature.home.presentation

import androidx.compose.foundation.text.input.TextFieldState

data class HomeScreenUiState(
    val textFiledState: TextFieldState,
    val isSearchActive: Boolean,
    val currentDestination: NavOptions = NavOptions.ILLust
)