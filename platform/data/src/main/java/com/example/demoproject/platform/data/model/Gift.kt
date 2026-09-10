package com.example.demoproject.platform.data.model

data class GiftConfig(
    val gifts: List<Gift>,
    val version: Int,
)

data class Gift(
    val id: Long,
    val title: String,
    val price: Int,
    val iconUrl: String,
    val svgaName: String,
    val svgaUrl: String,
    val unlitIconUrl: String,
    val isNew: Boolean,
)

data class GiftSendResult(
    val balance: Int,
    val mtime: Long,
    val gift: Gift?,
)

/**
 * `gift/send` `from_type` scenes.
 * - [CHAT]: chat gift; `room_id` not required
 * - [CALL]: in-call gift; `room_id` required (HTTP room key / channel string, not only `room_session_id`)
 * - [INTIMACY]: intimacy-scene gift
 */
object GiftFromType {
    const val CHAT: Int = 1
    const val CALL: Int = 2
    const val INTIMACY: Int = 10
}
