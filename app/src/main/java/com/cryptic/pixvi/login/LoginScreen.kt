package com.cryptic.pixvi.login

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.net.toUri

@Composable
fun LoginScreen(
    innerPadding: PaddingValues,
    loginViewmodel: LoginViewmodel
) {
    val content = LocalContext.current
    val loginState by loginViewmodel.loginState.collectAsStateWithLifecycle()

    val customTabsIntent = CustomTabsIntent.Builder().build()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
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
                val redirectUri = loginViewmodel.onLoginStart().toUri()
                try {
                    customTabsIntent.launchUrl(content, redirectUri)
                }catch(e: ActivityNotFoundException){
                    Log.e("LoginScreen","Browser with custom tabs not available: ${e.message}")
                    try{
                        content.startActivity(Intent(Intent.ACTION_VIEW,redirectUri))
                    }catch (e2: ActivityNotFoundException) {
                        Log.e("LoginScreen", "Fallback browser also not found: ${e2.message}")
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Blue,
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
}