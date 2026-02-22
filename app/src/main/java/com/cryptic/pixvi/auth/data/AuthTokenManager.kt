package com.cryptic.pixvi.auth.data

import android.os.SystemClock
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

class AuthTokenManager(
    private val dataStore: DataStore<Preferences>,
) {

    private val HASHED_REFRESH_TOKEN = stringPreferencesKey("hashed_refresh_token") //active hash token
    private val _accessToken = MutableStateFlow<String?>(null)
    val accessToken = _accessToken.asStateFlow()

    private var expiryElapsedTime: Long = 0L

    private var hasRefreshToken: Boolean = false

    fun saveToken(token: String, validityTill: Int) {
        _accessToken.value = token
        expiryElapsedTime = SystemClock.elapsedRealtime() +
                (validityTill * 1000L) -  60_000L  // 1 min buffer
    }

    fun getToken(): String? = accessToken.value

    fun isTokenExpired(): Boolean {
        return SystemClock.elapsedRealtime() >= expiryElapsedTime
    }

    fun clear() {
        _accessToken.value = null
    }

    suspend fun initialize() {
        hasRefreshToken = dataStore.data.first()[HASHED_REFRESH_TOKEN] != null
    }

    fun hasStoredSession(): Boolean = hasRefreshToken


//    suspend fun getRefreshToken(): String? {
//        return dataStore.data.first()[HASHED_REFRESH_TOKEN]
//    }
//
//    suspend fun saveRefreshToken(hashedToken: String) {
//        dataStore.edit { it[HASHED_REFRESH_TOKEN] = hashedToken }
//        hasRefreshToken = true
//    }

    suspend fun clearAll() {
        dataStore.edit { it.remove(HASHED_REFRESH_TOKEN) }
        hasRefreshToken = false
        _accessToken.value = null
    }
}