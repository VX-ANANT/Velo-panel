package com.example.ui.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * VelorixSoundManager
 * High-performance, low-latency audio sound effects engine for Velorix Super Admin & Tournaments.
 *
 * Uses direct PCM AudioTrack playback instead of SoundPool to completely avoid MediaCodec / OMX
 * component interface queries on virtual devices and cloud emulators.
 */
class VelorixSoundManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Cached raw PCM byte arrays in memory (total < 250 KB)
    private val pcmCache = ConcurrentHashMap<Int, ByteArray>()

    // Track pools for polyphonic, instant audio playback
    private val activeTracks = ConcurrentHashMap<Int, AudioTrack>()

    // Volume level: soft and tasteful (0.0 to 1.0)
    var masterVolume: Float = 0.50f
    var isMuted: Boolean = false

    init {
        // Pre-load PCM byte buffers in IO coroutine to ensure 0ms latency on first click
        scope.launch {
            loadPcmBuffer(SOUND_BEAT_TAP, R.raw.sfx_mj_beat_tap)
            loadPcmBuffer(SOUND_SNAP_POP, R.raw.sfx_mj_snap_pop)
            loadPcmBuffer(SOUND_BRASS_HIT, R.raw.sfx_mj_brass_hit)
            loadPcmBuffer(SOUND_ORCHESTRA_HIT, R.raw.sfx_mj_orchestra_hit)
            loadPcmBuffer(SOUND_SUBTLE_CHIME, R.raw.sfx_chime_subtle)
        }
    }

    private fun loadPcmBuffer(soundId: Int, resId: Int) {
        try {
            val inputStream: InputStream = appContext.resources.openRawResource(resId)
            val bytes = inputStream.use { it.readBytes() }
            val pcmData = extractPcmData(bytes)
            pcmCache[soundId] = pcmData
        } catch (e: Throwable) {
            android.util.Log.d("VelorixSoundManager", "Audio buffer preload skipped: ${e.message}")
        }
    }

    /**
     * Extracts raw PCM 16-bit audio data by finding the 'data' chunk in standard WAV files.
     */
    private fun extractPcmData(bytes: ByteArray): ByteArray {
        for (i in 0 until minOf(bytes.size - 8, 100)) {
            if (bytes[i] == 'd'.code.toByte() &&
                bytes[i + 1] == 'a'.code.toByte() &&
                bytes[i + 2] == 't'.code.toByte() &&
                bytes[i + 3] == 'a'.code.toByte()
            ) {
                val offset = i + 8
                if (offset < bytes.size) {
                    return bytes.copyOfRange(offset, bytes.size)
                }
            }
        }
        return if (bytes.size > 44) bytes.copyOfRange(44, bytes.size) else bytes
    }

    /**
     * Billie Jean-inspired acoustic punchy kick + crisp rimshot tap.
     * Perfect for: Action chips, sending a prompt, category clicks.
     */
    fun playBeatTap(volumeScale: Float = 1.0f) {
        playSound(SOUND_BEAT_TAP, R.raw.sfx_mj_beat_tap, volumeScale)
    }

    /**
     * Smooth Criminal / Bad inspired finger snap & slap-bass funk pop.
     * Perfect for: Tab switching, toggles, copying text, selecting options.
     */
    fun playSnapPop(volumeScale: Float = 1.0f) {
        playSound(SOUND_SNAP_POP, R.raw.sfx_mj_snap_pop, volumeScale)
    }

    /**
     * Quincy Jones / 80s Thriller synth brass chord stab (Em9).
     * Perfect for: Confirming & deploying tournaments to Firebase, major commits.
     */
    fun playBrassHit(volumeScale: Float = 1.0f) {
        playSound(SOUND_BRASS_HIT, R.raw.sfx_mj_brass_hit, volumeScale)
    }

    /**
     * Staccato orchestral hit.
     * Perfect for: Anti-cheat enforcement, banning a cheater, critical alerts.
     */
    fun playOrchestraHit(volumeScale: Float = 1.0f) {
        playSound(SOUND_ORCHESTRA_HIT, R.raw.sfx_mj_orchestra_hit, volumeScale)
    }

    /**
     * Subtle glass chime.
     * Perfect for: Action completion, ticket resolved, successful load.
     */
    fun playSubtleChime(volumeScale: Float = 1.0f) {
        playSound(SOUND_SUBTLE_CHIME, R.raw.sfx_chime_subtle, volumeScale)
    }

    private fun playSound(soundId: Int, resId: Int, volumeScale: Float) {
        if (isMuted) return
        val finalVol = (masterVolume * volumeScale).coerceIn(0f, 1f)
        if (finalVol <= 0.001f) return

        scope.launch {
            try {
                var pcm = pcmCache[soundId]
                if (pcm == null) {
                    loadPcmBuffer(soundId, resId)
                    pcm = pcmCache[soundId]
                }
                if (pcm == null || pcm.isEmpty()) return@launch

                // Clean up previous track for this sound slot if finished or playing
                val existing = activeTracks[soundId]
                if (existing != null) {
                    try {
                        if (existing.playState == AudioTrack.PLAYSTATE_PLAYING) {
                            existing.stop()
                        }
                        existing.release()
                    } catch (_: Throwable) {}
                }

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val audioFormat = AudioFormat.Builder()
                    .setSampleRate(44100)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()

                val track = AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(pcm.size)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                if (track.state == AudioTrack.STATE_INITIALIZED) {
                    track.write(pcm, 0, pcm.size)
                    track.setVolume(finalVol)
                    track.play()
                    activeTracks[soundId] = track
                } else {
                    track.release()
                }
            } catch (t: Throwable) {
                // Audio subsystem unavailable or muted in headless container - silent fallback
                android.util.Log.d("VelorixSoundManager", "Playback handled safely: ${t.message}")
            }
        }
    }

    fun release() {
        try {
            activeTracks.values.forEach { track ->
                try {
                    track.stop()
                    track.release()
                } catch (_: Throwable) {}
            }
            activeTracks.clear()
            pcmCache.clear()
        } catch (_: Throwable) {}
    }

    companion object {
        const val SOUND_BEAT_TAP = 1
        const val SOUND_SNAP_POP = 2
        const val SOUND_BRASS_HIT = 3
        const val SOUND_ORCHESTRA_HIT = 4
        const val SOUND_SUBTLE_CHIME = 5

        @Volatile
        private var INSTANCE: VelorixSoundManager? = null

        fun getInstance(context: Context): VelorixSoundManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VelorixSoundManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

/**
 * Convenient remember Composable to access VelorixSoundManager in any screen.
 */
@Composable
fun rememberVelorixSoundManager(): VelorixSoundManager {
    val context = LocalContext.current
    return remember { VelorixSoundManager.getInstance(context) }
}
