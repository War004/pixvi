package com.example.pixvi

import androidx.datastore.preferences.preferencesDataStore
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

val Context.dataStore by preferencesDataStore(name = "pixvi_settings")

val DARK_MODE_KEY = booleanPreferencesKey("wants_dark_mode")
val BATTERY_SAVER_MODE = booleanPreferencesKey("battery_saver")

val SPEECH_LANGUAGE = stringPreferencesKey("speech_lan_code")
val PRIMARY_TRANSLATION_LAN = stringPreferencesKey("primary_trans_lan_code")

val PICTURE_QUALITY= stringPreferencesKey("picture_quality")