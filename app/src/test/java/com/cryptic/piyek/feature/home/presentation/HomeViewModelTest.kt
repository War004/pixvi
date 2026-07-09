package com.cryptic.piyek.feature.home.presentation

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
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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

    @BeforeEach
    fun setUp() {
        every { iLLustRepo.contentList } returns contentListFlow
    }

    // ========================================================================
    // UI Mode Tests
    // ========================================================================

    @Test
    fun `initial uiMode is ILLust`() = runTest(testDispatcher) {
        // Act
        val viewModel = HomeViewModel(iLLustRepo)

        // Assert
        assertEquals(NavOptions.ILLust, viewModel.uiMode.value)
    }

    @Test
    fun `changeUiMode updates uiMode to Manga`() = runTest(testDispatcher) {
        // Arrange
        val viewModel = HomeViewModel(iLLustRepo)

        // Act
        viewModel.changeUiMode(NavOptions.Manga)

        // Assert
        assertEquals(NavOptions.Manga, viewModel.uiMode.value)
    }

    @Test
    fun `changeUiMode updates uiMode to Novel`() = runTest(testDispatcher) {
        // Arrange
        val viewModel = HomeViewModel(iLLustRepo)

        // Act
        viewModel.changeUiMode(NavOptions.Novel)

        // Assert
        assertEquals(NavOptions.Novel, viewModel.uiMode.value)
    }

    // ========================================================================
    // Active Artwork Extraction Tests
    // ========================================================================

    @Test
    fun `activeArtwork emits null when contentList is null`() = runTest(testDispatcher) {
        // Arrange
        val viewModel = HomeViewModel(iLLustRepo)
        val collectedArtworks = mutableListOf<com.cryptic.piyek.core.content.data.model.Artwork?>()

        // Act
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.activeArtwork.collect { collectedArtworks.add(it) }
        }

        testScheduler.advanceUntilIdle()

        // Assert
        assertNull(collectedArtworks.last())

        job.cancel()
    }

    @Test
    fun `activeArtwork emits correct artwork based on focusedIndex`() = runTest(testDispatcher) {
        // Arrange
        val artwork1 = createMockArtwork(id = 1L)
        val artwork2 = createMockArtwork(id = 2L)
        val artwork3 = createMockArtwork(id = 3L)

        val viewModel = HomeViewModel(iLLustRepo)
        val collectedArtworks = mutableListOf<com.cryptic.piyek.core.content.data.model.Artwork?>()

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
        // Arrange
        val artwork1 = createMockArtwork(id = 1L)

        val viewModel = HomeViewModel(iLLustRepo)
        val collectedArtworks = mutableListOf<com.cryptic.piyek.core.content.data.model.Artwork?>()

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
