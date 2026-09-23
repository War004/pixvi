package com.cryptic.piyek.feature.home.presentation

import com.cryptic.piyek.core.auth.domain.model.OauthUserPublic
import com.cryptic.piyek.core.auth.domain.repository.OAuthUserRepository
import com.cryptic.piyek.core.content.data.model.Artwork
import com.cryptic.piyek.core.content.data.model.ArtworkContentList
import com.cryptic.piyek.core.content.data.model.RecommendationNonNovelPara
import com.cryptic.piyek.core.content.data.model.createMockArtwork
import com.cryptic.piyek.core.content.domain.repo.CoreContentApiRepo
import com.cryptic.piyek.util.MainDispatcherExtension
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
    private val mangaContentListFlow = MutableStateFlow<ArtworkContentList?>(null)
    private val activeAccountFlow = MutableStateFlow<OauthUserPublic?>(null)

    private val iLLustRepo = mockk<CoreContentApiRepo<ArtworkContentList, RecommendationNonNovelPara, ArtworkContentList>>(relaxed = true)
    private val mangaRepo = mockk<CoreContentApiRepo<ArtworkContentList, RecommendationNonNovelPara, ArtworkContentList>>(relaxed = true)
    private val oAuthUserRepository = mockk<OAuthUserRepository>(relaxed = true)
    private val activeAccountId = 1001L

    @BeforeEach
    fun setUp() {
        every { iLLustRepo.contentList } returns contentListFlow
        every { mangaRepo.contentList } returns mangaContentListFlow
        every { oAuthUserRepository.observeActiveAccount() } returns activeAccountFlow
    }

    private fun createViewModel(
        userRepo: OAuthUserRepository = oAuthUserRepository
    ): HomeViewModel {
        return HomeViewModel(
            iLLustRepo = iLLustRepo,
            mangaRepo = mangaRepo,
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
    fun `initial state contains empty currentAccount when no active account emitted`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        assertEquals(0L, viewModel.uiState.value.currentAccount.accountId)
        assertFalse(viewModel.uiState.value.isSearchActive)
    }

    @Test
    fun `changeUiMode updates uiMode to Manga`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        viewModel.action(HomeScreenAction.NewUiMode(NavOptions.Manga))
        testScheduler.advanceUntilIdle()
        assertEquals(NavOptions.Manga, viewModel.uiState.value.currentDestination)
        job.cancel()
    }

    @Test
    fun `changeUiMode updates uiMode to Novel`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        viewModel.action(HomeScreenAction.NewUiMode(NavOptions.Novel))
        testScheduler.advanceUntilIdle()
        assertEquals(NavOptions.Novel, viewModel.uiState.value.currentDestination)
        job.cancel()
    }

    // ========================================================================
    // Search Status Tests
    // ========================================================================

    @Test
    fun `SearchStatusChange action updates isSearchActive`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.action(HomeScreenAction.SearchStatusChange(true))
        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isSearchActive)

        viewModel.action(HomeScreenAction.SearchStatusChange(false))
        testScheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isSearchActive)
        job.cancel()
    }

    // ========================================================================
    // Account Detail Tests
    // ========================================================================

    @Test
    fun `uiState reactively updates currentAccount when active account flow emits`() = runTest(testDispatcher) {
        val mockUser = OauthUserPublic(
            id = activeAccountId,
            name = "Pixiv Artist",
            account = "pixiv_artist",
            mailAddress = "artist@pixiv.net",
            bestProfilePicUrl = "https://example.com/avatar.jpg"
        )

        val viewModel = createViewModel()
        val collectedStates = mutableListOf<HomeScreenUiState>()

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { collectedStates.add(it) }
        }

        testScheduler.advanceUntilIdle()

        // Act: Emit active user
        activeAccountFlow.value = mockUser
        testScheduler.advanceUntilIdle()

        // Assert
        val currentAccount = collectedStates.last().currentAccount
        assertEquals(activeAccountId, currentAccount.accountId)
        assertEquals("Pixiv Artist", currentAccount.accountName)
        assertEquals("artist@pixiv.net", currentAccount.accountEmail)
        assertEquals("https://example.com/avatar.jpg", currentAccount.profilePicBestUrl)

        job.cancel()
    }

    @Test
    fun `ChangeAccount action calls saveActiveUser on repository`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.action(HomeScreenAction.ChangeAccount(2002L))
        testScheduler.advanceUntilIdle()

        io.mockk.coVerify(exactly = 1) { oAuthUserRepository.saveActiveUser(2002L) }
    }

    @Test
    fun `LogOut with more than one account deletes user and switches to first remaining account`() = runTest(testDispatcher) {
        val mockUser = OauthUserPublic(
            id = activeAccountId,
            name = "Pixiv Artist",
            account = "pixiv_artist",
            mailAddress = "artist@pixiv.net",
            bestProfilePicUrl = "https://example.com/avatar.jpg"
        )
        val viewModel = createViewModel()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        activeAccountFlow.value = mockUser
        testScheduler.advanceUntilIdle()

        io.mockk.coEvery { oAuthUserRepository.getTotalAccountCount() } returns 2
        io.mockk.coEvery { oAuthUserRepository.getFirstAccountId() } returns 2002L

        // Act
        viewModel.action(HomeScreenAction.LogOut)
        testScheduler.advanceUntilIdle()

        // Assert
        io.mockk.coVerify(exactly = 1) { oAuthUserRepository.deleteUser(activeAccountId) }
        io.mockk.coVerify(exactly = 1) { oAuthUserRepository.getFirstAccountId() }
        io.mockk.coVerify(exactly = 1) { oAuthUserRepository.saveActiveUser(2002L) }
        io.mockk.coVerify(exactly = 0) { oAuthUserRepository.removeActiveAccount() }

        job.cancel()
    }

    @Test
    fun `LogOut with only one account deletes user and removes active account preference`() = runTest(testDispatcher) {
        val mockUser = OauthUserPublic(
            id = activeAccountId,
            name = "Pixiv Artist",
            account = "pixiv_artist",
            mailAddress = "artist@pixiv.net",
            bestProfilePicUrl = "https://example.com/avatar.jpg"
        )
        val viewModel = createViewModel()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        activeAccountFlow.value = mockUser
        testScheduler.advanceUntilIdle()

        io.mockk.coEvery { oAuthUserRepository.getTotalAccountCount() } returns 1

        // Act
        viewModel.action(HomeScreenAction.LogOut)
        testScheduler.advanceUntilIdle()

        // Assert
        io.mockk.coVerify(exactly = 1) { oAuthUserRepository.deleteUser(activeAccountId) }
        io.mockk.coVerify(exactly = 0) { oAuthUserRepository.getFirstAccountId() }
        io.mockk.coVerify(exactly = 1) { oAuthUserRepository.removeActiveAccount() }

        job.cancel()
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

    // ========================================================================
    // Active Manga Extraction Tests
    // ========================================================================

    @Test
    fun `activeManga emits null when mangaContentList is null`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val collectedManga = mutableListOf<Artwork?>()

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.activeManga.collect { collectedManga.add(it) }
        }

        testScheduler.advanceUntilIdle()
        assertNull(collectedManga.last())

        job.cancel()
    }

    @Test
    fun `activeManga emits correct artwork based on focusedIndex`() = runTest(testDispatcher) {
        val artwork1 = createMockArtwork(id = 10L)
        val artwork2 = createMockArtwork(id = 20L)
        val artwork3 = createMockArtwork(id = 30L)

        val viewModel = createViewModel()
        val collectedManga = mutableListOf<Artwork?>()

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.activeManga.collect { collectedManga.add(it) }
        }

        testScheduler.advanceUntilIdle()

        // Act: Emit content with focusedIndex = 1
        mangaContentListFlow.value = ArtworkContentList(
            artworkList = listOf(artwork1, artwork2, artwork3),
            nextUrl = "",
            focusedIndex = 1
        )

        testScheduler.advanceUntilIdle()

        // Assert: Should emit artwork2 (index 1)
        assertEquals(artwork2, collectedManga.last())

        job.cancel()
    }

    @Test
    fun `activeManga updates when focusedIndex changes`() = runTest(testDispatcher) {
        val artwork1 = createMockArtwork(id = 10L)
        val artwork2 = createMockArtwork(id = 20L)

        val viewModel = createViewModel()
        val collectedManga = mutableListOf<Artwork?>()

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.activeManga.collect { collectedManga.add(it) }
        }

        testScheduler.advanceUntilIdle()

        // Emit with focusedIndex = 0
        mangaContentListFlow.value = ArtworkContentList(
            artworkList = listOf(artwork1, artwork2),
            nextUrl = "",
            focusedIndex = 0
        )
        testScheduler.advanceUntilIdle()
        assertEquals(artwork1, collectedManga.last())

        // Update with focusedIndex = 1
        mangaContentListFlow.value = ArtworkContentList(
            artworkList = listOf(artwork1, artwork2),
            nextUrl = "",
            focusedIndex = 1
        )
        testScheduler.advanceUntilIdle()
        assertEquals(artwork2, collectedManga.last())

        job.cancel()
    }

    @Test
    fun `activeManga emits null when focusedIndex is out of bounds`() = runTest(testDispatcher) {
        val artwork1 = createMockArtwork(id = 10L)

        val viewModel = createViewModel()
        val collectedManga = mutableListOf<Artwork?>()

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.activeManga.collect { collectedManga.add(it) }
        }

        testScheduler.advanceUntilIdle()

        // Act: Emit content with focusedIndex = 99 (out of bounds)
        mangaContentListFlow.value = ArtworkContentList(
            artworkList = listOf(artwork1),
            nextUrl = "",
            focusedIndex = 99
        )

        testScheduler.advanceUntilIdle()

        // Assert: Should emit null because getOrNull(99) is null
        assertNull(collectedManga.last())

        job.cancel()
    }

    @Test
    fun `activeManga emits null when artworkList is empty`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val collectedManga = mutableListOf<Artwork?>()

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.activeManga.collect { collectedManga.add(it) }
        }

        testScheduler.advanceUntilIdle()

        // Act: Emit content with empty list
        mangaContentListFlow.value = ArtworkContentList(
            artworkList = emptyList(),
            nextUrl = "",
            focusedIndex = 0
        )

        testScheduler.advanceUntilIdle()

        // Assert: Should emit null
        assertNull(collectedManga.last())

        job.cancel()
    }

    // ========================================================================
    // Current Active Artwork Switching Tests
    // ========================================================================

    @Test
    fun `currentActiveArtwork emits activeArtwork when destination is ILLust`() = runTest(testDispatcher) {
        val illustArtwork = createMockArtwork(id = 100L)
        val mangaArtwork = createMockArtwork(id = 200L)

        contentListFlow.value = ArtworkContentList(
            artworkList = listOf(illustArtwork),
            nextUrl = "",
            focusedIndex = 0
        )
        mangaContentListFlow.value = ArtworkContentList(
            artworkList = listOf(mangaArtwork),
            nextUrl = "",
            focusedIndex = 0
        )

        val viewModel = createViewModel()
        val collectedArtworks = mutableListOf<Artwork?>()

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.currentActiveArtwork.collect { collectedArtworks.add(it) }
        }

        testScheduler.advanceUntilIdle()

        // By default destination is ILLust -> should emit illustArtwork
        assertEquals(illustArtwork, collectedArtworks.last())

        job.cancel()
    }

    @Test
    fun `currentActiveArtwork switches to activeManga when destination changes to Manga`() = runTest(testDispatcher) {
        val illustArtwork = createMockArtwork(id = 100L)
        val mangaArtwork = createMockArtwork(id = 200L)

        contentListFlow.value = ArtworkContentList(
            artworkList = listOf(illustArtwork),
            nextUrl = "",
            focusedIndex = 0
        )
        mangaContentListFlow.value = ArtworkContentList(
            artworkList = listOf(mangaArtwork),
            nextUrl = "",
            focusedIndex = 0
        )

        val viewModel = createViewModel()
        val collectedArtworks = mutableListOf<Artwork?>()

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.currentActiveArtwork.collect { collectedArtworks.add(it) }
        }

        testScheduler.advanceUntilIdle()
        assertEquals(illustArtwork, collectedArtworks.last())

        // Switch destination to Manga
        viewModel.action(HomeScreenAction.NewUiMode(NavOptions.Manga))
        testScheduler.advanceUntilIdle()

        // Should now emit mangaArtwork
        assertEquals(mangaArtwork, collectedArtworks.last())

        job.cancel()
    }

    @Test
    fun `currentActiveArtwork emits null when destination is Novel`() = runTest(testDispatcher) {
        val illustArtwork = createMockArtwork(id = 100L)

        contentListFlow.value = ArtworkContentList(
            artworkList = listOf(illustArtwork),
            nextUrl = "",
            focusedIndex = 0
        )

        val viewModel = createViewModel()
        val collectedArtworks = mutableListOf<Artwork?>()

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.currentActiveArtwork.collect { collectedArtworks.add(it) }
        }

        testScheduler.advanceUntilIdle()
        assertEquals(illustArtwork, collectedArtworks.last())

        // Switch to Novel
        viewModel.action(HomeScreenAction.NewUiMode(NavOptions.Novel))
        testScheduler.advanceUntilIdle()

        assertNull(collectedArtworks.last())

        job.cancel()
    }
}
