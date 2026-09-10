package com.example.demoproject.platform.analytics

import android.app.Application

interface AnalyticsInitializer {
    fun initialize(application: Application)
}
