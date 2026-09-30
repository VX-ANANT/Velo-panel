package com.example.ui.common

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.annotation.RawRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * High-performance, hardware-accelerated looping background video player
 * designed for seamless, stutter-free playback behind glassmorphic UI elements.
 */
@Composable
fun LoopingBackgroundVideo(
    @RawRes videoResId: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(videoResId) {
        onDispose {
            mediaPlayer?.run {
                try {
                    if (isPlaying) stop()
                    release()
                } catch (_: Exception) {}
            }
            mediaPlayer = null
        }
    }

    AndroidView(
        factory = { ctx ->
            TextureView(ctx).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                        try {
                            mediaPlayer?.release()
                            val player = MediaPlayer().apply {
                                val afd = ctx.resources.openRawResourceFd(videoResId)
                                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                                afd.close()
                                setSurface(Surface(surface))
                                isLooping = true
                                setVolume(0f, 0f)
                                setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                                setOnPreparedListener { mp ->
                                    try {
                                        mp.start()
                                    } catch (_: Exception) {}
                                }
                                prepareAsync()
                            }
                            mediaPlayer = player
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        mediaPlayer?.run {
                            try {
                                if (isPlaying) stop()
                                release()
                            } catch (_: Exception) {}
                        }
                        mediaPlayer = null
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                }
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
