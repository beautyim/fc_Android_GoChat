package com.example.demoproject.platform.network.result

import android.content.Context
import com.example.demoproject.platform.network.R
import java.util.Locale

/**
 * User-facing network copy. Bound from [com.example.demoproject.platform.data.network.NetworkRuntime]
 * so SafeApiCall / repositories can localize without holding a Context on every call.
 */
object NetworkUserMessages {
    @Volatile
    private var appContext: Context? = null

    fun bind(context: Context) {
        appContext = context.applicationContext
    }

    fun requestFailed(): String {
        val context = appContext ?: return AppResult.DEFAULT_REQUEST_FAILED_MESSAGE
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(Locale.getDefault())
        return context.createConfigurationContext(config).getString(R.string.network_request_failed)
    }
}
