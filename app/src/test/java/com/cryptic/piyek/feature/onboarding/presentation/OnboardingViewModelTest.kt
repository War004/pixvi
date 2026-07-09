package com.cryptic.piyek.feature.onboarding.presentation

import com.cryptic.piyek.core.CResponse
import com.cryptic.piyek.core.auth.domain.repository.OAuthUserRepository
import com.cryptic.piyek.core.content.data.model.Artwork
import com.cryptic.piyek.core.content.data.model.createMockArtwork
import com.cryptic.piyek.feature.onboarding.OnboardingManager
import com.cryptic.piyek.feature.onboarding.domain.repo.WalkthroughRepo
import com.cryptic.piyek.util.MainDispatcherExtension
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @RegisterExtension
    val mainDispatcherExtension = MainDispatcherExtension(testDispatcher)

    private val oAuthUserRepo = mockk<OAuthUserRepository>(relaxed = true)
    private val onboardingManager = mockk<OnboardingManager>()
    private val walkThroughRepo = mockk<WalkthroughRepo>()

    @BeforeEach
    fun setUp() {
        coEvery { oAuthUserRepo.saveCodeVerifier(any()) } just Runs
    }

    // NOTE: @AfterEach and mockkStatic are completely removed

    @Test
    fun `launchLoginPage generates redirect login URL and saves code verifier on injected dispatcher`() = runTest(testDispatcher) {
        // Arrange
        val mockVerifier = "verifier_xyz"
        val mockRedirectUrl = "https://example.com/oauth/authorize"

        every { onboardingManager.generateCodeVerifier() } returns mockVerifier
        every { onboardingManager.generateLoginUrl(mockVerifier) } returns mockRedirectUrl
        coEvery { walkThroughRepo.getWalkthroughList() } returns CResponse.Success(emptyList())

        // Pass testDispatcher directly into the constructor parameter
        val viewModel = OnboardingViewModel(
            oAuthUserRepo = oAuthUserRepo,
            onboardingManager = onboardingManager,
            walkThroughRepo = walkThroughRepo,
            ioDispatcher = testDispatcher // <-- Injecting testDispatcher here
        )

        // Act
        val redirectUrl = viewModel.launchLoginPage()

        // Assert
        assertEquals(mockRedirectUrl, redirectUrl)

        // Execute the coroutines launched inside viewModelScope
        testScheduler.runCurrent()

        // Verify saveCodeVerifier was completed
        coVerify(exactly = 1) { oAuthUserRepo.saveCodeVerifier(mockVerifier) }
    }

    @Test
    fun `onboardingState emits Idle and then Success with artworks when walkthrough list is fetched successfully`() = runTest(testDispatcher) {
        // Arrange
        val mockArtworks = List<Artwork>(5) { index ->
            createMockArtwork()
        }
        coEvery { walkThroughRepo.getWalkthroughList() } returns CResponse.Success(mockArtworks)

        // Pass testDispatcher here as well
        val viewModel = OnboardingViewModel(oAuthUserRepo, onboardingManager, walkThroughRepo, testDispatcher)
        val collectedStates = mutableListOf<OnboardingUiState>()

        // Act
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.onboardingState.collect { collectedStates.add(it) }
        }
        testScheduler.runCurrent()

        // Assert
        assertEquals(2, collectedStates.size)
        assertEquals(mockArtworks, collectedStates[1].walkthroughArtworks)

        collectJob.cancel()
    }
}