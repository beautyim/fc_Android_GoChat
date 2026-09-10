package com.example.demoproject.platform.analytics.adjust

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.example.demoproject.platform.common.log.AppLogger
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class AdjustAttributionPayload(
    val adid: String? = null,
    val gpsAdid: String? = null,
    val installReferrer: String? = null,
    val trackerToken: String? = null,
    val trackerName: String? = null,
    val network: String? = null,
    val campaign: String? = null,
    val adgroup: String? = null,
    val creative: String? = null,
    val clickLabel: String? = null,
    val costType: String? = null,
    val costAmount: Double? = null,
    val costCurrency: String? = null,
    val fbInstallReferrer: String? = null,
) {
    fun hasSignals(): Boolean = listOf(
        adid,
        gpsAdid,
        installReferrer,
        trackerToken,
        trackerName,
        network,
        campaign,
        adgroup,
        creative,
        clickLabel,
        costType,
        costCurrency,
        fbInstallReferrer,
    ).any { !it.isNullOrBlank() } || costAmount != null
}

interface AdjustAttributionProvider {
    suspend fun collect(): AdjustAttributionPayload
}

class DefaultAdjustAttributionProvider(
    context: Context,
) : AdjustAttributionProvider {
    private val appContext = context.applicationContext

    override suspend fun collect(): AdjustAttributionPayload = coroutineScope {
        val snapshotDeferred = async { AdjustAttributionState.awaitFirstUpdate(ATTRIBUTION_WAIT_TIMEOUT_MS) }
        val gpsAdidDeferred = async { readGoogleAdvertisingId() }
        val referrerDeferred = async { readInstallReferrer() }
        val snapshot = snapshotDeferred.await()
        val gpsAdid = gpsAdidDeferred.await()
        val referrer = referrerDeferred.await()
        AdjustAttributionPayload(
            adid = snapshot?.adid,
            gpsAdid = gpsAdid,
            installReferrer = referrer?.installReferrer,
            trackerToken = snapshot?.trackerToken,
            trackerName = snapshot?.trackerName,
            network = snapshot?.network,
            campaign = snapshot?.campaign,
            adgroup = snapshot?.adgroup,
            creative = snapshot?.creative,
            clickLabel = snapshot?.clickLabel,
            costType = snapshot?.costType,
            costAmount = snapshot?.costAmount,
            costCurrency = snapshot?.costCurrency,
            fbInstallReferrer = snapshot?.fbInstallReferrer,
        )
    }

    private fun readGoogleAdvertisingId(): String? =
        runCatching {
            AdvertisingIdClient.getAdvertisingIdInfo(appContext)?.id
        }.onFailure { error ->
            AppLogger.w(TAG, "gaid read failed: ${error.message}")
        }.getOrNull()
            ?.takeIf { it.isNotBlank() }

    private suspend fun readInstallReferrer(): InstallReferrerSnapshot? =
        withTimeoutOrNull(INSTALL_REFERRER_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val client = InstallReferrerClient.newBuilder(appContext).build()
                continuation.invokeOnCancellation { runCatching { client.endConnection() } }
                client.startConnection(
                    object : InstallReferrerStateListener {
                        override fun onInstallReferrerSetupFinished(responseCode: Int) {
                            val snapshot = if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                                runCatching {
                                    client.installReferrer?.let { details ->
                                        InstallReferrerSnapshot(installReferrer = details.installReferrer)
                                    }
                                }.onFailure { error ->
                                    AppLogger.w(TAG, "install referrer read failed: ${error.message}")
                                }.getOrNull()
                            } else {
                                AppLogger.i(TAG, "install referrer unavailable responseCode=$responseCode")
                                null
                            }
                            runCatching { client.endConnection() }
                            if (continuation.isActive) continuation.resume(snapshot)
                        }

                        override fun onInstallReferrerServiceDisconnected() {
                            if (continuation.isActive) continuation.resume(null)
                        }
                    },
                )
            }
        }

    private data class InstallReferrerSnapshot(
        val installReferrer: String?,
    )

    private companion object {
        const val TAG = "AdjustAttribution"
        const val INSTALL_REFERRER_TIMEOUT_MS = 3_000L
        const val ATTRIBUTION_WAIT_TIMEOUT_MS = 4_000L
    }
}
