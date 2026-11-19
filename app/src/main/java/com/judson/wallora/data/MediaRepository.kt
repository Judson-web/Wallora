package com.judson.wallora.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.util.ArrayDeque
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

object MediaRepository {
    private val SUPPORTED_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "webp", "bmp", "heic", "heif", "avif",
        "gif", "mp4", "m4v", "mkv", "webm", "mov", "3gp", "ts"
    )

    fun fromDocument(resolver: ContentResolver, uri: Uri): MediaItem {
        val name = resolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else null
        } ?: "Selected media"
        return createItem(uri, name, resolver.getType(uri))
    }

    fun fromFile(file: File): MediaItem {
        val extension = file.extension.lowercase()
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        return createItem(Uri.fromFile(file), file.name, mime)
    }

    private fun createItem(uri: Uri, name: String, mime: String?): MediaItem {
        val extension = name.substringAfterLast('.', "").lowercase()
        val kind = when {
            mime == "image/gif" || extension == "gif" -> MediaKind.GIF
            mime?.startsWith("image/") == true ||
                extension in setOf("jpg", "jpeg", "png", "webp", "bmp", "heic", "heif", "avif") ->
                MediaKind.IMAGE
            mime?.startsWith("video/") == true ||
                extension in setOf("mp4", "m4v", "mkv", "webm", "mov", "3gp", "ts") ->
                MediaKind.VIDEO
            else -> MediaKind.UNKNOWN
        }
        return MediaItem(uri, name, mime, kind)
    }

    fun persistReadPermission(resolver: ContentResolver, uri: Uri, flags: Int) {
        val takeFlags = flags and
            (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        runCatching { resolver.takePersistableUriPermission(uri, takeFlags) }
    }

    /**
     * Recursively indexes supported media inside a user-granted SAF folder.
     * It traverses nested folders without an arbitrary media-count ceiling.
     */
    suspend fun scanTree(context: Context, treeUri: Uri): List<MediaItem> {
        val coroutineContext = currentCoroutineContext()
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: error("The selected folder is no longer available.")
        check(root.canRead()) { "Wallora no longer has access to this folder." }

        val indexed = LinkedHashMap<String, MediaItem>()
        fun visit(directory: DocumentFile, depth: Int) {
            if (depth > 32) return
            val children = runCatching { directory.listFiles() }.getOrDefault(emptyArray())
            children.forEach { child ->
                coroutineContext.ensureActive()
                val name = child.name ?: return@forEach
                when {
                    child.isDirectory -> visit(child, depth + 1)
                    child.isFile -> {
                        val mime = runCatching { context.contentResolver.getType(child.uri) }
                            .getOrNull()
                        val extension = name.substringAfterLast('.', "").lowercase()
                        if (mime?.startsWith("image/") == true ||
                            mime?.startsWith("video/") == true ||
                            extension in SUPPORTED_EXTENSIONS
                        ) {
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

    /**
     * Indexes all readable shared-storage volumes when Android's storage access
     * has been granted to this sideloaded app. Android still blocks protected
     * app-private Android/data and Android/obb directories.
     *
     * The traversal is iterative, cancellable, de-duplicates mounted volumes,
     * and reports progress without imposing an arbitrary file-count limit.
     */
    suspend fun scanAllSharedStorage(
        context: Context,
        onProgress: (Int) -> Unit = {}
    ): List<MediaItem> {
        val coroutineContext = currentCoroutineContext()
        val roots = LinkedHashMap<String, File>()

        fun addRoot(candidate: File?) {
            if (candidate == null) return
            val canonical = runCatching { candidate.canonicalFile }.getOrNull() ?: return
            if (canonical.exists() && canonical.isDirectory) roots[canonical.path] = canonical
        }

        addRoot(Environment.getExternalStorageDirectory())

        // Discover removable/shared volumes from their app-specific mount paths.
        context.getExternalFilesDirs(null).filterNotNull().forEach { appFiles ->
            var current = runCatching { appFiles.canonicalFile }.getOrNull() ?: return@forEach
            while (current.parentFile != null && !current.name.equals("Android", ignoreCase = true)) {
                current = current.parentFile ?: break
            }
            if (current.name.equals("Android", ignoreCase = true)) {
                addRoot(current.parentFile)
            }
        }

        check(roots.isNotEmpty()) { "No readable shared-storage volume was found." }

        val pending = ArrayDeque<File>()
        roots.values.forEach(pending::addLast)
        val visitedDirectories = HashSet<String>()
        val indexed = LinkedHashMap<String, MediaItem>()

        while (pending.isNotEmpty()) {
            coroutineContext.ensureActive()
            val directory = pending.removeLast()
            val canonicalDirectory = runCatching { directory.canonicalFile }.getOrNull() ?: continue
            if (!visitedDirectories.add(canonicalDirectory.path)) continue

            val children = runCatching { canonicalDirectory.listFiles() }.getOrNull() ?: continue
            for (child in children) {
                coroutineContext.ensureActive()
                if (child.isDirectory) {
                    if (!isAndroidProtectedDirectory(child)) pending.addLast(child)
                    continue
                }
                if (!child.isFile || !child.canRead()) continue
                val extension = child.extension.lowercase()
                val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                if (extension !in SUPPORTED_EXTENSIONS &&
                    mime?.startsWith("image/") != true && mime?.startsWith("video/") != true
                ) continue

                val item = runCatching { fromFile(child) }.getOrNull()
                    ?.takeIf { it.kind != MediaKind.UNKNOWN } ?: continue
                indexed[item.uri.toString()] = item
                if (indexed.size % 100 == 0) onProgress(indexed.size)
            }
        }

        onProgress(indexed.size)
        return indexed.values.toList()
    }

    private fun isAndroidProtectedDirectory(directory: File): Boolean {
        val parent = directory.parentFile ?: return false
        return parent.name.equals("Android", ignoreCase = true) &&
            (directory.name.equals("data", ignoreCase = true) ||
                directory.name.equals("obb", ignoreCase = true))
    }
}
