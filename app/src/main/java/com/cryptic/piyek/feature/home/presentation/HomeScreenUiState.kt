package com.cryptic.piyek.feature.home.presentation

import androidx.compose.foundation.text.input.TextFieldState

data class HomeScreenUiState(
    val textFiledState: TextFieldState,
    val isSearchActive: Boolean,
    val currentDestination: NavOptions = NavOptions.ILLust,
    val currentAccount: CurrentAccount
)

data class CurrentAccount(
    val bannerImageUrl: String? = null,
    val profilePicBestUrl: String? = null,
    val accountName: String? = null,
    val accountId: Long,
    val accountEmail: String? = null
)

fun CurrentAccount.isEmpty(): Boolean{
    return this.accountName.isNullOrEmpty() || this.accountEmail.isNullOrEmpty()
}