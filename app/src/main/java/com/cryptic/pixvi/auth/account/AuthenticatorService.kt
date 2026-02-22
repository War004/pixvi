package com.cryptic.pixvi.auth.account

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.cryptic.pixvi.MyApplication

class AuthenticatorService : Service() {
    private lateinit var authenticator: PixviAuthenticator



    override fun onCreate() {
        val appContainer = (application as MyApplication).container
        authenticator = PixviAuthenticator(this, appContainer.authTokenManager)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return authenticator.iBinder
    }
}