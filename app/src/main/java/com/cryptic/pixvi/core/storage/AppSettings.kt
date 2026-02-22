package com.cryptic.pixvi.core.storage

/*Temporary*/
data class AppSettings(
    val imageQuality: Int = 0,
    val isDarkMode: Boolean = false,
    val isBatterySaver: Boolean = false,
    val showNSFW: Boolean = false
)