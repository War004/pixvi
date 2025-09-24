package com.example.pixvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.pixvi.network.api.RetrofitClient
import com.example.pixvi.viewModels.HomeIllustViewModel
import com.example.pixvi.viewModels.MangaViewModel

class ViewModelFactory(private val appContainer: AppContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            // When the system asks for a HomeIllustViewModel...
            modelClass.isAssignableFrom(HomeIllustViewModel::class.java) -> {
                HomeIllustViewModel(
                    RetrofitClient.apiService,
                    appContainer.bookmarkRepository,
                    appContainer.settingsRepository
                ) as T
            }

            // When the system asks for a MangaViewModel...
            modelClass.isAssignableFrom(MangaViewModel::class.java) -> {
                MangaViewModel(
                    RetrofitClient.apiService,
                    appContainer.bookmarkRepository,
                    appContainer.settingsRepository
                ) as T
            }

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}