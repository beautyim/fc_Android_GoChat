package com.example.demoproject.platform.mqtt

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchSignalParserTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `success parses direct room timing and peer`() {
        val signal = MatchSignalParser.parse(
            raw = """
                {
                  "type": 4,
                  "data": {
                    "a_type": 2,
                    "data": {
                      "key": 1,
                      "match_session_id": 88001,
                      "match_id": 99002,
                      "user_info": {"uid": 10240002, "nickname": "Anna"},
                      "room_info": {
                        "room_id": "sc_1",
                        "channel": "ch_1",
                        "token": "token",
                        "uid": 12345,
                        "fencing_token": "fence",
                        "match_time": 60,
                        "next_time": 6,
                        "rtc_mode": 1,
                        "is_amv": 0
                      }
                    }
                  }
                }
            """.trimIndent(),
            json = json,
        ) as MatchSignal.Success

        assertEquals(88001L, signal.matchSessionId)
        assertEquals("10240002", signal.peer?.userId)
        assertEquals(60, signal.room?.matchTimeSeconds)
        assertTrue(signal.room?.canJoinDirectly == true)
        assertFalse(signal.room?.isReceiveOnly == true)
    }

    @Test
    fun `type 7 fallback creates peer from uid`() {
        val signal = MatchSignalParser.parse(
            """{"type":7,"data":{"type":25,"uid":10240002}}""",
            json,
        ) as MatchSignal.Success

        assertEquals("10240002", signal.peer?.userId)
    }

    @Test
    fun `match call invite parses receive only room`() {
        val signal = MatchSignalParser.parse(
            """
                {
                  "type":3,
                  "data":{
                    "a_type":1,
                    "is_match_call":1,
                    "room_id":"sc_2",
                    "channel":"ch_2",
                    "token":"token",
                    "uid":10,
                    "rtc_mode":2,
                    "is_amv":1,
                    "user_info":{"uid":20}
                  }
                }
            """.trimIndent(),
            json,
        ) as MatchSignal.RoomReady

        assertTrue(signal.room.isReceiveOnly)
        assertEquals("20", signal.peer?.userId)
    }

    @Test
    fun `empty is terminal`() {
        val signal = MatchSignalParser.parse(
            """
                {
                  "type":4,
                  "data":{"a_type":2,"data":{"key":3,"match_session_id":88001}}
                }
            """.trimIndent(),
            json,
        ) as MatchSignal.Terminal

        assertEquals(MatchSignal.Terminal.Kind.Empty, signal.kind)
        assertEquals(88001L, signal.matchSessionId)
    }
}
