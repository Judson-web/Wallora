package com.judson.wallora.wallpaper

import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Minimal video live-wallpaper renderer. Production work should add persisted URI
 * selection, decoder capability checks, configurable scaling, frame pacing,
 * audio policy, error recovery and a dedicated settings screen.
 */
class VideoWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = VideoEngine()

    private inner class VideoEngine : Engine() {
        private var player: ExoPlayer? = null
        private var visible = false

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            createPlayer(holder)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            player?.setVideoSurface(holder.surface)
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            player?.playWhenReady = isVisible
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            player?.clearVideoSurface()
            releasePlayer()
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            releasePlayer()
            super.onDestroy()
        }

        private fun createPlayer(holder: SurfaceHolder) {
            if (player != null) return
            val prefs = getSharedPreferences("wallora", MODE_PRIVATE)
            val uriString = prefs.getString("active_video_uri", null) ?: return
            val exo = ExoPlayer.Builder(this@VideoWallpaperService).build()
            player = exo
            exo.setVideoSurface(holder.surface)
            exo.setMediaItem(MediaItem.fromUri(uriString))
            exo.repeatMode = Player.REPEAT_MODE_ONE
            exo.volume = if (prefs.getBoolean("video_muted", true)) 0f else 1f
            exo.prepare()
            exo.playWhenReady = visible
        }

        private fun releasePlayer() {
            player?.release()
            player = null
        }
    }
}
