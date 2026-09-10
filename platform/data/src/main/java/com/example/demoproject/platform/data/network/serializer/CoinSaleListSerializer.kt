package com.example.demoproject.platform.data.network.serializer

import com.example.demoproject.platform.data.network.dto.CoinProductDto
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

/**
 * `sale_list` may be `null`, a single product object, or an array of products.
 */
object CoinSaleListSerializer : KSerializer<List<CoinProductDto>?> {
    private val listSerializer = ListSerializer(CoinProductDto.serializer()).nullable

    override val descriptor: SerialDescriptor = listSerializer.descriptor

    override fun serialize(encoder: Encoder, value: List<CoinProductDto>?) {
        listSerializer.serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): List<CoinProductDto>? {
        if (decoder !is JsonDecoder) {
            return listSerializer.deserialize(decoder)
        }
        return when (val element = decoder.decodeJsonElement()) {
            JsonNull -> null
            is JsonArray -> decoder.json.decodeFromJsonElement(
                ListSerializer(CoinProductDto.serializer()),
                element,
            )
            is JsonObject -> listOf(
                decoder.json.decodeFromJsonElement(CoinProductDto.serializer(), element),
            )
            else -> null
        }
    }
}
