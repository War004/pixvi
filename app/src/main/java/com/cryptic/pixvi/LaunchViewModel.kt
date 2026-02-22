package com.cryptic.pixvi

import android.graphics.pdf.PdfDocument
import androidx.compose.runtime.collectAsState
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptic.pixvi.auth.data.AuthTokenManager
import com.cryptic.pixvi.core.downloader.pdf.DownloadPdfRepo
import com.cryptic.pixvi.core.network.repo.OauthRepo
import com.cryptic.pixvi.core.storage.StringPreferencesKeys.CURRENT_USER_EMAIL
import com.cryptic.pixvi.core.storage.StringPreferencesKeys.HASHED_REFRESH_TOKEN
import com.cryptic.pixvi.core.storage.StringPreferencesKeys.IMAGE_QUALITY
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

class LaunchViewModel(
    dataStore: DataStore<Preferences>,
    private val authManager: AuthTokenManager,
    private val oauthRepo: OauthRepo,
) : ViewModel() {
    // Helper to ensure we don't run the check multiple times if the flow restarts
    private var isSessionChecked = false

    var pendingPdfData: PdfDocument? = null

    val uiState: StateFlow<MainActivityUiState> = dataStore.data
        .onStart {
            // Guard against re-running on simple configuration changes
            if (!isSessionChecked) {
                isSessionChecked = true

                // Attempt to refresh the session BEFORE emitting the first state
                val refreshSuccess = oauthRepo.updateToken()

                if (!refreshSuccess) {
                    //CRITICAL ERROR: NOT HANDLING NO INTERNET
                    // If refresh fails, wipe the data
                    authManager.clearAll()
                }
            }
        }
        .map { preferences ->
            val accountName = preferences[CURRENT_USER_EMAIL]
            val refreshToken = preferences[HASHED_REFRESH_TOKEN]

            if (!accountName.isNullOrBlank() && !refreshToken.isNullOrEmpty()) {
                MainActivityUiState.LoggedIn
            } else {
                MainActivityUiState.LoggedOut
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MainActivityUiState.Loading
        )

    val _pendingPdf = MutableStateFlow(PendingPdfFileRequest())
    val pendingPdfFiles: StateFlow<PendingPdfFileRequest> =  _pendingPdf.asStateFlow()

    fun addPdfToExport(newItem: PendingPdfFileRequest){
        
    }
}

// Define the UI State
sealed interface MainActivityUiState {
    data object Loading : MainActivityUiState
    data object LoggedIn : MainActivityUiState
    data object LoggedOut : MainActivityUiState
}


data class PendingPdfFileRequest(
    val sessionId: String? = null,
    val start: Int? = null,
    val end: Int? = null,
    val title: String? = null
)