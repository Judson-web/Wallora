package com.judson.wallora.wallpaper

import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

/**
 * Video live-wallpaper renderer with per-video URI, mute, framing, speed and loop settings.
 * Android/OEM wallpaper services still control whether a preview can be committed.
 */
@OptIn(UnstableApi::class)
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
            exo.videoScalingMode = if (prefs.getBoolean("video_fill", true)) {
                C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            } else {
                C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            }
            exo.volume = if (prefs.getBoolean("video_muted", true)) 0f else 1f
            val speed = prefs.getFloat("video_speed", 1f).coerceIn(0.5f, 2f)
            exo.playbackParameters = PlaybackParameters(speed)
            exo.prepare()
            exo.playWhenReady = visible
        }

        private fun releasePlayer() {
            player?.release()
            player = null
        }
    }
}
