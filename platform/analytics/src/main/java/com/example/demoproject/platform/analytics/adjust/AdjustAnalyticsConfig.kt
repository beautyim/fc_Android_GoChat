package com.example.demoproject.platform.analytics.adjust

import com.adjust.sdk.AdjustConfig
import com.example.demoproject.platform.analytics.AnalyticsEvent
import com.example.demoproject.platform.analytics.BuildConfig

class AdjustAnalyticsConfig() {
    val enabled: Boolean = BuildConfig.ADJUST_ENABLED
    val appToken: String = BuildConfig.ADJUST_APP_TOKEN
    val environment: String =
        if (BuildConfig.ADJUST_ENVIRONMENT.equals(PRODUCTION, ignoreCase = true)) {
            AdjustConfig.ENVIRONMENT_PRODUCTION
        } else {
            AdjustConfig.ENVIRONMENT_SANDBOX
        }

    fun eventToken(event: AnalyticsEvent): String = when (event) {
        is AnalyticsEvent.Active -> BuildConfig.ADJUST_EVENT_ACTIVE
        is AnalyticsEvent.FirstDialog -> BuildConfig.ADJUST_EVENT_FIRST_DIALOG
        is AnalyticsEvent.OrderSubmit -> BuildConfig.ADJUST_EVENT_ORDER_SUBMIT
        is AnalyticsEvent.Pay -> BuildConfig.ADJUST_EVENT_PAY
        is AnalyticsEvent.Register -> BuildConfig.ADJUST_EVENT_REGISTER
    }

    private companion object {
        const val PRODUCTION = "production"
    }
}
