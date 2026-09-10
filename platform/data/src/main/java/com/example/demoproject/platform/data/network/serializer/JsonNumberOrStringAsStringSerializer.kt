package com.example.demoproject.platform.data.network.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Backend fields such as [moment_id], [uid] and [last_id] are sometimes
 * JSON strings (`"6717"`) and sometimes numbers; both round-trip to a
 * single Kotlin [String] for the domain mappers.
 */
object JsonNumberOrStringAsStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("JsonNumberOrStringAsString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)

    override fun deserialize(decoder: Decoder): String {
        if (decoder !is JsonDecoder) {
            return decoder.decodeString()
        }
        return when (val e = decoder.decodeJsonElement()) {
            JsonNull -> "0"
            else -> {
                val p = runCatching { e.jsonPrimitive }.getOrNull() ?: return "0"
                if (p.isString) p.content
                else (p.longOrNull ?: 0L).toString()
            }
        }
    }
}
