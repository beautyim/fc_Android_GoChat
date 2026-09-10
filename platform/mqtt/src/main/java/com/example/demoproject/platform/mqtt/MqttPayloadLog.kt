package com.example.demoproject.platform.mqtt

import com.example.demoproject.platform.common.log.AppLogger

internal fun String.toMqttLogPreview(): String {
    val preview = replace("\r", "\\r").replace("\n", "\\n")
    return SENSITIVE_JSON_FIELD.replace(preview) { match ->
        "${match.groupValues[1]}\"***\""
    }
}

internal fun logMqttPayloadPreview(tag: String, messagePrefix: String, payload: String) {
    val preview = payload.toMqttLogPreview()
    if (preview.length <= LOG_CHUNK_SIZE) {
        AppLogger.d(tag, "$messagePrefix preview=$preview")
        return
    }
    val total = (preview.length + LOG_CHUNK_SIZE - 1) / LOG_CHUNK_SIZE
    AppLogger.d(tag, "$messagePrefix preview (len=${preview.length}, parts=$total)")
    var cursor = 0
    var part = 1
    while (cursor < preview.length) {
        val end = minOf(cursor + LOG_CHUNK_SIZE, preview.length)
        AppLogger.d(tag, "$messagePrefix preview [Part $part/$total]: ${preview.substring(cursor, end)}")
        cursor = end
        part++
    }
}

private const val LOG_CHUNK_SIZE = 2000

private val SENSITIVE_JSON_FIELD = Regex(
    pattern = "(\"[^\"]*(?:token|password|passphrase|secret|key)[^\"]*\"\\s*:\\s*)\"[^\"]*\"",
    option = RegexOption.IGNORE_CASE,
)
