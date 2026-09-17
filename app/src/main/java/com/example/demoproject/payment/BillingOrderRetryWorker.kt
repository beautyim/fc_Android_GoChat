package com.example.demoproject.payment

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.demoproject.platform.analytics.AnalyticsHolder
import com.example.demoproject.platform.data.network.NetworkRuntime
import java.util.concurrent.TimeUnit

class BillingOrderRetryWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val runtime = NetworkRuntime.get(applicationContext)
        val coordinator = StorePurchaseCoordinator(
            context = applicationContext,
            repository = runtime.billingRepository,
            orderStore = runtime.billingOrderStore,
            vipStatusStore = runtime.vipStatusStore,
            isFakePaymentEnabled = { runtime.appPrefs.isFakePaymentEnabled() },
            analyticsTracker = AnalyticsHolder.tracker,
            payAnalyticsReporter = AnalyticsHolder.payReporter,
            onPaymentSucceeded = {
                runtime.coinRepository.getRechargePage()
                runtime.vipRepository.getVipPage()
            },
        )
        return runCatching {
            AnalyticsHolder.payReporter?.flushPending()
            coordinator.retryPendingOrders()
            Result.success()
        }.getOrElse {
            if (runAttemptCount >= MAX_RETRIES) Result.success() else Result.retry()
        }
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "billing_order_retry"
        private const val IMMEDIATE_WORK_NAME = "billing_order_retry_immediate"
        private const val MAX_RETRIES = 3

        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<BillingOrderRetryWorker>(15, TimeUnit.MINUTES)
                .setConstraints(networkConstraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun enqueueImmediate(context: Context) {
            val request = OneTimeWorkRequestBuilder<BillingOrderRetryWorker>()
                .setConstraints(networkConstraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        private fun networkConstraints(): Constraints =
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
    }
}
