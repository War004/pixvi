package com.example.pixvi.utils

import ConnectivityObserver
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.pixvi.viewModels.ImageQuality
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * A singleton object that monitors network performance to dynamically adjust
 * the image quality for the "AUTO" mode.
 *
 * This monitor is lifecycle-aware, context-aware, and robust against rapid
 * network state changes thanks to debouncing.
 */

object NetworkPerformanceMonitor {

    // --- Configuration Constants ---
    private const val EWMA_ALPHA = 0.3
    private const val COOLDOWN_REQUEST_COUNT = 3
    private const val NETWORK_CHANGE_DEBOUNCE_MS = 2500L

    private data class QualityThresholds(
        val defaultQuality: ImageQuality,
        val downgradeScoreMs: Double,
        val upgradeScoreMs: Double
    )

    private val WIFI_THRESHOLDS = QualityThresholds(
        defaultQuality = ImageQuality.LARGE,
        downgradeScoreMs = 350.0,
        upgradeScoreMs = 150.0
    )
    private val CELLULAR_THRESHOLDS = QualityThresholds(
        defaultQuality = ImageQuality.MEDIUM,
        downgradeScoreMs = 600.0,
        upgradeScoreMs = 100.0
    )

    // --- State ---
    private var performanceScore: Double = 200.0
    private var requestsSinceChange = 0
    private var isWifi: Boolean = true

    private val scope = CoroutineScope(Dispatchers.Default + Job())

    private val _currentAutoQuality = MutableStateFlow(WIFI_THRESHOLDS.defaultQuality)
    val currentAutoQuality: StateFlow<ImageQuality> = _currentAutoQuality.asStateFlow()

    /**
     * PUBLIC API: MUST be called once from the Application's onCreate method.
     */
    fun register(context: Context, connectivityObserver: ConnectivityObserver) {

        // Listen to the stream from ConnectivityObserver
        scope.launch {
            connectivityObserver.observe().collect { status ->
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val caps = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                val newIsWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

                onNetworkStateChanged(status, newIsWifi)
            }
        }
    }

    /**
     * Called whenever the observer emits a new status.
     */
    @Synchronized
    private fun onNetworkStateChanged(status: ConnectivityObserver.Status, newIsWifi: Boolean) {
        // Only reset if the network type has actually changed.
        if (newIsWifi != isWifi || status is ConnectivityObserver.Status.Lost || status is ConnectivityObserver.Status.Unavailable) {
            isWifi = newIsWifi
            val newDefault = if (isWifi && status == ConnectivityObserver.Status.Available) {
                WIFI_THRESHOLDS.defaultQuality
            } else {
                CELLULAR_THRESHOLDS.defaultQuality
            }
            Log.d("NetworkMonitor", "Network changed. Wifi: $isWifi. Resetting quality to $newDefault")
            _currentAutoQuality.value = newDefault
            performanceScore = 200.0
            requestsSinceChange = 0
        }
    }

    /**
     * The private method that actually updates the state after debouncing.
     */
    @Synchronized
    private fun updateAndResetState(capabilities: NetworkCapabilities?) {
        val newIsWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

        if (newIsWifi != isWifi) {
            isWifi = newIsWifi
            val newDefault = if (isWifi) WIFI_THRESHOLDS.defaultQuality else CELLULAR_THRESHOLDS.defaultQuality
            Log.d("NetworkMonitor", "DEBOUNCED: Network changed. Wifi: $isWifi. Resetting quality to $newDefault")
            _currentAutoQuality.value = newDefault
            performanceScore = 200.0
            requestsSinceChange = 0
        }
    }

    /**
     * PUBLIC API: Called to report the performance of a 'large' image load attempt.
     */
    fun recordLoadTime(durationMs: Long) {
        performanceScore = (1 - EWMA_ALPHA) * performanceScore + EWMA_ALPHA * durationMs
        Log.d("NetworkMonitor", "Performance recorded (${durationMs}ms). New score: $performanceScore")
        evaluateQuality()
    }

    /**
     * The core logic engine that decides whether to change quality.
     */
    private fun evaluateQuality() {
        requestsSinceChange++
        if (requestsSinceChange < COOLDOWN_REQUEST_COUNT) return

        val thresholds = if (isWifi) WIFI_THRESHOLDS else CELLULAR_THRESHOLDS

        if (_currentAutoQuality.value == ImageQuality.LARGE && performanceScore > thresholds.downgradeScoreMs) {
            Log.d("NetworkMonitor", "Downgrading auto quality to MEDIUM. Score: $performanceScore > ${thresholds.downgradeScoreMs}")
            _currentAutoQuality.value = ImageQuality.MEDIUM
            requestsSinceChange = 0
            return
        }

        if (_currentAutoQuality.value == ImageQuality.MEDIUM && performanceScore < thresholds.upgradeScoreMs) {
            Log.d("NetworkMonitor", "Upgrading auto quality to LARGE. Score: $performanceScore < ${thresholds.upgradeScoreMs}")
            _currentAutoQuality.value = ImageQuality.LARGE
            requestsSinceChange = 0
        }
    }
}