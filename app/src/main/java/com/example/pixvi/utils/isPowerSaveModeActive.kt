package com.example.pixvi.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * A composable utility that returns the current power save mode state and updates
 * automatically when the user toggles it in the system settings.
 *
 * This function encapsulates the logic for interacting with Android's PowerManager
 * and listening to system broadcasts, providing a clean, stateful boolean
 * for use in any composable.
 *
 * @return `true` if Battery Saver is ON, `false` otherwise.
 */
@Composable
fun isPowerSaveModeActive(): Boolean {
    val context = LocalContext.current
    // Get the PowerManager system service
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    var isPowerSaveMode by remember { mutableStateOf(powerManager.isPowerSaveMode) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
                    isPowerSaveMode = powerManager.isPowerSaveMode
                }
            }
        }

        context.registerReceiver(receiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED))
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    return isPowerSaveMode
}