package com.cryptic.piyek.feature.home.presentation

sealed interface NavOptions{
    data object ILLust: NavOptions
    data object Manga: NavOptions
    data object Novel: NavOptions
}