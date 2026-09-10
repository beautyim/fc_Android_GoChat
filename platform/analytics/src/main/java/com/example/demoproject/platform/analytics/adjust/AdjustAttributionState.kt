package com.example.demoproject.platform.analytics.adjust

import com.adjust.sdk.AdjustAttribution
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

data class AdjustAttributionSnapshot(
    val adid: String?,
    val trackerToken: String?,
    val trackerName: String?,
    val network: String?,
    val campaign: String?,
    val adgroup: String?,
    val creative: String?,
    val clickLabel: String?,
    val costType: String?,
    val costAmount: Double?,
    val costCurrency: String?,
    val fbInstallReferrer: String?,
)

object AdjustAttributionState {
    @Volatile
    private var snapshot: AdjustAttributionSnapshot? = null

    @Volatile
    private var firstUpdateSignal: CompletableDeferred<Unit> = CompletableDeferred()

    fun update(attribution: AdjustAttribution?) {
        snapshot = attribution?.toSnapshot()
        if (!firstUpdateSignal.isCompleted) firstUpdateSignal.complete(Unit)
    }

    fun current(): AdjustAttributionSnapshot? = snapshot

    /**
     * Adjust resolves attribution asynchronously after SDK init, so a snapshot may not be
     * ready yet when we're about to collect data for `/adjust/add`. Wait briefly for the first
     * `onAttributionChanged` callback so we don't upload a payload that only carries
     * device-level signals (GAID/install referrer) while missing the real campaign data.
     */
    suspend fun awaitFirstUpdate(timeoutMs: Long): AdjustAttributionSnapshot? {
        if (!firstUpdateSignal.isCompleted) {
            withTimeoutOrNull(timeoutMs) { firstUpdateSignal.await() }
        }
        return snapshot
    }
}

private fun AdjustAttribution.toSnapshot(): AdjustAttributionSnapshot =
    AdjustAttributionSnapshot(
        adid = readString("adid"),
        trackerToken = readString("trackerToken"),
        trackerName = readString("trackerName"),
        network = readString("network"),
        campaign = readString("campaign"),
        adgroup = readString("adgroup"),
        creative = readString("creative"),
        clickLabel = readString("clickLabel"),
        costType = readString("costType"),
        costAmount = readDouble("costAmount"),
        costCurrency = readString("costCurrency"),
        fbInstallReferrer = readString("fbInstallReferrer"),
    )

private fun AdjustAttribution.readString(name: String): String? {
    val fromGetter = readMemberValue<Any?>(getterName(name)) as? String
    if (!fromGetter.isNullOrBlank()) return fromGetter
    val fromField = readMemberValue<Any?>(name) as? String
    return fromField?.takeIf { it.isNotBlank() }
}

private fun AdjustAttribution.readDouble(name: String): Double? {
    val fromGetter = readMemberValue<Any?>(getterName(name))
    if (fromGetter is Number) return fromGetter.toDouble()
    val fromField = readMemberValue<Any?>(name)
    return (fromField as? Number)?.toDouble()
}

private fun getterName(name: String): String =
    "get" + name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

private fun <T> AdjustAttribution.readMemberValue(memberName: String): T? =
    runCatching {
        val method = javaClass.methods.firstOrNull { it.name == memberName && it.parameterCount == 0 }
        @Suppress("UNCHECKED_CAST")
        if (method != null) return@runCatching method.invoke(this) as? T
        val field = javaClass.declaredFields.firstOrNull { it.name == memberName } ?: return@runCatching null
        field.isAccessible = true
        field.get(this) as? T
    }.getOrNull()
