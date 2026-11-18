package com.judson.wallora.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local-only persistence for imported document URIs.
 * Wallora stores references and metadata, not copies of the user's media.
 */
object MediaLibraryStore {
    private const val PREFS = "wallora_library"
    private const val ITEMS = "items"

    fun load(context: Context): List<MediaItem> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(ITEMS, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val value = array.getJSONObject(index)
                    val kind = runCatching { MediaKind.valueOf(value.optString("kind", "UNKNOWN")) }
                        .getOrDefault(MediaKind.UNKNOWN)
                    add(
                        MediaItem(
                            uri = Uri.parse(value.getString("uri")),
                            displayName = value.optString("name", "Wallpaper"),
                            mimeType = value.optString("mime").takeIf { it.isNotBlank() },
                            kind = kind,
                            isFavorite = value.optBoolean("favorite", false)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun add(context: Context, item: MediaItem) {
        val current = load(context).filterNot { it.uri == item.uri }
        save(context, current + item)
    }

    fun addAll(context: Context, incoming: List<MediaItem>) {
        val merged = LinkedHashMap<String, MediaItem>()
        load(context).forEach { merged[it.uri.toString()] = it }
        incoming.forEach { merged[it.uri.toString()] = it }
        save(context, merged.values.toList())
    }

    fun toggleFavorite(context: Context, uri: Uri) {
        save(context, load(context).map { if (it.uri == uri) it.copy(isFavorite = !it.isFavorite) else it })
    }

    fun remove(context: Context, uri: Uri) {
        save(context, load(context).filterNot { it.uri == uri })
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    private fun save(context: Context, items: List<MediaItem>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put("uri", item.uri.toString())
                    .put("name", item.displayName)
                    .put("mime", item.mimeType ?: "")
                    .put("kind", item.kind.name)
                    .put("favorite", item.isFavorite)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(ITEMS, array.toString())
            .apply()
    }
}
