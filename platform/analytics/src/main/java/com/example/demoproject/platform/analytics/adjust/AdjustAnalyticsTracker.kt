package com.example.demoproject.platform.analytics.adjust

import android.app.Application
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustConfig
import com.adjust.sdk.AdjustEvent
import com.adjust.sdk.LogLevel
import com.example.demoproject.platform.analytics.AnalyticsEvent
import com.example.demoproject.platform.analytics.AnalyticsInitializer
import com.example.demoproject.platform.analytics.AnalyticsTracker
import com.example.demoproject.platform.analytics.BuildConfig
import com.example.demoproject.platform.common.log.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AdjustAnalyticsTracker constructor(
    private val config: AdjustAnalyticsConfig,
    private val eventStore: AnalyticsEventStore,
) : AnalyticsTracker, AnalyticsInitializer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var initialized: Boolean = false

    /**
     * Invoked after an Adjust conversion event is successfully submitted to the SDK
     * (active / first_dialog / order_submit / pay / register). Used to trigger `POST /adjust/add`.
     */
    @Volatile
    var onConversionEventTracked: (() -> Unit)? = null

    override fun initialize(application: Application) {
        AppLogger.i(
            TAG,
            "init requested enabled=${config.enabled} environment=${config.environment} " +
                "appToken=${config.appToken.toFingerprint()} tokens=${config.tokenSummary()}",
        )
        if (!config.enabled) {
            AppLogger.i(TAG, "init skipped reason=disabled")
            return
        }
        if (config.appToken.isBlank()) {
            AppLogger.w(TAG, "init skipped reason=missing_app_token")
            return
        }

        val sdkLogLevel = if (BuildConfig.DEBUG) LogLevel.VERBOSE else LogLevel.WARN
        val adjustConfig = AdjustConfig(application, config.appToken, config.environment).apply {
            setLogLevel(sdkLogLevel)
            setOnAttributionChangedListener { attribution ->
                AdjustAttributionState.update(attribution)
                val snapshot = AdjustAttributionState.current()
                AppLogger.i(
                    TAG,
                    "attribution updated adid=${snapshot?.adid?.toFingerprint().orEmpty()} " +
                        "tracker=${snapshot?.trackerToken?.toFingerprint().orEmpty()} " +
                        "campaign=${snapshot?.campaign.orEmpty()}",
                )
            }
        }
        runCatching {
            Adjust.initSdk(adjustConfig)
        }.onSuccess {
            initialized = true
            AppLogger.i(
                TAG,
                "init success environment=${config.environment} sdkLogLevel=$sdkLogLevel " +
                    "appToken=${config.appToken.toFingerprint()}",
            )
        }.onFailure { error ->
            initialized = false
            AppLogger.e(
                TAG,
                "init failed environment=${config.environment} appToken=${config.appToken.toFingerprint()} " +
                    "error=${error.message}",
                error,
            )
        }
    }

    override fun track(event: AnalyticsEvent) {
        AppLogger.d(
            TAG,
            "track requested ${event.logSummary()} initialized=$initialized enabled=${config.enabled}",
        )
        if (!config.enabled) {
            AppLogger.i(TAG, "track skipped reason=disabled ${event.logSummary()}")
            return
        }
        if (event.isSandboxData) {
            AppLogger.i(TAG, "track skipped reason=sandbox_data ${event.logSummary()}")
            return
        }
        if (event is AnalyticsEvent.FirstDialog) {
            AppLogger.d(TAG, "first_dialog dedupe check started ${event.logSummary()}")
            scope.launch {
                if (eventStore.markFirstDialogTrackedIfNeeded()) {
                    AppLogger.i(TAG, "first_dialog dedupe passed ${event.logSummary()}")
                    trackNow(event)
                } else {
                    AppLogger.i(TAG, "track skipped reason=duplicate_first_dialog ${event.logSummary()}")
                }
            }
            return
        }
        trackNow(event)
    }

    private fun trackNow(event: AnalyticsEvent) {
        if (!initialized) {
            AppLogger.w(TAG, "track skipped reason=not_initialized ${event.logSummary()}")
            return
        }

        val token = config.eventToken(event)
        if (token.isBlank()) {
            AppLogger.w(TAG, "track skipped reason=missing_event_token ${event.logSummary()}")
            return
        }

        val callbackParameters = event.callbackParameters()
        val revenueSummary = (event as? AnalyticsEvent.Pay)?.revenueLogSummary().orEmpty()
        AppLogger.i(
            TAG,
            "track dispatching ${event.logSummary()} eventToken=${token.toFingerprint()} " +
                "callbackParams=${callbackParameters.toLogString()}$revenueSummary",
        )
        val adjustEvent = AdjustEvent(token).apply {
            addCallbackParameter("event_name", event.name)
            callbackParameters.forEach { (key, value) ->
                if (value.isNotBlank()) addCallbackParameter(key, value)
            }
            if (event is AnalyticsEvent.Pay && event.revenue != null && !event.currency.isNullOrBlank()) {
                setRevenue(event.revenue, event.currency)
            }
        }
        runCatching {
            Adjust.trackEvent(adjustEvent)
        }.onSuccess {
            AppLogger.i(
                TAG,
                "track submitted ${event.logSummary()} eventToken=${token.toFingerprint()}",
            )
            onConversionEventTracked?.invoke()
        }.onFailure { error ->
            AppLogger.e(
                TAG,
                "track failed ${event.logSummary()} eventToken=${token.toFingerprint()} error=${error.message}",
                error,
            )
        }
    }

    private fun AnalyticsEvent.callbackParameters(): Map<String, String> = when (this) {
        is AnalyticsEvent.Active -> emptyMap()
        is AnalyticsEvent.FirstDialog -> mapOf("user_id" to userId)
        is AnalyticsEvent.OrderSubmit -> mapOf(
            "goods_id" to goodsId.toString(),
            "product_id" to productId,
            "order_no" to orderNo,
        )
        is AnalyticsEvent.Pay -> buildMap {
            put("goods_id", goodsId.toString())
            put("product_id", productId)
            put("order_no", orderNo)
            revenue?.let { put("revenue", it.toString()) }
            currency?.let { put("currency", it) }
        }
        is AnalyticsEvent.Register -> mapOf("user_id" to userId)
    }

    private fun AdjustAnalyticsConfig.tokenSummary(): String = buildMap {
        put("active", eventToken(AnalyticsEvent.Active()).toFingerprint())
        put("first_dialog", eventToken(AnalyticsEvent.FirstDialog(userId = "")).toFingerprint())
        put("order_submit", eventToken(AnalyticsEvent.OrderSubmit(0L, "", "")).toFingerprint())
        put("pay", eventToken(AnalyticsEvent.Pay(0L, "", "", null, null)).toFingerprint())
        put("register", eventToken(AnalyticsEvent.Register(userId = "")).toFingerprint())
    }.entries.joinToString(prefix = "{", postfix = "}") { (key, value) -> "$key=$value" }

    private fun AnalyticsEvent.logSummary(): String = when (this) {
        is AnalyticsEvent.Active ->
            "event=$name sandbox=$isSandboxData"
        is AnalyticsEvent.FirstDialog ->
            "event=$name user=${userId.toFingerprint()} sandbox=$isSandboxData"
        is AnalyticsEvent.OrderSubmit ->
            "event=$name goodsId=$goodsId productId=$productId orderNo=${orderNo.toFingerprint()} sandbox=$isSandboxData"
        is AnalyticsEvent.Pay ->
            "event=$name goodsId=$goodsId productId=$productId orderNo=${orderNo.toFingerprint()} " +
                "hasRevenue=${revenue != null} currency=${currency.orEmpty()} sandbox=$isSandboxData"
        is AnalyticsEvent.Register ->
            "event=$name user=${userId.toFingerprint()} sandbox=$isSandboxData"
    }

    private fun AnalyticsEvent.Pay.revenueLogSummary(): String =
        if (revenue != null && !currency.isNullOrBlank()) {
            " revenue=$revenue currency=$currency"
        } else {
            " revenue=<none>"
        }

    private fun Map<String, String>.toLogString(): String =
        entries.joinToString(prefix = "{", postfix = "}") { (key, value) ->
            "$key=${value.toLogValue(key)}"
        }

    private fun String.toLogValue(key: String): String =
        when {
            isBlank() -> "<blank>"
            key.contains("user", ignoreCase = true) ||
                key.contains("order", ignoreCase = true) ||
                key.contains("token", ignoreCase = true) -> toFingerprint()
            else -> this
        }

    private fun String.toFingerprint(): String =
        if (isBlank()) {
            "<blank>"
        } else {
            "len=$length hash=${hashCode().toUInt().toString(16)}"
        }

    private companion object {
        const val TAG = "AdjustAnalytics"
    }
}
