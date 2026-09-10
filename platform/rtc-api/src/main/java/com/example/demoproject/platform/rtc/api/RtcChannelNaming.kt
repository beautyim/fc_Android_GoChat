package com.example.demoproject.platform.rtc.api

import kotlin.math.absoluteValue

/**
 * Helpers for stable 1v1 channel names and Agora UIDs derived from product user ids.
 * (Inspired by common Agora samples; prefix is BerryCam-specific, not copied from other apps.)
 */
object RtcChannelNaming {

    private const val CHANNEL_PREFIX = "berrycam_vc_"

    /** Same channel string for both peers regardless of who calls whom. */
    fun privateVideoChannel(currentUserId: Long, peerUserId: Long): String {
        val first = minOf(currentUserId, peerUserId)
        val second = maxOf(currentUserId, peerUserId)
        return "${CHANNEL_PREFIX}${first}_${second}"
    }

    fun Long.toRtcUid(): Int {
        if (this in 1L..Int.MAX_VALUE.toLong()) return toInt()
        return hashCode().absoluteValue.takeIf { it != 0 } ?: 1
    }

    fun rtcUid(userId: Long): Int = userId.toRtcUid()
}
