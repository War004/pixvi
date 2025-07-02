package com.example.pixvi.ui.theme

sealed class AppState{
    object Loading: AppState()
    object Authenticated : AppState()
    object Unauthenticated : AppState()
}