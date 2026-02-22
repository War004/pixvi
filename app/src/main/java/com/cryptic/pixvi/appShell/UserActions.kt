package com.cryptic.pixvi.appShell

sealed class UserActions{
    data class OnQueryChange(val text:String): UserActions()
    data class ProfileBoxOpen(val choice: Boolean): UserActions()
    data class ChangeScreen(val newScreenType: Navigation): UserActions()
    data class ChangeDrawerState(val newFocus: Boolean): UserActions()
    data class ChangeNavMenuVisibility(val newFocus: Boolean): UserActions()
}