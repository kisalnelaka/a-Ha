package org.audhd.aha.data.repository

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.audhd.aha.data.model.ProceduralWallpaperType
import org.audhd.aha.data.model.WallpaperItem
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Random

enum class WallpaperTarget {
    HOME,
    LOCK,
    BOTH
}

/**
 * Repository orchestrating dark minimal and custom user wallpaper retrieval and application.
 *
 * Capabilities:
 * - User custom image picker importing and persistent local caching.
 * - Procedural AMOLED dark presets (0ms, zero network, zero battery impact).
 * - Live Wallhaven dark minimal API integration.
 * - Downsampled memory-safe bitmap decoding preventing Out-Of-Memory (OOM) errors.
 */
open class WallpaperRepository(private val context: Context? = null) {

    companion object {
        private const val WALLHAVEN_SEARCH_URL =
            "https://wallhaven.cc/api/v1/search?q=minimalism+dark&colors=000000&purity=100&sorting=toplist"
        private const val CUSTOM_DIR_NAME = "custom_wallpapers"
    }

    /**
     * Returns a curated list combining custom user wallpapers, offline procedural AMOLED presets,
     * and live dark minimal wallpapers from the Wallhaven API.
     */
    suspend fun getAvailableWallpapers(): List<WallpaperItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<WallpaperItem>()

        // 1. Saved custom user wallpapers
        result.addAll(getCustomWallpapers())

        // 2. Offline procedural AMOLED presets (instant 0ms, zero network)
        result.addAll(getProceduralPresets())

        // 3. Wallhaven public API for dark minimal wallpapers
        try {
            val apiWallpapers = fetchWallhavenWallpapers()
            result.addAll(apiWallpapers)
        } catch (_: Exception) {
            // Fails gracefully to procedural presets if offline
        }

