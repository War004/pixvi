package com.cryptic.pixvi.experimental

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.HardwareRenderer
import android.graphics.PixelFormat
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.os.Build
import android.os.Debug
import android.os.Environment
import android.provider.MediaStore
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

/**
 * Benchmark test to compare different blur implementations fairly.
 * Tests both Cold Boot (heavy setup/teardown) and Warm Cached (reusing contexts).
 *
 * Run with: ./gradlew connectedAndroidTest --tests "*.BlurBenchmarkTest"
 */
@RunWith(AndroidJUnit4::class)
class BlurBenchmarkTest {

    private lateinit var context: Context
    private lateinit var testBitmap: Bitmap

    companion object {
        private const val TAG = "BlurBench"
        private const val WARMUP_ITERATIONS = 3
        private const val BENCHMARK_ITERATIONS = 10
        private const val BLUR_RADIUS = 25f // Max radius for RenderScript

        // Set this to true to run the visual verification test
        private const val RUN_VISUAL_VERIFICATION = true
    }

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        val testContext = InstrumentationRegistry.getInstrumentation().context
        testBitmap = try {
            testContext.assets.open("test_blur_image.jpg").use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            } ?: throw Exception("Decode returned null")
        } catch (e: Exception) {
            Log.w(TAG, "Test image not found, creating synthetic bitmap")
            createSyntheticTestBitmap(1200, 800)
        }

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "Test Image: ${testBitmap.width}x${testBitmap.height}")
        Log.d(TAG, "Device: ${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    private fun createSyntheticTestBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val r = (x * 255 / width); val g = (y * 255 / height)
                val b = ((x + y) * 127 / (width + height))
                pixels[y * width + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    // ─────────────────────────────────────────────────────────────────
    // TEST 1: COLD BOOT (Creates and Destroys Contexts Every Time)
    // ─────────────────────────────────────────────────────────────────
    @Test
    fun benchmarkColdBoot() = runBlocking {
        Log.d(TAG, "🧪 RUNNING COLD BOOT BENCHMARKS (High Overhead)...")
        val results = mutableListOf<BenchmarkResult>()

        // Testing 10%, 25%, 50%, and 100%
        val scaleFactors = listOf(0.10f, 0.25f, 0.50f, 1.0f)

        for (scale in scaleFactors) {
            val scalePct = "${(scale * 100).toInt()}%"

            // 1. SlidingWindow (Cold)
            results.add(benchmark("Cold SlidingWindow ($scalePct)") {
                val blurrer = SlidingWindowBoxBlur()
                val inputCopy = testBitmap.copy(Bitmap.Config.ARGB_8888, false)
                val res = blurrer.blur(inputCopy, SlidingWindowBoxBlur.BlurConfig(scaleFactor = scale))
                blurrer.clearCache() // Destroys cached buffers
                res
            })

            // 2. RenderScript (Cold)
            @Suppress("DEPRECATION")
            results.add(benchmark("Cold RenderScript ($scalePct)") {
                ColdRenderHelpers.renderScriptBlurWithScale(context, testBitmap, BLUR_RADIUS, scale)
            })

            // 3. RenderEffect (Cold)
            if (Build.VERSION.SDK_INT >= 31) {
                results.add(benchmark("Cold RenderEffect ($scalePct)") {
                    ColdRenderHelpers.renderEffectBlur(testBitmap, BLUR_RADIUS, scale)
                })
            }
        }
        printResults("COLD BOOT RESULTS", results)
    }

    // ─────────────────────────────────────────────────────────────────
    // TEST 2: WARM / CACHED (Reuses Contexts and Buffers - Fair Race)
    // ─────────────────────────────────────────────────────────────────
    @Test
    fun benchmarkWarmCached() = runBlocking {
        Log.d(TAG, "🔥 RUNNING WARM CACHED BENCHMARKS (Raw Speed)...")
        val results = mutableListOf<BenchmarkResult>()

        // Testing 10%, 25%, 50%, and 100%
        val scaleFactors = listOf(0.10f, 0.25f, 0.50f, 1.0f)

        for (scale in scaleFactors) {
            val scalePct = "${(scale * 100).toInt()}%"

            // 1. SlidingWindow (Warm)
            val slidingBlur = SlidingWindowBoxBlur()
            results.add(benchmark("Warm SlidingWindow ($scalePct)") {
                val inputCopy = testBitmap.copy(Bitmap.Config.ARGB_8888, false)
                slidingBlur.blur(inputCopy, SlidingWindowBoxBlur.BlurConfig(scaleFactor = scale))
            })
            slidingBlur.clearCache()

            // 2. RenderScript (Warm)
            val warmRS = WarmRenderScriptHelper(context)
            results.add(benchmark("Warm RenderScript ($scalePct)") {
                warmRS.blurWithScale(testBitmap, BLUR_RADIUS, scale)
            })
            warmRS.destroy()

            // 3. RenderEffect (Warm)
            if (Build.VERSION.SDK_INT >= 31) {
                val warmRE = WarmRenderEffectHelper(testBitmap.width, testBitmap.height, scale)
                results.add(benchmark("Warm RenderEffect ($scalePct)") {
                    warmRE.blur(testBitmap, BLUR_RADIUS)
                })
                warmRE.destroy()
            }
        }
        printResults("WARM CACHED RESULTS", results)
    }

    // ─────────────────────────────────────────────────────────────────────
    // TEST 3: VISUAL VERIFICATION (Saves images to device)
    // ─────────────────────────────────────────────────────────────────────
    @Test
    fun saveBlurOutputForVerification() {
        if (!RUN_VISUAL_VERIFICATION) {
            Log.i(TAG, "Skipping visual verification (RUN_VISUAL_VERIFICATION = false)")
            return
        }

        runBlocking {
            Log.d(TAG, "═══════════════════════════════════════════════════════════════════════════════════════════════")
            Log.d(TAG, "                           SAVING BLUR OUTPUTS FOR VERIFICATION                                 ")
            Log.d(TAG, "═══════════════════════════════════════════════════════════════════════════════════════════════")
            Log.d(TAG, "Output: Pictures/blur_test folder (accessible via Gallery or File Manager)")

            // Save original image
            saveImage(testBitmap, "00_original.png")
            Log.d(TAG, "Saved: 00_original.png")

            val slidingBlur = SlidingWindowBoxBlur()
            val scaleFactors = listOf(0.10f, 0.25f, 0.50f, 1.0f)

            for (scale in scaleFactors) {
                val scalePercent = (scale * 100).toInt()

                // SlidingWindowBoxBlur output
                val inputCopy = testBitmap.copy(Bitmap.Config.ARGB_8888, false)
                val slidingResult = slidingBlur.blur(inputCopy, SlidingWindowBoxBlur.BlurConfig(
                    scaleFactor = scale,
                    kernelSize = 9,
                    passes = 2
                ))
                saveImage(slidingResult, "sliding_${scalePercent}pct.png")
                Log.d(TAG, "Saved: sliding_${scalePercent}pct.png")
                slidingResult.recycle()

                // RenderScript output
                @Suppress("DEPRECATION")
                val rsResult = ColdRenderHelpers.renderScriptBlurWithScale(context, testBitmap, BLUR_RADIUS, scale)
                saveImage(rsResult, "renderscript_${scalePercent}pct.png")
                Log.d(TAG, "Saved: renderscript_${scalePercent}pct.png")
                rsResult.recycle()

                // RenderEffect output (API 31+)
                if (Build.VERSION.SDK_INT >= 31) {
                    val reResult = ColdRenderHelpers.renderEffectBlur(testBitmap, BLUR_RADIUS, scale)
                    saveImage(reResult, "rendereffect_${scalePercent}pct.png")
                    Log.d(TAG, "Saved: rendereffect_${scalePercent}pct.png")
                    reResult.recycle()
                }
            }
            slidingBlur.clearCache()

            Log.d(TAG, "═══════════════════════════════════════════════════════════════════════════════════════════════")
            Log.d(TAG, "All images saved to Pictures/blur_test folder!")
            Log.d(TAG, "═══════════════════════════════════════════════════════════════════════════════════════════════")
        }
    }

    private fun saveImage(bitmap: Bitmap, filename: String) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/blur_test")
        }

        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )

        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // BENCHMARK RUNNER & PRINTER
    // ─────────────────────────────────────────────────────────────────
    private suspend fun benchmark(name: String, block: suspend () -> Bitmap): BenchmarkResult {
        val runtime = Runtime.getRuntime()

        // Warmup JIT Compiler
        repeat(WARMUP_ITERATIONS) { block().recycle() }

        System.gc()
        Thread.sleep(100)

        val startCpu = Debug.threadCpuTimeNanos()
        val startHeap = runtime.totalMemory() - runtime.freeMemory()
        val startNative = Debug.getNativeHeapAllocatedSize()
        var peakHeap = startHeap
        var peakNative = startNative

        val times = mutableListOf<Long>()
        repeat(BENCHMARK_ITERATIONS) {
            val time = measureTimeMillis { block().recycle() }
            times.add(time)

            val curHeap = runtime.totalMemory() - runtime.freeMemory()
            val curNative = Debug.getNativeHeapAllocatedSize()
            if (curHeap > peakHeap) peakHeap = curHeap
            if (curNative > peakNative) peakNative = curNative
        }

        val endCpu = Debug.threadCpuTimeNanos()
        return BenchmarkResult(
            name = name,
            averageMs = times.average(), minMs = times.minOrNull() ?: 0, maxMs = times.maxOrNull() ?: 0,
            peakHeapKB = ((peakHeap - startHeap) / 1024).coerceAtLeast(0),
            peakNativeKB = ((peakNative - startNative) / 1024).coerceAtLeast(0),
            cpuTimeMs = (endCpu - startCpu) / 1_000_000
        )
    }

    private fun printResults(title: String, results: List<BenchmarkResult>) {
        Log.d(TAG, "════════════════════════════════════════════════════════════════════════════════════")
        Log.d(TAG, "                           $title")
        Log.d(TAG, "════════════════════════════════════════════════════════════════════════════════════")
        Log.d(TAG, String.format("%-26s %7s %6s %6s %9s %9s %7s", "Method", "Avg", "Min", "Max", "Heap(KB)", "Natv(KB)", "CPU(ms)"))
        Log.d(TAG, "────────────────────────────────────────────────────────────────────────────────────")

        results.sortedBy { it.averageMs }.forEach { r ->
            Log.d(TAG, String.format("%-26s %5.1fms %4dms %4dms %7dKB %8dKB %5dms",
                r.name, r.averageMs, r.minMs, r.maxMs, r.peakHeapKB, r.peakNativeKB, r.cpuTimeMs))
        }
        Log.d(TAG, "════════════════════════════════════════════════════════════════════════════════════\n")
    }

    data class BenchmarkResult(
        val name: String, val averageMs: Double, val minMs: Long, val maxMs: Long,
        val peakHeapKB: Long, val peakNativeKB: Long, val cpuTimeMs: Long
    )
}

