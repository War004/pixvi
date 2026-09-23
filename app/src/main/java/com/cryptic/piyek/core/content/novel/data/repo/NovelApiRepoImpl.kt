package com.cryptic.piyek.core.content.novel.data.repo

import com.cryptic.piyek.core.CResponse
import com.cryptic.piyek.core.content.data.model.RecommendationNovelPara
import com.cryptic.piyek.core.content.domain.repo.CoreContentApiRepo
import com.cryptic.piyek.core.content.novel.data.model.Novel
import com.cryptic.piyek.core.content.novel.data.model.toDomain
import com.cryptic.piyek.core.content.novel.data.remote.NovelApiService
import com.cryptic.piyek.core.content.novel.domain.model.NovelDomain
import com.cryptic.piyek.core.content.novel.domain.model.NovelHome
import com.cryptic.piyek.core.data.local.BookmarkRestrict
import com.cryptic.piyek.core.network.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class NovelApiRepoImpl(
    private val novelApiService: NovelApiService
): CoreContentApiRepo<NovelHome, RecommendationNovelPara, NovelHome>{

    private val _contentList = MutableStateFlow<NovelHome?>(null)
    override val contentList: StateFlow<NovelHome?> = _contentList.asStateFlow()

    override suspend fun getRecommendation(parameters: RecommendationNovelPara): CResponse<Unit> {
        val response = novelApiService.getRecommendation(
            wantsRanking = parameters.includeRanking,
            includePrivacyPolicy = parameters.includePrivacyPolicy,
            readNovelIds = parameters.readNovelIds.map { it.toString() },
            readNovelDateTimes = parameters.readNovelDateTimes,
            viewNovelIds = parameters.readNovelIds.map { it.toString() },
            viewNovelDateTimes = parameters.viewNovelDataTimes
        )

        return when(response){
            is NetworkResult.Success -> {
                if(response.data.novels.isNotEmpty()){
                    val novelList = response.data.novels.map { it.toDomain() }
                    val rankingNovelList = response.data.rankingNovels.map { it.toDomain() }
                    val nextUrl = response.data.nextUrl?:""

                    _contentList.update {
                        NovelHome(
                            novels = novelList,
                            rankingNovels = rankingNovelList,
                            nextUrl = nextUrl,
                            privacyPolicy = response.data.privacyPolicy?.toDomain()
                        )
                    }
                    CResponse.Success(Unit)
                } else {
                    CResponse.Failed(
                        exception = Exception("API returned no content.")
                    )
                }
            }
            is NetworkResult.Error -> {
                CResponse.Failed(
                    exception = Exception(response.message)
                )
            }
            is NetworkResult.Exception -> {
                CResponse.Failed(
                    exception = response.e
                )
            }
        }
    }

    override suspend fun getMoreRecommendation(): CResponse<Unit> {
        val currentContent = _contentList.value
            ?: return CResponse.Failed(IllegalStateException("Initial recommendations must be loaded before requesting more."))

        val nextUrl = currentContent.nextUrl
            .takeIf { it.isNotBlank() }
            ?: return CResponse.Failed(Exception("No more pages available."))

        return when (val networkResponse = novelApiService.getMoreRecommendation(nextUrl)) {
            is NetworkResult.Success -> {
                val incomingNovels = networkResponse.data.novels
                if (incomingNovels.isNotEmpty()) {
                    val mappedNovels = incomingNovels.map { it.toDomain() }

                    _contentList.update { state ->
                        state?.copy(
                            novels = (state.novels + mappedNovels).distinctBy { item -> item.id },
                            nextUrl = networkResponse.data.nextUrl?:""
                        )
                    }
                    CResponse.Success(Unit)
                } else {
                    CResponse.Failed(Exception("API returned no content."))
                }
            }
            is NetworkResult.Error -> CResponse.Failed(Exception(networkResponse.message))
            is NetworkResult.Exception -> CResponse.Failed(exception = networkResponse.e)
        }
    }

    override suspend fun addBookmark(
        postId: Long,
        restrict: BookmarkRestrict
    ): CResponse<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteBookmark(postId: Long): CResponse<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun getRelatedOnBookMark(data: NovelHome): CResponse<NovelHome> {
        TODO("Not yet implemented")
    }

    override suspend fun getRelatedForPost(data: NovelHome): CResponse<NovelHome> {
        TODO("Not yet implemented")
    }

    override fun changeFocusedIndex(index: Int) {
        TODO("Not yet implemented")
    }


}