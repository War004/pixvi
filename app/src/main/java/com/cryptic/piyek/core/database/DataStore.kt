package com.cryptic.piyek.core.database

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_info")
val ACTIVE_USER_ACCOUNT = longPreferencesKey("username")
val CODE_VERIFIER = stringPreferencesKey("code_verifier")