// ──────────────────────────────────────────────────────────────────────────────
// HELPER CLASSES FOR WARM / CACHED ALLOCATIONS
// ──────────────────────────────────────────────────────────────────────────────

@Suppress("DEPRECATION")
class WarmRenderScriptHelper(context: Context) {
    private val rs = RenderScript.create(context)
    private val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))

    // Cache allocations to prevent GC thrashing if sizes don't change
    private var cachedInput: Allocation? = null
    private var cachedOutput: Allocation? = null
    private var cachedW = -1; private var cachedH = -1

    fun blurWithScale(bitmap: Bitmap, radius: Float, scaleFactor: Float): Bitmap {
        val smallW = (bitmap.width * scaleFactor).toInt().coerceAtLeast(1)
        val smallH = (bitmap.height * scaleFactor).toInt().coerceAtLeast(1)

        val scaled = if (scaleFactor < 1.0f) Bitmap.createScaledBitmap(bitmap, smallW, smallH, true) else bitmap
        val mutableScaled = if (scaled.isMutable) scaled else scaled.copy(Bitmap.Config.ARGB_8888, true)
        if (scaled != mutableScaled && scaled != bitmap) scaled.recycle()

        if (cachedW != smallW || cachedH != smallH) {
            cachedInput?.destroy()
            cachedOutput?.destroy()
            cachedInput = Allocation.createFromBitmap(rs, mutableScaled)
            cachedOutput = Allocation.createTyped(rs, cachedInput!!.type)
            cachedW = smallW; cachedH = smallH
        } else {
            cachedInput!!.copyFrom(mutableScaled)
        }

        script.setRadius(radius.coerceIn(0.1f, 25f))
        script.setInput(cachedInput)
        script.forEach(cachedOutput)
        cachedOutput!!.copyTo(mutableScaled)

        return if (scaleFactor < 1.0f) {
            Bitmap.createScaledBitmap(mutableScaled, bitmap.width, bitmap.height, true).also { mutableScaled.recycle() }
        } else mutableScaled
    }

    fun destroy() {
        cachedInput?.destroy(); cachedOutput?.destroy()
        script.destroy(); rs.destroy()
    }
}

