package com.cryptic.pixvi.core.network.repo

import coil3.network.NetworkRequest
import com.cryptic.pixvi.core.network.NetworkResult
import com.cryptic.pixvi.core.network.api.PixivApiService
import com.cryptic.pixvi.core.network.model.ArtworkRequest
import com.cryptic.pixvi.core.network.model.BookmarkTypes
import com.cryptic.pixvi.core.network.model.artwork.ArtworkPageResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Call

class PixivApiRepo(
    private val api: PixivApiService
) {

    fun loadImage(url: String): Call<ResponseBody>{
        return api.downloadFile(url)
    }

    suspend fun loadArtworkPage(request: ArtworkRequest): NetworkResult<ArtworkPageResponse>{
        return withContext(Dispatchers.IO) {
            when(request){
                is ArtworkRequest.FreshLoad -> {
                     api.getArtworkRecommendation(
                        filter = request.filter,
                        wantRanking = request.includeRanking,
                        includePolicy = request.includePolicy
                    )
                }
                is ArtworkRequest.NextPage -> {
                    api.nextArtworkRecommendation(url = request.url)
                }
            }
        }
    }

    suspend fun artworkBookmarkToggle(artworkId: Long, isLiked: Boolean, bookmarkType: BookmarkTypes): NetworkResult<Unit>{
        return withContext(Dispatchers.IO){
            if(isLiked){
                api.addBookmarkArtwork(
                    artworkId = artworkId,
                    restrict = bookmarkType.value
                )
            }
            else{
                api.removeBookmarkArtwork(
                    artworkId = artworkId
                )
            }
        }
    }

}