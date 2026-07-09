package com.cryptic.piyek.feature.onboarding.data.repo

import com.cryptic.piyek.core.CResponse
import com.cryptic.piyek.core.content.data.model.Artwork
import com.cryptic.piyek.core.content.data.model.createMockArtwork
import com.cryptic.piyek.core.content.data.remote.ArtworkResponse
import com.cryptic.piyek.core.content.data.remote.toDomain
import com.cryptic.piyek.core.network.NetworkResult
import com.cryptic.piyek.feature.onboarding.data.remote.PixivWalkThroughApiService
import com.cryptic.piyek.feature.onboarding.data.remote.WalkthroughResponse
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class WalkthroughRepoImplTest {

    private val onBoardingApi = mockk<PixivWalkThroughApiService>()

    private lateinit var repository: WalkthroughRepoImpl

    @BeforeEach
    fun setUp() {
        mockkStatic("com.cryptic.piyek.core.content.data.remote.ArtworkResponseKt")
        repository = WalkthroughRepoImpl(onBoardingApi)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic("com.cryptic.piyek.core.content.data.remote.ArtworkResponseKt")
    }

    // --- Success ---

    @Test
    fun `getWalkthroughList returns Success with mapped artworks when API succeeds`() = runTest {
        // Arrange
        val mockArtworkResponse1 = mockk<ArtworkResponse>()
        val mockArtworkResponse2 = mockk<ArtworkResponse>()
        val domainArtwork1 = createMockArtwork(id = 1L)
        val domainArtwork2 = createMockArtwork(id = 2L)

        val walkthroughResponse = WalkthroughResponse(
            illusts = listOf(mockArtworkResponse1, mockArtworkResponse2)
        )

        coEvery { onBoardingApi.getWalkthroughIllusts() } returns
                NetworkResult.Success(walkthroughResponse)
        every { mockArtworkResponse1.toDomain() } returns domainArtwork1
        every { mockArtworkResponse2.toDomain() } returns domainArtwork2

        // Act
        val result = repository.getWalkthroughList()

        // Assert
        assertTrue(result is CResponse.Success)
        val data = (result as CResponse.Success).data
        assertEquals(2, data.size)
        assertEquals(domainArtwork1, data[0])
        assertEquals(domainArtwork2, data[1])
    }

    // --- API Error ---

    @Test
    fun `getWalkthroughList returns Failed when API returns error`() = runTest {
        // Arrange
        coEvery { onBoardingApi.getWalkthroughIllusts() } returns
                NetworkResult.Error(500, "Internal Server Error")

        // Act
        val result = repository.getWalkthroughList()

        // Assert
        assertTrue(result is CResponse.Failed)
        val exception = (result as CResponse.Failed).exception
        assertEquals("HTTP 500: Internal Server Error", exception.message)
    }

    // --- Network Exception ---

    @Test
    fun `getWalkthroughList returns Failed when network throws exception`() = runTest {
        // Arrange
        val networkException = IOException("No internet connection")
        coEvery { onBoardingApi.getWalkthroughIllusts() } returns
                NetworkResult.Exception(networkException)

        // Act
        val result = repository.getWalkthroughList()

        // Assert
        assertTrue(result is CResponse.Failed)
        val exception = (result as CResponse.Failed).exception
        assertEquals(networkException, exception)
    }
}
