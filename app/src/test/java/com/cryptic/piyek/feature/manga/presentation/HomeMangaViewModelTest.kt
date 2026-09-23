package com.cryptic.piyek.feature.manga.presentation

import com.cryptic.piyek.core.CResponse
import com.cryptic.piyek.core.content.data.model.ArtworkContentList
import com.cryptic.piyek.core.content.data.model.RecommendationNonNovelPara
import com.cryptic.piyek.core.content.data.model.createMockArtwork
import com.cryptic.piyek.core.content.domain.repo.CoreContentApiRepo
import com.cryptic.piyek.core.data.local.BookmarkRestrict
import com.cryptic.piyek.util.MainDispatcherExtension
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class HomeMangaViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @RegisterExtension
    val mainDispatcherExtension = MainDispatcherExtension(testDispatcher)

    private val contentListFlow = MutableStateFlow<ArtworkContentList?>(null)

    private val mangaRepo = mockk<CoreContentApiRepo<ArtworkContentList, RecommendationNonNovelPara, ArtworkContentList>>(relaxed = true)

    @BeforeEach
    fun setUp() {
        every { mangaRepo.contentList } returns contentListFlow
    }

    private fun createViewModel(): HomeMangaViewModel {
        return HomeMangaViewModel(mangaRepo)
    }

    // ========================================================================
    // UI State Mapping Tests
    // ========================================================================

    @Test
    fun `initial uiState is Loading`() = runTest(testDispatcher) {
        // Arrange
        coEvery { mangaRepo.getRecommendation(any()) } returns CResponse.Success(Unit)

        // Act
        val viewModel = createViewModel()
        val collectedStates = mutableListOf<HomeMangaUiState>()

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { collectedStates.add(it) }
        }

        // Assert: First emission should be Loading
        assertTrue(collectedStates.isNotEmpty())
        assertEquals(ScreenStatus.Loading, collectedStates.first().status)

        job.cancel()
    }

    @Test
    fun `uiState emits Idle with artworks after successful getRecommendation`() = runTest(testDispatcher) {
        // Arrange
        val mockArtworks = listOf(createMockArtwork(id = 1L), createMockArtwork(id = 2L))
        val mockContent = ArtworkContentList(
            artworkList = mockArtworks,
            rankingArtworkList = emptyList(),
            nextUrl = "",
            focusedIndex = 0
        )

        coEvery { mangaRepo.getRecommendation(any()) } coAnswers {
            contentListFlow.value = mockContent
            CResponse.Success(Unit)
        }

        // Act
        val viewModel = createViewModel()
        val collectedStates = mutableListOf<HomeMangaUiState>()

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { collectedStates.add(it) }
        }

        testScheduler.advanceUntilIdle()

        // Assert: Should eventually have Idle status with artworks
        val lastState = collectedStates.last()
        assertEquals(ScreenStatus.Idle, lastState.status)
        assertEquals(2, lastState.artworkList.size)
        assertEquals(mockArtworks, lastState.artworkList)

        job.cancel()
    }

    @Test
    fun `uiState contains error message when getRecommendation fails`() = runTest(testDispatcher) {
        // Arrange
        coEvery { mangaRepo.getRecommendation(any()) } returns
                CResponse.Failed(Exception("Server unreachable"))

        // Act
        val viewModel = createViewModel()
        val collectedStates = mutableListOf<HomeMangaUiState>()

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { collectedStates.add(it) }
        }

        testScheduler.advanceUntilIdle()

        // Assert
        val lastState = collectedStates.last()
        assertEquals("Server unreachable", lastState.errorMessage)

        job.cancel()
    }

    // ========================================================================
    // Action Handling Tests
    // ========================================================================

    @Test
    fun `Retry action triggers getRecommendation`() = runTest(testDispatcher) {
        // Arrange: Initial load succeeds
        coEvery { mangaRepo.getRecommendation(any()) } returns CResponse.Success(Unit)
        val viewModel = createViewModel()

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        // Act: User taps retry
        viewModel.onAction(HomeMangaAction.Retry)
        testScheduler.advanceUntilIdle()

        // Assert: getRecommendation was called twice (initial onStart + retry)
        coVerify(atLeast = 2) { mangaRepo.getRecommendation(any()) }

        job.cancel()
    }

    @Test
    fun `LoadMore action triggers getMoreRecommendation when not already loading`() = runTest(testDispatcher) {
        // Arrange
        coEvery { mangaRepo.getRecommendation(any()) } returns CResponse.Success(Unit)
        coEvery { mangaRepo.getMoreRecommendation() } returns CResponse.Success(Unit)

        val viewModel = createViewModel()

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        // Act
        viewModel.onAction(HomeMangaAction.LoadMore)
        testScheduler.advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { mangaRepo.getMoreRecommendation() }

        job.cancel()
    }

    @Test
    fun `UpdateFocusedIndex calls repo changeFocusedIndex`() = runTest(testDispatcher) {
        // Arrange
        coEvery { mangaRepo.getRecommendation(any()) } returns CResponse.Success(Unit)
        val viewModel = createViewModel()

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        // Act
        viewModel.onAction(HomeMangaAction.UpdateFocusedIndex(5))

        // Assert
        verify(exactly = 1) { mangaRepo.changeFocusedIndex(5) }

        job.cancel()
    }

    @Test
    fun `ToggleBookmark deletes bookmark when currently bookmarked`() = runTest(testDispatcher) {
        // Arrange
        coEvery { mangaRepo.getRecommendation(any()) } returns CResponse.Success(Unit)
        coEvery { mangaRepo.deleteBookmark(42L) } returns CResponse.Success(Unit)

        val viewModel = createViewModel()

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        // Act
        viewModel.onAction(
            HomeMangaAction.ToggleBookmark(
                postId = 42L,
                isCurrentlyBookmarked = true,
                restrict = null
            )
        )
        testScheduler.advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { mangaRepo.deleteBookmark(42L) }
        coVerify(exactly = 0) { mangaRepo.addBookmark(any(), any()) }

        job.cancel()
    }

    @Test
    fun `ToggleBookmark adds bookmark when not currently bookmarked`() = runTest(testDispatcher) {
        // Arrange
        coEvery { mangaRepo.getRecommendation(any()) } returns CResponse.Success(Unit)
        coEvery { mangaRepo.addBookmark(42L, BookmarkRestrict.PUBLIC) } returns CResponse.Success(Unit)

        val viewModel = createViewModel()

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        // Act: Toggle with default restrict (null -> should default to PUBLIC)
        viewModel.onAction(
            HomeMangaAction.ToggleBookmark(
                postId = 42L,
                isCurrentlyBookmarked = false,
                restrict = null
            )
        )
        testScheduler.advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { mangaRepo.addBookmark(42L, BookmarkRestrict.PUBLIC) }
        coVerify(exactly = 0) { mangaRepo.deleteBookmark(any()) }

        job.cancel()
    }

    @Test
    fun `ToggleBookmark adds bookmark with specified restrict mode`() = runTest(testDispatcher) {
        // Arrange
        coEvery { mangaRepo.getRecommendation(any()) } returns CResponse.Success(Unit)
        coEvery { mangaRepo.addBookmark(42L, BookmarkRestrict.PRIVATE) } returns CResponse.Success(Unit)

        val viewModel = createViewModel()

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        // Act
        viewModel.onAction(
            HomeMangaAction.ToggleBookmark(
                postId = 42L,
                isCurrentlyBookmarked = false,
                restrict = BookmarkRestrict.PRIVATE
            )
        )
        testScheduler.advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { mangaRepo.addBookmark(42L, BookmarkRestrict.PRIVATE) }

        job.cancel()
    }
}
