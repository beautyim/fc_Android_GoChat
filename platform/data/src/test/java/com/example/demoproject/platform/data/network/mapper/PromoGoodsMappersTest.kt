package com.example.demoproject.platform.data.network.mapper

import com.example.demoproject.platform.data.network.dto.VipAlertResponseDto
import com.example.demoproject.platform.data.promotion.TreasureUserTier
import com.example.demoproject.platform.data.repository.BillingProductType
import com.example.demoproject.platform.data.repository.PromoGoodsContent
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PromoGoodsMappersTest {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun rechargeAlert_listCoinProduct_isNotVipPopup() {
        val dto = json.decodeFromString(
            VipAlertResponseDto.serializer(),
            """
            {
              "func_name":"recharge_alert",
              "func_data":{
                "balance":196,
                "list":[{
                  "id":200123,
                  "sku":"com.gochat.coins.tier3",
                  "is_hot":1,
                  "product_type":1,
                  "money":3.89,
                  "original":7.07,
                  "original_desc":"$7.07",
                  "money_desc":"$3.89",
                  "sale_desc":"45% OFF",
                  "save_rate":"45%",
                  "unit":1,
                  "diamond":241,
                  "title":"241 Coins",
                  "currency_unit":1,
                  "hidden":0,
                  "sale_icon":"s/coin/sale_50_icon.png",
                  "label_type":2,
                  "coin_icon":"gochat/s/coin/300.png",
                  "label":"BEST VALUE",
                  "save_money":"$3.18"
                }],
                "default":200123,
                "default_sku":"com.gochat.coins.tier3"
              }
            }
            """.trimIndent(),
        )

        // PaidNonVip previously preferred any list row decoded as VIP.
        val goods = dto.toPromoGoodsOrNull(TreasureUserTier.PaidNonVip)
        assertNotNull(goods)
        assertEquals(BillingProductType.Coins, goods!!.productType)
        assertEquals("com.gochat.coins.tier3", goods.sku)
        assertTrue(goods.content is PromoGoodsContent.SmallCoins)
        val small = goods.content as PromoGoodsContent.SmallCoins
        assertEquals(241, small.coins)
        assertFalse(small.coinIconUrl.isNullOrBlank())
    }

    @Test
    fun vipDiscountAlert_vipPayItem_staysVipPopup() {
        val dto = json.decodeFromString(
            VipAlertResponseDto.serializer(),
            """
            {
              "func_name":"vip_discount_alert",
              "func_data":{
                "vip_pay_item":{
                  "id":200204,
                  "sku":"com.gochat.vip.week.tr",
                  "title":"Week",
                  "days":7,
                  "month":1,
                  "money":1.89,
                  "money_desc":"$1.89",
                  "product_type":2,
                  "original":"$4.99",
                  "give_coins":98,
                  "match":1,
                  "extra_rewards":{"match":1}
                }
              }
            }
            """.trimIndent(),
        )

        val goods = dto.toPromoGoodsOrNull(TreasureUserTier.PaidNonVip)
        assertNotNull(goods)
        assertEquals(BillingProductType.Vip, goods!!.productType)
        assertTrue(goods.content is PromoGoodsContent.Vip)
        val vip = goods.content as PromoGoodsContent.Vip
        assertEquals("1 Week VIP", vip.vipTitle)
        assertEquals(98, vip.bonusCoins)
    }
}
