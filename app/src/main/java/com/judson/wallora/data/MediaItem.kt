package com.judson.wallora.data

import android.net.Uri

enum class MediaKind { IMAGE, VIDEO, GIF, UNKNOWN }

data class MediaItem(
    val uri: Uri,
    val displayName: String,
    val mimeType: String?,
    val kind: MediaKind
)
