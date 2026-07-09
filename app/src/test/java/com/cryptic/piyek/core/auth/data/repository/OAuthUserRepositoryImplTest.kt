package com.cryptic.piyek.core.auth.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import com.cryptic.piyek.core.database.ACTIVE_USER_ACCOUNT
import com.cryptic.piyek.core.database.CODE_VERIFIER
import com.cryptic.piyek.core.auth.data.local.db.OAuthUserDao
import com.cryptic.piyek.core.auth.data.local.db.OAuthUserEntity
import com.cryptic.piyek.core.auth.data.local.db.toDomainModel
import com.cryptic.piyek.core.auth.data.local.token.TokenStorage
import com.cryptic.piyek.core.auth.data.remote.OAuthApiService
import com.cryptic.piyek.core.auth.data.remote.OAuthUser
import com.cryptic.piyek.core.auth.data.remote.OAuthUserResponse
import com.cryptic.piyek.core.auth.data.remote.toDomain
import com.cryptic.piyek.core.network.NetworkResult
import com.cryptic.piyek.core.tink.TinkMethod
import com.cryptic.piyek.core.auth.domain.model.OauthUser
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class OAuthUserRepositoryImplTest {

    private val dao = mockk<OAuthUserDao>(relaxed = true)
    private val dataStore = mockk<DataStore<Preferences>>()
    private val oAuthApiService = mockk<OAuthApiService>()
    private val tinkMethod = mockk<TinkMethod>()
    private val tokenStorage = mockk<TokenStorage>(relaxed = true)

    private lateinit var repository: OAuthUserRepositoryImpl

    private val testUser = OauthUser(
        id = 42L,
        name = "John Doe",
        account = "johndoe",
        mailAddress = "john@example.com",
        bestProfilePicUrl = "https://example.com/pic.png",
        accessToken = null,
        encryptedRefreshToken = "encrypted_refresh_token"
    )

    private val testEntity = OAuthUserEntity(
        id = 42L,
        name = "John Doe",
        account = "johndoe",
        mailAddress = "john@example.com",
        bestProfilePicUrl = "https://example.com/pic.png",
        encryptedRefreshToken = "encrypted_refresh_token"
    )

    @BeforeEach
    fun setUp() {
        // Initialize static mocks for extension mappers
        mockkStatic("com.cryptic.piyek.core.auth.data.local.db.OAuthUserEntityKt")
        mockkStatic("com.cryptic.piyek.core.auth.data.remote.OAuthUserResponseKt")

        repository = OAuthUserRepositoryImpl(dao, dataStore, oAuthApiService, tinkMethod, tokenStorage)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic("com.cryptic.piyek.core.auth.data.local.db.OAuthUserEntityKt")
        unmockkStatic("com.cryptic.piyek.core.auth.data.remote.OAuthUserResponseKt")
    }

    // --- Local DB Tests ---

    @Test
    fun `saveUser correctly maps domain to entity and inserts into DAO`() = runTest {
        // Arrange
        val encryptedToken = "encrypted_ref_token"

        // Act
        repository.saveUser(testUser, encryptedToken)

        // Assert
        coVerify(exactly = 1) {
            dao.insertUser(withArg { entity ->
                assertEquals(testUser.id, entity.id)
                assertEquals(testUser.name, entity.name)
                assertEquals(encryptedToken, entity.encryptedRefreshToken)
            })
        }
    }

    @Test
    fun `getUser returns mapped domain user when entity is found in DB`() = runTest {
        // Arrange
        coEvery { dao.getUserById(42L) } returns testEntity
        every { testEntity.toDomainModel() } returns testUser

        // Act
        val result = repository.getUser(42L)

        // Assert
        assertEquals(testUser, result)
    }

    @Test
    fun `getUser returns null when entity is not found in DB`() = runTest {
        // Arrange
        coEvery { dao.getUserById(42L) } returns null

        // Act
        val result = repository.getUser(42L)

        // Assert
        assertNull(result)
    }

    @Test
    fun `updateEmail invokes DAO method`() = runTest {
        repository.updateEmail(42L, "new@mail.com")
        coVerify(exactly = 1) { dao.updateEmail(42L, "new@mail.com") }
    }

    @Test
    fun `updateName invokes DAO method`() = runTest {
        repository.updateName(42L, "New Name")
        coVerify(exactly = 1) { dao.updateName(42L, "New Name") }
    }

    @Test
    fun `updateProfilePicture invokes DAO method`() = runTest {
        repository.updateProfilePicture(42L, "new_url")
        coVerify(exactly = 1) { dao.updateProfilePic(42L, "new_url") }
    }

    @Test
    fun `deleteUser invokes DAO method`() = runTest {
        repository.deleteUser(42L)
        coVerify(exactly = 1) { dao.deleteUserById(42L) }
    }

    // --- DataStore Read/Write Tests ---

    @Test
    fun `saveActiveUser successfully writes id to DataStore`() = runTest {
        // Arrange: Capture the transform lambda of datastore.updateData
        var capturedPreferences: Preferences? = null
        coEvery { dataStore.updateData(any()) } coAnswers {
            val transform = firstArg<suspend (Preferences) -> Preferences>()
            val result = transform(emptyPreferences())
            capturedPreferences = result
            result
        }

        // Act
        repository.saveActiveUser(100L)

        // Assert
        assertEquals(100L, capturedPreferences?.get(ACTIVE_USER_ACCOUNT))
    }

    @Test
    fun `saveCodeVerifier writes value when verifier is not null`() = runTest {
        // Arrange
        var capturedPreferences: Preferences? = null
        coEvery { dataStore.updateData(any()) } coAnswers {
            val transform = firstArg<suspend (Preferences) -> Preferences>()
            val result = transform(emptyPreferences())
            capturedPreferences = result
            result
        }

        // Act
        repository.saveCodeVerifier("my_verifier")

        // Assert
        assertEquals("my_verifier", capturedPreferences?.get(CODE_VERIFIER))
    }

    @Test
    fun `saveCodeVerifier removes value when verifier is null`() = runTest {
        // Arrange: Start with a preferences state that already has a verifier
        val initialPreferences = preferencesOf(CODE_VERIFIER to "old_verifier")
        var capturedPreferences: Preferences? = null
        coEvery { dataStore.updateData(any()) } coAnswers {
            val transform = firstArg<suspend (Preferences) -> Preferences>()
            val result = transform(initialPreferences)
            capturedPreferences = result
            result
        }

        // Act
        repository.saveCodeVerifier(null)

        // Assert
        assertNull(capturedPreferences?.get(CODE_VERIFIER))
    }

    @Test
    fun `getRefreshTokenForActiveAcc returns decrypted token on success`() = runTest {
        // Arrange
        val activeId = 42L
        val encryptedToken = "encrypted_refresh_token"
        val decryptedToken = "decrypted_ref_token"

        every { dataStore.data } returns flowOf(preferencesOf(ACTIVE_USER_ACCOUNT to activeId))
        coEvery { dao.getUserById(activeId) } returns testEntity
        every { testEntity.toDomainModel() } returns testUser
        coEvery { tinkMethod.getToken(encryptedToken) } returns decryptedToken

        // Act
        val result = repository.getRefreshTokenForActiveAcc()

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(decryptedToken, result.getOrNull())
    }

    @Test
    fun `getRefreshTokenForActiveAcc returns failure when active account key is missing`() = runTest {
        // Arrange
        every { dataStore.data } returns flowOf(emptyPreferences())

        // Act
        val result = repository.getRefreshTokenForActiveAcc()

        // Assert
        assertTrue(result.isFailure)
        assertEquals("No active account found.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `getRefreshTokenForActiveAcc returns failure when user is missing from DB`() = runTest {
        // Arrange
        val activeId = 42L
        every { dataStore.data } returns flowOf(preferencesOf(ACTIVE_USER_ACCOUNT to activeId))
        coEvery { dao.getUserById(activeId) } returns null

        // Act
        val result = repository.getRefreshTokenForActiveAcc()

        // Assert
        assertTrue(result.isFailure)
        assertEquals("Cannot find the active account information from the db", result.exceptionOrNull()?.message)
    }

    @Test
    fun `getRefreshTokenForActiveAcc returns failure when Tink decryption throws`() = runTest {
        // Arrange
        val activeId = 42L
        val tinkException = Exception("Decryption Error")

        every { dataStore.data } returns flowOf(preferencesOf(ACTIVE_USER_ACCOUNT to activeId))
        coEvery { dao.getUserById(activeId) } returns testEntity
        every { testEntity.toDomainModel() } returns testUser
        coEvery { tinkMethod.getToken(any()) } throws tinkException

        // Act
        val result = repository.getRefreshTokenForActiveAcc()

        // Assert
        assertTrue(result.isFailure)
        assertEquals(tinkException, result.exceptionOrNull())
    }

    // --- Remote API Auth Token Tests ---

    @Test
    fun `getToken returns Success containing domain user when NetworkResult is Success`() = runTest {
        // Arrange
        val mockApiResponse = mockk<OAuthUserResponse>()
        val mockResponsePayload = mockk<OAuthUser>()

        // Stub generic properties dynamically
        every { mockApiResponse.response } returns mockResponsePayload
        every { mockResponsePayload.refreshToken } returns "raw_refresh"

        coEvery { oAuthApiService.getToken(any(), any(), any(), any(), any(), any(), any()) } returns
                NetworkResult.Success(mockApiResponse)

        coEvery { tinkMethod.saveToken("raw_refresh") } returns "encrypted_refresh"
        every { mockApiResponse.toDomain("encrypted_refresh") } returns testUser

        // Act
        val result = repository.getToken("v", "c", "g", "r", "cId", "cS", false)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(testUser, result.getOrNull())
    }

    @Test
    fun `getToken returns Failure when API returns error`() = runTest {
        // Arrange
        coEvery { oAuthApiService.getToken(any(), any(), any(), any(), any(), any(), any()) } returns
                NetworkResult.Error(401, "Unauthorized")

        // Act
        val result = repository.getToken("v", "c", "g", "r", "cId", "cS", false)

        // Assert
        assertTrue(result.isFailure)
        assertEquals("API Error 401: Unauthorized", result.exceptionOrNull()?.message)
    }

    @Test
    fun `getToken returns Failure when network client throws exception`() = runTest {
        // Arrange
        val networkException = IOException("No internet connection")
        coEvery { oAuthApiService.getToken(any(), any(), any(), any(), any(), any(), any()) } returns
                NetworkResult.Exception(networkException)

        // Act
        val result = repository.getToken("v", "c", "g", "r", "cId", "cS", false)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(networkException, result.exceptionOrNull())
    }

    @Test
    fun `getToken returns Failure when Tink secure storage throws exception during save`() = runTest {
        // Arrange
        val mockApiResponse = mockk<OAuthUserResponse>()
        val mockResponsePayload = mockk<OAuthUser>()
        val tinkException = RuntimeException("Tink KeyStore Failed")

        every { mockApiResponse.response } returns mockResponsePayload
        every { mockResponsePayload.refreshToken } returns "raw_refresh"

        coEvery { oAuthApiService.getToken(any(), any(), any(), any(), any(), any(), any()) } returns
                NetworkResult.Success(mockApiResponse)
        coEvery { tinkMethod.saveToken("raw_refresh") } throws tinkException

        // Act
        val result = repository.getToken("v", "c", "g", "r", "cId", "cS", false)

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Secure storage encryption failed:") == true)
        assertEquals(tinkException, result.exceptionOrNull()?.cause)
    }

    @Test
    fun `exchangeCodeForToken returns Success when NetworkResult is Success`() = runTest {
        // Arrange
        val mockApiResponse = mockk<OAuthUserResponse>()
        val mockResponsePayload = mockk<OAuthUser>()

        every { mockApiResponse.response } returns mockResponsePayload
        every { mockResponsePayload.accessToken } returns "access_token_123"

        coEvery { oAuthApiService.updateToken(any(), any(), any(), any(), any()) } returns
                NetworkResult.Success(mockApiResponse)

        // Act
        val result = repository.exchangeCodeForToken("g", "cId", "cS", false, "refresh")

        // Assert
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { tokenStorage.setAccessToken("access_token_123") }
    }

    @Test
    fun `exchangeCodeForToken returns Failure when secure storage throws exception during save`() = runTest {
        // Arrange
        val mockApiResponse = mockk<OAuthUserResponse>()
        val mockResponsePayload = mockk<OAuthUser>()
        val storageException = RuntimeException("Keystore is corrupted")

        every { mockApiResponse.response } returns mockResponsePayload
        every { mockResponsePayload.accessToken } returns "access_token_123"

        coEvery { oAuthApiService.updateToken(any(), any(), any(), any(), any()) } returns
                NetworkResult.Success(mockApiResponse)

        // Mock tokenStorage to throw an exception when updating the token
        coEvery { tokenStorage.setAccessToken("access_token_123") } throws storageException

        // Act
        val result = repository.exchangeCodeForToken("g", "cId", "cS", false, "refresh")

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Secure storage encryption failed:") == true)
        assertEquals(storageException, result.exceptionOrNull()?.cause)
    }

    @Test
    fun `exchangeCodeForToken returns Failure when API returns error`() = runTest {
        // Arrange
        coEvery { oAuthApiService.updateToken(any(), any(), any(), any(), any()) } returns
                NetworkResult.Error(400, "Bad Request")

        // Act
        val result = repository.exchangeCodeForToken("g", "cId", "cS", false, "refresh")

        // Assert
        assertTrue(result.isFailure)
        assertEquals("API Error 400: Bad Request", result.exceptionOrNull()?.message)
    }

    @Test
    fun `exchangeCodeForToken returns Failure when network client throws exception`() = runTest {
        // Arrange
        val networkException = IOException("No internet connection")
        coEvery { oAuthApiService.updateToken(any(), any(), any(), any(), any()) } returns
                NetworkResult.Exception(networkException)

        // Act
        val result = repository.exchangeCodeForToken("g", "cId", "cS", false, "refresh")

        // Assert
        assertTrue(result.isFailure)
        assertEquals(networkException, result.exceptionOrNull())
    }
}