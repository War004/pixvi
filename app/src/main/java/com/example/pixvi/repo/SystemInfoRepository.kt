package com.example.pixvi.repo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * A repository that provides a reactive Flow for the system's battery saver status.
 *
 * This class encapsulates the logic for interacting with Android's PowerManager
 * and listening to system broadcasts, making it a clean and reusable data source.
 *
 * @param context The application context, needed to access system services and register receivers.
 */
class SystemInfoRepository(private val context: Context) {

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    /**
     * A cold flow that emits the battery saver status.
     *
     * It emits the initial value immediately upon collection and then new values
     * whenever the system's power save mode changes.
     */
    fun isBatterySaverOn(): Flow<Boolean> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
                    trySend(powerManager.isPowerSaveMode)
                }
            }
        }
        context.registerReceiver(
            receiver,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        )

        trySend(powerManager.isPowerSaveMode)

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }
}