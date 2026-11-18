package com.judson.wallora

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder

/**
 * Shared Coil pipeline with Android video-frame extraction enabled.
 * This keeps video tiles lightweight while preserving the same URI-based library.
 */
class WalloraApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }
}
