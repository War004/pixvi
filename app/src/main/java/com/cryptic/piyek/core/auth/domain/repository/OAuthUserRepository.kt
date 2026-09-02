package com.cryptic.piyek.core.auth.domain.repository

import com.cryptic.piyek.core.auth.domain.model.OauthUser
import com.cryptic.piyek.core.auth.domain.model.OauthUserPublic
import kotlinx.coroutines.flow.Flow

interface OAuthUserRepository {
    suspend fun saveUser(user: OauthUser, encryptedRefreshToken: String)
    suspend fun getUser(id: Long): OauthUser?
    suspend fun updateEmail(id: Long, newEmail: String)
    fun observeActiveAccountId(): Flow<Long?>
    suspend fun updateName(id: Long, newName: String)
    suspend fun updateProfilePicture(id: Long, newPicUrl: String)
    suspend fun deleteUser(id: Long)
    suspend fun removeActiveAccount()
    suspend fun saveActiveUser(id:Long)
    suspend fun saveCodeVerifier(verifier: String?)
    suspend fun getRefreshTokenForActiveAcc(): Result<String>
    suspend fun getTotalAccountCount(): Int
    suspend fun getFirstAccountId(): Long
    fun getAllAccountVisualInfo(): Flow<List<OauthUserPublic>>
    fun observeActiveAccount(): Flow<OauthUserPublic?>
    suspend fun getToken(
        codeVerifier: String,
        code: String,
        grantType: String,
        redirectUrl: String,
        clientId: String,
        clientSecret: String,
        includePolicy: Boolean
    ): Result<OauthUser>

    suspend fun exchangeCodeForToken(
        grantType: String,
        clientId: String,
        clientSecret: String,
        includePolicy: Boolean,
        refreshToken: String,
    ): Result<Unit>

    suspend fun setAccessTokenInMemory(): Result<Unit>
}