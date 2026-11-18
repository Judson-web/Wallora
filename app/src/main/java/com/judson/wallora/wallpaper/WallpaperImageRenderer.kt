package com.judson.wallora.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Renders a selected still image into a screen-shaped bitmap.
 *
 * The renderer bounds decoded source dimensions and output dimensions to avoid
 * unbounded allocations when users select very large photographs.
 */
object WallpaperImageRenderer {
    private const val MAX_DECODE_EDGE = 4096
    private const val MAX_OUTPUT_WIDTH = 1440
    private const val MAX_OUTPUT_HEIGHT = 3200

    fun render(
        context: Context,
        sourceUri: Uri,
        fill: Boolean,
        dim: Float,
        rotationDegrees: Int
    ): Bitmap {
        val decoded = decode(context, sourceUri)
        val rotated = rotate(decoded, rotationDegrees)
        try {
            val metrics = context.resources.displayMetrics
            val screenWidth = max(1, metrics.widthPixels)
            val screenHeight = max(1, metrics.heightPixels)
            val outputScale = min(
                1f,
                min(
                    MAX_OUTPUT_WIDTH.toFloat() / screenWidth,
                    MAX_OUTPUT_HEIGHT.toFloat() / screenHeight
                )
            )
            val outputWidth = max(1, (screenWidth * outputScale).roundToInt())
            val outputHeight = max(1, (screenHeight * outputScale).roundToInt())
            val output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            canvas.drawColor(Color.BLACK)

            val scaleX = outputWidth.toFloat() / rotated.width
            val scaleY = outputHeight.toFloat() / rotated.height
            val scale = if (fill) max(scaleX, scaleY) else min(scaleX, scaleY)
            val drawnWidth = rotated.width * scale
            val drawnHeight = rotated.height * scale
            val destination = RectF(
                (outputWidth - drawnWidth) / 2f,
                (outputHeight - drawnHeight) / 2f,
                (outputWidth + drawnWidth) / 2f,
                (outputHeight + drawnHeight) / 2f
            )
            val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
            canvas.drawBitmap(rotated, null, destination, imagePaint)

            val dimAmount = dim.coerceIn(0f, 0.8f)
            if (dimAmount > 0f) {
                val overlay = Paint().apply {
                    color = Color.argb((dimAmount * 255f).roundToInt(), 0, 0, 0)
                }
                canvas.drawRect(0f, 0f, outputWidth.toFloat(), outputHeight.toFloat(), overlay)
            }
            return output
        } finally {
            if (rotated !== decoded) rotated.recycle()
            decoded.recycle()
        }
    }

    fun writeJpeg(bitmap: Bitmap, output: java.io.OutputStream) {
        check(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)) {
            "The edited image could not be encoded."
        }
    }

    private fun decode(context: Context, uri: Uri): Bitmap {
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(resolver, uri)
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val longestEdge = max(info.size.width, info.size.height)
                if (longestEdge > MAX_DECODE_EDGE) {
                    val factor = MAX_DECODE_EDGE.toFloat() / longestEdge
                    decoder.setTargetSize(
                        max(1, (info.size.width * factor).roundToInt()),
                        max(1, (info.size.height * factor).roundToInt())
                    )
                }
            }
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: error("Unable to open the selected image.")
        check(bounds.outWidth > 0 && bounds.outHeight > 0) { "The selected file is not a readable image." }

        var sampleSize = 1
        while (max(bounds.outWidth, bounds.outHeight) / sampleSize > MAX_DECODE_EDGE) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("Unable to decode the selected image.")
    }

    private fun rotate(source: Bitmap, degrees: Int): Bitmap {
        val normalized = ((degrees % 360) + 360) % 360
        if (normalized == 0) return source
        val matrix = Matrix().apply { postRotate(normalized.toFloat()) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
}
