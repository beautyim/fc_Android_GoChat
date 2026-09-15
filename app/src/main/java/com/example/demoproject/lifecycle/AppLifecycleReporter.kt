package com.example.demoproject.lifecycle

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.demoproject.platform.analytics.AnalyticsEvent
import com.example.demoproject.platform.analytics.AnalyticsTracker
import com.example.demoproject.platform.analytics.adjust.AdjustAttributionProvider
import com.example.demoproject.platform.common.log.AppLogger
import com.example.demoproject.platform.data.device.DefaultDeviceFingerprint
import com.example.demoproject.platform.data.local.pref.AppPrefs
import com.example.demoproject.platform.data.network.NetworkRuntime
import com.example.demoproject.platform.data.repository.AdjustAttributionPayload
import com.example.demoproject.platform.data.repository.AppSocketInfo
import com.example.demoproject.platform.mqtt.MqttConfig
import com.example.demoproject.platform.mqtt.MqttRuntime
import com.example.demoproject.platform.network.result.AppResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.example.demoproject.platform.analytics.adjust.AdjustAttributionPayload as AnalyticsAdjustPayload

/**
 * Reports app open/init/close, first-install Adjust `active`, and `/adjust/add` on conversion events.
 */
class AppLifecycleReporter(
    private val application: Application,
    private val analyticsTracker: AnalyticsTracker? = null,
    private val adjustAttributionProvider: AdjustAttributionProvider? = null,
    private val appPrefs: AppPrefs? = null,
) : DefaultLifecycleObserver {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val installStatMutex = Mutex()
    private var started = false
    private var foregroundGeneration = 0L

    fun start() {
        if (started) return
        started = true
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        AppLogger.i(TAG, "AppLifecycleReporter started")
    }

    /**
     * `POST /adjust/add` for a conversion event. Called every time Adjust successfully tracks
     * active / first_dialog / order_submit / pay / register — not gated by once-per-install.
     */
    fun requestAdjustAttributionUpload() {
        scope.launch {
            installStatMutex.withLock {
                reportAdjustAttributionForEvent()
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        val generation = ++foregroundGeneration
        scope.launch {
            mutex.withLock {
                val runtime = NetworkRuntime.get(application)
                val token = runtime.sessionManager.accessTokenSnapshot
                if (token.isNullOrBlank()) {
                    AppLogger.d(TAG, "skip open/init: no session")
                    return@withLock
                }
                when (val init = runtime.appSessionRepository.initApp()) {
                    is AppResult.Success -> {
                        saveMqttConfig(init.data.socket)
                        runtime.accountBalanceStore.update(init.data.accountMoney)
                        runtime.chatUnreadStore.update(init.data.messageUnread)
                        AppLogger.i(TAG, "/app/init ok")
                        reportInstallStatIfNeeded()
                    }
                    is AppResult.Failure -> {
                        AppLogger.w(TAG, "/app/init failed: ${init.message}")
                    }
                }
                if (generation != foregroundGeneration) return@withLock
                val fingerprint = DefaultDeviceFingerprint(application)
                val risks = fingerprint.riskSignals()
                runtime.appSessionRepository.openApp(
                    hasProxy = (risks["has_proxy"] as? Number)?.toInt() == 1,
                    hasVpn = (risks["has_vpn"] as? Number)?.toInt() == 1,
                )
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        scope.launch {
            mutex.withLock {
                val runtime = NetworkRuntime.get(application)
                if (runtime.sessionManager.accessTokenSnapshot.isNullOrBlank()) return@withLock
                runtime.appSessionRepository.closeApp()
            }
        }
    }

    private fun reportInstallStatIfNeeded() {
        val prefs = appPrefs ?: return
        scope.launch {
            installStatMutex.withLock {
                if (!prefs.installReported.first()) {
                    AppLogger.d(TAG, "/app/stat requesting first-install check")
                    val runtime = NetworkRuntime.get(application)
                    when (val result = runtime.appSessionRepository.checkFirstInstall()) {
                        is AppResult.Success -> {
                            AppLogger.i(TAG, "/app/stat resolved isFirstInstall=${result.data}")
                            if (result.data) {
                                AppLogger.i(TAG, "/app/stat first install detected, tracking active event")
                                analyticsTracker?.track(AnalyticsEvent.Active())
                            }
                            prefs.setFirstInstallDetected(result.data)
                            prefs.setInstallReported(true)
                        }
                        is AppResult.Failure -> {
                            AppLogger.w(TAG, "/app/stat failed: ${result.message}")
                        }
                    }
                } else {
                    AppLogger.d(TAG, "/app/stat skipped: already reported this install")
                }
            }
        }
    }

    private suspend fun reportAdjustAttributionForEvent() {
        val provider = adjustAttributionProvider
        if (provider == null) {
            AppLogger.w(TAG, "/adjust/add skipped: no attribution provider configured")
            return
        }
        AppLogger.d(TAG, "/adjust/add collecting attribution signals for conversion event")
        val payload = provider.collect().toRepositoryPayload()
        AppLogger.i(TAG, "/adjust/add collected ${payload.logSummary()}")
        if (!payload.hasSignals()) {
            AppLogger.w(TAG, "/adjust/add uploading empty attribution payload for conversion event")
        }
        AppLogger.i(TAG, "/adjust/add uploading ${payload.logSummary()}")
        val runtime = NetworkRuntime.get(application)
        when (val result = runtime.appSessionRepository.addAdjustAttribution(payload)) {
            is AppResult.Success -> {
                AppLogger.i(TAG, "/adjust/add attribution uploaded for conversion event")
            }
            is AppResult.Failure -> {
                AppLogger.w(TAG, "/adjust/add failed: ${result.message}")
            }
        }
    }

    private suspend fun saveMqttConfig(socket: AppSocketInfo) {
        if (socket.host.isBlank() || socket.clientId.isBlank()) {
            AppLogger.w(TAG, "/app/init socket_info missing host or client_id")
            return
        }
        val topic = socket.topic.takeIf { it.isNotBlank() } ?: "user/${socket.clientId}"
        MqttRuntime.get(application).saveConfig(
            MqttConfig(
                mqttHost = socket.host,
                clientId = socket.clientId,
                username = socket.username,
                password = socket.password,
                subscriptions = listOf(topic),
            ),
        )
        AppLogger.i(TAG, "/app/init mqtt config saved host=${socket.host}")
    }

    private fun AnalyticsAdjustPayload.toRepositoryPayload(): AdjustAttributionPayload =
        AdjustAttributionPayload(
            adid = adid,
            gpsAdid = gpsAdid,
            installReferrer = installReferrer,
            trackerToken = trackerToken,
            trackerName = trackerName,
            network = network,
            campaign = campaign,
            adgroup = adgroup,
            creative = creative,
            clickLabel = clickLabel,
            costType = costType,
            costAmount = costAmount,
            costCurrency = costCurrency,
            fbInstallReferrer = fbInstallReferrer,
        )

    private fun AdjustAttributionPayload.logSummary(): String {
        val hasReferrer = !installReferrer.isNullOrBlank()
        val hasFbReferrer = !fbInstallReferrer.isNullOrBlank()
        return "payload adid=${adid.toFingerprint()} gpsAdid=${gpsAdid.toFingerprint()} " +
            "trackerToken=${trackerToken.toFingerprint()} trackerName=${trackerName.orEmpty()} " +
            "network=${network.orEmpty()} campaign=${campaign.orEmpty()} adgroup=${adgroup.orEmpty()} " +
            "creative=${creative.orEmpty()} clickLabel=${clickLabel.orEmpty()} costType=${costType.orEmpty()} " +
            "costAmount=${costAmount ?: "<none>"} costCurrency=${costCurrency.orEmpty()} " +
            "hasInstallReferrer=$hasReferrer hasFbInstallReferrer=$hasFbReferrer " +
            "hasAdjustSignals=${hasAdjustAttributionSignals()}"
    }

    private fun String?.toFingerprint(): String =
        if (isNullOrBlank()) {
            "<blank>"
        } else {
            "len=$length hash=${hashCode().toUInt().toString(16)}"
        }

    private companion object {
        const val TAG = "AppLifecycleReporter"
    }
}
