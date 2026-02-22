package com.cryptic.pixvi.appShell

sealed class SettingAction{
    data class ChangeImageQuality(val quality: Int): SettingAction()
    data class ChangeDarkMode(val isDarkMode: Boolean): SettingAction()
    data class ShowNsfw(val userChoice: Boolean): SettingAction()
}