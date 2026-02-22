package com.cryptic.pixvi

import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cryptic.pixvi.ui.theme.PixviTheme
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.cryptic.pixvi.appShell.MainAppShell
import com.cryptic.pixvi.appShell.MainAppShellViewModel
import com.cryptic.pixvi.core.storage.dataStore
import com.cryptic.pixvi.login.LoginScreen
import com.cryptic.pixvi.login.LoginViewmodel
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.Serializable
import androidx.work.WorkManager
import com.cryptic.pixvi.artwork.ui.ArtworkPageScreen
import com.cryptic.pixvi.artwork.viewmodel.ArtworkPageViewModel
import com.cryptic.pixvi.notification.NotificationScreen
import com.cryptic.pixvi.notification.NotificationViewModel

class MainActivity : ComponentActivity() {
    private val app by lazy { application as MyApplication }
    private val oauthRepo by lazy { app.container.oAuthRepo }
    private val intentChannel = Channel<Intent>(Channel.BUFFERED)

    override fun onCreate(savedInstanceState: Bundle?) {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog() // logs the error to Logcat
                    // .penaltyDeath() // un-comment this to CRASH if a violation is found
                    .build()
            )
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        intent?.let { intentChannel.trySend(it) }

        // Initialize the startup ViewModel
        val mainViewModel: LaunchViewModel by viewModels {
            viewModelFactory {
                initializer {
                    LaunchViewModel(
                        applicationContext.dataStore,
                        app.container.authTokenManager,
                        oauthRepo
                    )
                }
            }
        }

