package com.judson.wallora.wallpaper

import android.graphics.Color
import android.graphics.Movie
import java.io.ByteArrayOutputStream
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import kotlin.math.max
import kotlin.math.min

/**
 * Animated GIF live wallpaper renderer.
 *
 * The GIF is decoded from the selected document URI and drawn into the wallpaper
 * surface on a paced frame loop. Files are capped at 32 MB and 16 megapixels
 * before they are accepted. The platform's preview/confirmation UI remains
 * responsible for applying the wallpaper.
 */
@Suppress("DEPRECATION")
class GifWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = GifEngine()

    private inner class GifEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var movie: Movie? = null
        private var movieStartedAt = 0L
        private var visible = false
        private var surfaceWidth = 0
        private var surfaceHeight = 0
        private var fill = true

        private val frameTask = object : Runnable {
            override fun run() {
                if (!visible || movie == null) {
                    handler.removeCallbacks(this)
                    return
                }
                drawFrame()
                if (visible && movie != null) handler.postDelayed(this, FRAME_INTERVAL_MS)
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            loadMovie()
            drawFrame()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceWidth = width
            surfaceHeight = height
            drawFrame()
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            handler.removeCallbacks(frameTask)
            if (isVisible) {
                if (movie == null) loadMovie()
                if (movie != null) {
                    movieStartedAt = SystemClock.uptimeMillis()
                    handler.post(frameTask)
                }
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            handler.removeCallbacks(frameTask)
            movie = null
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            handler.removeCallbacks(frameTask)
            movie = null
            super.onDestroy()
        }

        private fun loadMovie() {
            val prefs = getSharedPreferences(PREFERENCES, MODE_PRIVATE)
            val uri = prefs.getString(ACTIVE_GIF_URI, null)?.let(Uri::parse) ?: return
            fill = prefs.getBoolean(GIF_FILL, true)
            val declaredSize = runCatching {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val sizeColumn = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (cursor.moveToFirst() && sizeColumn >= 0 && !cursor.isNull(sizeColumn)) {
                        cursor.getLong(sizeColumn)
                    } else null
                }
            }.getOrNull()
            if (declaredSize != null && declaredSize > MAX_GIF_BYTES) return

            // Some document providers omit OpenableColumns.SIZE. Enforce the
            // same cap while streaming so a missing size cannot trigger a huge allocation.
            val encoded = runCatching {
                contentResolver.openInputStream(uri)?.use { input ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(16 * 1024)
                    var total = 0
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > MAX_GIF_BYTES) error("GIF exceeds the 32 MB safety limit.")
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                }
            }.getOrElse {
                movie = null
                return
            } ?: return
            val decoded = runCatching {
                Movie.decodeByteArray(encoded, 0, encoded.size)
            }.getOrNull()
            movie = decoded?.takeIf {
                it.width() > 0 && it.height() > 0 &&
                    it.width().toLong() * it.height().toLong() <= MAX_GIF_PIXELS
            }
            movieStartedAt = SystemClock.uptimeMillis()
        }

        private fun drawFrame() {
            val currentMovie = movie ?: return
            val holder = surfaceHolder
            val canvas = runCatching { holder.lockCanvas() }.getOrNull() ?: return
            try {
                val width = if (surfaceWidth > 0) surfaceWidth else canvas.width
                val height = if (surfaceHeight > 0) surfaceHeight else canvas.height
                if (width <= 0 || height <= 0) return

                canvas.drawColor(Color.BLACK)
                val sourceWidth = max(1, currentMovie.width())
                val sourceHeight = max(1, currentMovie.height())
                val scaleX = width.toFloat() / sourceWidth
                val scaleY = height.toFloat() / sourceHeight
                val scale = if (fill) max(scaleX, scaleY) else min(scaleX, scaleY)
                val drawnWidth = sourceWidth * scale
                val drawnHeight = sourceHeight * scale
                val left = (width - drawnWidth) / 2f
                val top = (height - drawnHeight) / 2f

                val duration = max(1, currentMovie.duration())
                val elapsed = (SystemClock.uptimeMillis() - movieStartedAt).coerceAtLeast(0L)
                val frameTime = (elapsed % duration).toInt()
                currentMovie.setTime(frameTime)

                canvas.save()
                canvas.translate(left, top)
                canvas.scale(scale, scale)
                currentMovie.draw(canvas, 0f, 0f)
                canvas.restore()
            } finally {
                runCatching { holder.unlockCanvasAndPost(canvas) }
            }
        }
    }

    companion object {
        private const val FRAME_INTERVAL_MS = 33L
        private const val MAX_GIF_BYTES = 32L * 1024L * 1024L
        private const val MAX_GIF_PIXELS = 16_000_000L
        private const val PREFERENCES = "wallora"
        private const val ACTIVE_GIF_URI = "active_gif_uri"
        private const val GIF_FILL = "gif_fill"
    }
}
