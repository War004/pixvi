package com.cryptic.pixvi

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.cryptic.pixvi.appShell.SettingAction
import com.cryptic.pixvi.core.storage.AppSettings

val LocalImmersiveMode = compositionLocalOf<(Boolean) -> Unit> {
    error("No Immersive Mode handler provided")
}

val LocalAppSettings = compositionLocalOf { AppSettings() }

val LocalSettingEvent = staticCompositionLocalOf<(SettingAction) -> Unit> {
    {}
}