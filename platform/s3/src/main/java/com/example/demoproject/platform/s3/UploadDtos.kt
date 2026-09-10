package com.example.demoproject.platform.s3

import com.example.demoproject.platform.network.serializer.LenientIntSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

@Serializable
data class GetUploadParamsRequestDto(
    @SerialName("type") val type: Int = TYPE_IMAGE,
    @SerialName("flag") val flag: String = "album",
    @SerialName("md5") val md5: String? = null,
    @SerialName("width") val width: Int? = null,
    @SerialName("height") val height: Int? = null,
) {
    companion object {
        const val TYPE_IMAGE: Int = 1
        const val TYPE_VIDEO: Int = 2
        const val TYPE_AUDIO: Int = 3
    }
}

@Serializable
data class UploadParamsDto(
    @SerialName("host") val host: String? = null,
    @SerialName("token") val token: String? = null,
    @SerialName("cover_token") val coverToken: String? = null,
    @SerialName("date") val date: String? = null,
    @SerialName("upload_type") val uploadType: String? = null,
    @SerialName("cover_upload_type") val coverUploadType: String? = null,
    @Serializable(with = UploadDirDtoSerializer::class)
    @SerialName("dir") val dir: UploadDirDto? = null,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("upload_id") val uploadId: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("is_upload") val isUpload: Int = 0,
    /** Present when `is_upload=1` reuses a prior success (often instead of nested `dir`). */
    @SerialName("small_url") val smallUrl: String? = null,
    /** Prior video cover path when `is_upload=1`. */
    @SerialName("cover_url") val coverUrl: String? = null,
)

@Serializable
data class UploadDirDto(
    @SerialName("path") val path: String = "",
    @SerialName("cover_path") val coverPath: String = "",
    @SerialName("small_url") val smallUrl: String = "",
    @SerialName("cover_upload_type") val coverUploadType: String = "",
)

object UploadDirDtoSerializer : KSerializer<UploadDirDto?> {
    override val descriptor: SerialDescriptor = UploadDirDto.serializer().descriptor

    override fun deserialize(decoder: Decoder): UploadDirDto? {
        val jsonDecoder = decoder as? JsonDecoder
            ?: return decoder.decodeNullableSerializableValue(UploadDirDto.serializer())
        return when (val element = jsonDecoder.decodeJsonElement()) {
            JsonNull -> null
            is JsonPrimitive -> element.content
                .takeIf { it.isNotBlank() }
                ?.let { UploadDirDto(path = it) }
            else -> jsonDecoder.json.decodeFromJsonElement(UploadDirDto.serializer(), element)
        }
    }

    override fun serialize(encoder: Encoder, value: UploadDirDto?) {
        val jsonEncoder = encoder as? JsonEncoder
        if (jsonEncoder == null) {
            encoder.encodeNullableSerializableValue(UploadDirDto.serializer(), value)
            return
        }
        val element: JsonElement = value?.let {
            jsonEncoder.json.encodeToJsonElement(UploadDirDto.serializer(), it)
        } ?: JsonNull
        jsonEncoder.encodeJsonElement(element)
    }
}

@Serializable
data class UploadSuccessRequestDto(
    @SerialName("id") val id: List<Int>,
)

data class MediaUploadResult(
    val remotePath: String,
    val smallUrl: String? = null,
    val coverPath: String? = null,
    val uploadId: Int = 0,
    val width: Int? = null,
    val height: Int? = null,
    val durationSec: Int? = null,
)
