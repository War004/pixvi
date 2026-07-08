package com.cryptic.piyek.core.auth.data.local.token

import kotlinx.coroutines.sync.Mutex

interface TokenStorage {
    /**
     * Synchronously retrieves the current access token.
     * Crucial for the OkHttp Interceptor because it runs on standard,
     * synchronous threads and cannot call suspending functions.
     */
    fun getAccessToken(): String?

    /**
     * Synchronously updates the access token in memory.
     * There would be 4 ways by which the token can be set
     * - When the user opens the app
     * - When the user switches the account
     * - When the user login from the onboarding screen
     * - When the existing access tokens gives an 401 or 403 error
     */
    fun setAccessToken(token: String?)

    /**
     * Clears the access token from memory.
     */
    fun clearToken()

    /**
     * A Mutex instance used by repositories or use cases to serialize
     * write or refresh operations in asynchronous coroutine contexts.
     */
    val writeMutex: Mutex
}