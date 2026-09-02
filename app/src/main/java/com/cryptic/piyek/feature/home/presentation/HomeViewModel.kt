package com.cryptic.piyek.feature.home.presentation

import android.util.Log
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptic.piyek.core.auth.SessionManager
import com.cryptic.piyek.core.auth.domain.repository.OAuthUserRepository
import com.cryptic.piyek.core.content.data.model.Artwork
import com.cryptic.piyek.core.content.data.model.ArtworkContentList
import com.cryptic.piyek.core.content.data.model.RecommendationNonNovelPara
import com.cryptic.piyek.core.content.domain.repo.CoreContentApiRepo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    iLLustRepo: CoreContentApiRepo<ArtworkContentList, RecommendationNonNovelPara, ArtworkContentList>,
    private val oAuthUserRepository: OAuthUserRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
): ViewModel() {

    private val _searchState = MutableStateFlow(false)
    private val _uiModeState: MutableStateFlow<NavOptions> = MutableStateFlow(NavOptions.ILLust)
    val searchTextFieldState = TextFieldState("")

    val uiState: StateFlow<HomeScreenUiState> = combine(
        _searchState,
        _uiModeState,
        oAuthUserRepository.observeActiveAccount()
    ) { isSearchActive, uiMode, activeUser ->
        HomeScreenUiState(
            textFiledState = searchTextFieldState,
            isSearchActive = isSearchActive,
            currentDestination = uiMode,
            currentAccount = CurrentAccount(
                accountId = activeUser?.id ?: 0L,
                accountName = activeUser?.name,
                accountEmail = activeUser?.mailAddress,
                profilePicBestUrl = activeUser?.bestProfilePicUrl
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = HomeScreenUiState(
            textFiledState = searchTextFieldState,
            isSearchActive = false,
            currentDestination = NavOptions.ILLust,
            currentAccount = CurrentAccount(accountId = 0L)
        )
    )

    val destinationWindows: List<NavOptions> = listOf(NavOptions.ILLust, NavOptions.Manga, NavOptions.Novel)

    val activeArtwork: StateFlow<Artwork?> = iLLustRepo.contentList
        .map { contentList ->
            contentList?.artworkList?.getOrNull(contentList.focusedIndex)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )

    fun action(action: HomeScreenAction){
        when(action){
            is HomeScreenAction.SearchStatusChange -> changeSearchStatus(action.newValue)
            is HomeScreenAction.NewUiMode -> changeUiMode(action.newUIMode)
            is HomeScreenAction.ActiveAccountDetails -> {}
            is HomeScreenAction.LogOut -> logOut()
            is HomeScreenAction.ChangeAccount -> changeActiveAccount(action.targetAccountId)
        }
    }

    private fun changeActiveAccount(targetAccountId: Long) {
        viewModelScope.launch(ioDispatcher) {
            oAuthUserRepository.saveActiveUser(targetAccountId)
        }
    }

    private fun logOut() {
        val currentAccountId = uiState.value.currentAccount.accountId
        if (currentAccountId <= 0L) return

        viewModelScope.launch(ioDispatcher) {
            val accountCount = oAuthUserRepository.getTotalAccountCount()
            if (accountCount > 1) {
                oAuthUserRepository.deleteUser(currentAccountId)
                val nextAccountId = oAuthUserRepository.getFirstAccountId()
                oAuthUserRepository.saveActiveUser(nextAccountId)
            } else {
                oAuthUserRepository.deleteUser(currentAccountId)
                oAuthUserRepository.removeActiveAccount()
            }
        }
    }

    private fun changeUiMode(newUiMode: NavOptions){
        _uiModeState.update { newUiMode }
    }

    private fun changeSearchStatus(isFocused: Boolean){
        _searchState.update { isFocused }
    }
}