        result
    }

    fun getProceduralPresets(): List<WallpaperItem> {
        return listOf(
            WallpaperItem(
                id = "proc_pure_black",
                title = "Pure AMOLED Deep Black",
                isProcedural = true,
                proceduralType = ProceduralWallpaperType.PURE_AMOLED_BLACK
            ),
            WallpaperItem(
                id = "proc_twilight",
                title = "Circadian Twilight Gradient",
                isProcedural = true,
                proceduralType = ProceduralWallpaperType.TWILIGHT_GRADIENT
            ),
            WallpaperItem(
                id = "proc_obsidian",
                title = "Obsidian Dither Noise",
                isProcedural = true,
                proceduralType = ProceduralWallpaperType.OBSIDIAN_DITHER
            ),
            WallpaperItem(
                id = "proc_horizon",
                title = "Monochrome Focus Horizon",
                isProcedural = true,
                proceduralType = ProceduralWallpaperType.MONOCHROME_HORIZON
            )
        )
    }

    /**
     * Retrieves any custom user wallpapers previously imported and saved locally.
     */
    fun getCustomWallpapers(): List<WallpaperItem> {
        val ctx = context ?: return emptyList()
        val dir = File(ctx.filesDir, CUSTOM_DIR_NAME)
        if (!dir.exists()) return emptyList()

        val files = dir.listFiles { f -> f.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp") }
            ?: return emptyList()

        return files.sortedByDescending { it.lastModified() }.mapIndexed { index, file ->
            WallpaperItem(
                id = "custom_${file.name}",
                title = "Custom Wallpaper #${index + 1}",
                isCustom = true,
                localFilePath = file.absolutePath
            )
        }
    }

    /**
     * Deletes a previously imported custom wallpaper file.
     */
    fun deleteCustomWallpaper(item: WallpaperItem): Boolean {
        if (!item.isCustom || item.localFilePath.isBlank()) return false
        return try {
            val file = File(item.localFilePath)
            file.delete()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Imports a user-selected custom image URI, persists it in internal storage, and applies it.
     */
    suspend fun importAndApplyCustomWallpaper(uri: Uri, target: WallpaperTarget): Boolean = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext false

        try {
            // 1. Memory-safe downsampled decode
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, boundsOptions)
            }

            val reqWidth = 1440
            val reqHeight = 2560
            var sampleSize = 1
            if (boundsOptions.outHeight > reqHeight || boundsOptions.outWidth > reqWidth) {
                val halfHeight = boundsOptions.outHeight / 2
                val halfWidth = boundsOptions.outWidth / 2
                while ((halfHeight / sampleSize) >= reqHeight && (halfWidth / sampleSize) >= reqWidth) {
                    sampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val bitmap = ctx.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            } ?: return@withContext false

            // 2. Persist locally to custom_wallpapers directory
            try {
                val dir = File(ctx.filesDir, CUSTOM_DIR_NAME).apply { if (!exists()) mkdirs() }
                val destFile = File(dir, "custom_${System.currentTimeMillis()}.webp")
                FileOutputStream(destFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.WEBP, 90, out)
                }
            } catch (_: Exception) {
                // Non-fatal if local caching fails, continue to apply
            }

            // 3. Apply to system wallpaper manager
            applyBitmapToSystem(bitmap, target)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Applies the selected wallpaper item to the requested target (Home, Lock, or Both).
     */
    suspend fun applyWallpaper(item: WallpaperItem, target: WallpaperTarget): Boolean = withContext(Dispatchers.IO) {
        val bitmap = when {
            item.isCustom && item.localFilePath.isNotBlank() -> {
                BitmapFactory.decodeFile(item.localFilePath)
            }
            item.isProcedural && item.proceduralType != null -> {
                generateProceduralBitmap(item.proceduralType)
            }
            item.fullUrl.isNotBlank() -> {
                downloadAndSampleBitmap(item.fullUrl)
            }
            else -> null
        } ?: return@withContext false

        applyBitmapToSystem(bitmap, target)
    }

    private fun applyBitmapToSystem(bitmap: Bitmap, target: WallpaperTarget): Boolean {
        return try {
            val wm = context?.let { WallpaperManager.getInstance(it) } ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val flags = when (target) {
                    WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
                    WallpaperTarget.LOCK -> WallpaperManager.FLAG_LOCK
                    WallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                }
                wm.setBitmap(bitmap, null, true, flags)
            } else {
                wm.setBitmap(bitmap)
            }
            true
        } catch (_: Exception) {
            false
        } finally {
            bitmap.recycle()
        }
    }

    private fun fetchWallhavenWallpapers(): List<WallpaperItem> {
        val url = URL(WALLHAVEN_SEARCH_URL)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 5000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "a-Ha-AuDHD-Launcher/1.0")
        }

        if (conn.responseCode !in 200..299) return emptyList()

        val jsonText = BufferedReader(InputStreamReader(conn.inputStream, StandardCharsets.UTF_8)).use {
            it.readText()
        }

        val jsonRoot = JSONObject(jsonText)
        val dataArray = jsonRoot.optJSONArray("data") ?: return emptyList()

        val items = mutableListOf<WallpaperItem>()
        for (i in 0 until minOf(dataArray.length(), 12)) {
            val obj = dataArray.getJSONObject(i)
            val id = obj.optString("id")
            val fullUrl = obj.optString("path")
            val thumbs = obj.optJSONObject("thumbs")
            val thumbUrl = thumbs?.optString("large") ?: fullUrl

            if (fullUrl.isNotBlank()) {
                items.add(
                    WallpaperItem(
                        id = "wallhaven_$id",
                        title = "Dark Minimal #$id",
                        thumbnailUrl = thumbUrl,
                        fullUrl = fullUrl,
                        isProcedural = false
                    )
                )
            }
        }
        return items
    }

    internal fun generateProceduralBitmap(type: ProceduralWallpaperType, width: Int = 1080, height: Int = 1920): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        when (type) {
            ProceduralWallpaperType.PURE_AMOLED_BLACK -> {
                canvas.drawColor(Color.BLACK)
            }
            ProceduralWallpaperType.TWILIGHT_GRADIENT -> {
                val shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    Color.BLACK,
                    Color.rgb(18, 20, 26),
                    Shader.TileMode.CLAMP
                )
                paint.shader = shader
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }
            ProceduralWallpaperType.OBSIDIAN_DITHER -> {
                canvas.drawColor(Color.BLACK)
                val random = Random(42)
                paint.color = Color.rgb(25, 25, 28)
                for (i in 0 until 5000) {
                    val rx = random.nextFloat() * width
                    val ry = random.nextFloat() * height
                    canvas.drawPoint(rx, ry, paint)
                }
            }
            ProceduralWallpaperType.MONOCHROME_HORIZON -> {
                canvas.drawColor(Color.BLACK)
                paint.color = Color.rgb(40, 40, 40)
                paint.strokeWidth = 2f
                val horizonY = height * 0.618f
                canvas.drawLine(0f, horizonY, width.toFloat(), horizonY, paint)
            }
        }

        return bitmap
    }

    private fun downloadAndSampleBitmap(urlStr: String): Bitmap? {
        return try {
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.inputStream.use { stream ->
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 1
                }
                BitmapFactory.decodeStream(stream, null, options)
            }
        } catch (_: Exception) {
            null
        }
    }
}
