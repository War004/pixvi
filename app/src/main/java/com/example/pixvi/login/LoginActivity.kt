package com.example.pixvi.login

import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

//For login through the settings app or similar
class LoginActivity : ComponentActivity() {

    private lateinit var viewModel: AuthViewModel
    private var accountAuthenticatorResponse: AccountAuthenticatorResponse? = null
    private var resultBundle: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        accountAuthenticatorResponse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(
                AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
                AccountAuthenticatorResponse::class.java
            )
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
        }
        accountAuthenticatorResponse?.onRequestContinued()

        val loginAction = intent.getStringExtra(PixivAuthenticator.LOGIN_ACTION_KEY)

        // Initialize view model
        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        viewModel.initialize(applicationContext)

        // Set content with Compose UI
        setContent {
            LoginScreen(
                innerPadding = PaddingValues(), // No padding needed for a full activity
                authViewModel = viewModel
            )
        }

        // Observe login state to handle account authenticator response
        lifecycleScope.launch {
            viewModel.loginState.collectLatest { state ->
                if (state is LoginState.Success) {
                    // Prepare result bundle for account authenticator
                    val bundle = Bundle()
                    bundle.putString(AccountManager.KEY_ACCOUNT_NAME, state.username)
                    bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, "com.example.pixivapitesttwo.account")
                    bundle.putString(AccountManager.KEY_AUTHTOKEN, state.accessToken)

                    // Set the result bundle
                    setAccountAuthenticatorResult(bundle)

                    // Finish the activity after a short delay to show success state
                    if (loginAction != null) {
                        // If activity was launched by account authenticator, finish immediately
                        finish()
                    } else {
                        // Otherwise wait a bit to show the success UI
                        android.os.Handler(mainLooper).postDelayed({
                            finish()
                        }, 1500)
                    }
                }
            }
        }
    }

    /**
     * Sets the result that is to be sent as the result of the request that caused this
     * Activity to be launched.
     */
    private fun setAccountAuthenticatorResult(result: Bundle) {
        resultBundle = result
    }

    /**
     * Send the result bundle back to the AccountAuthenticator.
     */
    override fun finish() {
        if (accountAuthenticatorResponse != null) {
            if (resultBundle != null) {
                accountAuthenticatorResponse?.onResult(resultBundle)
            } else {
                accountAuthenticatorResponse?.onError(AccountManager.ERROR_CODE_CANCELED, "Canceled")
            }
            accountAuthenticatorResponse = null
        }
        super.finish()
    }
}

@Composable
fun LoginScreen(
    innerPadding: PaddingValues,
    authViewModel: AuthViewModel
) {
    val loginState by authViewModel.loginState.collectAsStateWithLifecycle()
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) // Date formatter for token expiry

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Pixiv Login",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            when (val state = loginState) {
                is LoginState.Idle -> {
                    Text(
                        text = "Connect your Pixiv account to get started",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { authViewModel.startLoginFlow() },
                        modifier = Modifier.fillMaxWidth(0.7f)
                    ) {
                        Text("Login with Pixiv")
                    }
                }

                is LoginState.Loading -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading...")
                }

                is LoginState.Initiated -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Login initiated. Follow the steps in the browser...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                is LoginState.Success -> {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Success",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Welcome, ${state.username}!",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text("Account successfully connected")
                            Spacer(modifier = Modifier.height(8.dp))
                            val expiryDate = Date(state.tokenExpiry)
                            Text("Token valid until: ${sdf.format(expiryDate)}")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { authViewModel.logout() }
                    ) {
                        Text("Logout")
                    }
                }

                is LoginState.Error -> {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Error: ${state.message}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { authViewModel.startLoginFlow() }
                    ) {
                        Text("Try Again")
                    }
                }
            }
        }
    }
}