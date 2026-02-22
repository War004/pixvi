package com.cryptic.pixvi.auth.account

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.cryptic.pixvi.auth.data.AuthTokenManager
import com.cryptic.pixvi.auth.util.generateCodeChallenge
import com.cryptic.pixvi.auth.util.generateCodeVerifier
import kotlinx.coroutines.runBlocking

class PixviAuthenticator(
    private val context: Context,
    private val authTokenManager: AuthTokenManager
): AbstractAccountAuthenticator(context){

    val customTabsIntent = CustomTabsIntent.Builder().build()

    override fun addAccount(
        p0: AccountAuthenticatorResponse?,
        p1: String?,
        p2: String?,
        p3: Array<out String?>?,
        p4: Bundle?
    ): Bundle? {

        val codeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallenge(codeVerifier)

        val redirectUrl = "https://app-api.pixiv.net/web/v1/login?code_challenge=$codeChallenge&code_challenge_method=S256&client=pixiv-android"
        try {
            customTabsIntent.launchUrl(context, redirectUrl.toUri())
        }catch(e: ActivityNotFoundException){
            Log.e("LoginScreen","Browser with custom tabs not available: ${e.message}")
            try{
                context.startActivity(Intent(Intent.ACTION_VIEW, redirectUrl.toUri()))
            }catch (e2: ActivityNotFoundException) {
                Log.e("LoginScreen", "Fallback browser also not found: ${e2.message}")
            }
        }
        //launch an intent to the browser

        //handle multiple account in the main activity
        return Bundle()
    }

    override fun getAccountRemovalAllowed(
        response: AccountAuthenticatorResponse?,
        account: Account?
    ): Bundle {

        // 1. Use runBlocking to ENSURE data is cleared before we proceed.
        // This is safe because Authenticator runs on a background thread.
        runBlocking {
            try {
                // We use the hash we discussed earlier to find the correct token
                // Assuming your manager has a method to clear specific or all tokens
                authTokenManager.clearAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Now tell the system it's safe to delete the account
        val result = Bundle()
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true)
        response?.onResult(result)
        return result
    }

    override fun confirmCredentials(
        p0: AccountAuthenticatorResponse?,
        p1: Account?,
        p2: Bundle?
    ): Bundle? {
        TODO("Not yet implemented")
    }

    override fun editProperties(
        p0: AccountAuthenticatorResponse?,
        p1: String?
    ): Bundle? {
        TODO("Not yet implemented")
    }

    override fun getAuthToken(
        p0: AccountAuthenticatorResponse?,
        p1: Account?,
        p2: String?,
        p3: Bundle?
    ): Bundle? {
        TODO("Not yet implemented")
    }

    override fun getAuthTokenLabel(p0: String?): String? {
        TODO("Not yet implemented")
    }

    override fun hasFeatures(
        p0: AccountAuthenticatorResponse?,
        p1: Account?,
        p2: Array<out String?>?
    ): Bundle? {
        TODO("Not yet implemented")
    }

    override fun updateCredentials(
        p0: AccountAuthenticatorResponse?,
        p1: Account?,
        p2: String?,
        p3: Bundle?
    ): Bundle? {
        TODO("Not yet implemented")
    }

}