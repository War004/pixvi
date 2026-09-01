package com.cryptic.piyek.feature.home.presentation

import com.cryptic.piyek.core.auth.domain.model.OauthUser
import com.cryptic.piyek.core.auth.domain.repository.OAuthUserRepository
import com.cryptic.piyek.core.content.data.model.Artwork
import com.cryptic.piyek.core.content.data.model.ArtworkContentList
import com.cryptic.piyek.core.content.data.model.RecommendationNonNovelPara
import com.cryptic.piyek.core.content.data.model.createMockArtwork
import com.cryptic.piyek.core.content.domain.repo.CoreContentApiRepo
import com.cryptic.piyek.util.MainDispatcherExtension
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @RegisterExtension
    val mainDispatcherExtension = MainDispatcherExtension(testDispatcher)

    private val contentListFlow = MutableStateFlow<ArtworkContentList?>(null)

    private val iLLustRepo = mockk<CoreContentApiRepo<ArtworkContentList, RecommendationNonNovelPara, ArtworkContentList>>(relaxed = true)
    private val oAuthUserRepository = mockk<OAuthUserRepository>(relaxed = true)
    private val activeAccountId = 1001L

    @BeforeEach
    fun setUp() {
        every { iLLustRepo.contentList } returns contentListFlow
    }

    private fun createViewModel(
        accountId: Long = activeAccountId,
        userRepo: OAuthUserRepository = oAuthUserRepository
    ): HomeViewModel {
        return HomeViewModel(
            iLLustRepo = iLLustRepo,
            activeAccountId = accountId,
            oAuthUserRepository = userRepo,
            ioDispatcher = testDispatcher
        )
    }

    // ========================================================================
    // UI Mode & Initial State Tests
    // ========================================================================

    @Test
    fun `initial uiMode is ILLust`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        assertEquals(NavOptions.ILLust, viewModel.uiState.value.currentDestination)
    }

    @Test
    fun `initial state contains activeAccountId in currentAccount`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        assertEquals(activeAccountId, viewModel.uiState.value.currentAccount.accountId)
        assertFalse(viewModel.uiState.value.isSearchActive)
    }

    @Test
    fun `changeUiMode updates uiMode to Manga`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.action(HomeScreenAction.NewUiMode(NavOptions.Manga))
        assertEquals(NavOptions.Manga, viewModel.uiState.value.currentDestination)
    }

    @Test
    fun `changeUiMode updates uiMode to Novel`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.action(HomeScreenAction.NewUiMode(NavOptions.Novel))
        assertEquals(NavOptions.Novel, viewModel.uiState.value.currentDestination)
    }

    // ========================================================================
    // Search Status Tests
    // ========================================================================

    @Test
    fun `SearchStatusChange action updates isSearchActive`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.action(HomeScreenAction.SearchStatusChange(true))
        assertTrue(viewModel.uiState.value.isSearchActive)

        viewModel.action(HomeScreenAction.SearchStatusChange(false))
        assertFalse(viewModel.uiState.value.isSearchActive)
    }

    // ========================================================================
    // Account Detail Tests
    // ========================================================================

    @Test
    fun `ActiveAccountDetails action updates currentAccount with user details from repo`() = runTest(testDispatcher) {
        val mockUser = OauthUser(
            id = activeAccountId,
            name = "Pixiv Artist",
            account = "pixiv_artist",
            mailAddress = "artist@pixiv.net",
            bestProfilePicUrl = "https://example.com/avatar.jpg",
            accessToken = null,
            encryptedRefreshToken = "encrypted_token_123"
        )
        coEvery { oAuthUserRepository.getUser(activeAccountId) } returns mockUser

        val viewModel = createViewModel()

        viewModel.action(HomeScreenAction.ActiveAccountDetails)
        testScheduler.advanceUntilIdle()

        val currentAccount = viewModel.uiState.value.currentAccount
        assertEquals(activeAccountId, currentAccount.accountId)
        assertEquals("Pixiv Artist", currentAccount.accountName)
        assertEquals("artist@pixiv.net", currentAccount.accountEmail)
        assertEquals("https://example.com/avatar.jpg", currentAccount.profilePicBestUrl)
        coVerify(exactly = 1) { oAuthUserRepository.getUser(activeAccountId) }
    }

    @Test
    fun `ActiveAccountDetails action does not query repo when account details are already populated`() = runTest(testDispatcher) {
        val mockUser = OauthUser(
            id = activeAccountId,
            name = "Pixiv Artist",
            account = "pixiv_artist",
            mailAddress = "artist@pixiv.net",
            bestProfilePicUrl = "https://example.com/avatar.jpg",
            accessToken = null,
            encryptedRefreshToken = "encrypted_token_123"
        )
        coEvery { oAuthUserRepository.getUser(activeAccountId) } returns mockUser

        val viewModel = createViewModel()

        // First action populates details
        viewModel.action(HomeScreenAction.ActiveAccountDetails)
        testScheduler.advanceUntilIdle()

        // Second action should skip querying because details are not empty
        viewModel.action(HomeScreenAction.ActiveAccountDetails)
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) { oAuthUserRepository.getUser(activeAccountId) }
    }

    // ========================================================================
    // Active Artwork Extraction Tests
    // ========================================================================

    @Test
    fun `activeArtwork emits null when contentList is null`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val collectedArtworks = mutableListOf<Artwork?>()

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.activeArtwork.collect { collectedArtworks.add(it) }
        }

        testScheduler.advanceUntilIdle()
        assertNull(collectedArtworks.last())

        job.cancel()
    }

    @Test
    fun `activeArtwork emits correct artwork based on focusedIndex`() = runTest(testDispatcher) {
        val artwork1 = createMockArtwork(id = 1L)
        val artwork2 = createMockArtwork(id = 2L)
        val artwork3 = createMockArtwork(id = 3L)

        val viewModel = createViewModel()
        val collectedArtworks = mutableListOf<Artwork?>()

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.activeArtwork.collect { collectedArtworks.add(it) }
        }

        testScheduler.advanceUntilIdle()

        // Act: Emit content with focusedIndex = 1
        contentListFlow.value = ArtworkContentList(
            artworkList = listOf(artwork1, artwork2, artwork3),
            nextUrl = "",
            focusedIndex = 1
        )

        testScheduler.advanceUntilIdle()

        // Assert: Should emit artwork2 (index 1)
        assertEquals(artwork2, collectedArtworks.last())

        job.cancel()
    }

    @Test
    fun `activeArtwork emits null when focusedIndex is out of bounds`() = runTest(testDispatcher) {
        val artwork1 = createMockArtwork(id = 1L)

        val viewModel = createViewModel()
        val collectedArtworks = mutableListOf<Artwork?>()

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.activeArtwork.collect { collectedArtworks.add(it) }
        }

        testScheduler.advanceUntilIdle()

        // Act: Emit content with focusedIndex = 99 (out of bounds)
        contentListFlow.value = ArtworkContentList(
            artworkList = listOf(artwork1),
            nextUrl = "",
            focusedIndex = 99
        )

        testScheduler.advanceUntilIdle()

        // Assert: Should emit null because getOrNull(99) is null
        assertNull(collectedArtworks.last())

        job.cancel()
    }
}
