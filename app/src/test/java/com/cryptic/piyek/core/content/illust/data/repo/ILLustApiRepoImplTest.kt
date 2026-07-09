package com.cryptic.piyek.core.content.illust.data.repo

import com.cryptic.piyek.core.CResponse
import com.cryptic.piyek.core.content.data.model.Artwork
import com.cryptic.piyek.core.content.data.model.ArtworkContentList
import com.cryptic.piyek.core.content.data.model.RecommendationNonNovelPara
import com.cryptic.piyek.core.content.data.model.createMockArtwork
import com.cryptic.piyek.core.content.data.remote.ArtworkResponse
import com.cryptic.piyek.core.content.data.remote.ILLustMangaRecommendationResponse
import com.cryptic.piyek.core.content.data.remote.toDomain
import com.cryptic.piyek.core.content.illust.data.remote.ILLustApiService
import com.cryptic.piyek.core.data.local.BookmarkRestrict
import com.cryptic.piyek.core.network.NetworkResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ILLustApiRepoImplTest {

    private val ilLustApiService = mockk<ILLustApiService>()

    private lateinit var repo: ILLustApiRepoImpl

    private val defaultParams = RecommendationNonNovelPara(
        includeRanking = true,
        includePrivacyPolicy = false
    )

    @BeforeEach
    fun setUp() {
        mockkStatic("com.cryptic.piyek.core.content.data.remote.ArtworkResponseKt")
        repo = ILLustApiRepoImpl(ilLustApiService)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic("com.cryptic.piyek.core.content.data.remote.ArtworkResponseKt")
    }

    // ========================================================================
    // Helper to seed repo state for tests that need pre-existing content
    // ========================================================================

    private suspend fun seedContentList(
        artworks: List<Artwork>,
        rankingArtworks: List<Artwork> = emptyList(),
        nextUrl: String = "https://api.example.com/next"
    ) {
        val artworkResponses = artworks.map { artwork ->
            mockk<ArtworkResponse>().also { response ->
                every { response.toDomain() } returns artwork
            }
        }
        val rankingResponses = rankingArtworks.map { artwork ->
            mockk<ArtworkResponse>().also { response ->
                every { response.toDomain() } returns artwork
            }
        }

        val apiResponse = ILLustMangaRecommendationResponse(
            artwork = artworkResponses,
            contestExists = false,
            nextUrl = nextUrl,
            rankingArtwork = rankingResponses
        )

        coEvery {
            ilLustApiService.getRecommendation(any(), any(), any())
        } returns NetworkResult.Success(apiResponse)

        repo.getRecommendation(defaultParams)
    }

    // ========================================================================
    // getRecommendation Tests
    // ========================================================================

    @Test
    fun `getRecommendation returns Success and caches artworks when API succeeds`() = runTest {
        // Arrange
        val artwork1 = createMockArtwork(id = 1L)
        val artwork2 = createMockArtwork(id = 2L)
        val rankingArtwork = createMockArtwork(id = 100L)

        val artworkResponse1 = mockk<ArtworkResponse>()
        val artworkResponse2 = mockk<ArtworkResponse>()
        val rankingArtworkResponse = mockk<ArtworkResponse>()

        every { artworkResponse1.toDomain() } returns artwork1
        every { artworkResponse2.toDomain() } returns artwork2
        every { rankingArtworkResponse.toDomain() } returns rankingArtwork

        val apiResponse = ILLustMangaRecommendationResponse(
            artwork = listOf(artworkResponse1, artworkResponse2),
            contestExists = false,
            nextUrl = "https://api.example.com/next",
            rankingArtwork = listOf(rankingArtworkResponse)
        )

        coEvery {
            ilLustApiService.getRecommendation(any(), any(), any())
        } returns NetworkResult.Success(apiResponse)

        // Act
        val result = repo.getRecommendation(defaultParams)

        // Assert
        assertTrue(result is CResponse.Success)

        val cached = repo.contentList.value
        assertNotNull(cached)
        assertEquals(2, cached!!.artworkList.size)
        assertEquals(artwork1, cached.artworkList[0])
        assertEquals(artwork2, cached.artworkList[1])
        assertEquals(1, cached.rankingArtworkList.size)
        assertEquals(rankingArtwork, cached.rankingArtworkList[0])
        assertEquals("https://api.example.com/next", cached.nextUrl)
        assertEquals(0, cached.focusedIndex)
    }

    @Test
    fun `getRecommendation returns Failed when API returns empty artwork list`() = runTest {
        // Arrange
        val apiResponse = ILLustMangaRecommendationResponse(
            artwork = emptyList(),
            contestExists = false,
            nextUrl = ""
        )

        coEvery {
            ilLustApiService.getRecommendation(any(), any(), any())
        } returns NetworkResult.Success(apiResponse)

        // Act
        val result = repo.getRecommendation(defaultParams)

        // Assert
        assertTrue(result is CResponse.Failed)
        assertEquals("API returned no content.", (result as CResponse.Failed).exception.message)
    }

    @Test
    fun `getRecommendation returns Failed when API returns error`() = runTest {
        // Arrange
        coEvery {
            ilLustApiService.getRecommendation(any(), any(), any())
        } returns NetworkResult.Error(403, "Forbidden")

        // Act
        val result = repo.getRecommendation(defaultParams)

        // Assert
        assertTrue(result is CResponse.Failed)
        assertEquals("Forbidden", (result as CResponse.Failed).exception.message)
    }

    @Test
    fun `getRecommendation returns Failed when network throws exception`() = runTest {
        // Arrange
        val networkException = IOException("Connection timeout")
        coEvery {
            ilLustApiService.getRecommendation(any(), any(), any())
        } returns NetworkResult.Exception(networkException)

        // Act
        val result = repo.getRecommendation(defaultParams)

        // Assert
        assertTrue(result is CResponse.Failed)
        assertEquals(networkException, (result as CResponse.Failed).exception)
    }

    // ========================================================================
    // getMoreRecommendation Tests
    // ========================================================================

    @Test
    fun `getMoreRecommendation appends new artworks and deduplicates`() = runTest {
        // Arrange: Seed with existing content
        val existing1 = createMockArtwork(id = 1L)
        val existing2 = createMockArtwork(id = 2L)
        seedContentList(listOf(existing1, existing2), nextUrl = "https://api.example.com/page2")

        // Prepare next page response with one duplicate (id=2) and one new (id=3)
        val newArtwork = createMockArtwork(id = 3L)
        val duplicateArtwork = createMockArtwork(id = 2L) // duplicate id

        val newResponse = mockk<ArtworkResponse>()
        val dupResponse = mockk<ArtworkResponse>()
        every { newResponse.toDomain() } returns newArtwork
        every { dupResponse.toDomain() } returns duplicateArtwork

        val nextPageResponse = ILLustMangaRecommendationResponse(
            artwork = listOf(dupResponse, newResponse),
            contestExists = false,
            nextUrl = "https://api.example.com/page3"
        )

        coEvery {
            ilLustApiService.getMoreRecommendation("https://api.example.com/page2")
        } returns NetworkResult.Success(nextPageResponse)

        // Act
        val result = repo.getMoreRecommendation()

        // Assert
        assertTrue(result is CResponse.Success)

        val cached = repo.contentList.value
        assertNotNull(cached)
        // Should be 3 (deduplicated: ids 1, 2, 3)
        assertEquals(3, cached!!.artworkList.size)
        assertEquals("https://api.example.com/page3", cached.nextUrl)
    }

    @Test
    fun `getMoreRecommendation returns Failed when nextUrl is blank`() = runTest {
        // Arrange: Seed with content that has a blank nextUrl
        val artwork1 = createMockArtwork(id = 1L)
        seedContentList(listOf(artwork1), nextUrl = "")

        // Act
        val result = repo.getMoreRecommendation()

        // Assert
        assertTrue(result is CResponse.Failed)
        assertEquals("No more pages available.", (result as CResponse.Failed).exception.message)
    }

    @Test
    fun `getMoreRecommendation returns Failed when API returns empty list`() = runTest {
        // Arrange
        val artwork1 = createMockArtwork(id = 1L)
        seedContentList(listOf(artwork1), nextUrl = "https://api.example.com/page2")

        val emptyResponse = ILLustMangaRecommendationResponse(
            artwork = emptyList(),
            contestExists = false,
            nextUrl = ""
        )

        coEvery {
            ilLustApiService.getMoreRecommendation("https://api.example.com/page2")
        } returns NetworkResult.Success(emptyResponse)

        // Act
        val result = repo.getMoreRecommendation()

        // Assert
        assertTrue(result is CResponse.Failed)
        assertEquals("API returned no content.", (result as CResponse.Failed).exception.message)
    }

    @Test
    fun `getMoreRecommendation returns Failed when API returns error`() = runTest {
        // Arrange
        val artwork1 = createMockArtwork(id = 1L)
        seedContentList(listOf(artwork1), nextUrl = "https://api.example.com/page2")

        coEvery {
            ilLustApiService.getMoreRecommendation("https://api.example.com/page2")
        } returns NetworkResult.Error(500, "Server Error")

        // Act
        val result = repo.getMoreRecommendation()

        // Assert
        assertTrue(result is CResponse.Failed)
        assertEquals("Server Error", (result as CResponse.Failed).exception.message)
    }

    @Test
    fun `getMoreRecommendation returns Failed when network throws exception`() = runTest {
        // Arrange
        val artwork1 = createMockArtwork(id = 1L)
        seedContentList(listOf(artwork1), nextUrl = "https://api.example.com/page2")

        val networkException = IOException("DNS resolution failed")
        coEvery {
            ilLustApiService.getMoreRecommendation("https://api.example.com/page2")
        } returns NetworkResult.Exception(networkException)

        // Act
        val result = repo.getMoreRecommendation()

        // Assert
        assertTrue(result is CResponse.Failed)
        assertEquals(networkException, (result as CResponse.Failed).exception)
    }

    // ========================================================================
    // addBookmark Tests
    // ========================================================================

    @Test
    fun `addBookmark returns Success and updates isBookmarked in cache`() = runTest {
        // Arrange: Seed with an unbookmarked artwork
        val artwork = createMockArtwork(id = 10L, isBookmarked = false, totalBookmarks = 50)
        seedContentList(listOf(artwork))

        coEvery {
            ilLustApiService.addBookmark(10L, "PUBLIC")
        } returns NetworkResult.Success(Unit)

        // Act
        val result = repo.addBookmark(10L, BookmarkRestrict.PUBLIC)

        // Assert
        assertTrue(result is CResponse.Success)

        val cached = repo.contentList.value
        assertNotNull(cached)
        val updatedArtwork = cached!!.artworkList.first { it.id == 10L }
        assertTrue(updatedArtwork.isBookmarked)
        assertEquals(51, updatedArtwork.totalBookmarks)
    }

    @Test
    fun `addBookmark returns Failed when content list is null`() = runTest {
        // Arrange: No seed → contentList is null
        coEvery {
            ilLustApiService.addBookmark(10L, "PUBLIC")
        } returns NetworkResult.Success(Unit)

        // Act
        val result = repo.addBookmark(10L, BookmarkRestrict.PUBLIC)

        // Assert
        assertTrue(result is CResponse.Failed)
        assertEquals("Content list is empty/null", (result as CResponse.Failed).exception.message)
    }

    @Test
    fun `addBookmark returns Failed when post does not exist in list`() = runTest {
        // Arrange: Seed with artwork id=1, but try to bookmark id=999
        val artwork = createMockArtwork(id = 1L, isBookmarked = false)
        seedContentList(listOf(artwork))

        coEvery {
            ilLustApiService.addBookmark(999L, "PUBLIC")
        } returns NetworkResult.Success(Unit)

        // Act
        val result = repo.addBookmark(999L, BookmarkRestrict.PUBLIC)

        // Assert
        assertTrue(result is CResponse.Failed)
        assertEquals("Cannot find the targeted post", (result as CResponse.Failed).exception.message)
    }

    @Test
    fun `addBookmark returns Failed when API returns error`() = runTest {
        // Arrange
        val artwork = createMockArtwork(id = 10L, isBookmarked = false)
        seedContentList(listOf(artwork))

        coEvery {
            ilLustApiService.addBookmark(10L, "PUBLIC")
        } returns NetworkResult.Error(401, "Unauthorized")

        // Act
        val result = repo.addBookmark(10L, BookmarkRestrict.PUBLIC)

        // Assert
        assertTrue(result is CResponse.Failed)
        assertEquals("Unauthorized", (result as CResponse.Failed).exception.message)
    }

    @Test
    fun `addBookmark returns Failed when network throws exception`() = runTest {
        // Arrange
        val artwork = createMockArtwork(id = 10L, isBookmarked = false)
        seedContentList(listOf(artwork))

        val networkException = IOException("Timeout")
        coEvery {
            ilLustApiService.addBookmark(10L, "PUBLIC")
        } returns NetworkResult.Exception(networkException)

        // Act
        val result = repo.addBookmark(10L, BookmarkRestrict.PUBLIC)

        // Assert
        assertTrue(result is CResponse.Failed)
        assertEquals(networkException, (result as CResponse.Failed).exception)
    }

    // ========================================================================
    // deleteBookmark Tests
    // ========================================================================

    @Test
    fun `deleteBookmark returns Success and clears isBookmarked in cache`() = runTest {
        // Arrange: Seed with a bookmarked artwork
        val artwork = createMockArtwork(id = 20L, isBookmarked = true, totalBookmarks = 100)
        seedContentList(listOf(artwork))

        coEvery {
            ilLustApiService.deleteBookmark(20L)
        } returns NetworkResult.Success(Unit)

        // Act
        val result = repo.deleteBookmark(20L)

        // Assert
        assertTrue(result is CResponse.Success)

        val cached = repo.contentList.value
        assertNotNull(cached)
        val updatedArtwork = cached!!.artworkList.first { it.id == 20L }
        assertTrue(!updatedArtwork.isBookmarked)
        assertEquals(99, updatedArtwork.totalBookmarks)
    }

    @Test
    fun `deleteBookmark returns Failed when content list is null`() = runTest {
        // Arrange: No seed
        coEvery {
            ilLustApiService.deleteBookmark(20L)
        } returns NetworkResult.Success(Unit)

        // Act
        val result = repo.deleteBookmark(20L)

        // Assert
        assertTrue(result is CResponse.Failed)
        assertEquals("Content list is empty/null", (result as CResponse.Failed).exception.message)
    }

    @Test
    fun `deleteBookmark returns Failed when post does not exist in list`() = runTest {
        // Arrange
        val artwork = createMockArtwork(id = 1L, isBookmarked = true)
        seedContentList(listOf(artwork))

        coEvery {
            ilLustApiService.deleteBookmark(999L)
        } returns NetworkResult.Success(Unit)

        // Act
        val result = repo.deleteBookmark(999L)

        // Assert
        assertTrue(result is CResponse.Failed)
        assertEquals("Cannot find the targeted post", (result as CResponse.Failed).exception.message)
    }

    @Test
    fun `deleteBookmark returns Failed when API returns error`() = runTest {
        // Arrange
        val artwork = createMockArtwork(id = 20L, isBookmarked = true)
        seedContentList(listOf(artwork))

        coEvery {
            ilLustApiService.deleteBookmark(20L)
        } returns NetworkResult.Error(400, "Bad Request")

        // Act
        val result = repo.deleteBookmark(20L)

        // Assert
        assertTrue(result is CResponse.Failed)
        assertEquals("Bad Request", (result as CResponse.Failed).exception.message)
    }

    @Test
    fun `deleteBookmark returns Failed when network throws exception`() = runTest {
        // Arrange
        val artwork = createMockArtwork(id = 20L, isBookmarked = true)
        seedContentList(listOf(artwork))

        val networkException = IOException("Connection reset")
        coEvery {
            ilLustApiService.deleteBookmark(20L)
        } returns NetworkResult.Exception(networkException)

        // Act
        val result = repo.deleteBookmark(20L)

        // Assert
        assertTrue(result is CResponse.Failed)
        assertEquals(networkException, (result as CResponse.Failed).exception)
    }

    // ========================================================================
    // changeFocusedIndex Tests
    // ========================================================================

    @Test
    fun `changeFocusedIndex updates focused index in contentList`() = runTest {
        // Arrange: Seed with content
        val artwork1 = createMockArtwork(id = 1L)
        val artwork2 = createMockArtwork(id = 2L)
        seedContentList(listOf(artwork1, artwork2))

        assertEquals(0, repo.contentList.value?.focusedIndex)

        // Act
        repo.changeFocusedIndex(1)

        // Assert
        assertEquals(1, repo.contentList.value?.focusedIndex)
    }
}
