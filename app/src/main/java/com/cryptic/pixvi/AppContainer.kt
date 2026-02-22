package com.cryptic.pixvi

import android.accounts.AccountManager
import android.content.Context
import androidx.work.WorkManager
import com.cryptic.pixvi.auth.account.AccountMetaDataKey
import com.cryptic.pixvi.core.storage.dataStore
import com.cryptic.pixvi.core.network.AuthNetworkBuilder
import com.cryptic.pixvi.auth.data.AuthTokenManager
import com.cryptic.pixvi.core.network.NetworkClient
import com.cryptic.pixvi.core.network.repo.OauthRepo
import com.cryptic.pixvi.core.network.repo.PixivApiRepo
import com.cryptic.pixvi.auth.data.TokenStorage
import com.cryptic.pixvi.core.network.interceptor.AuthInterceptor
import com.cryptic.pixvi.auth.account.PixivAccountManager
import com.cryptic.pixvi.core.downloader.image.DownloadImageRepo
import com.cryptic.pixvi.database.notification.NotificationRepo
import com.cryptic.pixvi.core.downloader.pdf.DownloadPdfRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(private val context: Context){

    private val accountManager: AccountManager = AccountManager.get(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val authServiceBuilder by lazy {
        AuthNetworkBuilder().createAuthService()
    }

    private val tokenShop by lazy {
        TokenStorage(context)
    }
    val authTokenManager by lazy {
        AuthTokenManager(context.dataStore)
    }
    val pixivAccountManager by lazy {
        PixivAccountManager(context)
    }

    val oAuthRepo by lazy {
        OauthRepo(
            api = authServiceBuilder,
            tokenShop = tokenShop,
            dataStore = context.dataStore,
            authTokenManager = authTokenManager
        )
    }

    private val authInterceptor by lazy {
        AuthInterceptor(
            authTokenManager = authTokenManager,
            onRefreshToken = {
                oAuthRepo.updateToken()
            }
        )
    }

    private val networkClient by lazy {
        NetworkClient(
            injector = authInterceptor,
            context = context
        )
    }

    val pixivRepo by lazy {
        PixivApiRepo(
            api = networkClient.apiService
        )
    }

    val imageLoader by lazy {
        networkClient.imageLoader
    }

    val downloadImageRepo by lazy {
        DownloadImageRepo(context.applicationContext)
    }
    /**
     * Starts monitoring for account removal from Android Settings.
     * When the Pixvi account is removed externally, this clears the auth state
     * causing the app to navigate to the login screen.
     * 
     * Call this from Application.onCreate() after creating the container.
     */
    fun startAccountMonitoring() {
        accountManager.addOnAccountsUpdatedListener(
            { accounts ->
                val pixviAccountExists = accounts.any { 
                    it.type == AccountMetaDataKey.ACCOUNT_TYPE 
                }
                
                if (!pixviAccountExists) {
                    // Account was removed externally — clear auth state
                    // This will trigger LaunchViewModel to emit LoggedOut
                    scope.launch {
                        authTokenManager.clearAll()
                    }
                }
            },
            null,  // Use main looper
            true   // Get current state immediately on registration
        )
    }

    private val database by lazy {
        AppDatabase.getDatabase(context)
    }
    val notificationRepo by lazy {
        NotificationRepo(database.notificationDao())
    }

    val downloadPdfRepo by lazy {
        DownloadPdfRepo(context, WorkManager.getInstance(context))
    }
}