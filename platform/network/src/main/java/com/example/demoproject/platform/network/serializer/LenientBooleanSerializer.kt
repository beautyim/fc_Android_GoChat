package com.example.demoproject.platform.network.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Lenient [Boolean] [KSerializer].
 *
 * The BerryCam backend is known to ship a given boolean field under any of these
 * shapes across endpoints and across versions:
 *
 *   `true | false | 1 | 0 | "1" | "0" | "true" | "false" | null | ""`
 *
 * Apply per-field via `@Serializable(with = LenientBooleanSerializer::class)`
 * on the specific DTO property — the serializer is intentionally **not**
 * auto-wired onto every boolean (kotlinx.serialization has no equivalent of
 * Gson's [com.google.gson.TypeAdapterFactory] and, more importantly, explicit
 * opt-in documents the risk at the call site).
 *
 * On encode, a native JSON boolean is always emitted so round-tripping is
 * lossless in the write direction.
 *
 * Unknown / unparseable content deserialises to `false` rather than throwing,
 * mirroring the tolerance of the legacy Gson `BooleanTypeAdapter`.
 */
object LenientBooleanSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LenientBoolean", PrimitiveKind.BOOLEAN)

    override fun serialize(encoder: Encoder, value: Boolean) {
        encoder.encodeBoolean(value)
    }

    override fun deserialize(decoder: Decoder): Boolean {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeBoolean()
        val element = jsonDecoder.decodeJsonElement()
        if (element !is JsonPrimitive) return false
        element.booleanOrNull?.let { return it }
        element.intOrNull?.let { return it != 0 }
        val content = element.content.trim()
        if (content.isEmpty()) return false
        return when (content.lowercase()) {
            "true", "1" -> true
            "false", "0" -> false
            else -> content.toIntOrNull()?.let { it != 0 } ?: false
        }
    }
}
