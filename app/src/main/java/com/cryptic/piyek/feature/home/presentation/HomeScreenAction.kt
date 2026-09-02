package com.cryptic.piyek.feature.home.presentation

sealed interface HomeScreenAction {
    data class SearchStatusChange(val newValue: Boolean): HomeScreenAction
    data class NewUiMode(val newUIMode: NavOptions): HomeScreenAction
    data object ActiveAccountDetails: HomeScreenAction
    data object LogOut: HomeScreenAction
    data class ChangeAccount(val targetAccountId: Long): HomeScreenAction
}