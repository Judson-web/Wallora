package com.judson.wallora.data

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns

object MediaRepository {
    fun fromDocument(resolver: ContentResolver, uri: Uri): MediaItem {
        val name = resolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else null
        } ?: "Selected media"
        val mime = resolver.getType(uri)
        val kind = when {
            mime == "image/gif" -> MediaKind.GIF
            mime?.startsWith("image/") == true -> MediaKind.IMAGE
            mime?.startsWith("video/") == true -> MediaKind.VIDEO
            else -> MediaKind.UNKNOWN
        }
        return MediaItem(uri, name, mime, kind)
    }

    fun persistReadPermission(resolver: ContentResolver, uri: Uri, flags: Int) {
        val takeFlags = flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        runCatching { resolver.takePersistableUriPermission(uri, takeFlags) }
    }
}
