package com.example.demoproject.platform.network.crypto

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.TreeMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Double-MD5 signer replicating the JavaScript reference implementation used by
 * the legacy web client. Input is a parsed JSON tree, output is a new tree with
 * `times` and `sign` injected at the top level.
 *
 * Canonical string rules (must stay aligned with the server):
 *  - top-level keys sorted lexicographically (a [TreeMap]); nested maps keep the
 *    incoming document order
 *  - scalars formatted as `key=urlEncode(value)` joined by `&`
 *  - booleans collapse to `1` / `0`
 *  - nested maps use square-bracket notation: `parent[child]=…`
 *  - arrays emit the index in brackets: `list[0]=…`
 *  - NaN / null values are dropped silently
 *  - URL encoding follows `encodeURIComponent` (patches `+`, `*`, `~`)
 *
 * Signature = `md5(md5(canonical) + secret)`.
 */
@Singleton
class ApiSigner @Inject constructor() {

    /**
     * Inject `times` and `sign` into [body]. Original content is preserved; only
     * the top-level object is cloned.
     *
     * `times` is emitted as a JSON **number** (Long), matching the byte-for-byte
     * output of the legacy Gson client: the server historically validated the
     * request JSON as well as the signature, and a stringified timestamp can
     * make the envelope diverge from the server's expected schema even though
     * the canonicalised signature still matches.
     */
    fun sign(body: JsonObject, secret: String): JsonObject {
        val sortedTop: TreeMap<String, JsonElement> = TreeMap(body)
        val times = System.currentTimeMillis()
        sortedTop["times"] = JsonPrimitive(times)

        val canonical = canonicalize(sortedTop)
        val innerHash = md5Hex(canonical)
        val signHash = md5Hex(innerHash + secret)
        sortedTop["sign"] = JsonPrimitive(signHash)

        return JsonObject(sortedTop)
    }

    private fun canonicalize(root: Map<String, JsonElement>): String {
        val accumulator = StringBuilder(root.size * 32)
        emitMapEntries(accumulator, root, null)
        return if (accumulator.isNotEmpty() && accumulator[0] == '&') {
            accumulator.substring(1)
        } else {
            accumulator.toString()
        }
    }

    private fun emit(sink: StringBuilder, value: JsonElement, key: String?) {
        when (value) {
            is JsonNull -> return
            is JsonPrimitive -> emitPrimitive(sink, value, key)
            is JsonObject -> emitMapEntries(sink, value, key)
            is JsonArray -> emitArray(sink, value, key)
        }
    }

    private fun emitPrimitive(sink: StringBuilder, value: JsonPrimitive, key: String?) {
        if (key == null) return
        if (value.isString) {
            appendScalar(sink, key, value.content)
            return
        }
        val raw = value.content
        // Booleans collapse to 1/0 to mirror the JS reference implementation.
        if (raw == "true") {
            appendScalar(sink, key, "1")
            return
        }
        if (raw == "false") {
            appendScalar(sink, key, "0")
            return
        }
        // Drop NaN to stay byte-compatible with the server canonicalizer.
        val asDouble = raw.toDoubleOrNull()
        if (asDouble != null && asDouble.isNaN()) return
        appendScalar(sink, key, raw)
    }

    private fun emitMapEntries(sink: StringBuilder, map: Map<String, JsonElement>, key: String?) {
        for ((childKey, childValue) in map) {
            val nextKey = if (key == null) childKey else key + bracket(childKey)
            emit(sink, childValue, nextKey)
        }
    }

    private fun emitArray(sink: StringBuilder, array: JsonArray, key: String?) {
        array.forEachIndexed { index, item ->
            val nextKey = if (key == null) index.toString() else key + bracket(index.toString())
            emit(sink, item, nextKey)
        }
    }

    private fun appendScalar(sink: StringBuilder, key: String?, raw: String) {
        if (key == null) return
        sink.append('&').append(key).append('=').append(encodeComponent(raw))
    }

    private fun encodeComponent(raw: String): String = try {
        URLEncoder.encode(raw, CHARSET)
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("%7E", "~")
    } catch (_: Exception) {
        raw
    }

    private fun bracket(raw: String): String = BRACKET_OPEN + raw + BRACKET_CLOSE

    private fun md5Hex(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return buildString(bytes.size * 2) {
            for (b in bytes) {
                val v = b.toInt() and 0xff
                if (v < 0x10) append('0')
                append(Integer.toHexString(v))
            }
        }
    }

    private companion object {
        const val CHARSET = "UTF-8"
        // Brackets are always URL-encoded; the contents between them are not.
        val BRACKET_OPEN: String = URLEncoder.encode("[", CHARSET)
        val BRACKET_CLOSE: String = URLEncoder.encode("]", CHARSET)
    }
}
