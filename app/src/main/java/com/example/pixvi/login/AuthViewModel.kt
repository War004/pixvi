package com.example.pixvi.login

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pixvi.utils.PixivAuthUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * ViewModel class responsible for handling authentication logic for the Pixiv API.
 * Manages the login flow, token exchange, secure storage of tokens via AccountManager, and logout.
 * Uses OAuth 2.0 PKCE flow.
 */
class AuthViewModel : ViewModel(), TokenProvider {
    private val TAG = "AuthViewModel"
    private val oauthService = OAuthService()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    // PKCE values remain in memory while ViewModel is active, but persisted for flow continuity

    private var applicationContext: Context? = null
    private var accountHelper: PixivAccountHelper? = null

    private val TOKEN_REFRESH_THRESHOLD = 10 * 60 * 1000 // 10 minutes

    private val _launchLoginUrlEvent = MutableSharedFlow<Uri>()
    val launchLoginUrlEvent: SharedFlow<Uri> = _launchLoginUrlEvent.asSharedFlow()

    // --- Constants for Standard SharedPreferences ---
    private val PKCE_PREFS_NAME = "pixiv_pkce_prefs" // Use a different name than old encrypted prefs
    private val KEY_CODE_VERIFIER = "code_verifier"
    private val KEY_STATE = "state"
    // --- End Constants ---

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
        accountHelper = PixivAccountHelper(context.applicationContext)
        checkExistingLogin()
    }

    /**
     * Check if user is already logged in using Android AccountManager.
     * Refreshes token proactively if needed.
     */
    private fun checkExistingLogin() {
        val helper = accountHelper ?: return
        Log.d(TAG, "Checking login status via AccountManager...")

        try {
            if (helper.isLoggedIn()) {
                val account = helper.getAccount() // Get account once
                val username = account?.name
                val accessToken = helper.getAccessToken() // Uses peekAuthToken
                val refreshToken = helper.getRefreshToken()
                val tokenExpiry = helper.getTokenExpiry()

                if (username != null && accessToken != null && refreshToken != null) {
                    Log.d(TAG, "Account found for $username. Expiry: $tokenExpiry, Current: ${System.currentTimeMillis()}")
                    when {
                        tokenExpiry <= System.currentTimeMillis() -> {
                            Log.d(TAG, "Token expired. Refreshing...")
                            AuthManager.updateToken(null) // Token expired
                            refreshAccessToken(refreshToken) // Refresh needed
                        }
                        tokenExpiry - System.currentTimeMillis() <= TOKEN_REFRESH_THRESHOLD -> {
                            Log.d(TAG, "Token expiring soon. Refreshing proactively...")
                            AuthManager.updateToken(accessToken) // Still valid for now
                            refreshAccessToken(refreshToken) // Refresh proactively
                        }
                        else -> {
                            Log.d(TAG, "Token is valid.")
                            AuthManager.updateToken(accessToken) // Token is valid
                            _loginState.value = LoginState.Success(
                                username = username,
                                accessToken = accessToken,
                                tokenExpiry = tokenExpiry
                            )
                        }
                    }
                } else {
                    // Account exists but data is incomplete
                    AuthManager.updateToken(null)
                    Log.w(TAG, "Incomplete account data found in AccountManager for ${username ?: "unknown account"}. Clearing state.")
                    _loginState.value = LoginState.Idle

                }
            } else {
                // No account found
                Log.d(TAG, "No account found in AccountManager.")
                AuthManager.updateToken(null)
                _loginState.value = LoginState.Idle
            }
        } catch (e: Exception) {
            // Error accessing AccountManager
            AuthManager.updateToken(null)
            Log.e(TAG, "Error checking AccountManager login status: ${e.message}", e)
            _loginState.value = LoginState.Error("Failed to check login status") // Or Idle
        }
    }

    /**
     * Refreshes the access token using the stored refresh token from AccountManager.
     * Updates the Android account system and the login state.
     */
    private fun refreshAccessToken(storedRefreshToken: String) {
        val currentContext = applicationContext ?: run {
            Log.e(TAG, "Context is null, cannot refresh token.")
            AuthManager.updateToken(null)
            return
        }

        viewModelScope.launch {
            Log.d(TAG, "Attempting to refresh access token...")
            if (_loginState.value !is LoginState.Loading) {
                _loginState.value = LoginState.Loading
            }

            try {
                val currentTime = PixivAuthUtils.getCurrentTimeFormatted()
                val clientHash = PixivAuthUtils.generateClientHash(currentTime)
                val idpUrlsResponse = oauthService.getIdpUrls(currentTime, clientHash)
                val tokenResponse = oauthService.refreshToken(
                    idpUrlsResponse.`auth-token`,
                    storedRefreshToken,
                    currentTime,
                    clientHash
                )

                AuthManager.updateToken(tokenResponse.access_token)
                val tokenExpiry = System.currentTimeMillis() + (tokenResponse.expires_in * 1000)

                // Update AccountManager ONLY
                accountHelper?.updateAccessToken(
                    accessToken = tokenResponse.access_token,
                    tokenExpiry = tokenExpiry
                    // Optionally update refresh token if API returns a new one
                    // val newRefreshToken = tokenResponse.refresh_token ?: storedRefreshToken
                    // accountHelper?.updateRefreshToken(newRefreshToken) // Requires adding this method to Helper
                )

                _loginState.value = LoginState.Success(
                    username = tokenResponse.user.name,
                    accessToken = tokenResponse.access_token,
                    tokenExpiry = tokenExpiry
                )
                Log.d(TAG, "Access token refreshed successfully via refreshAccessToken for user: ${tokenResponse.user.name}")

            } catch (e: Exception) {
                AuthManager.updateToken(null)
                Log.e(TAG, "Error refreshing access token: ${e.message}", e)
                _loginState.value = LoginState.Error("Session expired. Please log in again.")
                // Consider calling logout() to ensure cleanup
                logout()
            }
        }
    }

    /**
     * Implements [TokenProvider.getRefreshedAccessToken]
     * Called by interceptor. Refreshes token using AccountManager data.
     */
    override suspend fun getRefreshedAccessToken(): String? {
        Log.d(TAG, "getRefreshedAccessToken called by interceptor.")
        val currentContext = applicationContext ?: run {
            Log.e(TAG, "ApplicationContext is null in getRefreshedAccessToken.")
            AuthManager.updateToken(null)
            return null
        }
        val helper = accountHelper ?: run {
            Log.e(TAG, "PixivAccountHelper is null in getRefreshedAccessToken.")
            AuthManager.updateToken(null)
            return null
        }

        val storedRefreshToken = helper.getRefreshToken()
        if (storedRefreshToken.isNullOrEmpty()) {
            Log.e(TAG, "No refresh token found in AccountManager for interceptor refresh.")
            AuthManager.updateToken(null)
            viewModelScope.launch { // Use viewModelScope for UI state changes if any
                _loginState.value = LoginState.Error("Session expired. Please log in again.")
            }
            return null
        }

        try {
            Log.i(TAG, "Attempting to refresh token via interceptor for user: ${helper.getAccount()?.name}")
            val currentTime = PixivAuthUtils.getCurrentTimeFormatted()
            val clientHash = PixivAuthUtils.generateClientHash(currentTime)

            val idpUrlsResponse = oauthService.getIdpUrls(currentTime, clientHash)
            val tokenResponse = oauthService.refreshToken(
                idpUrlsResponse.`auth-token`,
                storedRefreshToken,
                currentTime,
                clientHash
            )

            AuthManager.updateToken(tokenResponse.access_token)
            val tokenExpiry = System.currentTimeMillis() + (tokenResponse.expires_in * 1000)

            helper.updateAccessToken(
                accessToken = tokenResponse.access_token,
                tokenExpiry = tokenExpiry
            )


            Log.i(TAG, "Token refreshed successfully via interceptor for user: ${tokenResponse.user.name}")
            _loginState.value = LoginState.Success(
                username = tokenResponse.user.name,
                accessToken = tokenResponse.access_token,
                tokenExpiry = tokenExpiry
            )
            return tokenResponse.access_token
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing access token via interceptor: ${e.message}", e)
            AuthManager.updateToken(null)
            viewModelScope.launch {
                _loginState.value = LoginState.Error("Session refresh failed. Please log in again.")
            }
            helper.removeAccount()
            return null
        }
    }

    /**
     * Starts the OAuth 2.0 PKCE login flow.
     */
    fun startLoginFlow() {
        val context = applicationContext ?: run {
            Log.e(TAG, "Context is null, cannot start login flow.")
            _loginState.value = LoginState.Error("Internal application error.")
            return
        }

        viewModelScope.launch {
            try {
                _loginState.value = LoginState.Loading

                val localCodeVerifier = generateCodeVerifier() // Generate fresh values
                val codeChallenge = generateCodeChallenge(localCodeVerifier)
                val localState = generateRandomState()

                Log.d(TAG, "Generated State: $localState")
                // Save PKCE values to Standard SharedPreferences
                saveCodeVerifierToPrefs(context, localCodeVerifier)
                saveStateToPrefs(context, localState)

                val loginUrl = "https://app-api.pixiv.net/web/v1/login?code_challenge=$codeChallenge&code_challenge_method=S256&client=pixiv-android&state=$localState"
                val loginUri = loginUrl.toUri()
                Log.d(TAG, "Login url: $loginUrl")

                _launchLoginUrlEvent.emit(loginUri)
                _loginState.value = LoginState.Initiated // Indicate flow has started

            } catch (e: Exception) {
                Log.e(TAG, "Error starting login flow: ${e.message}", e)
                _loginState.value = LoginState.Error(e.message ?: "Unknown error starting login")
            }
        }
    }

    /**
     * Handles the OAuth 2.0 redirect URI. Exchanges code for tokens, saves to AccountManager.
     */
    fun handleAuthRedirect(uri: Uri?, context: Context) {
        Log.d(TAG, "handleAuthRedirect called with URI: $uri")
        if (uri == null) {
            Log.e(TAG, "Invalid redirect URI: null")
            AuthManager.updateToken(null)
            _loginState.value = LoginState.Error("Invalid redirect URI")
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginState.Loading

            try {
                val code = uri.getQueryParameter("code")
                val receivedState = uri.getQueryParameter("state")
                Log.d(TAG, "Extracted code: $code, Extracted state: $receivedState")

                // Retrieve and verify state from Standard SharedPreferences
                val savedState = getStoredStateFromPrefs(context)
                Log.d(TAG, "Saved state from Prefs: $savedState")

                // State verification (CSRF protection)
                //It doesn't work......
                /*if (receivedState == null || receivedState != savedState) {
                    Log.e(TAG, "State verification failed: Received[$receivedState] != Saved[$savedState]")
                    AuthManager.updateToken(null)
                    _loginState.value = LoginState.Error("State verification failed")
                    // Clear potentially misused state from prefs
                    clearPkcePrefs(context)
                    return@launch
                }*/

                if (code.isNullOrEmpty()) {
                    Log.e(TAG, "No authorization code received")
                    AuthManager.updateToken(null)
                    _loginState.value = LoginState.Error("No authorization code received")
                    clearPkcePrefs(context) // Clear PKCE data as flow failed
                    return@launch
                }

                // Retrieve code verifier from Standard SharedPreferences
                val storedCodeVerifier = getStoredCodeVerifierFromPrefs(context)
                if (storedCodeVerifier.isEmpty()) {
                    Log.e(TAG, "Stored code verifier missing from Prefs")
                    AuthManager.updateToken(null)
                    _loginState.value = LoginState.Error("Internal error: Code verifier missing")
                    return@launch
                }

                val currentTime = PixivAuthUtils.getCurrentTimeFormatted()
                val clientHash = PixivAuthUtils.generateClientHash(currentTime)
                val idpUrlsResponse = oauthService.getIdpUrls(currentTime, clientHash)

                // Exchange code for tokens
                val tokenResponse = oauthService.exchangeToken(
                    idpUrlsResponse.`auth-token`,
                    code,
                    storedCodeVerifier,
                    currentTime,
                    clientHash
                )

                AuthManager.updateToken(tokenResponse.access_token)
                val tokenExpiry = System.currentTimeMillis() + (tokenResponse.expires_in * 1000)

                // Add account to AccountManager ONLY
                accountHelper?.addAccount(
                    username = tokenResponse.user.name,
                    userId = tokenResponse.user.id,
                    accessToken = tokenResponse.access_token,
                    refreshToken = tokenResponse.refresh_token,
                    tokenExpiry = tokenExpiry
                )

                // REMOVE: Call to saveTokens(context, tokenResponse)

                _loginState.value = LoginState.Success(
                    username = tokenResponse.user.name,
                    accessToken = tokenResponse.access_token,
                    tokenExpiry = tokenExpiry
                )
                Log.d(TAG, "Auth redirect handled successfully. User: ${tokenResponse.user.name}")

            } catch (e: Exception) {
                AuthManager.updateToken(null)
                Log.e(TAG, "Error in auth redirect: ${e.message}", e)
                _loginState.value = LoginState.Error(e.message ?: "Unknown error during token exchange")
            } finally {
                // Clean up PKCE data from prefs after use (success or failure)
                clearPkcePrefs(context)
            }
        }
    }

    /**
     * Logs out the user by removing the account from AccountManager and clearing AuthManager.
     */
    fun logout() {
        Log.d(TAG, "Logging out user: ${accountHelper?.getAccount()?.name}")
        // Remove account from Android account system
        val removed = accountHelper?.removeAccount()
        Log.d(TAG, "Account removal result: $removed")

        // Clear in-memory token
        AuthManager.updateToken(null)

        // Update state
        _loginState.value = LoginState.Idle

        // Optionally clear PKCE prefs as well, although they should be cleared after flow completion
        applicationContext?.let { clearPkcePrefs(it) }
    }

    // --- PKCE utility functions (generateCodeVerifier, generateCodeChallenge, generateRandomState) remain the same ---
    private fun generateCodeVerifier(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun generateCodeChallenge(verifier: String): String {
        val bytes = verifier.toByteArray()
        val messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(bytes)
        val digest = messageDigest.digest()
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun generateRandomState(): String {
        val random = SecureRandom()
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
    // --- End PKCE utilities ---


    // --- Standard SharedPreferences for PKCE Data ---

    /**
     * Gets an instance of standard SharedPreferences for PKCE data.
     */
    private fun getPkcePreferences(context: Context): SharedPreferences {
        // Use standard SharedPreferences in private mode
        return context.getSharedPreferences(PKCE_PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Saves the code verifier to standard SharedPreferences.
     */
    private fun saveCodeVerifierToPrefs(context: Context, codeVerifier: String) {
        val prefs = getPkcePreferences(context)
        prefs.edit { putString(KEY_CODE_VERIFIER, codeVerifier) }
        Log.d(TAG, "Saved code verifier to standard prefs.")
    }

    /**
     * Retrieves the stored code verifier from standard SharedPreferences.
     */
    private fun getStoredCodeVerifierFromPrefs(context: Context): String {
        val prefs = getPkcePreferences(context)
        return prefs.getString(KEY_CODE_VERIFIER, "") ?: ""
    }

    /**
     * Saves the state value to standard SharedPreferences.
     */
    private fun saveStateToPrefs(context: Context, state: String) {
        val prefs = getPkcePreferences(context)
        prefs.edit { putString(KEY_STATE, state) }
        Log.d(TAG, "Saved state to standard prefs.")
    }

    /**
     * Retrieves the stored state value from standard SharedPreferences.
     */
    private fun getStoredStateFromPrefs(context: Context): String {
        val prefs = getPkcePreferences(context)
        return prefs.getString(KEY_STATE, "") ?: ""
    }

    /**
     * Clears the stored PKCE data (code verifier and state) from SharedPreferences.
     */
    private fun clearPkcePrefs(context: Context) {
        val prefs = getPkcePreferences(context)
        prefs.edit {
            remove(KEY_CODE_VERIFIER)
            remove(KEY_STATE)
        }
        Log.d(TAG, "Cleared PKCE data from standard prefs.")
    }
}