@RequiresApi(31)
class WarmRenderEffectHelper(originalW: Int, originalH: Int, private val scaleFactor: Float) {
    private val targetW = (originalW * scaleFactor).toInt().coerceAtLeast(1)
    private val targetH = (originalH * scaleFactor).toInt().coerceAtLeast(1)

    private val imageReader = ImageReader.newInstance(targetW, targetH, PixelFormat.RGBA_8888, 1, HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT)
    private val renderNode = RenderNode("BlurNode").apply { setPosition(0, 0, targetW, targetH) }
    private val renderer = HardwareRenderer().apply {
        setContentRoot(renderNode)
        setSurface(imageReader.surface)
    }

    fun blur(bitmap: Bitmap, radius: Float): Bitmap {
        val scaledBitmap = if (scaleFactor < 1.0f) Bitmap.createScaledBitmap(bitmap, targetW, targetH, true) else bitmap
        renderNode.setRenderEffect(RenderEffect.createBlurEffect(radius.coerceIn(0.1f, 150f), radius.coerceIn(0.1f, 150f), Shader.TileMode.CLAMP))

        val canvas = renderNode.beginRecording()
        canvas.drawBitmap(scaledBitmap, 0f, 0f, null)
        renderNode.endRecording()

        renderer.createRenderRequest().setWaitForPresent(true).syncAndDraw()

        val image = imageReader.acquireLatestImage()
        val hardwareBuffer = image.hardwareBuffer
        val resultBitmap = if (hardwareBuffer != null) {
            Bitmap.wrapHardwareBuffer(hardwareBuffer, null)!!.copy(Bitmap.Config.ARGB_8888, false)
        } else scaledBitmap.copy(Bitmap.Config.ARGB_8888, false)

        hardwareBuffer?.close(); image.close()
        if (scaleFactor < 1.0f && scaledBitmap != bitmap) scaledBitmap.recycle()

        return if (scaleFactor < 1.0f) {
            Bitmap.createScaledBitmap(resultBitmap, bitmap.width, bitmap.height, true).also { if (it != resultBitmap) resultBitmap.recycle() }
        } else resultBitmap
    }

