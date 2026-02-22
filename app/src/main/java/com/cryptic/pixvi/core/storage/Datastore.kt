package com.cryptic.pixvi.core.storage

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore by preferencesDataStore(name = "current_user")

val Context.userSettingsStore by preferencesDataStore(name = "current_user_settings")