package com.cryptic.piyek.core.auth.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.cryptic.piyek.core.database.ACTIVE_USER_ACCOUNT
import com.cryptic.piyek.core.database.CODE_VERIFIER
import com.cryptic.piyek.core.auth.data.local.db.OAuthUserDao
import com.cryptic.piyek.core.auth.data.local.db.OAuthUserEntity
import com.cryptic.piyek.core.auth.data.local.db.toDomainModel
import com.cryptic.piyek.core.auth.data.local.token.TokenStorage
import com.cryptic.piyek.core.auth.data.remote.OAuthApiService
import com.cryptic.piyek.core.auth.data.remote.toDomain
import com.cryptic.piyek.core.network.NetworkResult
import com.cryptic.piyek.core.tink.TinkMethod
import com.cryptic.piyek.core.auth.domain.model.OauthUser
import com.cryptic.piyek.core.auth.domain.repository.OAuthUserRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.withLock

class OAuthUserRepositoryImpl(
    private val dao: OAuthUserDao,
    private val dataStore: DataStore<Preferences>,
    private val oAuthApiService: OAuthApiService,
    private val tinkMethod: TinkMethod,
    private val tokenStorage: TokenStorage
) : OAuthUserRepository {

    override suspend fun saveUser(user: OauthUser, encryptedRefreshToken: String) {
        val entity = OAuthUserEntity(
            id = user.id,
            name = user.name,
            account = user.account,
            mailAddress = user.mailAddress,
            bestProfilePicUrl = user.bestProfilePicUrl,
            encryptedRefreshToken = encryptedRefreshToken
        )
        dao.insertUser(entity)
    }

    override suspend fun getUser(id: Long): OauthUser? {
        return dao.getUserById(id)?.toDomainModel()
    }

    override suspend fun updateEmail(id: Long, newEmail: String) {
        dao.updateEmail(id, newEmail)
    }

    override suspend fun updateName(id: Long, newName: String) {
        dao.updateName(id, newName)
    }

    override suspend fun updateProfilePicture(id: Long, newPicUrl: String) {
        dao.updateProfilePic(id, newPicUrl)
    }

    override suspend fun deleteUser(id: Long) {
        dao.deleteUserById(id)
    }

    override suspend fun saveActiveUser(id:Long){
        dataStore.edit { prefs ->
            prefs[ACTIVE_USER_ACCOUNT] = id
        }
    }

    override suspend fun saveCodeVerifier(verifier: String?) {
        dataStore.edit { prefs ->
            if (verifier != null) {
                prefs[CODE_VERIFIER] = verifier
            } else {
                prefs.remove(CODE_VERIFIER)
            }
        }
    }

    override suspend fun getRefreshTokenForActiveAcc(): Result<String> {
        val preferences = dataStore.data.first()
        val activeAccount= preferences[ACTIVE_USER_ACCOUNT]

        if (activeAccount!=null){
            val result = getUser(activeAccount)
            if(result!=null){
                try{
                    val refreshToken = tinkMethod.getToken(result.encryptedRefreshToken)
                    return Result.success(refreshToken)
                } catch (e: Exception){
                    return Result.failure(e)
                }
            }else {
                return Result.failure(Exception("Cannot find the active account information from the db"))
            }
        }else {
            return Result.failure(Exception("No active account found."))
        }
    }

    override suspend fun getToken(
        codeVerifier: String,
        code: String,
        grantType: String,
        redirectUrl: String,
        clientId: String,
        clientSecret: String,
        includePolicy: Boolean
    ): Result<OauthUser> {

        val response = oAuthApiService.getToken(
            codeVerifier = codeVerifier,
            code = code,
            grantType = grantType,
            redirectUrl = redirectUrl,
            clientId = clientId,
            clientSecret = clientSecret,
            includePolicy = includePolicy
        )

        return when(response) {
            is NetworkResult.Success -> {
                try {
                    val encryptedRefreshToken = tinkMethod.saveToken(response.data.response.refreshToken)
                    Result.success(response.data.toDomain(encryptedRefreshToken))
                } catch (e: Exception) {
                    Result.failure(Exception("Secure storage encryption failed: ${e.message}", e))
                }
            }
            is NetworkResult.Error -> {
                Result.failure(Exception("API Error ${response.code}: ${response.message}"))
            }
            is NetworkResult.Exception -> {
                Result.failure(response.e)
            }
        }
    }

    override suspend fun exchangeCodeForToken(
        grantType: String,
        clientId: String,
        clientSecret: String,
        includePolicy: Boolean,
        refreshToken: String,
    ): Result<Unit> {

        val response = oAuthApiService.updateToken(
            clientId = clientId,
            clientSecret = clientSecret,
            grantType = grantType,
            refreshToken = refreshToken,
            includePolicy = includePolicy
        )

        return when(response) {
            is NetworkResult.Success -> {
                try {
                    tokenStorage.setAccessToken(response.data.response.accessToken)
                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(Exception("Secure storage encryption failed: ${e.message}", e))
                }
            }
            is NetworkResult.Error -> {
                Result.failure(Exception("API Error ${response.code}: ${response.message}"))
            }
            is NetworkResult.Exception -> {
                Result.failure(response.e)
            }
        }
    }

    override suspend fun setAccessTokenInMemory(): Result<Unit> {
        TODO("Not yet implemented")
    }
    /*
    override suspend fun setAccessTokenInMemory(): Result<Unit> {
        val refreshToken = getRefreshTokenForActiveAcc()

        if(refreshToken.isSuccess){
            val decryptedRefreshToken = refreshToken.getOrNull()
                ?: return Result.failure(Exception("Cannot find refresh token"))
        }
    }*/
}