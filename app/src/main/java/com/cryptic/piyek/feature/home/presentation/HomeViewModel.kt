package com.cryptic.piyek.feature.home.presentation

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptic.piyek.core.content.data.model.Artwork
import com.cryptic.piyek.core.content.data.model.ArtworkContentList
import com.cryptic.piyek.core.content.data.model.RecommendationNonNovelPara
import com.cryptic.piyek.core.content.domain.repo.CoreContentApiRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class HomeViewModel(
    private val iLLustRepo: CoreContentApiRepo<ArtworkContentList, RecommendationNonNovelPara, ArtworkContentList>
    //others repo for the future
): ViewModel() {
    private val _uiMode = MutableStateFlow<NavOptions>(NavOptions.ILLust)
    val uiMode = _uiMode.asStateFlow()

    private val _uiState = MutableStateFlow<HomeScreenUiState>(HomeScreenUiState(
        textFiledState = TextFieldState(""),
        isSearchActive = false
    ))

    val uiState: StateFlow<HomeScreenUiState> = _uiState.asStateFlow()

    val activeArtwork: StateFlow<Artwork?> = iLLustRepo.contentList
        .map { contentList ->
            // Safely extract the artwork using getOrNull to prevent IndexOutOfBounds runtime crashes
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
        }
    }
    private fun changeUiMode(newUiMode: NavOptions){
        _uiMode.value = newUiMode
    }

    private fun changeSearchStatus(isFocused: Boolean){
        _uiState.update {
            it.copy(isSearchActive = isFocused)
        }
    }
}