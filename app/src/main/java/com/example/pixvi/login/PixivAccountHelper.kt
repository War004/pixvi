package com.example.pixvi.login


import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.example.pixvi.login.PixivAuthenticator.Companion.KEY_REFRESH_TOKEN
import com.example.pixvi.login.PixivAuthenticator.Companion.KEY_TOKEN_EXPIRY
import com.example.pixvi.login.PixivAuthenticator.Companion.KEY_USER_ID

/**
 * Helper class for managing Pixiv accounts in the Android account system.
 * Provides methods to add, retrieve, update, and remove accounts.
 */
class PixivAccountHelper(private val context: Context) {
    private val TAG = "PixivAccountHelper"
    private val accountManager: AccountManager = AccountManager.get(context)

    // Account type must match the one defined in authenticator.xml
    private val accountType = "com.example.pixivapitesttwo.account"

    /**
     * Adds a Pixiv account to the Android account system.
     * If an account with the same name already exists, it will be updated.
     *
     * @param username The username/display name for the account
     * @param userId The Pixiv user ID
     * @param accessToken The OAuth access token
     * @param refreshToken The OAuth refresh token
     * @param tokenExpiry The expiration timestamp for the access token
     * @return True if the account was added or updated successfully, false otherwise
     */
    fun addAccount(
        username: String,
        userId: String,
        accessToken: String,
        refreshToken: String,
        tokenExpiry: Long
    ): Boolean {
        Log.d(TAG, "Adding account for user: $username")

        try {
            // Check if account already exists
            val existingAccount = getAccount()
            if (existingAccount != null) {
                // Update existing account
                Log.d(TAG, "Account already exists, updating token")
                accountManager.setAuthToken(existingAccount, "full_access", accessToken)
                accountManager.setUserData(existingAccount, KEY_REFRESH_TOKEN, refreshToken)
                accountManager.setUserData(existingAccount, KEY_TOKEN_EXPIRY, tokenExpiry.toString())
                accountManager.setUserData(existingAccount, KEY_USER_ID, userId)
                return true
            }

            // Create new account
            val account = Account(username, accountType)

            val userData = Bundle().apply {
                putString(KEY_USER_ID, userId)
                putString(KEY_REFRESH_TOKEN, refreshToken)
                putString(KEY_TOKEN_EXPIRY, tokenExpiry.toString())
            }

            // Add the account
            val addAccountResult = accountManager.addAccountExplicitly(account, null, userData)
            if (addAccountResult) {
                // Set the auth token
                accountManager.setAuthToken(account, "full_access", accessToken)
                Log.d(TAG, "Account added successfully")
                return true
            }

            Log.e(TAG, "Failed to add account")
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error adding account", e)
            return false
        }
    }

    /**
     * Gets the Pixiv account if it exists in the system.
     *
     * @return The Account object or null if no account exists
     */
    fun getAccount(): Account? {
        val accounts = accountManager.getAccountsByType(accountType)
        return accounts.firstOrNull()
    }

    /**
     * Gets the current access token for the Pixiv account.
     *
     * @return The access token or null if no account exists or token is not set
     */
    fun getAccessToken(): String? {
        val account = getAccount() ?: return null
        return accountManager.peekAuthToken(account, "full_access")
    }

    /**
     * Gets the refresh token for the Pixiv account.
     *
     * @return The refresh token or null if no account exists
     */
    fun getRefreshToken(): String? {
        val account = getAccount() ?: return null
        return accountManager.getUserData(account, KEY_REFRESH_TOKEN)
    }

    /**
     * Gets the token expiry time for the Pixiv account.
     *
     * @return The token expiry timestamp or 0 if no account exists or value not set
     */
    fun getTokenExpiry(): Long {
        val account = getAccount() ?: return 0
        val expiryStr = accountManager.getUserData(account, KEY_TOKEN_EXPIRY)
        return expiryStr?.toLongOrNull() ?: 0
    }

    /**
     * Gets the user ID for the Pixiv account.
     *
     * @return The user ID or null if no account exists
     */
    fun getUserId(): String? {
        val account = getAccount() ?: return null
        return accountManager.getUserData(account, KEY_USER_ID)
    }

    /**
     * Removes the Pixiv account from the system.
     *
     * @return True if the account was removed successfully, false otherwise
     */
    fun removeAccount(): Boolean {
        val account = getAccount() ?: return false

        try {
            // Use the non-deprecated method for newer Android versions
            val future = accountManager.removeAccount(account, null, null, null)
            return future.result.getBoolean(AccountManager.KEY_BOOLEAN_RESULT)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing account", e)
            return false
        }
    }

    /**
     * Checks if the user is logged in (has a valid account).
     *
     * @return True if the user is logged in, false otherwise
     */
    fun isLoggedIn(): Boolean {
        return getAccount() != null
    }

    /**
     * Updates the access token for the account.
     *
     * @param accessToken The new access token
     * @param tokenExpiry The new expiry time
     * @return True if the update was successful, false otherwise
     */
    fun updateAccessToken(accessToken: String, tokenExpiry: Long): Boolean {
        val account = getAccount() ?: return false

        try {
            accountManager.setAuthToken(account, "full_access", accessToken)
            accountManager.setUserData(account, KEY_TOKEN_EXPIRY, tokenExpiry.toString())
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating access token", e)
            return false
        }
    }
}