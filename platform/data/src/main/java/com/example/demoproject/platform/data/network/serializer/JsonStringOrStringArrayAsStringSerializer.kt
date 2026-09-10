package com.example.demoproject.platform.data.network.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Normalizes backend language fields that alternate between `"en,es"` and
 * `["en", "es"]` into the comma-separated shape existing mappers already use.
 */
object JsonStringOrStringArrayAsStringSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("JsonStringOrStringArrayAsString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String?) {
        encoder.encodeString(value.orEmpty())
    }

    override fun deserialize(decoder: Decoder): String? {
        if (decoder !is JsonDecoder) {
            return decoder.decodeString()
        }
        return when (val element = decoder.decodeJsonElement()) {
            JsonNull -> null
            is JsonArray -> element.joinToString(",") { item ->
                runCatching { item.jsonPrimitive.content }.getOrDefault("")
            }.takeIf { it.isNotBlank() }
            else -> runCatching { element.jsonPrimitive.content }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
        }
    }
}
