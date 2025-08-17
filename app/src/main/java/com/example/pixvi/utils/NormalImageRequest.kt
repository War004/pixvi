package com.example.pixvi.utils

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import okhttp3.Headers

object NormalImageRequest {

    /**
     * Creates a complete ImageRequest with common configurations.
     */
    fun normalImageRequest(
        context: Context,
        imageUrl: String?,
        block: (ImageRequest.Builder) -> ImageRequest.Builder = { it }
    ): ImageRequest {
        val builder = ImageRequest.Builder(context)
            .data(imageUrl)
            .headers(commonHeaders) // Headers are applied automatically
            .crossfade(true)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)

        return block(builder).build()
    }

    private val commonHeaders by lazy {
        val appVersion = "6.143.0"
        val userAgent = "PixivAndroidApp/$appVersion (Android ${Build.VERSION.RELEASE}; ${Build.MODEL})"

        Headers.Builder()
            .add("Referer", "https://app-api.pixiv.net/")
            .add("User-Agent", userAgent)
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
        onState = { state ->
            if (state is coil.compose.AsyncImagePainter.State.Error) {
                Log.e("PixivAsyncImage", "Failed to load image: ${state.result.throwable}")
            }
        }
    )
}