        setContent {
            PixviTheme {
                // Collect the state safely
                val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()

                when (val state = uiState) {
                    is MainActivityUiState.Loading -> {
                        // While waiting for DataStore, show a simple loading screen
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    else -> {
                        // Determine start destination based on state
                        // Note: If state is NOT Loading, it must be LoggedIn or LoggedOut
                        val startDest = if (state is MainActivityUiState.LoggedIn) MainAppShell else LoginScreen
                        val backStack = rememberNavBackStack(startDest)

                        NavDisplay(
                            backStack = backStack,
                            onBack = { backStack.removeLastOrNull() },
                            entryDecorators = listOf(
                                rememberSaveableStateHolderNavEntryDecorator(),
                                rememberViewModelStoreNavEntryDecorator()
                            ),
                            entryProvider = { key ->
                                when (key) {
                                    is LoginScreen -> NavEntry(key) {
                                        // Login ViewModel Setup
                                        val loginViewModel: LoginViewmodel by viewModels(
                                            factoryProducer = {
                                                viewModelFactory {
                                                    initializer {
                                                        LoginViewmodel(
                                                            application = application,
                                                            oauthRepo = oauthRepo,
                                                            pixivAccountManager = app.container.pixivAccountManager,
                                                            navList = backStack
                                                        )
                                                    }
                                                }
                                            }
                                        )

                                        ObserveIntent(intentChannel) { intent ->
                                            processIntentData(
                                                intent = intent,
                                                save = { code -> loginViewModel.onRedirectRecvied(code) }
                                            )
                                        }

                                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                                            LoginScreen(
                                                innerPadding = innerPadding,
                                                loginViewmodel = loginViewModel
                                            )
                                        }
                                    }
                                    is MainAppShell -> NavEntry(key) {
                                        // Main App ViewModel Setup
                                        val mainAppShellViewModel: MainAppShellViewModel by viewModels(
                                            factoryProducer = {
                                                viewModelFactory {
                                                    initializer {
                                                        MainAppShellViewModel(
                                                            pixivAccountManager = app.container.pixivAccountManager,
                                                            application = application,
                                                            isBatterySaverOnInitial = false,
                                                        )
                                                    }
                                                }
                                            }
                                        )
                                        /*
                                        ObserveIntent(intentChannel) { intent ->
                                            processIntentData(
                                                intent = intent,
                                                save = { code -> mainAppShellViewModel.onRedirectRecvied(code) }
                                            )
                                        }*/

                                        // --- SAF Launcher for PDF saving (below API 29) ---

                                        //how to add thje intent here!!



                                        //Making a newNavHost inside the main app sheell
                                        // Pass the EXISTING viewModel instance.
                                        // Do not call viewModel() again inside the function call.

                                        //nested backstack
                                        val appShellBackstack = rememberNavBackStack(ArtworkScreen)

                                        MainAppShell(
                                            viewModel = mainAppShellViewModel,
                                            onNotificationNav = {backStack.add(NotificationScreen)},
                                        ){ innerPadding ->
                                            NavDisplay(
                                                backStack = appShellBackstack,
                                                onBack = {appShellBackstack.removeLastOrNull()},
                                                modifier = Modifier
                                                    //.padding(innerPadding)
                                                    .fillMaxSize(),
                                                entryDecorators = listOf(
                                                    rememberSaveableStateHolderNavEntryDecorator(),
                                                    rememberViewModelStoreNavEntryDecorator()
                                                ),
                                                entryProvider = { key->
                                                    when(key) {
                                                        is ArtworkScreen -> NavEntry(key){
                                                            LaunchedEffect(Unit) { Log.d("DEBUG", "Rendering ArtworkPageViewer") }
                                                            ArtworkPageScreen(
                                                                artworkPageViewModel = viewModel(
                                                                 factory = viewModelFactory {
                                                                     initializer {
                                                                         ArtworkPageViewModel(
                                                                             apiService = app.container.pixivRepo,
                                                                             downloadImageRepo = app.container.downloadImageRepo,
                                                                             downloadPdfRepo = app.container.downloadPdfRepo
                                                                         )
                                                                     }
                                                                 }
                                                                ),
                                                                displaySetting = LocalAppSettings.current,
                                                                displaySettingAction = LocalSettingEvent.current,
                                                                parentPadding = innerPadding
                                                            )
                                                        }
                                                        else -> NavEntry(key){
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxSize()
                                                                    .padding(innerPadding)
                                                            ) {
                                                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                                    Text("Unknown Screen: $key")
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                    
                                    is NotificationScreen -> NavEntry(key){
                                        NotificationScreen(
                                            viewModel = viewModel(
                                                factory = viewModelFactory { 
                                                    initializer { 
                                                        NotificationViewModel(
                                                            workManager = WorkManager.getInstance(
                                                                applicationContext
                                                            ),
                                                            notificationRepo = app.container.notificationRepo,
                                                            downloadPdfRepo = app.container.downloadPdfRepo
                                                        )
                                                    }
                                                }
                                            )
                                        )
                                    }
                                    else -> NavEntry(key) {}
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentChannel.trySend(intent)
    }
}

private fun processIntentData(intent: Intent, save:(String?)-> Unit ) {

    val data = intent.data
    /*
    Ex: pixiv://account/login?code=r92jtgW7zUF3eDSwELqw8E1Rp6WqTFKeNFUD4AhWVt4&via=login
     */
    if (data != null && data.toString().startsWith("pixiv://account")) {
        val code = data.getQueryParameter("code")
        if(code.isNullOrBlank()){
            //try to check any error, not tested
            val error = data.getQueryParameter("error")
            Log.e("Redirect","Code or state is not present. ${error?:"No additional message"}")
        }
        else{
            save(code)
        }
    }else{
        Log.e("Redirect","The redirect url is null or the end points have been changed")
        Log.e("Redirect",intent.data.toString())
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PixviTheme {
        Greeting("Android")
    }
}

@Serializable
data object LoginScreen: NavKey

@Serializable
data object MainAppShell: NavKey

@Composable
fun ObserveIntent(
    channel: Channel<Intent>,
    onIntentReceived: (Intent) -> Unit
) {
    // LaunchedEffect keeps listening as long as this screen is visible
    LaunchedEffect(Unit) {
        for (intent in channel) {
            onIntentReceived(intent)
        }
    }
}

@Serializable
data object ArtworkScreen: NavKey

@Serializable
data object NotificationScreen: NavKey