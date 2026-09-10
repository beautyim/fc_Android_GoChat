package com.example.demoproject.platform.data.network.serializer

import com.example.demoproject.platform.data.network.dto.UserDto
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * BerryCam backends usually ship `user_infos` as a uid-keyed JSON object, but
 * some envelopes (e.g. `/post/focus-list` with no rows) return `[]`.
 * Deserialising a JSON array as [Map] throws; this serializer accepts
 * `[]`, `{}`, map-shaped objects, and (defensively) non-empty arrays of
 * user objects keyed by [UserDto.uid].
 */
object UserInfosMapSerializer : KSerializer<Map<String, UserDto>> {
    private val delegate = MapSerializer(String.serializer(), UserDto.serializer())

    override val descriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): Map<String, UserDto> {
        val jsonDecoder = decoder as? JsonDecoder
            ?: return delegate.deserialize(decoder)
        val element = jsonDecoder.decodeJsonElement()
        return parse(element, jsonDecoder.json)
    }

    private fun parse(element: JsonElement, json: Json): Map<String, UserDto> =
        when (element) {
            is JsonArray -> element.toUidKeyedMap(json)
            is JsonObject -> json.decodeFromJsonElement(delegate, element)
            else -> emptyMap()
        }

    private fun JsonArray.toUidKeyedMap(json: Json): Map<String, UserDto> =
        mapNotNull { item ->
            runCatching {
                val user = json.decodeFromJsonElement(UserDto.serializer(), item)
                if (user.uid == 0L) null else user.uid.toString() to user
            }.getOrNull()
        }.toMap()

    override fun serialize(encoder: Encoder, value: Map<String, UserDto>) {
        delegate.serialize(encoder, value)
    }
}
