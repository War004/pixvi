package com.example.pixvi

import android.content.ContentValues.TAG
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.example.pixvi.login.AuthViewModel
import com.example.pixvi.network.api.RetrofitClient
import com.example.pixvi.preview.NotificationScreen
import com.example.pixvi.screens.LoginScreen
import com.example.pixvi.ui.theme.PixviTheme
import com.example.pixvi.viewModels.NotificationViewModel
import kotlinx.serialization.Serializable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.example.pixvi.viewModels.HomeIllustViewModel
import com.example.pixvi.viewModels.MangaViewModel
import androidx.activity.viewModels
import com.example.pixvi.screens.detail.DetailNovel
import com.example.pixvi.screens.detail.FullImageScreen
import com.example.pixvi.viewModels.HomeNovelViewModel
import com.example.pixvi.viewModels.HomeNovelViewModelFactory
import kotlinx.coroutines.launch
import com.example.pixvi.screens.detail.ContentType


sealed interface UiState {
    object Loading : UiState
    data class Success(val startDestination: String) : UiState
}

class MainActivity : ComponentActivity() {
    private lateinit var authViewModel: AuthViewModel

    private val app by lazy { application as MyApplication }

    private val homeIllustViewModel: HomeIllustViewModel by viewModels {
        app.appContainer.viewModelFactory
    }
    private val mangaViewModel: MangaViewModel by viewModels {
        app.appContainer.viewModelFactory
    }

    private val homeINovelViewModel: HomeNovelViewModel by viewModels {
        HomeNovelViewModelFactory(
            application = application,
            pixivApiService = RetrofitClient.apiService
        )
    }

    companion object {
        const val ACTION_HANDLE_AUTH_REDIRECT = "com.example.pixvi.HANDLE_AUTH_REDIRECT"
        const val MAIN_CONTENT_ROUTE = "main_content"
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        authViewModel.initialize(this)
        RetrofitClient.initialize(authViewModel)

        var uiState: UiState by mutableStateOf(UiState.Loading)

        lifecycleScope.launch {
            val destination = if (authViewModel.checkLoggedIn()) MAIN_CONTENT_ROUTE else "LoginScreen" //Illusation screen is the default screen.
            Log.d(TAG, "Initial destination determined: $destination")
            uiState = UiState.Success(destination)
        }

        splashScreen.setKeepOnScreenCondition {
            uiState is UiState.Loading
        }

        val appViewModels = AppViewModels(
            homeIllustViewModel = homeIllustViewModel,
            mangaViewModel = mangaViewModel,
            homeINovelViewModel = homeINovelViewModel
        )

        setContent {
            PixviTheme {
                val currentUiState = uiState
                if (currentUiState is UiState.Success) {
                    val rootNavController = rememberNavController()
                    val loginState by authViewModel.loginState.collectAsState()

                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        NavHost(
                            navController = rootNavController,
                            startDestination = currentUiState.startDestination,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Log.d(TAG, "NavHost startDestination: ${currentUiState.startDestination}")

                            composable(route = "LoginScreen") {
                                Log.d(TAG, "Composing LoginScreen")
                                LoginScreen(
                                    innerPadding = PaddingValues(),
                                    authViewModel = authViewModel,
                                    navController = rootNavController
                                )
                            }
                            composable(route = MAIN_CONTENT_ROUTE) {
                                MainContentScreen(
                                    rootNavController = rootNavController,
                                    authViewModel = authViewModel,
                                    pixivApiService = RetrofitClient.apiService,
                                    appViewModels = appViewModels,
                                    onLogout = authViewModel::logout,
                                    settingRepo = (app).appContainer.settingsRepository,
                                    batterySaverRepo = (app).appContainer.BatterySaverTheme
                                )
                            }
                            // Other screens not realted to navigation
                            composable<FullImageScreenRoute> { backStackEntry ->
                                // Automatically deserialize the arguments into our data class
                                val routeArgs = backStackEntry.toRoute<FullImageScreenRoute>()

                                FullImageScreen(
                                    contentType = routeArgs.contentType,
                                    navController = rootNavController,
                                    homeIllustViewModel = homeIllustViewModel,
                                    mangaViewModel = mangaViewModel,
                                    isBatterySaverTheme = (app).appContainer.BatterySaverTheme
                                )
                            }

                            composable<NovelDetailScreen>{backStackEntry ->
                                val args = backStackEntry.toRoute<NovelDetailScreen>()
                                DetailNovel(
                                    novelId = args.novelId,
                                    navController = rootNavController,
                                    pixivApiService = RetrofitClient.apiService
                                )
                            }

                            composable(
                                route = "NotificationScreen",
                                deepLinks = listOf(navDeepLink { uriPattern = "app://pixvi/notifications" })
                            ) {
                                val notificationViewModel: NotificationViewModel = viewModel()
                                NotificationScreen(notificationViewModel = notificationViewModel)
                            }
                        }
                    }
                }
            }
        }
        handleIntent(intent)
        Log.d(TAG, "onCreate completed")
    }

    // ... (rest of MainActivity remains the same)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent == null) return

        val uri = intent.data
        if (uri?.scheme == "pixiv" && intent.action != ACTION_HANDLE_AUTH_REDIRECT) {

            intent.action = ACTION_HANDLE_AUTH_REDIRECT

            authViewModel.handleAuthRedirect(uri, this)
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart called")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause called")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop called")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(TAG, "onConfigurationChanged called: orientation = ${newConfig.orientation}")
    }
}

@Serializable
data class FullImageScreenRoute(
    val contentType: ContentType
)

@Serializable
data class NovelDetailScreen(
    val novelId: Int
)

data class AppViewModels(
    val homeIllustViewModel: HomeIllustViewModel,
    val mangaViewModel: MangaViewModel,
    val homeINovelViewModel: HomeNovelViewModel
)