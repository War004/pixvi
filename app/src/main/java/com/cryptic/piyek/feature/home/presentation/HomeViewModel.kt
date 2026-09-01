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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    iLLustRepo: CoreContentApiRepo<ArtworkContentList, RecommendationNonNovelPara, ArtworkContentList>,
    private val activeAccountId: Long,
    private val oAuthUserRepository: OAuthUserRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    //others repo for the future
): ViewModel() {

    private val _uiState = MutableStateFlow<HomeScreenUiState>(HomeScreenUiState(
        textFiledState = TextFieldState(""),
        isSearchActive = false,
        currentAccount = CurrentAccount(accountId = activeAccountId)
    ))

    val uiState: StateFlow<HomeScreenUiState> = _uiState.asStateFlow()
    val destinationWindows: List<NavOptions> = listOf(NavOptions.ILLust, NavOptions.Manga,NavOptions.Novel)

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
            is HomeScreenAction.SearchStatusChange -> {changeSearchStatus(action.newValue)}
            is HomeScreenAction.NewUiMode -> {changeUiMode(action.newUIMode)}
            is HomeScreenAction.ActiveAccountDetails -> { getCurrentAccountDetail()}
        }
    }

    private fun getCurrentAccountDetail(){
        val currentUiState = _uiState.value

        if(currentUiState.currentAccount.accountId!=activeAccountId || currentUiState.currentAccount.isEmpty()){
            viewModelScope.launch(ioDispatcher) {
                try{
                    val account = oAuthUserRepository.getUser(activeAccountId)
                    if(account!=null){
                        _uiState.update { currentState ->
                            currentState.copy(
                                currentAccount = currentState.currentAccount.copy(
                                    profilePicBestUrl = account.bestProfilePicUrl,
                                    accountName = account.name,
                                    accountEmail = account.mailAddress
                                )
                            )
                        }
                    }
                }
                catch (e: Exception){
                    Log.e("ACTIVE ACCOUNT","Something happened in the db layer")
                }
            }
        }
    }
    /*
    Need to validate that the entries between the destinationWindow and avliable option in NavOptions are in sync.
    */
    private fun changeUiMode(newUiMode: NavOptions){
        _uiState.update {
            it.copy(currentDestination = newUiMode)
        }
    }

    private fun changeSearchStatus(isFocused: Boolean){
        _uiState.update {
            it.copy(isSearchActive = isFocused)
        }
    }
}