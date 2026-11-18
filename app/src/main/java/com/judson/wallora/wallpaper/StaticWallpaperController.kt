package com.judson.wallora.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.net.Uri

object StaticWallpaperController {
    fun apply(context: Context, uri: Uri, target: Int): Result<Unit> = runCatching {
        val manager = WallpaperManager.getInstance(context)
        check(manager.isWallpaperSupported) { "Wallpaper service is not supported on this device." }
        check(manager.isSetWallpaperAllowed) { "The device administrator or system policy prevents changing wallpaper." }
        val stream = requireNotNull(context.contentResolver.openInputStream(uri)) {
            "Unable to open selected image."
        }
        stream.use { manager.setStream(it, null, true, target) }
        Unit
    }
}
