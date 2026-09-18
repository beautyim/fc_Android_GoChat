package com.example.demoproject.product.call

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.example.demoproject.platform.common.log.AppLogger

/** Which ringing sound to loop for the current call phase. */
enum class CallRingtoneKind {
    /** Callee side: an incoming call is ringing. */
    Incoming,

    /** Caller side: waiting for the peer to pick up. */
    Outgoing,
}

/**
 * Loops a call-ringing sound effect for as long as a call is in the "ringing" state,
 * covering both the outgoing "dialing" wait and the incoming "ringing" screen.
 * Sounds are bundled app assets rather than the device system ringtone, so playback
 * is consistent across devices.
 */
class CallRingtonePlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var activeKind: CallRingtoneKind? = null

    fun start(kind: CallRingtoneKind) {
        if (activeKind == kind && mediaPlayer != null) return
        stop()
        activeKind = kind
        mediaPlayer = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                context.resources.openRawResourceFd(kind.toRawResId()).use { afd ->
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }
                isLooping = true
                setOnPreparedListener { it.start() }
                setOnErrorListener { _, what, extra ->
                    AppLogger.w(TAG, "ringtone playback error what=$what extra=$extra")
                    true
                }
                prepareAsync()
            }
        }.onFailure { error ->
            AppLogger.w(TAG, "failed to start ringtone($kind): ${error.message}")
            activeKind = null
        }.getOrNull()
    }

    fun stop() {
        val player = mediaPlayer ?: return
        mediaPlayer = null
        activeKind = null
        runCatching {
            if (player.isPlaying) player.stop()
        }
        runCatching { player.release() }
    }

    private fun CallRingtoneKind.toRawResId(): Int = when (this) {
        CallRingtoneKind.Incoming -> R.raw.call_ring_incoming
        CallRingtoneKind.Outgoing -> R.raw.call_ring_outgoing
    }

    private companion object {
        const val TAG = "CallRingtonePlayer"
    }
}
