package com.cryptic.pixvi.core.storage

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object StringPreferencesKeys{
    val CURRENT_USER_EMAIL = stringPreferencesKey("current_user_email")
    val HASHED_REFRESH_TOKEN = stringPreferencesKey("hashed_refresh_token")

    val IMAGE_QUALITY = intPreferencesKey("image_quality")
    val DARK_MODE = booleanPreferencesKey("dark_mode")
    val BATTERY_SAVER = booleanPreferencesKey("battery_saver")
    val NSFW = booleanPreferencesKey("show_nsfw")
}