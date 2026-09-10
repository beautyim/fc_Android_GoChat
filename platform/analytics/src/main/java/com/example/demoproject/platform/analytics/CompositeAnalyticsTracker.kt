package com.example.demoproject.platform.analytics

import com.example.demoproject.platform.common.log.AppLogger

class CompositeAnalyticsTracker(
    private val trackers: List<AnalyticsTracker>,
) : AnalyticsTracker {

    override fun track(event: AnalyticsEvent) {
        trackers.forEach { tracker ->
            runCatching {
                tracker.track(event)
            }.onFailure { error ->
                AppLogger.w(TAG, "analytics track failed: ${tracker.javaClass.simpleName} event=${event.name}", error)
            }
        }
    }

    private companion object {
        const val TAG = "AnalyticsTrack"
    }
}
