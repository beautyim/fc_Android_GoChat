package com.example.demoproject.platform.data.network.dto

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class GooglePayDtosTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun check_decodesPaymentMethodPopup() {
        val dto = json.decodeFromString<GooglePayCheckResponseDto>(
            """{"pop_type":2,"pop":{"title":"Choose","list":[{"title":"Other","icon":"pay/h5.png","type":2}]}}""",
        )

        assertEquals(2, dto.popType)
        assertEquals("Choose", dto.pop?.title)
        assertEquals(2, dto.pop?.list?.single()?.type)
    }

    @Test
    fun create_decodesDirectExternalUrlAndTopLevelOrderFields() {
        val dto = json.decodeFromString<GooglePayCreateResponseDto>(
            """{"tran_no":"T1","product_id":"coin_1","goods_id":1,"pay_type":1,"price":"4.99","pay_url":"https://pay.example/1"}""",
        )

        assertEquals("T1", dto.tranNo)
        assertEquals("coin_1", dto.productId)
        assertEquals("https://pay.example/1", dto.payUrl)
    }

    @Test
    fun create_decodesCallbackOnlyExternalCheckout() {
        val dto = json.decodeFromString<GooglePayCreateResponseDto>(
            """{"callback":{"func_name":"open","func_data":{"page_name":"webview","data":{"redirect_url":"https://testpay.example.com?product_id=200121"}}}}""",
        )

        assertEquals("", dto.tranNo)
        assertEquals("", dto.productId)
        assertNotNull(dto.callback)
    }

    @Test
    fun cancel_keepsCamelCaseServerContract() {
        val encoded = json.encodeToString(
            GooglePayCancelRequestDto(tranNo = "T1", errorCode = 9, googleCode = 6),
        )
        val obj = Json.parseToJsonElement(encoded).jsonObject

        assertEquals("9", obj.getValue("errorCode").jsonPrimitive.content)
        assertEquals("6", obj.getValue("googleCode").jsonPrimitive.content)
        assertFalse(obj.containsKey("error_code"))
    }

    @Test
    fun messageSync_decodesPayEventsForRecovery() {
        val dto = json.decodeFromString<ConversationListResponseDto>(
            """{"pay_event_list":[{"event_id":"10086","transaction_id":"GPA.1","currency":"USD","value":"4.99","goods_id":101,"items":{"item_id":"coin_100"}}]}""",
        )

        val event = dto.payEventList.single()
        assertEquals(10086L, event.eventId)
        assertEquals("4.99", event.value)
        assertEquals("coin_100", event.items?.itemId)
    }
}
