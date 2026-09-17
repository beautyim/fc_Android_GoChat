package com.example.demoproject.platform.analytics

import com.example.demoproject.platform.analytics.adjust.AdjustAnalyticsTracker
import com.example.demoproject.platform.analytics.adjust.PayAnalyticsOutbox
import com.example.demoproject.platform.analytics.adjust.PaySubmitResult
import com.example.demoproject.platform.analytics.adjust.PendingPayRecord
import com.example.demoproject.platform.analytics.firebase.FirebaseAnalyticsTracker
import com.example.demoproject.platform.common.log.AppLogger

/**
 * Durable, idempotent reporter for purchase `pay` events (Adjust ROI + Firebase PURCHASE).
 * Call sites must use this instead of [AnalyticsTracker.track] for [AnalyticsEvent.Pay].
 */
class PayAnalyticsReporter(
    private val outbox: PayAnalyticsOutbox,
    private val adjustTracker: AdjustAnalyticsTracker,
    private val firebaseTracker: FirebaseAnalyticsTracker,
) {
    /**
     * Enqueues [event] under [dedupeKey] (defaults to [AnalyticsEvent.Pay.orderNo]), attempts
     * Adjust + Firebase, and marks terminal success so verify-path / MQTT cannot double-fire.
     */
    suspend fun reportPay(
        event: AnalyticsEvent.Pay,
        dedupeKey: String = event.orderNo,
    ) {
        val key = dedupeKey.trim()
        if (key.isNotEmpty() && outbox.isSubmitted(key)) {
            AppLogger.i(TAG, "pay skipped reason=already_submitted order=${key.toFingerprint()}")
            return
        }

        val pending = if (key.isNotEmpty()) {
            outbox.enqueue(
                PendingPayRecord(
                    orderNo = key,
                    goodsId = event.goodsId,
                    productId = event.productId,
                    revenue = event.revenue,
                    currency = event.currency,
                    isSandboxData = event.isSandboxData,
                ),
            )
        } else {
            null
        }

        val payEvent = event.copy(
            // Keep Adjust callback order_no as the real tran when present; fallback to dedupe key.
            orderNo = event.orderNo.ifBlank { key },
        )

        if (pending?.firebaseSent != true) {
            runCatching { firebaseTracker.track(payEvent) }
                .onFailure { error ->
                    AppLogger.w(TAG, "firebase pay track failed: ${error.message}", error)
                }
            if (key.isNotEmpty()) {
                outbox.markFirebaseSent(key)
            }
        }

        when (val result = adjustTracker.submitPay(payEvent)) {
            PaySubmitResult.Submitted,
            PaySubmitResult.SkippedTerminal,
            -> {
                if (key.isNotEmpty()) {
                    outbox.markSubmitted(key)
                }
                AppLogger.i(TAG, "pay settled result=$result order=${key.toFingerprint()}")
            }
            PaySubmitResult.SkippedRetryable,
            PaySubmitResult.Failed,
            -> {
                AppLogger.w(
                    TAG,
                    "pay left pending result=$result order=${key.toFingerprint()} " +
                        "(will retry on flush)",
                )
            }
        }
    }

    suspend fun flushPending() {
        val pending = outbox.pending()
        if (pending.isEmpty()) {
            AppLogger.d(TAG, "pay flush skipped: empty outbox")
            return
        }
        AppLogger.i(TAG, "pay flush starting count=${pending.size}")
        pending.forEach { record ->
            if (outbox.isSubmitted(record.orderNo)) {
                // Drop orphan pending rows that were already settled.
                outbox.markSubmitted(record.orderNo)
                return@forEach
            }
            reportPay(
                event = AnalyticsEvent.Pay(
                    goodsId = record.goodsId,
                    productId = record.productId,
                    orderNo = record.orderNo,
                    revenue = record.revenue,
                    currency = record.currency,
                    isSandboxData = record.isSandboxData,
                ),
                dedupeKey = record.orderNo,
            )
        }
    }

    suspend fun hasReported(dedupeKey: String): Boolean =
        dedupeKey.isNotBlank() && outbox.isSubmitted(dedupeKey.trim())

    private fun String.toFingerprint(): String =
        if (isBlank()) "<blank>" else "len=$length hash=${hashCode().toUInt().toString(16)}"

    private companion object {
        const val TAG = "PayAnalytics"
    }
}
