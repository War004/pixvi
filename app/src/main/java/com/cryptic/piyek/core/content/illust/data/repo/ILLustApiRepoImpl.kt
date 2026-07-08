package com.cryptic.piyek.core.content.illust.data.repo

import com.cryptic.piyek.core.CResponse
import com.cryptic.piyek.core.content.data.model.Artwork
import com.cryptic.piyek.core.content.data.model.ArtworkContentList
import com.cryptic.piyek.core.content.data.model.RecommendationNonNovelPara
import com.cryptic.piyek.core.content.data.remote.toDomain
import com.cryptic.piyek.core.content.domain.repo.CoreContentApiRepo
import com.cryptic.piyek.core.content.illust.data.remote.ILLustApiService
import com.cryptic.piyek.core.data.local.BookmarkRestrict
import com.cryptic.piyek.core.network.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ILLustApiRepoImpl(
    private val ilLustApiService: ILLustApiService
): CoreContentApiRepo<ArtworkContentList, RecommendationNonNovelPara, ArtworkContentList>{

    private val _contentList = MutableStateFlow<ArtworkContentList?>(null)

    override val contentList: StateFlow<ArtworkContentList?> = _contentList.asStateFlow()

    override fun changeFocusedIndex(index: Int) {
        _contentList.update {
            it?.copy(
                focusedIndex = index
            )
        }
    }

    override suspend fun getRecommendation(parameters: RecommendationNonNovelPara): CResponse<Unit> {
        val response = ilLustApiService.getRecommendation(
            filter = parameters.filter,
            includeRanking = parameters.includeRanking,
            includePrivacyPolicy = parameters.includePrivacyPolicy
        )
        return when(response){
            is NetworkResult.Success -> {
                if (response.data.artwork.isNotEmpty()){
                    val artworkList = response.data.artwork.map {
                        it.toDomain()
                    }
                    val rankingArtworkList = response.data.rankingArtwork.map { it.toDomain() }

                    val nextUrl = response.data.nextUrl

                    _contentList.update {
                        ArtworkContentList(
                            artworkList = artworkList,
                            rankingArtworkList = rankingArtworkList,
                            nextUrl = nextUrl,
                            focusedIndex = 0
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
            ?: return getRecommendation(
                RecommendationNonNovelPara(
                    includeRanking = true,
                    includePrivacyPolicy = false
                )
            )

        val nextUrl = currentContent.nextUrl
            .takeIf { it.isNotBlank() }
            ?: return CResponse.Failed(Exception("No more pages available."))

        return when(val networkResponse = ilLustApiService.getMoreRecommendation(nextUrl)){
            is NetworkResult.Success -> {
                if(networkResponse.data.artwork.isNotEmpty()) {
                    val data: List<Artwork> = networkResponse.data.artwork.map { it.toDomain() }
                    val existingList: List<Artwork> = currentContent.artworkList

                    _contentList.update {
                        it?.copy(
                            artworkList = (existingList + data).distinctBy { item: Artwork ->
                                item.id
                            },
                            nextUrl = networkResponse.data.nextUrl
                        )
                    }
                    CResponse.Success(Unit)
                }else{
                    CResponse.Failed(Exception("API returned no content."))
                }
            }
            is NetworkResult.Error -> CResponse.Failed(Exception(networkResponse.message))
            is NetworkResult.Exception -> CResponse.Failed(exception = networkResponse.e)
        }
    }

    override suspend fun addBookmark(postId: Long, restrict: BookmarkRestrict): CResponse<Unit> {
        val response = ilLustApiService.addBookmark(
            iLLust = postId,
            restrict = restrict.name
        )

        return when (response) {
            is NetworkResult.Success -> {
                val currentContent = _contentList.value
                    ?: return CResponse.Failed(Exception("Content list is empty/null"))

                val postExists = currentContent.artworkList.any { it.id == postId }
                if (!postExists) {
                    return CResponse.Failed(Exception("Cannot find the targeted post"))
                }

                _contentList.update { current ->
                    current?.copy(
                        artworkList = current.artworkList.map { artwork ->
                            if (artwork.id == postId) {
                                artwork.copy(
                                    isBookmarked = true,
                                    totalBookmarks = artwork.totalBookmarks + 1
                                )
                            } else {
                                artwork
                            }
                        }
                    )
                }

                CResponse.Success(Unit)
            }
            is NetworkResult.Error -> {
                CResponse.Failed(exception = Exception(response.message))
            }
            is NetworkResult.Exception -> {
                CResponse.Failed(response.e)
            }
        }
    }

    /*
    * Ui will determine if we should call this function or not. No need to check for direct isBookmarked here
     */
    override suspend fun deleteBookmark(postId: Long): CResponse<Unit> {
        return when (val response = ilLustApiService.deleteBookmark(postId)) {
            is NetworkResult.Success -> {
                val currentContent = _contentList.value
                    ?: return CResponse.Failed(Exception("Content list is empty/null"))

                val postExists = currentContent.artworkList.any { it.id == postId }
                if (!postExists) {
                    return CResponse.Failed(Exception("Cannot find the targeted post"))
                }

                _contentList.update { current ->
                    current?.copy(
                        artworkList = current.artworkList.map { artwork ->
                            if (artwork.id == postId && artwork.isBookmarked) {
                                artwork.copy(
                                    isBookmarked = false,
                                    totalBookmarks = artwork.totalBookmarks - 1
                                )
                            } else {
                                artwork
                            }
                        }
                    )
                }

                CResponse.Success(Unit)
            }
            is NetworkResult.Error -> {
                CResponse.Failed(exception = Exception(response.message))
            }
            is NetworkResult.Exception -> {
                CResponse.Failed(response.e)
            }
        }
    }

    override suspend fun getRelatedOnBookMark(data: ArtworkContentList): CResponse<ArtworkContentList> {
        return CResponse.Failed(exception = Exception("Not implemented Yet"))
    }

    override suspend fun getRelatedForPost(data: ArtworkContentList): CResponse<ArtworkContentList> {
        return CResponse.Failed(exception = Exception("Not implemented Yet"))
    }
}