package com.example.pixvi.settings

import android.content.res.Resources
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.example.pixvi.DARK_MODE_KEY
import com.example.pixvi.BATTERY_SAVER_MODE
import com.example.pixvi.PICTURE_QUALITY
import com.example.pixvi.SPEECH_LANGUAGE
import com.example.pixvi.PRIMARY_TRANSLATION_LAN
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class SettingsRepository(private val dataStore: DataStore<Preferences>){

    val isDarkMode: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[DARK_MODE_KEY] ?: false
    }

    private val _isBatterySaver = MutableStateFlow(false)

    val isBatterySaver: StateFlow<Boolean> = _isBatterySaver

    /*val isBatterySaver: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[BATTERY_SAVER_MODE] ?: false
    }*/

    //assumptions most novels are in ja.
    val speechLanCode: Flow<String> = dataStore.data.map { preferences ->
        preferences[SPEECH_LANGUAGE] ?: "ja"
    }

    val primaryTransLan: Flow<String> = dataStore.data.map { preferences ->
        preferences[PRIMARY_TRANSLATION_LAN] ?: Locale.getDefault().language
    }

    val pictureQuality: Flow<String> = dataStore.data.map { preferences ->
        preferences[PICTURE_QUALITY] ?: "HIGH"
    }

    //methods to modify the values
    suspend fun setTheme(isDark: Boolean){
        dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = isDark
        }
    }

    /*suspend fun setBatterySaverFlag(userChoice: Boolean){
        dataStore.edit { preferences ->
            preferences[BATTERY_SAVER_MODE] = userChoice
        }
    }*/

    fun setBatterySaverFlag(userChoice: Boolean){
        _isBatterySaver.value = userChoice
    }

    suspend fun changeSpeechLan(userSelectedLanCode: String){
        dataStore.edit { preferences ->
            preferences[SPEECH_LANGUAGE] = userSelectedLanCode
        }
    }

    suspend fun changePrimaryTransLan(userSelectedLanCode: String){
        dataStore.edit { preferences ->
            preferences[PRIMARY_TRANSLATION_LAN] = userSelectedLanCode
        }
    }

    suspend fun pictureQuality(quality: String){
        dataStore.edit { preferences ->
            preferences[PRIMARY_TRANSLATION_LAN] = quality
        }
    }
}