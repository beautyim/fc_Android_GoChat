package com.example.demoproject.platform.analytics

/**
 * Process-wide analytics access for feature modules (same pattern as CallKitHolder).
 * Installed once from [com.example.demoproject.DemoApplication].
 */
object AnalyticsHolder {
    @Volatile
    var tracker: AnalyticsTracker? = null

    /** Durable pay / ROI reporter — prefer over [tracker] for [AnalyticsEvent.Pay]. */
    @Volatile
    var payReporter: PayAnalyticsReporter? = null
}
