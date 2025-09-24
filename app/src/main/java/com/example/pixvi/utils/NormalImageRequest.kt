package com.example.pixvi.utils

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import okhttp3.Headers
import androidx.compose.ui.graphics.ColorFilter
import coil3.PlatformContext
import coil3.compose.AsyncImage
import coil3.memory.MemoryCache
import coil3.network.httpHeaders
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.network.NetworkHeaders

object NormalImageRequest {

    /**
     * Creates a complete ImageRequest with common configurations.
     */
    fun normalImageRequest(
        context: PlatformContext,
        imageUrl: String?,
        memoryCacheKey: MemoryCache.Key? = null,
        placeholderMemoryCacheKey: MemoryCache.Key? = null,
        block: (ImageRequest.Builder) -> ImageRequest.Builder = { it }
    ): ImageRequest {
        val builder = ImageRequest.Builder(context)
            .httpHeaders(commonHeaders)
            .data(imageUrl)
            .crossfade(true)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)

        if (memoryCacheKey != null) {
            builder.memoryCacheKey(memoryCacheKey)
        }
        if (placeholderMemoryCacheKey != null) {
            builder.placeholderMemoryCacheKey(placeholderMemoryCacheKey)
        }

        return block(builder).build()
    }

    private val commonHeaders: NetworkHeaders by lazy {
        val appVersion = "6.143.0"
        val userAgent = "PixivAndroidApp/$appVersion (Android ${Build.VERSION.RELEASE}; ${Build.MODEL})"
        NetworkHeaders.Builder()
            .set("Referer", "https://app-api.pixiv.net/")
            .set("User-Agent", userAgent)
            .build()
    }
}

/**
 * A custom wrapper around AsyncImage that automatically uses our
 * app's common image request configuration.
 */
@Composable
fun PixivAsyncImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    colorFilter: ColorFilter? = null,
    width: Int? = null,
    height: Int? = null
) {
    val context = LocalContext.current
    val imageRequest = remember(imageUrl, context, width, height) {
        NormalImageRequest.normalImageRequest(context, imageUrl) { builder ->
            if (width != null && height != null) {
                builder.size(width, height)
            }
            builder
        }
    }

    // Call the original AsyncImage with your custom request
    AsyncImage(
        model = imageRequest,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        colorFilter = colorFilter,
        onState = { state ->
            if (state is coil3.compose.AsyncImagePainter.State.Error) {
                Log.e("PixivAsyncImage", "Failed to load image: ${state.result.throwable}")
            }
        }
    )
}