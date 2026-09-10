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

/**
 * Decodes arbitrary JSON into a single string field:
 * - primitive string/number/bool -> raw textual value
 * - object/array -> compact JSON text
 * - null -> ""
 */
object JsonAnyAsStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("JsonAnyAsString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)

    override fun deserialize(decoder: Decoder): String {
        if (decoder !is JsonDecoder) {
            return runCatching { decoder.decodeString() }.getOrDefault("")
        }
        val element = decoder.decodeJsonElement()
        return when (element) {
            JsonNull -> ""
            else -> runCatching {
                val primitive = element.jsonPrimitive
                if (primitive.isString) primitive.content else primitive.content
            }.getOrElse { element.toString() }
        }
    }
}
