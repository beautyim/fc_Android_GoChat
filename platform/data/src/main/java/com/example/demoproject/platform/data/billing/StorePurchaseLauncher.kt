package com.example.demoproject.platform.data.billing

import android.app.Activity
import com.example.demoproject.platform.data.repository.StorePurchaseRequest
import com.example.demoproject.platform.data.repository.StorePurchaseResult

/**
 * App-layer Google Play purchase entry (BillingClient). Feature modules call this
 * without depending on `:app`.
 */
fun interface StorePurchaseLauncher {
    fun launch(
        activity: Activity,
        request: StorePurchaseRequest,
        onResult: (StorePurchaseResult) -> Unit,
    )
}

object StorePurchaseLauncherHolder {
    @Volatile
    var launcher: StorePurchaseLauncher? = null
}
