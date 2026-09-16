package com.example.demoproject.platform.mqtt

import com.example.demoproject.platform.callkit.CallMediaType
import com.example.demoproject.platform.callkit.signaling.CallSignalingClient
import com.example.demoproject.platform.callkit.signaling.EndReason
import com.example.demoproject.platform.callkit.signaling.OutgoingInviteRequest
import com.example.demoproject.platform.callkit.signaling.RejectReason
import com.example.demoproject.platform.callkit.signaling.SignalingCoinOffer
import com.example.demoproject.platform.callkit.signaling.SignalingEvent
import com.example.demoproject.platform.common.log.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Backend-mediated implementation of [CallSignalingClient].
 *
 * Outgoing call creation remains server-owned. This client listens to MQTT
 * packets and maps accept/reject/end actions back through [CallSignalingActions].
 */
class MqttCallSignalingClient(
    private val mqttManager: MqttManager,
    private val callActions: CallSignalingActions,
    private val currentUserIdProvider: CurrentUserIdProvider,
    private val json: Json,
) : CallSignalingClient {

    override val events: Flow<SignalingEvent> =
        mqttManager.messageFlow.mapNotNull { msg -> parseCallEvent(msg.payload) }

    override suspend fun sendInvite(request: OutgoingInviteRequest) {
        AppLogger.d(TAG, "sendInvite: server-mediated; no peer MQTT publish needed")
    }

    override suspend fun accept(inviteId: String) {
        val roomId = inviteId.trim()
        if (roomId.isEmpty()) {
            AppLogger.w(TAG, "accept: blank roomId")
            return
        }
        callActions.markCallSucceeded(roomId)
    }

    override suspend fun reject(inviteId: String, reason: RejectReason) {
        val roomId = inviteId.trim()
        if (roomId.isEmpty()) return
        callActions.endCall(roomId = roomId, source = "mqtt.reject:$reason")
        AppLogger.d(TAG, "reject roomId=$roomId reason=$reason")
    }

    override suspend fun cancel(inviteId: String) {
        val roomId = inviteId.trim()
        if (roomId.isEmpty()) return
        callActions.endCall(roomId = roomId, source = "mqtt.cancel")
        AppLogger.d(TAG, "cancel roomId=$roomId")
    }

    override suspend fun end(callId: String, reason: EndReason) {
        val roomId = callId.trim()
        if (roomId.isEmpty()) return
        callActions.endCall(roomId = roomId, source = "mqtt.end:$reason")
        AppLogger.d(TAG, "end roomId=$roomId reason=$reason")
    }

    private fun parseCallEvent(payload: String): SignalingEvent? {
        val root = runCatching { json.parseToJsonElement(payload).jsonObject }.getOrNull()
            ?: return null
        logMqttPayloadPreview(
            tag = TAG,
            messagePrefix = "parseCallEvent keys=${root.keys.sorted()}",
            payload = payload,
        )

        val msgType = root.intPrimitive("msg_type")
        if (msgType != null) {
            AppLogger.d(TAG, "parseCallEvent legacy msgType=$msgType")
            return when (msgType) {
                CallMqttNotificationDto.MSG_TYPE_CALL_INVITE -> parseIncomingInvite(root, root)
                CallMqttNotificationDto.MSG_TYPE_CALL_ENDED -> parseCallEnded(root, root)
                else -> null
            }
        }

        val type = root.intPrimitive("type")
        when (type) {
            3 -> {
                val data = root["data"]?.jsonObject ?: return null
                AppLogger.d(
                    TAG,
                    "parseCallEvent envelope type=$type a_type=${data.intPrimitive("a_type")} " +
                        "dataKeys=${data.keys.sorted()}",
                )
                return when (val aType = data.intPrimitive("a_type")) {
                    // Match rooms are handed to CallKit from the Match coordinator. Treating this
                    // packet as a normal invite races navigation and can create a second call.
                    1 -> if (data.intPrimitive("is_match_call") == 1) {
                        null
                    } else {
                        parseIncomingInvite(root, data)
                    }
                    2, 3 -> parseCallEnded(root, data, serverInitiated = aType == 2)
                    6 -> parseBalanceSync(root, data)
                    7 -> parseBalanceAlert(root, data)
                    8 -> parseInCallChat(root, data)
                    14 -> parseInviteAccepted(root, data)
                    else -> parsePeerMaskStatus(root, data, aType)
                }
            }
            1 -> {
                val data = root["data"]?.jsonObject ?: return null
                return when (data.intPrimitive("msg_type")) {
                    CallMqttNotificationDto.MSG_TYPE_CALL_INVITE,
                    EVENT_MSG_TYPE_CALL_INVITE,
                    -> parseIncomingInvite(root, data)
                    CallMqttNotificationDto.MSG_TYPE_CALL_ENDED -> parseCallEnded(root, data)
                    else -> null
                }
            }
            else -> return null
        }
    }

    private fun parseIncomingInvite(root: JsonObject, packet: JsonObject): SignalingEvent.IncomingInvite? {
        val callData = parseCallPayload(packet) ?: parseCallPayload(root) ?: run {
            AppLogger.w(TAG, "parseIncomingInvite: no call data packetKeys=${packet.keys}")
            return null
        }
        val packetPayload = extractCallDataPayloadObject(packet) ?: packet
        val rootPayload = extractCallDataPayloadObject(root) ?: root
        if (!callData.hasValidRoom) {
            AppLogger.w(TAG, "parseIncomingInvite: roomId missing, dropping")
            return null
        }
        val httpRoomId = callData.effectiveHttpRoomId
        val callerUserId = callData.effectiveCallerUid
            .takeIf { it > 0L }
            ?: packet.longPrimitive("send_uid").takeIf { it > 0L }
            ?: root.longPrimitive("send_uid")
        val rtcUid = resolveIncomingRtcUid(callData, packet, root)
        if (rtcUid == 0) {
            AppLogger.w(TAG, "parseIncomingInvite: rtcUid is 0 roomId=$httpRoomId")
        }
        AppLogger.d(
            TAG,
            "parseIncomingInvite ok roomId=$httpRoomId sessionId=${callData.numericRoomId} " +
                "callerUid=${callData.effectiveCallerUid} channel=${callData.effectiveChannel} uid=$rtcUid",
        )
        return SignalingEvent.IncomingInvite(
            inviteId = httpRoomId,
            callerUserId = callerUserId.toString(),
            callerName = resolveCallerName(callData, packetPayload, rootPayload),
            callerAvatar = resolveCallerAvatar(callData, packetPayload, rootPayload),
            callerAge = resolveCallerAge(packetPayload, rootPayload),
            callType = if (callData.type == CallMediaType.Voice) CallMediaType.Voice else CallMediaType.Video,
            channelId = callData.effectiveChannel,
            rtcToken = callData.token,
            rtcUid = rtcUid,
            rtcAppId = callData.appId,
            roomSessionId = callData.numericRoomId,
            callFreeMin = callData.callFreeMin,
            fencingToken = callData.fencingToken,
            peerVideoUrl = resolvePeerVideoUrl(packetPayload, rootPayload),
            peerCoverUrl = resolvePeerCoverUrl(packetPayload, rootPayload),
        )
    }

    private fun parseInviteAccepted(root: JsonObject, packet: JsonObject): SignalingEvent.InviteAccepted? {
        val roomId = resolveCallRoomKey(packet)
            ?: resolveCallRoomKey(root)
            ?: extractCallData(packet)?.effectiveHttpRoomId?.takeIf { it.isNotEmpty() }
            ?: extractCallData(root)?.effectiveHttpRoomId?.takeIf { it.isNotEmpty() }
            ?: return null
        AppLogger.d(TAG, "parseInviteAccepted roomId=$roomId")
        return SignalingEvent.InviteAccepted(inviteId = roomId, callId = roomId)
    }

    private fun parseBalanceSync(root: JsonObject, packet: JsonObject): SignalingEvent.BalanceSync? {
        val roomKey = resolveCallRoomKey(packet)
            ?: resolveCallRoomKey(root)
            ?: return null
        val balance = packet.intPrimitive("balance") ?: return null
        if (balance < 0) return null
        AppLogger.d(TAG, "parseBalanceSync roomKey=$roomKey balance=$balance")
        return SignalingEvent.BalanceSync(roomKey = roomKey, balance = balance)
    }

    private fun parseBalanceAlert(root: JsonObject, packet: JsonObject): SignalingEvent.BalanceAlert? {
        val roomKey = resolveCallRoomKey(packet)
            ?: resolveCallRoomKey(root)
            ?: return null
        val remaining = packet.intPrimitive("duration") ?: 0
        val total = packet.intPrimitive("total_duration") ?: 0
        if (remaining <= 0 && total <= 0) {
            AppLogger.w(TAG, "parseBalanceAlert drop roomKey=$roomKey no duration")
            return null
        }
        val alert = SignalingEvent.BalanceAlert(
            roomKey = roomKey,
            roomSessionId = packet.longPrimitive("room_session_id").takeIf { it > 0L }
                ?: root.longPrimitive("room_session_id").takeIf { it > 0L }
                ?: 0L,
            balance = packet.intPrimitive("balance") ?: 0,
            remainingSeconds = remaining,
            totalDurationSeconds = total,
            saleRechargeAlertTimeSeconds = packet.intPrimitive("sale_recharge_alert_time") ?: 0,
            rechargeAlertTimeSeconds = packet.intPrimitive("recharge_alert_time") ?: 0,
            payItem = parseCoinOffer(nestedObject(packet, "pay_item")),
            salePayItem = parseCoinOffer(nestedObject(packet, "sale_pay_item")),
            vipPayItem = parseCoinOffer(nestedObject(packet, "vip_pay_item"))
                ?.copy(productType = 2),
        )
        AppLogger.d(
            TAG,
            "parseBalanceAlert roomKey=$roomKey balance=${alert.balance} remaining=$remaining total=$total",
        )
        return alert
    }

    private fun parseInCallChat(root: JsonObject, packet: JsonObject): SignalingEvent.InCallChat? {
        val roomKey = resolveCallRoomKey(packet)
            ?: resolveCallRoomKey(root)
            ?: return null
        // Tip payloads (msg_type=4) use `text`; chat-style may use content/msg_content.
        val content = packet.stringPrimitive("content")
            ?: packet.stringPrimitive("msg_content")
            ?: packet.stringPrimitive("text")
            ?: return null
        if (content.isBlank()) return null
        if (isCallSignalingWirePayload(content)) {
            AppLogger.d(TAG, "parseInCallChat drop signaling wire payload roomKey=$roomKey")
            return null
        }
        val msgType = packet.intPrimitive("msg_type") ?: 1
        val sendUid = packet.longPrimitive("send_uid").takeIf { it > 0L }
            ?: root.longPrimitive("send_uid").takeIf { it > 0L }
            ?: 0L
        val mtime = packet.longPrimitive("mtime").takeIf { it > 0L }
            ?: root.longPrimitive("mtime").takeIf { it > 0L }
            ?: System.currentTimeMillis()
        return SignalingEvent.InCallChat(
            roomKey = roomKey,
            msgType = msgType,
            content = content,
            sendUid = sendUid,
            messageId = "mqtt_${roomKey}_$mtime",
        )
    }

    private fun parsePeerMaskStatus(
        root: JsonObject,
        packet: JsonObject,
        aType: Int?,
    ): SignalingEvent.PeerMaskStatus? {
        // Exclude lifecycle / alert / chat a_types; require status and no end/duration/pay fields.
        if (aType != null && aType in EXCLUDED_MASK_A_TYPES) return null
        if (packet.containsKey("duration") ||
            packet.containsKey("pay_item") ||
            packet.containsKey("end_type")
        ) {
            return null
        }
        val status = packet.intPrimitive("status") ?: return null
        val roomKey = resolveCallRoomKey(packet)
            ?: resolveCallRoomKey(root)
            ?: return null
        AppLogger.d(TAG, "parsePeerMaskStatus roomKey=$roomKey status=$status aType=$aType")
        return SignalingEvent.PeerMaskStatus(
            roomKey = roomKey,
            masked = status == 1,
        )
    }

    private fun parseCoinOffer(obj: JsonObject?): SignalingCoinOffer? {
        if (obj == null) return null
        val id = obj.longPrimitive("id")
        val sku = obj.stringPrimitive("sku").orEmpty()
        if (id <= 0L && sku.isBlank()) return null
        val originalDesc = obj.stringPrimitive("original_desc")
            ?: obj.stringPrimitive("original")
            ?: ""
        return SignalingCoinOffer(
            id = id,
            sku = sku,
            productType = obj.intPrimitive("product_type") ?: 1,
            diamond = obj.intPrimitive("diamond") ?: 0,
            giveCoins = obj.intPrimitive("give_coins") ?: 0,
            moneyDesc = obj.stringPrimitive("money_desc").orEmpty(),
            originalDesc = originalDesc,
            saleDesc = obj.stringPrimitive("sale_desc").orEmpty(),
            saveRate = obj.stringPrimitive("save_rate").orEmpty(),
            title = obj.stringPrimitive("title").orEmpty(),
            label = obj.stringPrimitive("label").orEmpty(),
            days = obj.intPrimitive("days") ?: 0,
            month = obj.intPrimitive("month") ?: 0,
            matchCount = obj.intPrimitive("match")
                ?: nestedObject(obj, "extra_rewards")?.intPrimitive("match")
                ?: 0,
        )
    }

    private fun resolveCallerName(
        callData: CallMqttNotificationDto,
        packetPayload: JsonObject,
        rootPayload: JsonObject,
    ): String = callData.callerName
        .takeIf { it.isNotBlank() }
        ?: packetPayload.stringPrimitive("caller_name")
        ?: packetPayload.stringPrimitive("nickname")
        ?: packetPayload.stringPrimitive("nick_name")
        ?: packetPayload.stringPrimitive("name")
        ?: rootPayload.stringPrimitive("caller_name")
        ?: rootPayload.stringPrimitive("nickname")
        ?: rootPayload.stringPrimitive("nick_name")
        ?: rootPayload.stringPrimitive("name")
        ?: ""

    private fun resolveCallerAvatar(
        callData: CallMqttNotificationDto,
        packetPayload: JsonObject,
        rootPayload: JsonObject,
    ): String = listOf(
        callData.callerAvatar,
        packetPayload.stringPrimitive("caller_avatar"),
        packetPayload.stringPrimitive("small_avatar"),
        packetPayload.stringPrimitive("avatar"),
        packetPayload.stringPrimitive("head_img"),
        packetPayload.stringPrimitive("headimg"),
        rootPayload.stringPrimitive("caller_avatar"),
        rootPayload.stringPrimitive("small_avatar"),
        rootPayload.stringPrimitive("avatar"),
        rootPayload.stringPrimitive("head_img"),
        rootPayload.stringPrimitive("headimg"),
    ).firstOrNull { !it.isNullOrBlank() }.orEmpty()

    private fun resolveIncomingRtcUid(
        callData: CallMqttNotificationDto,
        packet: JsonObject,
        root: JsonObject,
    ): Int {
        if (callData.uid > 0) return callData.uid
        packet.longPrimitive("target_uid").toSafePositiveIntOrNull()?.let { return it }
        root.longPrimitive("target_uid").toSafePositiveIntOrNull()?.let { return it }
        return currentUserIdProvider.currentUserId?.toIntOrNull()?.takeIf { it > 0 } ?: 0
    }

    private fun Long.toSafePositiveIntOrNull(): Int? =
        takeIf { it in 1..Int.MAX_VALUE.toLong() }?.toInt()

    private fun parseCallEnded(
        root: JsonObject,
        packet: JsonObject,
        serverInitiated: Boolean = false,
    ): SignalingEvent.CallEnded? {
        val roomId = resolveCallRoomKey(packet)
            ?: resolveCallRoomKey(root)
            ?: extractCallData(packet)?.effectiveHttpRoomId?.takeIf { it.isNotEmpty() }
            ?: extractCallData(root)?.effectiveHttpRoomId?.takeIf { it.isNotEmpty() }
            ?: return null
        val hangUpUid = packet.stringPrimitive("hang_up_uid")?.toLongOrNull()
            ?: root.stringPrimitive("hang_up_uid")?.toLongOrNull()
        val selfUid = currentUserIdProvider.currentUserId?.toLongOrNull()
        val endType = packet.endTypePrimitive() ?: root.endTypePrimitive()
        val resolvedReason = when {
            endType == END_TYPE_BALANCE -> EndReason.InsufficientBalance
            serverInitiated -> EndReason.RemoteHangup
            hangUpUid != null && selfUid != null && hangUpUid == selfUid -> EndReason.Hangup
            else -> EndReason.RemoteHangup
        }
        AppLogger.d(
            TAG,
            "parseCallEnded roomId=$roomId endType=$endType reason=$resolvedReason serverInitiated=$serverInitiated",
        )
        return SignalingEvent.CallEnded(
            callId = roomId,
            reason = resolvedReason,
        )
    }

    private fun parseCallPayload(packet: JsonObject): CallMqttNotificationDto? {
        if (packet.containsKey("room_id")) {
            return buildCallDataFromJson(packet)
        }
        return extractCallData(packet)
    }

    private fun buildCallDataFromJson(packet: JsonObject): CallMqttNotificationDto? {
        val rawRoomId = packet.roomIdAsString().orEmpty()
        val roomSessionId = packet.longPrimitive("room_session_id").takeIf { it > 0L } ?: 0L
        if (rawRoomId.isEmpty() || rawRoomId == "0") {
            if (roomSessionId <= 0L) return null
        }
        return CallMqttNotificationDto(
            roomId = rawRoomId.takeIf { it.isNotEmpty() && it != "0" } ?: roomSessionId.toString(),
            channel = packet.stringPrimitive("channel").orEmpty(),
            channelName = packet.stringPrimitive("channel_name").orEmpty(),
            token = packet.stringPrimitive("token").orEmpty(),
            appId = packet.stringPrimitive("app_id").orEmpty(),
            uid = packet.intPrimitive("uid")
                ?: packet.intPrimitive("target_uid")
                ?: 0,
            type = packet.intPrimitive("type")
                ?: packet.intPrimitive("call_type")
                ?: 1,
            callerUid = resolveCallerUidFromJson(packet),
            callerName = resolveDisplayName(packet),
            callerAvatar = resolveAvatar(packet),
            roomSessionId = roomSessionId,
            callFreeMin = resolveCallFreeMinFromJson(packet),
            fencingToken = packet.stringPrimitive("fencing_token"),
        ).takeIf { it.hasValidRoom }
    }

    /** Prefer raw `room_id` (string or number); fall back to `room_session_id`. */
    private fun resolveCallRoomKey(packet: JsonObject): String? {
        packet.roomIdAsString()?.let { return it }
        packet.longPrimitive("room_session_id").takeIf { it > 0L }?.let { return it.toString() }
        return null
    }

    private fun JsonObject.roomIdAsString(): String? {
        val primitive = this["room_id"]?.jsonPrimitive ?: return null
        val content = primitive.content.trim()
        return content.takeIf { it.isNotEmpty() && it != "0" }
    }

    private fun resolveCallerUidFromJson(packet: JsonObject): Long {
        packet.longPrimitive("caller_uid").takeIf { it > 0L }?.let { return it }
        nestedObject(packet, "caller")?.longPrimitive("uid")?.takeIf { it > 0L }?.let { return it }
        packet["caller"]?.jsonPrimitive?.content?.toLongOrNull()?.takeIf { it > 0L }?.let { return it }
        nestedObject(packet, "user_info")?.longPrimitive("uid")?.takeIf { it > 0L }?.let { return it }
        packet.longPrimitive("send_uid").takeIf { it > 0L }?.let { return it }
        return 0L
    }

    /**
     * `user_info.call_free_min` on the incoming-invite push — sole source of truth for whether
     * this specific incoming call is free, per product rule. Only read here, at invite time;
     * never re-derived from later MQTT balance-alert pushes.
     */
    private fun resolveCallFreeMinFromJson(packet: JsonObject): Int =
        nestedObject(packet, "user_info")?.intPrimitive("call_free_min") ?: 0

    /**
     * `user_info.video` is the peer video show on call invite / create payloads
     * (see API `data.user_info.video`). Prefer `url`, then `video_url`.
     */
    private fun resolvePeerVideoUrl(packetPayload: JsonObject, rootPayload: JsonObject): String {
        listOf(packetPayload, rootPayload).forEach { root ->
            nestedObject(root, "user_info")?.let { user ->
                nestedObject(user, "video")?.let { video ->
                    video.stringPrimitive("url")?.let { return it }
                    video.stringPrimitive("video_url")?.let { return it }
                }
            }
            nestedObject(root, "video")?.let { video ->
                video.stringPrimitive("url")?.let { return it }
                video.stringPrimitive("video_url")?.let { return it }
            }
        }
        return ""
    }

    private fun resolvePeerCoverUrl(packetPayload: JsonObject, rootPayload: JsonObject): String {
        listOf(packetPayload, rootPayload).forEach { root ->
            nestedObject(root, "user_info")?.let { user ->
                nestedObject(user, "video")?.let { video ->
                    video.stringPrimitive("cover_url")?.let { return it }
                    video.stringPrimitive("small_photo_url")?.let { return it }
                    video.stringPrimitive("small_url")?.let { return it }
                }
            }
            nestedObject(root, "video")?.let { video ->
                video.stringPrimitive("cover_url")?.let { return it }
                video.stringPrimitive("small_photo_url")?.let { return it }
                video.stringPrimitive("small_url")?.let { return it }
            }
        }
        return ""
    }

    private fun resolveCallerAge(packetPayload: JsonObject, rootPayload: JsonObject): Int {
        listOf(packetPayload, rootPayload).forEach { root ->
            nestedObject(root, "user_info")?.let { user ->
                user.intPrimitive("age")?.takeIf { it > 0 }?.let { return it }
                birthdayToAge(user.stringPrimitive("birthday")).takeIf { it > 0 }?.let { return it }
            }
            nestedObject(root, "caller")?.let { caller ->
                caller.intPrimitive("age")?.takeIf { it > 0 }?.let { return it }
                birthdayToAge(caller.stringPrimitive("birthday")).takeIf { it > 0 }?.let { return it }
            }
            root.intPrimitive("age")?.takeIf { it > 0 }?.let { return it }
            birthdayToAge(root.stringPrimitive("birthday")).takeIf { it > 0 }?.let { return it }
        }
        return 0
    }

    private fun birthdayToAge(raw: String?): Int {
        if (raw.isNullOrBlank()) return 0
        val trimmed = raw.trim()
        val birth = runCatching { java.time.LocalDate.parse(trimmed) }.getOrNull()
            ?: trimmed.take(10).takeIf { it.length == 10 }?.let { head ->
                runCatching { java.time.LocalDate.parse(head) }.getOrNull()
            }
            ?: return 0
        return java.time.Period.between(birth, java.time.LocalDate.now()).years.coerceAtLeast(0)
    }

    private fun resolveDisplayName(packet: JsonObject): String {
        listOf("caller_name", "nickname", "nick_name", "name").forEach { key ->
            packet.stringPrimitive(key)?.let { return it }
        }
        nestedObject(packet, "caller")?.let { caller ->
            listOf("nickname", "nick_name", "name", "caller_name").forEach { key ->
                caller.stringPrimitive(key)?.let { return it }
            }
        }
        nestedObject(packet, "user_info")?.let { user ->
            listOf("nickname", "nick_name", "name").forEach { key ->
                user.stringPrimitive(key)?.let { return it }
            }
        }
        return ""
    }

    private fun resolveAvatar(packet: JsonObject): String {
        listOf("caller_avatar", "small_avatar", "avatar", "head_img", "headimg").forEach { key ->
            packet.stringPrimitive(key)?.let { return it }
        }
        nestedObject(packet, "caller")?.let { caller ->
            listOf("small_avatar", "avatar", "head_img", "headimg", "caller_avatar").forEach { key ->
                caller.stringPrimitive(key)?.let { return it }
            }
        }
        nestedObject(packet, "user_info")?.let { user ->
            listOf("small_avatar", "avatar", "head_img", "headimg").forEach { key ->
                user.stringPrimitive(key)?.let { return it }
            }
        }
        return ""
    }

    private fun isCallSignalingWirePayload(rawContent: String): Boolean {
        val t = rawContent.trim()
        if (t.isEmpty() || !t.startsWith("{")) return false
        val lower = t.lowercase()
        return "\"channel\"" in lower && "\"token\"" in lower && "\"uid\"" in lower
    }

    private fun nestedObject(root: JsonObject, key: String): JsonObject? =
        runCatching { root[key]?.jsonObject }.getOrNull()

    private fun JsonObject.intPrimitive(key: String): Int? =
        runCatching { this[key]?.jsonPrimitive?.intOrNull }.getOrNull()

    private fun JsonObject.endTypePrimitive(): Int? {
        val primitive = this["end_type"]?.jsonPrimitive ?: return null
        primitive.intOrNull?.let { return it }
        val content = primitive.content.trim()
        if (content.isEmpty()) return null
        return content.toIntOrNull()
    }

    private fun JsonObject.longPrimitive(key: String): Long {
        val primitive = this[key]?.jsonPrimitive ?: return 0L
        primitive.longOrNull?.let { return it }
        val content = primitive.content.trim()
        if (content.isEmpty()) return 0L
        return content.toLongOrNull()
            ?: content.toDoubleOrNull()?.toLong()
            ?: 0L
    }

    private fun extractCallData(root: JsonObject): CallMqttNotificationDto? {
        if (root.containsKey("room_id")) {
            return runCatching { json.decodeFromJsonElement<CallMqttNotificationDto>(root) }.getOrNull()
        }
        val msgContent = root.stringPrimitive("msg_content")
            ?: root.stringPrimitive("content")
            ?: return null
        val nested = runCatching { json.parseToJsonElement(msgContent).jsonObject }.getOrNull()
            ?: return null
        return runCatching { json.decodeFromJsonElement<CallMqttNotificationDto>(nested) }.getOrNull()
    }

    private fun extractCallDataPayloadObject(root: JsonObject): JsonObject? {
        if (root.containsKey("room_id")) return root
        val msgContent = root.stringPrimitive("msg_content")
            ?: root.stringPrimitive("content")
            ?: return null
        return runCatching { json.parseToJsonElement(msgContent).jsonObject }.getOrNull()
    }

    private fun JsonObject.stringPrimitive(key: String): String? =
        runCatching { this[key]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } }.getOrNull()

    private companion object {
        const val TAG = "MqttCallSignaling"
        /** Matches [com.example.demoproject.platform.data.network.dto.CallEndRequestDto.END_TYPE_BALANCE]. */
        const val END_TYPE_BALANCE: Int = 5
        /** `/event` topic invite uses msg_type=28 in current backend payloads. */
        const val EVENT_MSG_TYPE_CALL_INVITE: Int = 28
        /** a_types that are never peer-mask packets. */
        private val EXCLUDED_MASK_A_TYPES = setOf(1, 2, 3, 6, 7, 8, 14)
    }
}
