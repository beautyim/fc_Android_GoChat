package com.example.demoproject.platform.data.network.dto

import com.example.demoproject.platform.network.serializer.LenientBooleanSerializer
import com.example.demoproject.platform.network.serializer.LenientIntSerializer
import com.example.demoproject.platform.network.serializer.LenientLongSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `POST private-album/check` body.
 *
 * [mtime] should be the chat message timestamp when checking from a private chat bubble.
 */
@Serializable
data class PrivateAlbumCheckRequestDto(
    @Serializable(with = LenientLongSerializer::class)
    @SerialName("coach_uid") val coachUid: Long,
    @Serializable(with = LenientLongSerializer::class)
    @SerialName("media_id") val mediaId: Long,
    @Serializable(with = LenientLongSerializer::class)
    val mtime: Long = 0L,
)

/**
 * `POST private-album/check` success payload.
 *
 * Unlock-detail fields vary by media state; common album/message keys are optional.
 * Counts / balance are always merged in by the gateway on success.
 * Locked media often nests price under [originMediaInfo] rather than the root.
 */
@Serializable
data class PrivateAlbumCheckResponseDto(
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("photo_count") val photoCount: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("video_count") val videoCount: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    val balance: Int = 0,
    @Serializable(with = LenientBooleanSerializer::class)
    @SerialName("is_unlock") val isUnlock: Boolean = false,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("view_price") val viewPrice: Int = 0,
    @Serializable(with = LenientLongSerializer::class)
    @SerialName("media_id") val mediaId: Long = 0,
    @SerialName("origin_media_info")
    val originMediaInfo: PrivateAlbumOriginMediaInfoDto? = null,
    val url: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("small_url") val smallUrl: String? = null,
) {
    /** Prefers root [viewPrice], then [originMediaInfo]. */
    val resolvedViewPrice: Int
        get() = viewPrice.takeIf { it > 0 }
            ?: originMediaInfo?.viewPrice?.takeIf { it > 0 }
            ?: 0

    /** Prefers root [mediaId], then [originMediaInfo]. */
    val resolvedMediaId: Long
        get() = mediaId.takeIf { it > 0L }
            ?: originMediaInfo?.mediaId?.takeIf { it > 0L }
            ?: 0L

    /** Root paths first; unlocked check often nests under [originMediaInfo]. */
    val resolvedRawUrl: String?
        get() = url.trimToMediaPathOrNull()
            ?: originMediaInfo?.url.trimToMediaPathOrNull()

    val resolvedRawImageUrl: String?
        get() = imageUrl.trimToMediaPathOrNull()
            ?: originMediaInfo?.imageUrl.trimToMediaPathOrNull()

    val resolvedRawCoverUrl: String?
        get() = coverUrl.trimToMediaPathOrNull()
            ?: smallUrl.trimToMediaPathOrNull()
            ?: originMediaInfo?.coverUrl.trimToMediaPathOrNull()
            ?: originMediaInfo?.smallUrl.trimToMediaPathOrNull()

    val resolvedMediaType: Int
        get() = originMediaInfo?.mediaType?.takeIf { it > 0 } ?: 0
}

/** Nested media meta on `private-album/check` (locked price and/or unlocked CDN paths). */
@Serializable
data class PrivateAlbumOriginMediaInfoDto(
    @Serializable(with = LenientLongSerializer::class)
    @SerialName("media_id") val mediaId: Long = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("view_price") val viewPrice: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("media_type") val mediaType: Int = 0,
    val url: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("small_url") val smallUrl: String? = null,
)

private fun String?.trimToMediaPathOrNull(): String? =
    this?.trim()?.takeIf { it.isNotEmpty() }

/**
 * `POST private-album/unlock` body.
 *
 * [fromType]: `1=message`, `2=album list`.
 * [useType]: `1=prefer free counts then coins`, `2=counts only`.
 * [mediaId] is a string on the wire per the Spicy doc.
 */
@Serializable
data class PrivateAlbumUnlockRequestDto(
    @Serializable(with = LenientLongSerializer::class)
    @SerialName("coach_uid") val coachUid: Long,
    @SerialName("media_id") val mediaId: String = "",
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("from_type") val fromType: Int,
    @Serializable(with = LenientLongSerializer::class)
    val mtime: Long = 0L,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("is_all") val isAll: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("recharge_from_type") val rechargeFromType: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("use_type") val useType: Int = USE_TYPE_COUNTS_THEN_COINS,
    @Serializable(with = LenientLongSerializer::class)
    @SerialName("feeds_id") val feedsId: Long = 0L,
) {
    companion object {
        const val FROM_TYPE_MESSAGE: Int = 1
        const val FROM_TYPE_ALBUM_LIST: Int = 2
        const val USE_TYPE_COUNTS_THEN_COINS: Int = 1
        const val USE_TYPE_COUNTS_ONLY: Int = 2
    }
}

/**
 * `POST private-album/unlock` success payload.
 *
 * Already-unlocked media may return an empty `data` object — sentinels of `-1`
 * mean the field was absent so callers skip overwriting local balance/counts.
 */
@Serializable
data class PrivateAlbumUnlockResponseDto(
    @Serializable(with = LenientIntSerializer::class)
    val balance: Int = ABSENT,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("photo_count") val photoCount: Int = ABSENT,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("video_count") val videoCount: Int = ABSENT,
) {
    companion object {
        const val ABSENT: Int = -1
    }

    val hasBalance: Boolean get() = balance >= 0
    val hasPhotoCount: Boolean get() = photoCount >= 0
    val hasVideoCount: Boolean get() = videoCount >= 0
}
