package com.judson.wallora.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile

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

    /**
     * Recursively indexes supported media inside a user-granted Storage Access
     * Framework tree. A bounded depth and item count protect against accidentally
     * scanning enormous or cyclic provider trees.
     */
    fun scanTree(context: Context, treeUri: Uri, maxItems: Int = 2500): List<MediaItem> {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: error("The selected folder is no longer available.")
        check(root.canRead()) { "Wallora no longer has access to this folder." }

        val indexed = LinkedHashMap<String, MediaItem>()
        fun visit(directory: DocumentFile, depth: Int) {
            if (depth > 12 || indexed.size >= maxItems) return
            val children = runCatching { directory.listFiles() }.getOrDefault(emptyArray())
            children.forEach { child ->
                if (indexed.size >= maxItems) return
                val name = child.name ?: return@forEach
                if (name.startsWith(".")) return@forEach

                when {
                    child.isDirectory -> visit(child, depth + 1)
                    child.isFile -> {
                        val mime = runCatching { context.contentResolver.getType(child.uri) }
                            .getOrNull()
                        if (mime?.startsWith("image/") == true || mime?.startsWith("video/") == true) {
                            runCatching { fromDocument(context.contentResolver, child.uri) }
                                .getOrNull()
                                ?.takeIf { it.kind != MediaKind.UNKNOWN }
                                ?.let { indexed[it.uri.toString()] = it }
                        }
                    }
                }
            }
        }

        visit(root, 0)
        return indexed.values.toList()
    }
}
