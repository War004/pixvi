package com.example.pixvi.login

/**
 * Interface for providing a mechanism to refresh the access token.
 */
interface TokenProvider {
    /**
     * Attempts to refresh the access token.
     * This method should be called when an API request fails due to an expired/invalid access token.
     *
     * @return The new access token if successful, null otherwise.
     */
    suspend fun getRefreshedAccessToken(): String?
}