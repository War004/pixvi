package com.example.pixvi.viewModels
import com.example.pixvi.network.response.Home.basePost

import com.example.pixvi.network.api.PixivApiService
import com.example.pixvi.bookmark.BookmarkRepository
import com.example.pixvi.settings.SettingsRepository

class MangaViewModel(
    private val pixivApiService: PixivApiService,
    bookMarkRepository: BookmarkRepository,
    settingsRepository: SettingsRepository
) : BaseContentViewModel(bookMarkRepository, settingsRepository) {

    // Keep the original method names for backward compatibility
    fun loadInitialMangaRecommendations() = loadInitialContent()
    fun loadMoreMangaRecommendations() = loadMoreContent()

    override suspend fun fetchInitialData(): retrofit2.Response<basePost> {
        return pixivApiService.getRecommendedManga()
    }

    override suspend fun fetchMoreData(nextUrl: String): retrofit2.Response<basePost> {
        return pixivApiService.getNextManga(nextUrl)
    }

    override fun getContentTypeName(): String = "MangaViewModel"
}