package com.cryptic.pixvi.appShell

sealed class UiEvent{
    data object LogOut: UiEvent()
    data class ChangeAccount(val email: String): UiEvent()
}