package com.example.demoproject.platform.data.network.mapper

import com.example.demoproject.platform.data.repository.MatchStartAction
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MatchStartPayloadParserTest {

    @Test
    fun parse_acceptsSessionAndHeartIntervalAliases() {
        val aliases = listOf("heart_interval", "heartInterval", "heart_interval_sec")

        aliases.forEach { alias ->
            val payload = Json.parseToJsonElement(
                """{"session_id":88001,"match_id":99001,"$alias":5}""",
            )
            val result = MatchStartPayloadParser.parse(payload)

            assertEquals(88001L, result.sessionId)
            assertEquals(99001L, result.matchId)
            assertEquals(5, result.heartIntervalSeconds)
        }
    }

    @Test
    fun parse_ignoresNonPositiveHeartInterval() {
        val payload = Json.parseToJsonElement("""{"heart_interval":0}""")

        assertNull(MatchStartPayloadParser.parse(payload).heartIntervalSeconds)
    }

    @Test
    fun parse_fallsBackToUidForProfileUserId() {
        val payload = Json.parseToJsonElement(
            """{"match_info":{"uid":10240002,"nickname":"Anna"}}""",
        )

        val candidate = MatchStartPayloadParser.parse(payload).matchedUser

        assertEquals("10240002", candidate?.profileUserId)
    }

    @Test
    fun parseNext_ignoresMatchedUserAndForcesDirectCall() {
        val payload = Json.parseToJsonElement(
            """{"match_session_id":88002,"match_info":{"uid":10240002,"nickname":"Anna"}}""",
        )

        val result = MatchStartPayloadParser.parseNext(payload)

        assertNull(result.matchedUser)
        assertEquals(MatchStartAction.DirectVideoCall, result.nextAction)
    }
}
