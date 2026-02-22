package com.cryptic.pixvi.login

import android.app.Application
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.cryptic.pixvi.MainAppShell
import com.cryptic.pixvi.auth.account.PixivAccountManager
import com.cryptic.pixvi.core.storage.dataStore
import com.cryptic.pixvi.core.model.OAuthInfo
import com.cryptic.pixvi.core.network.repo.OauthRepo
import com.cryptic.pixvi.auth.util.generateCodeChallenge
import com.cryptic.pixvi.auth.util.generateCodeVerifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginViewmodel(
    private val application: Application,
    private val oauthRepo: OauthRepo,
    private val pixivAccountManager: PixivAccountManager,
    private val navList: NavBackStack<NavKey>

    ): ViewModel(){
    private val _loginState = MutableStateFlow(LoginState())
    val loginState = _loginState.asStateFlow()

    private var baseUrl = "https://app-api.pixiv.net/"
    private var hashMethod = "S256"
    private var client = "pixiv-android"
    private val HASHED_REFRESH_TOKEN = stringPreferencesKey("hashed_refresh_token")
    private val CURRENT_USER_EMAIL = stringPreferencesKey("current_user_email")

    /*
    Ex: https://app-api.pixiv.net/web/v1/login?code_challenge=TkmN43K3UrKdQBkKixSD5-lv_yDaMIHajlh2k-DXSL4&code_challenge_method=S256&client=pixiv-android
     */
    private fun makeRedirectUrl(): String {
        //code_challenge = Code_challenge(code_verifier)
        val codeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallenge(codeVerifier)

        val redirectUrl = "https://app-api.pixiv.net/web/v1/login?code_challenge=$codeChallenge&code_challenge_method=S256&client=pixiv-android"

        _loginState.update { currentState->
            currentState.copy(
                loginStatus = LoginStatus.Loading,
                codeVerifier = codeVerifier,
                codeChallenge = codeChallenge,
                lastRedirectUrl = redirectUrl
            )
        }
        return redirectUrl
    }

    fun onLoginStart():String{

        /*
        sent the code challenge with the url
        Ex: https://app-api.pixiv.net/web/v1/login?code_challenge=TkmN43K3UrKdQBkKixSD5-lv_yDaMIHajlh2k-DXSL4&code_challenge_method=S256&client=pixiv-android
        [browser activity]
        we get the url back with the state and the code
        https://app-api.pixiv.net/web/v1/users/auth/pixiv/callback?state=86tg1HoI6wZSjnyzra2VHdig6ByO4kMPH2Q2SLfjd1t0tPufsGI4hKrUI9wFY7ZQ&code=aw7GQzNpwq883mL00cQRd4Us8KRc8_oFiOqYP70-JZk
        Now, we have three things, code_challenge(generated on device), state and code(server generation)
        Now, we make the actual request to the server, on the end point: https://oauth.secure.pixiv.net/auth/token
        while inclduing the code_verifier(on device generation), code(brower given), client_id(hardcodeded), client_secret(hardcoded)
         */
        return makeRedirectUrl()
    }
    fun onRedirectRecvied(code: String?){
        //check if there is already active sesison, (Using the hashed token value)
        _loginState.update {
            it.copy(code = code)
        }
        Log.d("Login","codeVerifier = ${loginState.value.codeVerifier}")
        Log.d("Login","codeChallenge = ${loginState.value.codeChallenge}")
        Log.d("Login","code = ${loginState.value.code}")
        Log.d("Login","state = ${loginState.value.state}")
        Log.d("Login","lastUrl = ${loginState.value.lastRedirectUrl}")

        onNewCode()
    }

    private fun saveTokens(response: OAuthInfo){
        val values = loginState.value

        if(values.loginStatus == LoginStatus.Initiated()){
            _loginState.update {
                it.copy(loginStatus = LoginStatus.Initiated(
                    userName = response.user.name,
                    emailId = response.user.emailAddress
                ))
            }
        }
    }


    fun onNewCode(){
        val values = loginState.value
        if(!values.codeVerifier.isNullOrBlank() && !values.code.isNullOrBlank() && values.loginStatus == LoginStatus.Loading){
            //load to the next stage,
            viewModelScope.launch {
                try{
                    withContext(Dispatchers.IO) {

                        // 1. Network Call
                        val success = oauthRepo.login(
                            codeVerifier = values.codeVerifier,
                            code = values.code
                        )

                        if (success != null) {
                            val metaData = success.user

                            // 2. Save Tokens (Disk I/O) - Now safe in background
                            saveTokens(success)

                            // 3. Create Account (Database/System I/O) - Now safe
                            val haveAccountRegistered = pixivAccountManager.createAccount(
                                email = metaData.emailAddress,
                                userId = metaData.id,
                                name = metaData.name,
                                accountName = metaData.name,
                                isPremium = if (metaData.isPremium) 1 else 0,
                                profilePicBig = metaData.profileImageUrls.px170x170
                            )

                            // 4. Update DataStore (Disk I/O)
                            // Note: DataStore is technically main-safe, but keeping it here is fine
                            if (haveAccountRegistered) {
                                application.dataStore.edit { preferences ->
                                    preferences[CURRENT_USER_EMAIL] = metaData.emailAddress
                                }
                            }
                        }
                    }
                    //change the loginstatus
                    _loginState.update {
                        it.copy(loginStatus = LoginStatus.Success)
                    }
                    //navigate away

                    navList[navList.lastIndex] = MainAppShell
                    ///aaahhh

                }catch (e: Exception){
                    Log.d("ViewModel","Something happened, ${e.message}")
                }
            }
        }
        else{
            Log.d("ViewModel","Something happened")
        }
    }
}

data class LoginState(
    val loginStatus: LoginStatus = LoginStatus.Idle,
    val codeVerifier: String? = null,
    val codeChallenge: String? = null,
    val code: String? = null,
    val state: String? = null,
    val lastRedirectUrl: String? = null,
)

sealed class LoginStatus {
    object Idle : LoginStatus()

    object Loading : LoginStatus()

    data class Initiated(
        val userName: String? = null,
        val emailId: String? = null
    ) : LoginStatus()

    object Success : LoginStatus()
    data class Error(val message: String) : LoginStatus()
}