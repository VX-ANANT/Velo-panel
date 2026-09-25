package com.example.ui.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * VelorixSoundManager
 * High-performance, multi-stage sound effects engine for Velorix Super Admin & Tournaments.
 *
 * Primary Stage: Hardware-accelerated, low-latency OpenSL / AAudio SoundPool.
 * Secondary Stage: MediaPlayer.create fallback for uncompressed/direct stream playback.
 * Tertiary Stage: System ToneGenerator & Haptic feedback to guarantee perceptible feedback.
 */
class VelorixSoundManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Master volume (0.0f to 1.0f) & Mute state
    var masterVolume: Float = 0.85f
    var isMuted: Boolean = false

    private val soundPool: SoundPool
    private val soundIds = ConcurrentHashMap<Int, Int>()
    private val loadedSounds = ConcurrentHashMap<Int, Boolean>()

    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Throwable) {
            null
        }
    }

    private var toneGen: ToneGenerator? = null

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                soundIds.entries.find { it.value == sampleId }?.let { entry ->
                    loadedSounds[entry.key] = true
                }
            }
        }

        try {
            toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75)
        } catch (_: Throwable) {}

        // Preload sounds
        preloadSound(SOUND_BEAT_TAP, R.raw.sfx_mj_beat_tap)
        preloadSound(SOUND_SNAP_POP, R.raw.sfx_mj_snap_pop)
        preloadSound(SOUND_BRASS_HIT, R.raw.sfx_mj_brass_hit)
        preloadSound(SOUND_ORCHESTRA_HIT, R.raw.sfx_mj_orchestra_hit)
        preloadSound(SOUND_SUBTLE_CHIME, R.raw.sfx_chime_subtle)
    }

    private fun preloadSound(key: Int, resId: Int) {
        try {
            val sId = soundPool.load(appContext, resId, 1)
            soundIds[key] = sId
        } catch (e: Throwable) {
            android.util.Log.e("VelorixSound", "Failed to load sound $key: ${e.message}")
        }
    }

    /**
     * Punchy kick + crisp rimshot tap.
     * Perfect for: Action chips, sending a prompt, category clicks.
     */
    fun playBeatTap(volumeScale: Float = 1.0f) {
        playSound(SOUND_BEAT_TAP, R.raw.sfx_mj_beat_tap, volumeScale, ToneGenerator.TONE_PROP_BEEP)
        triggerHaptic(15)
    }

    /**
     * Finger snap & slap-bass funk pop.
     * Perfect for: Tab switching, toggles, copying text, selecting options.
     */
    fun playSnapPop(volumeScale: Float = 1.0f) {
        playSound(SOUND_SNAP_POP, R.raw.sfx_mj_snap_pop, volumeScale, ToneGenerator.TONE_PROP_ACK)
        triggerHaptic(12)
    }

    /**
     * Synth brass chord stab.
     * Perfect for: Confirming & deploying tournaments to Firebase, major commits.
     */
    fun playBrassHit(volumeScale: Float = 1.0f) {
        playSound(SOUND_BRASS_HIT, R.raw.sfx_mj_brass_hit, volumeScale, ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE)
        triggerHaptic(25)
    }

    /**
     * Staccato orchestral hit.
     * Perfect for: Anti-cheat enforcement, banning a cheater, critical alerts.
     */
    fun playOrchestraHit(volumeScale: Float = 1.0f) {
        playSound(SOUND_ORCHESTRA_HIT, R.raw.sfx_mj_orchestra_hit, volumeScale, ToneGenerator.TONE_PROP_PROMPT)
        triggerHaptic(30)
    }

    /**
     * Subtle glass chime.
     * Perfect for: Action completion, ticket resolved, successful load.
     */
    fun playSubtleChime(volumeScale: Float = 1.0f) {
        playSound(SOUND_SUBTLE_CHIME, R.raw.sfx_chime_subtle, volumeScale, ToneGenerator.TONE_PROP_BEEP2)
        triggerHaptic(14)
    }

    private fun triggerHaptic(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Throwable) {}
    }

    private fun playSound(key: Int, resId: Int, volumeScale: Float, toneFallback: Int) {
        if (isMuted) return
        val finalVol = (masterVolume * volumeScale).coerceIn(0.01f, 1f)

        scope.launch {
            // Stage 1: Try SoundPool
            val sId = soundIds[key]
            var streamId = 0
            if (sId != null && sId > 0) {
                try {
                    streamId = soundPool.play(sId, finalVol, finalVol, 1, 0, 1.0f)
                } catch (_: Throwable) {
                    streamId = 0
                }
            }

            // Stage 2: Fallback to MediaPlayer.create if SoundPool didn't play
            if (streamId == 0) {
                try {
                    val mp = MediaPlayer.create(appContext, resId)
                    if (mp != null) {
                        mp.setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        mp.setVolume(finalVol, finalVol)
                        mp.setOnCompletionListener { player ->
                            try {
                                player.reset()
                                player.release()
                            } catch (_: Throwable) {}
                        }
                        mp.start()
                        return@launch
                    }
                } catch (e: Throwable) {
                    android.util.Log.d("VelorixSound", "MediaPlayer fallback: ${e.message}")
                }

                // Stage 3: ToneGenerator synthesized fallback
                try {
                    toneGen?.startTone(toneFallback, 110)
                } catch (_: Throwable) {}
            }
        }
    }

    fun release() {
        try {
            soundPool.release()
            toneGen?.release()
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
