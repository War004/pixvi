package com.cryptic.pixvi.appShell

import android.accounts.Account
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptic.pixvi.artwork.data.ArtworkInfo
import com.cryptic.pixvi.artwork.data.ArtworkPage
import com.cryptic.pixvi.auth.account.AccountDetails
import com.cryptic.pixvi.auth.account.PixivAccountManager
import com.cryptic.pixvi.core.network.model.ArtworkRequest
import com.cryptic.pixvi.core.network.model.artwork.User
import com.cryptic.pixvi.core.network.repo.PixivApiRepo
import com.cryptic.pixvi.core.storage.AppSettings
import com.cryptic.pixvi.core.storage.StringPreferencesKeys.BATTERY_SAVER
import com.cryptic.pixvi.core.storage.StringPreferencesKeys.CURRENT_USER_EMAIL
import com.cryptic.pixvi.core.storage.StringPreferencesKeys.DARK_MODE
import com.cryptic.pixvi.core.storage.StringPreferencesKeys.IMAGE_QUALITY
import com.cryptic.pixvi.core.storage.StringPreferencesKeys.NSFW
import com.cryptic.pixvi.core.storage.dataStore
import com.cryptic.pixvi.core.storage.userSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.IOException

class MainAppShellViewModel(
    private val pixivAccountManager: PixivAccountManager,
    private val application: Application,
    isBatterySaverOnInitial: Boolean
): ViewModel() {
    data class AppShellUiStates(
        val currentScreen: Navigation = Navigation.ILLUSTRATIONS,
        val textQuery: String? = "",
        val account: Account? = null,
        val accountInfo: AccountDetails? = null,
        val showProfileDialog: Boolean = false,
        val showNavigationMenu: Boolean = false,
        val showModalNavigationDrawer: Boolean = false,
        val isLoading: Boolean = true,
        val isBatterySaverOn: Boolean,
        val notification: Int = 0
    )

    private val _uiState = MutableStateFlow(AppShellUiStates(isBatterySaverOn = isBatterySaverOnInitial))
    private val _events = MutableSharedFlow<UiEvent>()

    @OptIn(ExperimentalCoroutinesApi::class)
    val accountFlow = application.dataStore.data
        .map { it[CURRENT_USER_EMAIL] }
        .distinctUntilChanged()
        .mapLatest { email ->
            if (email != null) {
                // 1. Fetch Account
                val account = pixivAccountManager.getAccountInfo(email)

                // 2. Fetch Details (Suspend function)
                val details = account?.let { pixivAccountManager.getAccountDetails(it) }

                // 3. Return BOTH
                AccountData(account, details)
            } else {
                AccountData(null, null)
            }
        }

    val events = _events.asSharedFlow()
    // 2. Merge it with your existing UI State

    val uiState = combine(_uiState, accountFlow) { state, account ->
        state.copy(
            account = account.account,
            accountInfo = account.details,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppShellUiStates(
            isLoading = true,
            isBatterySaverOn = isBatterySaverOnInitial
        )
    )

    val appSettings: StateFlow<AppSettings> = application.userSettingsStore.data
        .map { preferences ->
            AppSettings(
                imageQuality = preferences[IMAGE_QUALITY] ?: 0,
                isDarkMode = preferences[DARK_MODE] ?: false,
                isBatterySaver = preferences[BATTERY_SAVER] ?: false,
                showNSFW = preferences[NSFW]?: true
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    fun onEvent(event: UserActions){
        when(event){
            is UserActions.ProfileBoxOpen -> changeProfileDialogOpen(event.choice)
            is UserActions.OnQueryChange -> updateText(searchQuery = event.text)
            is UserActions.ChangeScreen -> changeScreen(navigation = event.newScreenType)
            is UserActions.ChangeDrawerState -> changeModelNavigationDrawer(focus = event.newFocus)
            is UserActions.ChangeNavMenuVisibility -> changeModelNavigationDrawer(focus = event.newFocus)
        }
    }

    fun changeProfileDialogOpen(choice: Boolean){
        if(choice == uiState.value.showProfileDialog) return
        _uiState.update {
            it.copy(showProfileDialog = choice)
        }
    }
    fun onDisplaySettingChanges(settingAction: SettingAction){
        when(settingAction){
            is SettingAction.ChangeDarkMode -> {
                updateDarkTheme(settingAction.isDarkMode)
            }
            is SettingAction.ChangeImageQuality -> {
                updateImgQuality(settingAction.quality)
            }
            is SettingAction.ShowNsfw -> {
                manageNSFW(settingAction.userChoice)
            }
        }
    }

    private fun updateImgQuality(newQuality: Int){
        if (appSettings.value.imageQuality == newQuality || newQuality !in -1..1){
            Log.d("Image quality", "Same as previous or out of range")
            return
        }
        viewModelScope.launch {
            application.userSettingsStore.edit {
                it[IMAGE_QUALITY] = newQuality
            }
        }
    }

    private fun updateDarkTheme(wantsMode: Boolean){
        if(appSettings.value.isDarkMode == wantsMode){
            Log.d("UpdateDarkMode","Same as previous")
            return
        }
        viewModelScope.launch {
            application.userSettingsStore.edit {
                it[DARK_MODE] = wantsMode
            }
        }
    }

    private fun manageNSFW(newChoice: Boolean){
        if (appSettings.value.showNSFW == newChoice){
            Log.d("ManageNSFW","Same as previous")
            return
        }
        viewModelScope.launch {
            application.userSettingsStore.edit {
                it[NSFW] = newChoice
            }
        }
    }

    fun onUiEvent(uiEvent: UiEvent){
        when(uiEvent){
            is UiEvent.LogOut -> {
                //show a snakebar with stating what is happening
                //logout, and then clear the nav backstack
            }
            is UiEvent.ChangeAccount ->{
                //get the email id and load the account in background once confirmed
                //logout the user and then save the email put the main email as the data in the data store
            }
        }
    }

    fun onRedirectRecvied(code: String?){
        //emitt a pop up.
    }
    private fun updateText(searchQuery: String){
        _uiState.update {
            it.copy(textQuery = searchQuery)
        }
    }

    private fun changeScreen(navigation: Navigation){
        if(uiState.value.currentScreen == navigation) return
        _uiState.update {
            it.copy(currentScreen = navigation)
        }
    }

    private fun changeModelNavigationDrawer(focus: Boolean){
        if (uiState.value.showModalNavigationDrawer == focus) return
        _uiState.update {
            it.copy(showModalNavigationDrawer = focus)
        }
    }
    private fun changeNavigationMenuVisibility(focus: Boolean){
        if(uiState.value.showNavigationMenu == focus) return
        _uiState.update {
            it.copy(showModalNavigationDrawer = focus)
        }
    }

    private fun changeBatterySaver(isSaverOn: Boolean){
        _uiState.update {
            it.copy(isBatterySaverOn = isSaverOn)
        }
    }

    private fun logOut(){

    }

    private fun changeAccount(){

    }
}

enum class Navigation{
    HOME, ILLUSTRATIONS, MANGA, NOVEL, NEWEST,RANKING

}

data class AccountData(
    val account: Account?,
    val details: AccountDetails?
)