package com.cryptic.piyek.data.image


import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.drawable.toBitmap
import coil3.ImageLoader
import coil3.asDrawable
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import com.cryptic.piyek.core.image.blur.BlurWorkspacePool
import com.cryptic.piyek.core.image.blur.CpuBoxBlurEngine
import com.cryptic.piyek.core.image.blur.MicroBlurLruCache
import com.cryptic.piyek.core.image.blur.SingleFlightCoalescer
import com.cryptic.piyek.domain.image.BlurredImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.graphics.createBitmap
import coil3.size.Precision

class BlurredImageRepositoryImpl(
    private val context: Context,
    private val imageLoader: ImageLoader,
    private val workspacePool: BlurWorkspacePool,
    private val singleFlight: SingleFlightCoalescer,
    private val lruCache: MicroBlurLruCache,
    private val engine: CpuBoxBlurEngine
) : BlurredImageRepository {

    private val destRect = Rect(0, 0, CpuBoxBlurEngine.MICRO_WIDTH, CpuBoxBlurEngine.MICRO_HEIGHT)

    override fun getCached(url: String): Bitmap? {
        return lruCache.get(url)
    }

    override suspend fun getOrGenerateBlur(url: String): Bitmap? {
        val cached = lruCache.get(url)
        if (cached != null) {
            return cached
        }

        return singleFlight.joinOrExecute(url) {
            generateBlurInternal(url)
        }
    }

    private suspend fun generateBlurInternal(url: String): Bitmap? = withContext(Dispatchers.Default) {
        val request = ImageRequest.Builder(context)
            .data(url)
            .size(CpuBoxBlurEngine.MICRO_WIDTH, CpuBoxBlurEngine.MICRO_HEIGHT)
            .precision(Precision.INEXACT)
            .allowHardware(false) // Allow CPU software pixel reading
            .build()

        val result = imageLoader.execute(request)
        if (result !is SuccessResult) {
            return@withContext null
        }

        val sourceDrawable = result.image.asDrawable(context.resources)
        val sourceBitmap = (sourceDrawable as? BitmapDrawable)?.bitmap
            ?: sourceDrawable.toBitmap(config = Bitmap.Config.ARGB_8888)

        workspacePool.useWorkspace { workspace ->
            // Step 1: Bulk Native Skia Downsample onto 64x64 canvas
            workspace.canvas.drawBitmap(sourceBitmap, null, destRect, workspace.paint)

            // Step 2: Bulk Buffer Read into primitive array (Loads into 16 KB L1 Cache)
            workspace.bitmap.getPixels(
                workspace.bufferA,
                0,
                CpuBoxBlurEngine.MICRO_WIDTH,
                0,
                0,
                CpuBoxBlurEngine.MICRO_WIDTH,
                CpuBoxBlurEngine.MICRO_HEIGHT
            )

            // Step 3: Pure ART Mathematical Blur (sub-1ms, zero branches, exact ARGB channels)
            engine.applyBlur(workspace.bufferA, workspace.bufferB)

            // Step 4: Write back to an immutable micro-bitmap
            val outputBitmap =
                createBitmap(CpuBoxBlurEngine.MICRO_WIDTH, CpuBoxBlurEngine.MICRO_HEIGHT)
            outputBitmap.setPixels(
                workspace.bufferA,
                0,
                CpuBoxBlurEngine.MICRO_WIDTH,
                0,
                0,
                CpuBoxBlurEngine.MICRO_WIDTH,
                CpuBoxBlurEngine.MICRO_HEIGHT
            )

            // Step 5: Save to memory cache
            lruCache.put(url, outputBitmap)
            outputBitmap
        }
    }
}