package com.cryptic.piyek.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptic.piyek.core.CResponse
import com.cryptic.piyek.feature.onboarding.OnboardingManager
import com.cryptic.piyek.core.auth.domain.repository.OAuthUserRepository
import com.cryptic.piyek.core.content.data.model.Artwork
import com.cryptic.piyek.feature.onboarding.domain.repo.WalkthroughRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class OnboardingViewModel(
    private val oAuthUserRepo: OAuthUserRepository,
    private val onboardingManager: OnboardingManager,
    private val walkThroughRepo: WalkthroughRepo
): ViewModel() {

    private val loadTrigger = MutableSharedFlow<Unit>(replay = 1).apply {
        tryEmit(Unit)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val onboardingState: StateFlow<OnboardingUiState> = loadTrigger
        .flatMapLatest {
            flow {
                emit(OnboardingUiState(onboardingStatus = OnboardingStatus.Idle))

                when (val result = walkThroughRepo.getWalkthroughList()) {
                    is CResponse.Success -> {
                        emit(OnboardingUiState(
                            onboardingStatus = OnboardingStatus.Idle,
                            walkthroughArtworks = result.data
                        ))
                    }
                    is CResponse.Failed -> {
                        emit(OnboardingUiState(
                            onboardingStatus = OnboardingStatus.Idle,
                            errorMessage = result.exception.localizedMessage)
                        )
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = OnboardingUiState()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentArtwork: StateFlow<Artwork?> = onboardingState
        .map { it.walkthroughArtworks }
        .distinctUntilChanged()
        .flatMapLatest { artworks ->
            if (artworks.isEmpty()) flowOf<Artwork?>(null)
            else flow {
                var index = 0
                while (true) {
                    emit(artworks[index % artworks.size])
                    delay(9.seconds)
                    index++
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )

    fun launchLoginPage(): String{
        val codeVerifier = onboardingManager.generateCodeVerifier()
        val redirectUrl = onboardingManager.generateLoginUrl(codeVerifier)

        //save the code verifier in the dataStore
        viewModelScope.launch(Dispatchers.IO) {
            oAuthUserRepo.saveCodeVerifier(codeVerifier)
        }
        return redirectUrl
    }

    fun retry() {
        loadTrigger.tryEmit(Unit)
    }

    fun createRedirectUrl(): String {
        val codeVerifier = onboardingManager.generateCodeVerifier()
        return onboardingManager.generateLoginUrl(codeVerifier)
    }
}