    fun destroy() {
        renderer.destroy(); renderNode.discardDisplayList(); imageReader.close()
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// HELPER OBJECT FOR COLD SETUP
// ──────────────────────────────────────────────────────────────────────────────
object ColdRenderHelpers {
    @Suppress("DEPRECATION")
    fun renderScriptBlurWithScale(context: Context, bitmap: Bitmap, radius: Float, scaleFactor: Float): Bitmap {
        val smallW = (bitmap.width * scaleFactor).toInt().coerceAtLeast(1)
        val smallH = (bitmap.height * scaleFactor).toInt().coerceAtLeast(1)
        val scaled = if (scaleFactor < 1.0f) Bitmap.createScaledBitmap(bitmap, smallW, smallH, true) else bitmap
        val mutableScaled = if (scaled.isMutable) scaled else scaled.copy(Bitmap.Config.ARGB_8888, true)
        if (scaled != mutableScaled && scaled != bitmap) scaled.recycle()

        val rs = RenderScript.create(context)
        val input = Allocation.createFromBitmap(rs, mutableScaled)
        val output = Allocation.createTyped(rs, input.type)
        val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        script.setRadius(radius.coerceIn(0.1f, 25f))
        script.setInput(input)
        script.forEach(output)
        output.copyTo(mutableScaled)

        script.destroy(); input.destroy(); output.destroy(); rs.destroy()

        val result = if (scaleFactor < 1.0f) Bitmap.createScaledBitmap(mutableScaled, bitmap.width, bitmap.height, true) else mutableScaled
        if (result != mutableScaled && scaleFactor < 1.0f) mutableScaled.recycle()
        return result
    }

    @RequiresApi(31)
    fun renderEffectBlur(bitmap: Bitmap, radius: Float, scaleFactor: Float): Bitmap {
        val targetW = (bitmap.width * scaleFactor).toInt().coerceAtLeast(1)
        val targetH = (bitmap.height * scaleFactor).toInt().coerceAtLeast(1)
        val scaledBitmap = if (scaleFactor < 1.0f) Bitmap.createScaledBitmap(bitmap, targetW, targetH, true) else bitmap

        val imageReader = ImageReader.newInstance(targetW, targetH, PixelFormat.RGBA_8888, 1, HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT)
        val renderNode = RenderNode("BlurNode").apply {
            setPosition(0, 0, targetW, targetH)
            setRenderEffect(RenderEffect.createBlurEffect(radius.coerceIn(0.1f, 150f), radius.coerceIn(0.1f, 150f), Shader.TileMode.CLAMP))
        }
        val canvas = renderNode.beginRecording()
        canvas.drawBitmap(scaledBitmap, 0f, 0f, null)
        renderNode.endRecording()

        val renderer = HardwareRenderer().apply {
            setContentRoot(renderNode); setSurface(imageReader.surface)
        }
        renderer.createRenderRequest().setWaitForPresent(true).syncAndDraw()

        val image = imageReader.acquireLatestImage()
        val hardwareBuffer = image.hardwareBuffer
        val resultBitmap = if (hardwareBuffer != null) Bitmap.wrapHardwareBuffer(hardwareBuffer, null)!!.copy(Bitmap.Config.ARGB_8888, false) else scaledBitmap.copy(Bitmap.Config.ARGB_8888, false)

        hardwareBuffer?.close(); image.close(); renderer.destroy(); renderNode.discardDisplayList(); imageReader.close()
        if (scaleFactor < 1.0f && scaledBitmap != bitmap) scaledBitmap.recycle()

        return if (scaleFactor < 1.0f) Bitmap.createScaledBitmap(resultBitmap, bitmap.width, bitmap.height, true).also { if (it != resultBitmap) resultBitmap.recycle() } else resultBitmap
    }
}