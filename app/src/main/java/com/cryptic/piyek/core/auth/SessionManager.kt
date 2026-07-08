package com.cryptic.piyek.core.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.cryptic.piyek.AppAuthState
import com.cryptic.piyek.AppConfig
import com.cryptic.piyek.core.auth.domain.repository.OAuthUserRepository
import com.cryptic.piyek.core.database.ACTIVE_USER_ACCOUNT
import com.cryptic.piyek.core.tink.TinkMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.io.IOException

class SessionManager(
    private val dataStore: DataStore<Preferences>,
    private val oAuthUserRepository: OAuthUserRepository,
    private val tinkMethod: TinkMethod,
    private val appConfig: AppConfig,
    applicationScope: CoroutineScope
) {
    // Expose a stream for one-shot UI messages (Toasts / Error notifications)
    private val _events = Channel<String>(capacity = Channel.BUFFERED)
    val events: Flow<String> = _events.receiveAsFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val authState: StateFlow<AppAuthState> = dataStore.data
        .map { preferences -> preferences[ACTIVE_USER_ACCOUNT] }
        .distinctUntilChanged()
        .mapLatest { id ->
            if (id != null) {
                try {
                    val account = oAuthUserRepository.getUser(id)
                    if (account == null) {
                        AppAuthState.Unauthenticated
                    } else {
                        val refreshToken = tinkMethod.getToken(account.encryptedRefreshToken)

                        val accessTokenResult = oAuthUserRepository.exchangeCodeForToken(
                            grantType = "refresh_token",
                            clientId = appConfig.getClientId(),
                            clientSecret = appConfig.getClientSecret(),
                            includePolicy = false,
                            refreshToken = refreshToken
                        )

                        accessTokenResult.onFailure { exception ->
                            val errorMessage = if (exception is IOException) {
                                "No internet connection. Operating offline."
                            } else {
                                "Session refresh failed: ${exception.localizedMessage ?: "Unknown Error"}"
                            }
                            // Emit the message to the UI stream instead of showing a Toast directly
                            _events.send(errorMessage)
                        }

                        AppAuthState.Authenticated(id)
                    }
                } catch (e: Exception) {
                    _events.send("Authentication failed: ${e.localizedMessage}")
                    AppAuthState.Unauthenticated
                }
            } else {
                AppAuthState.Unauthenticated
            }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = applicationScope,
            started = SharingStarted.WhileSubscribed(5000), // <-- Stops background execution 5s after UI closes
            initialValue = AppAuthState.Loading
        )
}