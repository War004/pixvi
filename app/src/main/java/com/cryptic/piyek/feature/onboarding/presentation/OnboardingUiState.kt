package com.cryptic.piyek.feature.onboarding.presentation

import com.cryptic.piyek.core.content.data.model.Artwork

data class OnboardingUiState(
    val walkthroughArtworks: List<Artwork> = emptyList(),
    val onboardingStatus: OnboardingStatus = OnboardingStatus.Idle,
    val errorMessage: String? = null
)

sealed class OnboardingStatus{
    object Idle: OnboardingStatus()
    object Waiting: OnboardingStatus()
    object Success: OnboardingStatus()
}