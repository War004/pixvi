package com.example.pixvi.login


import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

/**
 * Service that exposes the Pixiv authenticator to the Android account framework.
 * This service must be declared in the manifest with the appropriate intent filter
 * to be recognized by the Android system.
 */
class PixivAuthenticatorService : Service() {
    private val TAG = "PixivAuthService"
    private lateinit var authenticator: PixivAuthenticator

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        authenticator = PixivAuthenticator(this)
    }

    /**
     * Return the authenticator's binder when the system binds to this service.
     */
    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "Service bound")
        return authenticator.iBinder
    }
}