package com.cryptic.piyek.feature.iLLust.presentation.home

import com.cryptic.piyek.core.CResponse
import com.cryptic.piyek.core.content.data.model.Artwork
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
class HomeILLustViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @RegisterExtension
    val mainDispatcherExtension = MainDispatcherExtension(testDispatcher)

    private val contentListFlow = MutableStateFlow<ArtworkContentList?>(null)

    private val iLLustRepo = mockk<CoreContentApiRepo<ArtworkContentList, RecommendationNonNovelPara, ArtworkContentList>>(relaxed = true)

    @BeforeEach
    fun setUp() {
        every { iLLustRepo.contentList } returns contentListFlow
    }

    private fun createViewModel(): HomeILLustViewModel {
        return HomeILLustViewModel(iLLustRepo)
    }

    // ========================================================================
    // UI State Mapping Tests
    // ========================================================================

    @Test
    fun `initial uiState is Loading`() = runTest(testDispatcher) {
        // Arrange
        coEvery { iLLustRepo.getRecommendation(any()) } returns CResponse.Success(Unit)

        // Act
        val viewModel = createViewModel()
        val collectedStates = mutableListOf<HomeILLustUiState>()

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

        coEvery { iLLustRepo.getRecommendation(any()) } coAnswers {
            contentListFlow.value = mockContent
            CResponse.Success(Unit)
        }

        // Act
        val viewModel = createViewModel()
        val collectedStates = mutableListOf<HomeILLustUiState>()

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { collectedStates.add(it) }
        }

        testScheduler.advanceUntilIdle()

        // Assert: Should eventually have Idle status with artworks
        val lastState = collectedStates.last()
        assertEquals(ScreenStatus.Idle, lastState.status)
        assertEquals(2, lastState.artworkList.size)

        job.cancel()
    }

    @Test
    fun `uiState emits error message when getRecommendation fails`() = runTest(testDispatcher) {
        // Arrange
        coEvery { iLLustRepo.getRecommendation(any()) } returns
                CResponse.Failed(Exception("Server unreachable"))

        // Act
        val viewModel = createViewModel()
        val collectedStates = mutableListOf<HomeILLustUiState>()

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { collectedStates.add(it) }
        }

        testScheduler.advanceUntilIdle()

        // Assert: Should have error message
        val lastState = collectedStates.last()
        assertEquals("Server unreachable", lastState.errorMessage)

        job.cancel()
    }

    // ========================================================================
    // Action Tests
    // ========================================================================

    @Test
    fun `Retry action triggers getRecommendation`() = runTest(testDispatcher) {
        // Arrange
        coEvery { iLLustRepo.getRecommendation(any()) } returns CResponse.Success(Unit)

        val viewModel = createViewModel()

        // Collect to activate the stateIn
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        testScheduler.advanceUntilIdle()

        // Act: Trigger Retry (this is a second call; first is from onStart)
        viewModel.onAction(HomeILLustAction.Retry)
        testScheduler.advanceUntilIdle()

        // Assert: getRecommendation should have been called at least twice (onStart + retry)
        coVerify(atLeast = 2) { iLLustRepo.getRecommendation(any()) }

        job.cancel()
    }

    @Test
    fun `LoadMore action calls getMoreRecommendation`() = runTest(testDispatcher) {
        // Arrange
        coEvery { iLLustRepo.getRecommendation(any()) } returns CResponse.Success(Unit)
        coEvery { iLLustRepo.getMoreRecommendation() } returns CResponse.Success(Unit)

        val viewModel = createViewModel()

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        testScheduler.advanceUntilIdle()

        // Act
        viewModel.onAction(HomeILLustAction.LoadMore)
        testScheduler.advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { iLLustRepo.getMoreRecommendation() }

        job.cancel()
    }

    @Test
    fun `UpdateFocusedIndex action calls changeFocusedIndex on repo`() = runTest(testDispatcher) {
        // Arrange
        coEvery { iLLustRepo.getRecommendation(any()) } returns CResponse.Success(Unit)

        val viewModel = createViewModel()

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        testScheduler.advanceUntilIdle()

        // Act
        viewModel.onAction(HomeILLustAction.UpdateFocusedIndex(5))

        // Assert
        verify(exactly = 1) { iLLustRepo.changeFocusedIndex(5) }

        job.cancel()
    }

    @Test
    fun `ToggleBookmark with isCurrentlyBookmarked true calls deleteBookmark`() = runTest(testDispatcher) {
        // Arrange
        coEvery { iLLustRepo.getRecommendation(any()) } returns CResponse.Success(Unit)
        coEvery { iLLustRepo.deleteBookmark(42L) } returns CResponse.Success(Unit)

        val viewModel = createViewModel()

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        testScheduler.advanceUntilIdle()

        // Act
        viewModel.onAction(HomeILLustAction.ToggleBookmark(postId = 42L, isCurrentlyBookmarked = true))
        testScheduler.advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { iLLustRepo.deleteBookmark(42L) }
        coVerify(exactly = 0) { iLLustRepo.addBookmark(any(), any()) }

        job.cancel()
    }

    @Test
    fun `ToggleBookmark with isCurrentlyBookmarked false calls addBookmark`() = runTest(testDispatcher) {
        // Arrange
        coEvery { iLLustRepo.getRecommendation(any()) } returns CResponse.Success(Unit)
        coEvery { iLLustRepo.addBookmark(42L, BookmarkRestrict.PUBLIC) } returns CResponse.Success(Unit)

        val viewModel = createViewModel()

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        testScheduler.advanceUntilIdle()

        // Act
        viewModel.onAction(HomeILLustAction.ToggleBookmark(postId = 42L, isCurrentlyBookmarked = false))
        testScheduler.advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { iLLustRepo.addBookmark(42L, BookmarkRestrict.PUBLIC) }
        coVerify(exactly = 0) { iLLustRepo.deleteBookmark(any()) }

        job.cancel()
    }

    @Test
    fun `ToggleBookmark with custom restrict calls addBookmark with that restrict`() = runTest(testDispatcher) {
        // Arrange
        coEvery { iLLustRepo.getRecommendation(any()) } returns CResponse.Success(Unit)
        coEvery { iLLustRepo.addBookmark(42L, BookmarkRestrict.PRIVATE) } returns CResponse.Success(Unit)

        val viewModel = createViewModel()

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        testScheduler.advanceUntilIdle()

        // Act
        viewModel.onAction(
            HomeILLustAction.ToggleBookmark(
                postId = 42L,
                isCurrentlyBookmarked = false,
                restrict = BookmarkRestrict.PRIVATE
            )
        )
        testScheduler.advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { iLLustRepo.addBookmark(42L, BookmarkRestrict.PRIVATE) }

        job.cancel()
    }
}
