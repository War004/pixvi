package com.cryptic.piyek.feature.onboarding.domain.usecase

import android.content.Intent
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.cryptic.piyek.core.database.ACTIVE_USER_ACCOUNT
import com.cryptic.piyek.AppConfig
import com.cryptic.piyek.core.auth.data.local.token.TokenManager
import com.cryptic.piyek.core.auth.data.local.token.TokenStorage
import com.cryptic.piyek.core.database.CODE_VERIFIER
import com.cryptic.piyek.core.auth.data.repository.OAuthUserRepositoryImpl
import com.cryptic.piyek.core.auth.domain.repository.OAuthUserRepository
import kotlinx.coroutines.flow.first
import java.io.IOException

class OAuthIntentProcessor(
    private val repository: OAuthUserRepository,
    private val dataStore: DataStore<Preferences>,
    private val appConfig: AppConfig,
    private val tokenManager: TokenStorage
) {
    private fun processIntentData(intent: Intent): Result<String> {
        val data = intent.data

        if (data != null && data.toString().startsWith("pixiv://account")) {
            val code = data.getQueryParameter("code")

            if (code.isNullOrBlank()) {
                val error = data.getQueryParameter("error") ?: "No Error Message Found"
                // Return failure containing an Exception with your message
                return Result.failure(Exception(error))
            } else {
                // Return the successful code
                return Result.success(code)
            }
        } else {
            Log.e("Redirect", "Invalid URI: ${intent.data}")
            return Result.failure(Exception("Intent url was null or endpoints changed"))
        }
    }

    suspend fun onNewIntent(intent: Intent): Result<Unit> {
        // We wrap the entire process in a single try-catch
        return try {
            val result = processIntentData(intent)

            if (result.isFailure) {
                return Result.failure(result.exceptionOrNull() ?: Exception("Failed to process intent data."))
            }

            val authCode = result.getOrNull() ?: return Result.failure(Exception("Authentication code was null."))
            val preferences = dataStore.data.first()
            val codeVerifier = preferences[CODE_VERIFIER]

            if (codeVerifier.isNullOrBlank()) {
                return Result.failure(Exception("Login flow was not initiated. Missing code verifier."))
            }

            repository.saveCodeVerifier(null)

            val response = repository.getToken(
                codeVerifier = codeVerifier,
                code = authCode,
                grantType = "authorization_code",
                redirectUrl = "https://app-api.pixiv.net/web/v1/users/auth/pixiv/callback",
                clientId = appConfig.getClientId(),
                clientSecret = appConfig.getClientSecret(),
                includePolicy = false
            )

            if (response.isFailure) {
                return Result.failure(response.exceptionOrNull() ?: Exception("Token exchange failed."))
            }

            val accountData = response.getOrNull() ?: return Result.failure(Exception("Account data was null."))

            repository.saveUser(
                user = accountData,
                encryptedRefreshToken = accountData.encryptedRefreshToken ?: ""
            )
            val activeAccountId = preferences[ACTIVE_USER_ACCOUNT]
            if (activeAccountId == null) {
                repository.saveActiveUser(accountData.id)
                tokenManager.setAccessToken(accountData.accessToken) //when requesting the refresh token, we also get the access token
            }

            Result.success(Unit)

        } catch (e: IOException) {
            Result.failure(Exception("Disk write failed. Please check your storage.", e))
        } catch (e: Exception) {
            Result.failure(Exception("An unexpected error occurred: ${e.localizedMessage}", e))
        }
    }
}