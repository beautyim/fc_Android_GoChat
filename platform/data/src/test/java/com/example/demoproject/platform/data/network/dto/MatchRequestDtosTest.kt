package com.example.demoproject.platform.data.network.dto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MatchRequestDtosTest {

    private val json = Json { encodeDefaults = true }

    @Test
    fun start_serializesSessionCapabilities() {
        val body = json.encodeToString(MatchStartRequestDto())
        val json = Json.parseToJsonElement(body).jsonObject

        assertEquals("1", json.value("match_type"))
        assertEquals("2", json.value("match_sex"))
        assertEquals("1", json.value("support_ack"))
        assertEquals("1", json["caps"]!!.jsonObject.value("amv"))
    }

    @Test
    fun next_omitsSessionCapabilities() {
        val body = json.encodeToString(MatchNextRequestDto())
        val json = Json.parseToJsonElement(body).jsonObject

        assertEquals(setOf("match_type", "match_sex"), json.keys)
        assertFalse(json.containsKey("support_ack"))
        assertFalse(json.containsKey("caps"))
    }

    @Test
    fun heart_usesMatchSessionId() {
        val body = json.encodeToString(MatchHeartRequestDto(matchSessionId = 88001))
        val json = Json.parseToJsonElement(body).jsonObject

        assertEquals(setOf("match_session_id"), json.keys)
        assertEquals("88001", json.value("match_session_id"))
    }

    @Test
    fun emptyList_serializesAsCloseArrayBody() {
        assertEquals("[]", json.encodeToString(emptyList<String>()))
    }

    private fun JsonObject.value(key: String): String = getValue(key).jsonPrimitive.content
}
