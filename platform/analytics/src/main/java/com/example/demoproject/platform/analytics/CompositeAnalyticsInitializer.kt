package com.example.demoproject.platform.analytics

import android.app.Application
import com.example.demoproject.platform.common.log.AppLogger

class CompositeAnalyticsInitializer(
    private val initializers: List<AnalyticsInitializer>,
) : AnalyticsInitializer {

    override fun initialize(application: Application) {
        initializers.forEach { initializer ->
            runCatching {
                initializer.initialize(application)
            }.onFailure { error ->
                AppLogger.w(TAG, "analytics initializer failed: ${initializer.javaClass.simpleName}", error)
            }
        }
    }

    private companion object {
        const val TAG = "AnalyticsInit"
    }
}
