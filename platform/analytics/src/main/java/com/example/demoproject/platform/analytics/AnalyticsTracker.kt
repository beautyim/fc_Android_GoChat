package com.example.demoproject.platform.analytics

interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)
}
