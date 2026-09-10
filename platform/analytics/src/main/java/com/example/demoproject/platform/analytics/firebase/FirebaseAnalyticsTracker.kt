package com.example.demoproject.platform.analytics.firebase

import android.app.Application
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.example.demoproject.platform.analytics.AnalyticsEvent
import com.example.demoproject.platform.analytics.AnalyticsInitializer
import com.example.demoproject.platform.analytics.AnalyticsTracker
import com.example.demoproject.platform.common.log.AppLogger

class FirebaseAnalyticsTracker constructor(
    private val firebaseAnalytics: FirebaseAnalytics,
) : AnalyticsTracker, AnalyticsInitializer {

    override fun initialize(application: Application) {
        AppLogger.i(TAG, "init success")
    }

    override fun track(event: AnalyticsEvent) {
        if (event.isSandboxData) {
            AppLogger.i(TAG, "track skipped reason=sandbox_data event=${event.name}")
            return
        }
        val firebaseEvent = event.toFirebaseEventName()
        val params = event.toFirebaseParams()
        runCatching {
            firebaseAnalytics.logEvent(firebaseEvent, params)
        }.onSuccess {
            AppLogger.i(TAG, "track submitted event=$firebaseEvent")
        }.onFailure { error ->
            AppLogger.w(TAG, "track failed event=$firebaseEvent error=${error.message}", error)
        }
    }

    private fun AnalyticsEvent.toFirebaseEventName(): String = when (this) {
        is AnalyticsEvent.Register -> FirebaseAnalytics.Event.SIGN_UP
        is AnalyticsEvent.Pay -> FirebaseAnalytics.Event.PURCHASE
        else -> name
    }

    private fun AnalyticsEvent.toFirebaseParams(): Bundle = Bundle().apply {
        putString("berrycam_event_name", name)
        when (val event = this@toFirebaseParams) {
            is AnalyticsEvent.Active -> Unit
            is AnalyticsEvent.FirstDialog -> {
                putString("user_hash", event.userId.toFingerprint())
            }
            is AnalyticsEvent.OrderSubmit -> {
                putLong("goods_id", event.goodsId)
                putString(FirebaseAnalytics.Param.ITEM_ID, event.productId)
                putString("order_hash", event.orderNo.toFingerprint())
            }
            is AnalyticsEvent.Pay -> {
                putLong("goods_id", event.goodsId)
                putString(FirebaseAnalytics.Param.ITEM_ID, event.productId)
                putString(FirebaseAnalytics.Param.TRANSACTION_ID, event.orderNo)
                event.revenue?.let { putDouble(FirebaseAnalytics.Param.VALUE, it) }
                event.currency?.takeIf { it.isNotBlank() }?.let {
                    putString(FirebaseAnalytics.Param.CURRENCY, it)
                }
            }
            is AnalyticsEvent.Register -> {
                putString(FirebaseAnalytics.Param.METHOD, "berrycam")
                putString("user_hash", event.userId.toFingerprint())
            }
        }
    }

    private fun String.toFingerprint(): String =
        if (isBlank()) "<blank>" else hashCode().toUInt().toString(16)

    private companion object {
        const val TAG = "FirebaseAnalytics"
    }
}
