package com.cryptic.piyek

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cryptic.piyek.feature.home.presentation.HomeScreen
import com.cryptic.piyek.feature.home.presentation.HomeViewModel
import com.cryptic.piyek.feature.iLLust.presentation.home.HomeILLustScreen
import com.cryptic.piyek.feature.iLLust.presentation.home.HomeILLustUiState
import com.cryptic.piyek.feature.iLLust.presentation.home.HomeILLustViewModel
import com.cryptic.piyek.feature.onboarding.presentation.OnboardingScreen
import com.cryptic.piyek.feature.onboarding.presentation.OnboardingViewModel
import com.cryptic.piyek.ui.theme.PiYekTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val app by lazy { application as MyApplication }
    private val oAuthIntentProcessor by lazy { app.container.oAuthIntentProcessor }
    private val sessionManager by lazy { app.container.sessionManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sessionManager.events.collect { errorMessage ->
                    Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        }

        setContent {
            val authState by sessionManager.authState.collectAsStateWithLifecycle()
            val backStack = remember { mutableStateListOf<Destinations>(Destinations.Home) }

            PiYekTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (val state = authState) {
                        is AppAuthState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        is AppAuthState.Unauthenticated -> {
                            val onBoardingVIewModel: OnboardingViewModel by viewModels(
                                factoryProducer = {
                                    viewModelFactory {
                                        initializer {
                                            OnboardingViewModel(
                                                oAuthUserRepo = app.container.oAuthUserRepository,
                                                onboardingManager = app.container.onBoardingManager,
                                                walkThroughRepo = app.container.walkthroughRepo
                                            )
                                        }
                                    }
                                }
                            )
                            OnboardingScreen(
                                innerPadding = innerPadding,
                                onboardingViewModel = onBoardingVIewModel,
                                colorExtractor = app.container.dominantColorExtractor
                            )
                        }
                        is AppAuthState.Authenticated -> {
                            val homeVIewModel: HomeViewModel by viewModels(
                                factoryProducer = {
                                    viewModelFactory {
                                        initializer {
                                            HomeViewModel(
                                                iLLustRepo = app.container.iLLustRepo
                                            )
                                        }
                                    }
                                }
                            )
                            val homeILLustVIewModel: HomeILLustViewModel by viewModels(
                                factoryProducer = {
                                    viewModelFactory {
                                        initializer {
                                            HomeILLustViewModel(
                                                iLLustRepo = app.container.iLLustRepo
                                            )
                                        }
                                    }
                                }
                            )
                            HomeScreen(
                                homeViewModel = homeVIewModel,
                                iLLustHomeScreen = {
                                    HomeILLustScreen(
                                        homeILLustVIewModel
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (intent.action == Intent.ACTION_VIEW && uri.scheme == "pixiv") {
            lifecycleScope.launch {
                val result = oAuthIntentProcessor.onNewIntent(intent)
                result.onSuccess {
                }.onFailure { exception ->
                    Log.e("MYACTIVITY", "OAuth processing failed", exception)
                    Toast.makeText(
                        this@MainActivity,
                        exception.message ?: "Login Failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}

sealed interface AppAuthState {
    data object Loading : AppAuthState
    data object Unauthenticated : AppAuthState
    data class Authenticated(val accountId: Long) : AppAuthState
}

sealed interface Destinations{
    object Home: Destinations
    object Detail: Destinations
    object Full: Destinations
}