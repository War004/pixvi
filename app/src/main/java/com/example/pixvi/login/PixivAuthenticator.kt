package com.example.pixvi.login


import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log

/**
 * Account authenticator for Pixiv integration with Android's account system.
 * This class handles various account operations and delegates to our custom auth flow.
 * It implements the methods required by the Android AccountManager framework to manage
 * Pixiv accounts in the system settings.
 */
class PixivAuthenticator(private val context: Context) : AbstractAccountAuthenticator(context) {
    private val TAG = "PixivAuthenticator"

    /**
     * Called when the user is trying to edit account properties.
     * We don't support direct property editing so we throw an exception.
     */
    override fun editProperties(
        response: AccountAuthenticatorResponse?,
        accountType: String?
    ): Bundle {
        throw UnsupportedOperationException("Account editing not supported")
    }

    /**
     * Called when the system or an app wants to add a new account.
     * We launch the login activity to start the Pixiv OAuth flow.
     */
    override fun addAccount(
        response: AccountAuthenticatorResponse?,
        accountType: String?,
        authTokenType: String?,
        requiredFeatures: Array<out String>?,
        options: Bundle?
    ): Bundle {
        Log.d(TAG, "addAccount called")

        // Create intent to launch our login activity
        val intent = Intent(context, LoginActivity::class.java).apply {
            putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
            // Flag to indicate we're adding a new account (vs. just getting a token)
            putExtra(LOGIN_ACTION_KEY, LOGIN_ACTION_ADD_ACCOUNT)
        }

        // Return the intent that will start our login flow
        val bundle = Bundle()
        bundle.putParcelable(AccountManager.KEY_INTENT, intent)
        return bundle
    }

    /**
     * Called to confirm that account credentials are still valid.
     * We don't support a separate confirmation UI, so we just return success
     * if the account exists.
     */
    override fun confirmCredentials(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        options: Bundle?
    ): Bundle {
        val result = Bundle()
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, account != null)
        return result
    }

    /**
     * Called when the system or an app needs an auth token for an existing account.
     * We either return a cached token or start the flow to get a new one.
     */
    override fun getAuthToken(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        authTokenType: String?,
        options: Bundle?
    ): Bundle {
        Log.d(TAG, "getAuthToken called for account ${account?.name}")
        val am = AccountManager.get(context)

        // Check if we already have a cached token
        var authToken = am.peekAuthToken(account, authTokenType)

        // If no cached token, try to use the refresh token to get a new one
        if (authToken.isNullOrEmpty()) {
            val refreshToken = am.getUserData(account, KEY_REFRESH_TOKEN)
            if (!refreshToken.isNullOrEmpty()) {
                // In a real implementation, you'd perform the token refresh here
                // For now, we'll just delegate to the login activity
                Log.d(TAG, "No cached token, need to refresh")
                val intent = Intent(context, LoginActivity::class.java).apply {
                    putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
                    putExtra(LOGIN_ACTION_KEY, LOGIN_ACTION_REFRESH_TOKEN)
                    putExtra(KEY_ACCOUNT_NAME, account?.name)
                    putExtra(KEY_ACCOUNT_TYPE, account?.type)
                }

                val bundle = Bundle()
                bundle.putParcelable(AccountManager.KEY_INTENT, intent)
                return bundle
            }
        }

        // If we have a valid token, return it
        if (!authToken.isNullOrEmpty()) {
            val result = Bundle()
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account?.name)
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account?.type)
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken)
            return result
        }

        // If we get here, we need user to log in again
        val intent = Intent(context, LoginActivity::class.java).apply {
            putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
            putExtra(KEY_ACCOUNT_NAME, account?.name)
            putExtra(KEY_ACCOUNT_TYPE, account?.type)
        }

        val bundle = Bundle()
        bundle.putParcelable(AccountManager.KEY_INTENT, intent)
        return bundle
    }

    /**
     * Returns a label for the auth token type.
     */
    override fun getAuthTokenLabel(authTokenType: String?): String {
        return "Full Access"
    }

    /**
     * Called when the user wants to update their credentials.
     * We launch the login activity for this.
     */
    override fun updateCredentials(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        authTokenType: String?,
        options: Bundle?
    ): Bundle {
        // We don't support updating credentials directly
        val intent = Intent(context, LoginActivity::class.java).apply {
            putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
            putExtra(LOGIN_ACTION_KEY, LOGIN_ACTION_UPDATE_CREDENTIALS)
            putExtra(KEY_ACCOUNT_NAME, account?.name)
            putExtra(KEY_ACCOUNT_TYPE, account?.type)
        }

        val bundle = Bundle()
        bundle.putParcelable(AccountManager.KEY_INTENT, intent)
        return bundle
    }

    /**
     * Checks if the account has requested features.
     * We don't support any special features.
     */
    override fun hasFeatures(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        features: Array<out String>?
    ): Bundle {
        val result = Bundle()
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false)
        return result
    }

    companion object {
        // Constants for login actions
        const val LOGIN_ACTION_KEY = "login_action"
        const val LOGIN_ACTION_ADD_ACCOUNT = "add_account"
        const val LOGIN_ACTION_UPDATE_CREDENTIALS = "update_credentials"
        const val LOGIN_ACTION_REFRESH_TOKEN = "refresh_token"

        // Keys for account data
        const val KEY_ACCOUNT_NAME = "account_name"
        const val KEY_ACCOUNT_TYPE = "account_type"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_TOKEN_EXPIRY = "token_expiry"
        const val KEY_USER_ID = "user_id"
    }
}