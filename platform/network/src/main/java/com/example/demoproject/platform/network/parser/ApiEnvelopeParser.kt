package com.example.demoproject.platform.network.parser

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Best-effort extractor of a structured error from a non-2xx response body or a
 * malformed envelope. Tries the BerryCam envelope first (`status`/`msg`) and falls back to
 * common alternatives (`code`/`error_code` + `message`).
 */
object ApiEnvelopeParser {

    fun parseCodeMessage(json: Json, rawBody: String): Pair<Int, String>? =
        try {
            val obj = json.parseToJsonElement(rawBody).jsonObject

            val code = obj["code"]?.jsonPrimitive?.parseIntOrFallback()
                ?: obj["error_code"]?.jsonPrimitive?.parseIntOrFallback()
                ?: obj["ok"]?.jsonPrimitive?.parseIntOrFallback()
                ?: obj["status"]?.jsonPrimitive?.parseIntOrFallback()

            val message = obj["message"]?.jsonPrimitive?.contentOrNull
                ?: obj["msg"]?.jsonPrimitive?.contentOrNull
                ?: obj["error"]?.jsonPrimitive?.contentOrNull
                ?: ""

            if (code == null) null else code to message
        } catch (_: Exception) {
            null
        }

    private fun JsonPrimitive.parseIntOrFallback(): Int? =
        intOrNull ?: contentOrNull?.toIntOrNull()
}
