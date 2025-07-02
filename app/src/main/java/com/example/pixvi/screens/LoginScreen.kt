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
import com.example.pixvi.R
import com.example.pixvi.login.AuthViewModel
import com.example.pixvi.login.LoginState

@Composable
fun LoginScreen(
    innerPadding: PaddingValues,
    authViewModel: AuthViewModel,
    navController: NavController,
) {
    val loginState by authViewModel.loginState.collectAsStateWithLifecycle()
    val context = LocalContext.current


    LaunchedEffect(Unit) { // Use a key that doesn't change if you only want one collector
        authViewModel.launchLoginUrlEvent.collect { loginUri ->
            val customTabsIntentBuilder = CustomTabsIntent.Builder()
            // Optional: Configure appearance of the Custom Tab
            // try {
            //     customTabsIntentBuilder.setToolbarColor(ContextCompat.getColor(context, R.color.your_primary_color))
            // } catch (resNotFound: Resources.NotFoundException) { /* ... */ }
            customTabsIntentBuilder.setShowTitle(true)

            val customTabsIntent = customTabsIntentBuilder.build()


            try {
                customTabsIntent.launchUrl(context, loginUri)
            } catch (e: ActivityNotFoundException) {
                Log.e("LoginScreen", "Chrome Custom Tabs not available. Falling back. ${e.message}")
                val browserIntent = Intent(Intent.ACTION_VIEW, loginUri)
                // For fallback, if you want to ensure it works, you might need FLAG_ACTIVITY_NEW_TASK
                // if there's any doubt about the context or if no browser is found for the current task.
                // However, usually for ACTION_VIEW, the system handles it well.
                // browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(browserIntent, null)
                } catch (e2: ActivityNotFoundException) {
                    Log.e("LoginScreen", "Fallback browser also not found: ${e2.message}")
                    // Consider updating loginState to Error via ViewModel if launch completely fails
                    // authViewModel.reportBrowserNotFoundError() // You'd need a method for this
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ){
        when(val state = loginState) {
            is LoginState.Idle ->{
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
                ){
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
            is LoginState.Loading -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Loading")
            }
            is LoginState.Initiated -> {
                //Do nothing, the webpage would open upon clicking
                //Reason of removing: Upon going back after the webpage opened it would show the text without any action to be taken, Now it would retain the home screen
                //Text("Follow the steps in the browser...")
            }
            is LoginState.Success -> {
                LaunchedEffect(state) { // Ensure navigation happens once per success state
                    navController.navigate("MainAppShell") {
                        popUpTo("LoginScreen") { inclusive = true }
                        launchSingleTop = true// Avoid going back to login
                    }
                }
            }
            is LoginState.Error -> {
                Text("Error: ${state.message}")
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {authViewModel.startLoginFlow()}
                ){
                    Text("Try Again")
                }
            }
        }
    }
}