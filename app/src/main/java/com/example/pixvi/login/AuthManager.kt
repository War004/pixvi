package com.example.pixvi.login

import android.util.Log

object AuthManager {
    private const val TAG = "AuthManager"
    private val lock = Any()

    @Volatile // Ensure writes are visible across threads
    private var currentAccessToken: String? = null

    /**
     * Updates the currently stored access token.
     * Should be called by AuthViewModel when the token changes.
     */
    fun updateToken(newToken: String?) {
        synchronized(lock) {
            currentAccessToken = newToken
        }
        Log.d(TAG, "Access token updated. IsPresent: ${newToken != null}")
    }

    /**
     * Gets the currently stored access token synchronously.
     * This is what the HeaderInterceptor will call.
     * Returns null if no token is currently set.
     */
    fun getCurrentAccessToken(): String? {
        synchronized(lock) {
            return currentAccessToken
        }
    }
}