package com.judson.wallora.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.net.Uri

object StaticWallpaperController {
    fun apply(context: Context, uri: Uri, target: Int): Result<Unit> = runCatching {
        val manager = WallpaperManager.getInstance(context)
        check(manager.isWallpaperSupported) { "Wallpaper service is not supported on this device." }
        check(manager.isSetWallpaperAllowed) { "The device administrator or system policy prevents changing wallpaper." }
        context.contentResolver.openInputStream(uri).use { stream ->
            requireNotNull(stream) { "Unable to open selected image." }
            manager.setStream(stream, null, true, target)
        }
    }
}
