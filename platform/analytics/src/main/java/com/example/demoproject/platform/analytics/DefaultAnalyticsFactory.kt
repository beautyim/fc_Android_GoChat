package com.example.demoproject.platform.analytics

import android.app.Application
import com.google.firebase.analytics.FirebaseAnalytics
import com.example.demoproject.platform.analytics.adjust.AdjustAttributionProvider
import com.example.demoproject.platform.analytics.adjust.AdjustAnalyticsConfig
import com.example.demoproject.platform.analytics.adjust.AdjustAnalyticsTracker
import com.example.demoproject.platform.analytics.adjust.AnalyticsEventStore
import com.example.demoproject.platform.analytics.adjust.DefaultAdjustAttributionProvider
import com.example.demoproject.platform.analytics.firebase.FirebaseAnalyticsTracker

/**
 * Manual DI factory (no Hilt Application component). Call [create] once from
 * `DemoApplication.onCreate()` and reuse [DefaultAnalytics.tracker] / [AnalyticsHolder].
 */
object DefaultAnalyticsFactory {

    fun create(application: Application): DefaultAnalytics {
        val adjust = AdjustAnalyticsTracker(
            config = AdjustAnalyticsConfig(),
            eventStore = AnalyticsEventStore(application.applicationContext),
        )
        val firebase = FirebaseAnalyticsTracker(FirebaseAnalytics.getInstance(application))
        val attributionProvider = DefaultAdjustAttributionProvider(application.applicationContext)
        return DefaultAnalytics(
            tracker = CompositeAnalyticsTracker(listOf(adjust, firebase)),
            initializer = CompositeAnalyticsInitializer(listOf(adjust, firebase)),
            adjustAttributionProvider = attributionProvider,
            adjustTracker = adjust,
        )
    }
}

data class DefaultAnalytics(
    val tracker: AnalyticsTracker,
    val initializer: AnalyticsInitializer,
    val adjustAttributionProvider: AdjustAttributionProvider,
    val adjustTracker: AdjustAnalyticsTracker,
)
