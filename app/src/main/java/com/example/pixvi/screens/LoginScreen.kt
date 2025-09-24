package com.example.pixvi.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.pixvi.MainActivity.Companion.MAIN_CONTENT_ROUTE
import com.example.pixvi.R
import com.example.pixvi.login.AuthViewModel
import com.example.pixvi.login.LoginState
import com.example.pixvi.utils.ContentRoutes

@Composable
fun LoginScreen(
    innerPadding: PaddingValues,
    authViewModel: AuthViewModel,
    navController: NavController,
) {
    val loginState by authViewModel.loginState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // This effect for launching the browser
    LaunchedEffect(Unit) {
        authViewModel.launchLoginUrlEvent.collect { loginUri ->
            val customTabsIntent = CustomTabsIntent.Builder().build()
            try {
                customTabsIntent.launchUrl(context, loginUri)
            } catch (e: ActivityNotFoundException) {
                Log.e("LoginScreen", "Chrome Custom Tabs not available. Falling back. ${e.message}")
                val browserIntent = Intent(Intent.ACTION_VIEW, loginUri)
                try {
                    context.startActivity(browserIntent, null)
                } catch (e2: ActivityNotFoundException) {
                    Log.e("LoginScreen", "Fallback browser also not found: ${e2.message}")
                }
            }
        }
    }

    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {
            navController.navigate(MAIN_CONTENT_ROUTE) {
                //removing the login screen from the backstack based on the LoginState
                popUpTo(navController.graph.findStartDestination().id) {
                    inclusive = true
                }
                // Ensures we don't create multiple copies of the main screen.
                launchSingleTop = true
            }
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (val state = loginState) {
            is LoginState.Idle -> {
                Text(
                    text = "Find something you like",
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 27.sp,
                    textAlign = TextAlign.Center
                )
                Button(
                    modifier = Modifier
                        .padding(top = 50.dp, bottom = 20.dp),
                    onClick = {
                        authViewModel.startLoginFlow()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.pixiv_blue),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Login",
                        textAlign = TextAlign.Center
                    )
                }
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Don't have an account?",
                    textAlign = TextAlign.Center
                )
            }
            is LoginState.Loading, is LoginState.Success -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Loading...")
            }
            is LoginState.Initiated -> {
                //Maybe show something but the app redirect to browers
            }

            is LoginState.Error -> {
                Text("Error: ${state.message}")
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