package com.cryptic.pixvi.core.network.repo

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cryptic.pixvi.auth.data.AuthTokenManager
import com.cryptic.pixvi.auth.data.GrantTypes
import com.cryptic.pixvi.auth.data.TokenStorage
import com.cryptic.pixvi.core.config.PixivConfigs
import com.cryptic.pixvi.core.model.OAuthInfo
import com.cryptic.pixvi.core.network.api.AuthApiService
import com.cryptic.pixvi.core.network.model.toOAuthInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class OauthRepo(
    private val api: AuthApiService,
    private val tokenShop: TokenStorage,
    private val dataStore: DataStore<Preferences>,
    private val authTokenManager: AuthTokenManager
){

    private val HASHED_REFRESH_TOKEN = stringPreferencesKey("hashed_refresh_token")
    
    //using withContext cause only retrofit run on background safetly

    suspend fun login(
        codeVerifier: String,
        code: String,
        grantType: String = GrantTypes.AUTH_CODE.value,
        redirectUrl: String = PixivConfigs.REDIRECT_URL_OAUTH,
        clientId: String = PixivConfigs.CLIENT_ID,
        clientSecret: String = PixivConfigs.CLIENT_SECRET,
        includePolicy: Boolean = false
    ): OAuthInfo? = withContext(Dispatchers.IO) {
        try{
            val response = api.getToken(
                codeVerifier = codeVerifier,
                code = code,
                grantType = grantType,
                redirectUrl = redirectUrl,
                clientId = clientId,
                clientSecret = clientSecret,
                includePolicy = includePolicy
            )

            Log.d("Login","accessToken = ${response.accessToken}")
            Log.d("Login","expriesIn = ${response.expiresIn}")
            Log.d("Login","tokenType = ${response.tokenType}")
            Log.d("Login","scope = ${response.scope}")
            Log.d("Login","refreshToken = ${response.refreshToken}")

            val previousHashedToken = dataStore.data
                .map { preferences -> preferences[HASHED_REFRESH_TOKEN] }
                .first()

            if(previousHashedToken != null){
                Log.d("Login(Oauth)","Only one session is allowed")
                return@withContext null
            }

            //generate the hashed refresh token and save using tint
            val hashedRefreshToken = tokenShop.saveToken(response.refreshToken)


            //save the hashed token
            dataStore.edit { preferences ->
                preferences[HASHED_REFRESH_TOKEN] = hashedRefreshToken
            }
            authTokenManager.saveToken(
                token = response.accessToken,
                validityTill = response.expiresIn
            )
            //emedded the access token
            //to emedded the access token we can save it in datastore
            //save the access token

            return@withContext response.toOAuthInfo()
        }
        catch (e: Exception){
            Log.e("OAuth","Something happened, ${e.message}")
            return@withContext null
        }
    }

    suspend fun updateToken(
        clientId: String = PixivConfigs.CLIENT_ID,
        clientSecret: String = PixivConfigs.CLIENT_SECRET,
        grantType: String = GrantTypes.REFRESH_CODE.value,
        includePolicy: Boolean = false
    ): Boolean = withContext(Dispatchers.IO){

        //get the hashed refresh token
        val hashedRefreshToken = dataStore.data
            .map { preferences -> preferences[HASHED_REFRESH_TOKEN] }
            .first()

        if (hashedRefreshToken != null) {
            val refreshToken = tokenShop.getToken(hashedRefreshToken)

            try{
                val response = api.updateToken(
                    clientId = clientId,
                    clientSecret = clientSecret,
                    grantType = grantType,
                    refreshToken = refreshToken,
                    includePolicy = includePolicy
                )

                authTokenManager.saveToken(
                    token = response.accessToken,
                    validityTill = response.expiresIn
                )
                return@withContext true

            }catch (e: Exception){
                Log.e("OAuth","Something happened, ${e.message}")
                return@withContext false
            }
        } else {
            return@withContext false
            // Handle the case where the user is logged out (e.g., return to Login)
        }
    }
}