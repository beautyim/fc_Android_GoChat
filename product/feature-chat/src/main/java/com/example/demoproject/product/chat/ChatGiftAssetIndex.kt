package com.example.demoproject.product.chat

/**
 * Icon / animation lookup for gift bubbles whose payload carries no asset URLs.
 *
 * Gift rows arrive in several shapes: `msg_type=5/7` usually embeds `icon` and `svga_url`,
 * while ask-gift (`msg_type=6`) and history rows often only carry `gift_id` / `gift_name`.
 * For those the `gift/config` catalog is the only asset source, so the detail page keeps this
 * index around and matches on gift id first, then on the gift title.
 */
internal class ChatGiftAssetIndex private constructor(
    private val assetsById: Map<Long, ChatGiftAssets>,
    private val assetsByTitle: Map<String, ChatGiftAssets>,
) {
    fun resolveIcon(giftId: Long, title: String): String = resolve(giftId, title)?.iconUrl.orEmpty()

    fun resolveAnimation(giftId: Long, title: String): String =
        resolve(giftId, title)?.animationUrl.orEmpty()

    private fun resolve(giftId: Long, title: String): ChatGiftAssets? {
        assetsById[giftId]?.let { return it }
        val key = title.giftTitleKey()
        if (key.isEmpty()) return null
        return assetsByTitle[key]
    }

    companion object {
        val Empty = ChatGiftAssetIndex(emptyMap(), emptyMap())

        fun from(gifts: List<ChatGiftUi>): ChatGiftAssetIndex {
            val known = gifts.filter { it.iconUrl.isNotBlank() || it.svgaUrl.isNotBlank() }
            if (known.isEmpty()) return Empty
            return ChatGiftAssetIndex(
                assetsById = known
                    .filter { it.id > 0L }
                    .associate { it.id to it.assets() },
                assetsByTitle = known
                    .filter { it.title.giftTitleKey().isNotEmpty() }
                    .associate { it.title.giftTitleKey() to it.assets() },
            )
        }
    }
}

internal data class ChatGiftAssets(
    val iconUrl: String,
    val animationUrl: String,
)

private fun ChatGiftUi.assets(): ChatGiftAssets =
    ChatGiftAssets(iconUrl = iconUrl, animationUrl = svgaUrl)

private fun String.giftTitleKey(): String = trim().lowercase()
