package com.example.pixvi.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.pixvi.bookmark.BookmarkRepository
import com.example.pixvi.network.api.PixivApiService
import com.example.pixvi.network.response.Home.basePost
import com.example.pixvi.settings.SettingsRepository

class HomeIllustViewModel(
    private val pixivApiService: PixivApiService,
    bookMarkRepository: BookmarkRepository,
    settingsRepository: SettingsRepository
) : BaseContentViewModel(bookMarkRepository, settingsRepository) {

    fun loadInitialRecommendations() = loadInitialContent()
    fun loadMoreRecommendations() = loadMoreContent()

    override suspend fun fetchInitialData(): retrofit2.Response<basePost> {
        return pixivApiService.getRecommendedIllusts()
    }

    override suspend fun fetchMoreData(nextUrl: String): retrofit2.Response<basePost> {
        return pixivApiService.getNextIllusts(nextUrl)
    }

    override fun getContentTypeName(): String = "HomeIllustViewModel"
}