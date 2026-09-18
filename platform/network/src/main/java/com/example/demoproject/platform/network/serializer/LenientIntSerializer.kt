package com.example.demoproject.platform.network.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Lenient [Int] [KSerializer].
 *
 * Matches [LenientBooleanSerializer] in spirit: BerryCam backend surfaces
 * numeric flags in any of
 *
 *   `1 | 0 | "1" | "0" | true | false | "true" | "false" | null | ""`
 *
 * across different endpoints / versions (e.g. `/login/handle`'s `is_register`
 * is a native JSON boolean even though callers treat it as a 0/1 flag).
 * Apply per-field via `@Serializable(with = LenientIntSerializer::class)`
 * on the specific DTO property.
 *
 * Unknown / unparseable content deserialises to `0` rather than throwing,
 * so a single poisoned field can't collapse an entire paging envelope into
 * a "data parsing error" on the UI.
 */
object LenientIntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LenientInt", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeInt(value)
    }

    override fun deserialize(decoder: Decoder): Int {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeInt()
        val element = jsonDecoder.decodeJsonElement()
        if (element !is JsonPrimitive) return 0
        element.intOrNull?.let { return it }
        element.booleanOrNull?.let { return if (it) 1 else 0 }
        val content = element.content.trim()
        if (content.isEmpty()) return 0
        return content.toIntOrNull()
            ?: content.toDoubleOrNull()?.toInt()
            ?: when (content.lowercase()) {
                "true" -> 1
                "false" -> 0
                else -> 0
            }
    }
}

/**
 * Lenient [Double] [KSerializer] for price fields that may arrive as strings.
 */
object LenientDoubleSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LenientDouble", PrimitiveKind.DOUBLE)

    override fun serialize(encoder: Encoder, value: Double) {
        encoder.encodeDouble(value)
    }

    override fun deserialize(decoder: Decoder): Double {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeDouble()
        val element = jsonDecoder.decodeJsonElement()
        if (element !is JsonPrimitive) return 0.0
        element.content.toDoubleOrNull()?.let { return it }
        val content = element.content.trim()
        if (content.isEmpty()) return 0.0
        return content.toDoubleOrNull() ?: 0.0
    }
}

/**
 * Lenient [Long] [KSerializer]. Same philosophy as [LenientIntSerializer],
 * needed for identifier fields (`uid`, `target_uid`, timestamps) which the
 * backend sometimes encodes as quoted strings to preserve precision on
 * JavaScript clients. Also accepts a single-element JSON array
 * (e.g. `private-album/check` unlocked `origin_media_info.media_id`).
 */
object LenientLongSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LenientLong", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Long) {
        encoder.encodeLong(value)
    }

    override fun deserialize(decoder: Decoder): Long {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeLong()
        return jsonDecoder.decodeJsonElement().toLenientLong()
    }
}

private fun JsonElement.toLenientLong(): Long = when (this) {
    is JsonPrimitive -> {
        longOrNull
            ?: content.trim().takeIf { it.isNotEmpty() }?.toLongOrNull()
            ?: content.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()?.toLong()
            ?: 0L
    }
    is JsonArray -> firstOrNull()?.toLenientLong() ?: 0L
    else -> 0L
}
