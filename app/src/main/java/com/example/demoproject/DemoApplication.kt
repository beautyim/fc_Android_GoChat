package com.example.demoproject

import android.app.Application
import com.example.demoproject.chat.ChatMqttInbox
import com.example.demoproject.chat.ChatMqttSyncCoordinator
import com.example.demoproject.lifecycle.AppLifecycleReporter
import com.example.demoproject.payment.BillingOrderRetryWorker
import com.example.demoproject.payment.PaymentMethodSheetController
import com.example.demoproject.payment.PayEventMqttCoordinator
import com.example.demoproject.payment.StorePurchaseCoordinator
import com.example.demoproject.platform.analytics.AnalyticsHolder
import com.example.demoproject.platform.analytics.DefaultAnalytics
import com.example.demoproject.platform.analytics.DefaultAnalyticsFactory
import com.example.demoproject.platform.callkit.CallCoordinator
import com.example.demoproject.platform.callkit.CallKitHolder
import com.example.demoproject.platform.data.billing.StorePurchaseLauncher
import com.example.demoproject.platform.data.billing.StorePurchaseLauncherHolder
import com.example.demoproject.platform.data.local.pref.AppPrefs
import com.example.demoproject.platform.data.network.NetworkRuntime
import com.example.demoproject.platform.data.repository.HttpCallSignalingActions
import com.example.demoproject.platform.mqtt.CurrentUserIdProvider
import com.example.demoproject.platform.mqtt.MqttCallSignalingClient
import com.example.demoproject.platform.mqtt.MqttRuntime
import com.example.demoproject.platform.rtc.agora.AgoraRtcClient
import com.example.demoproject.push.PushClient
import com.example.demoproject.push.PushGateway
import com.example.demoproject.wallet.AccountMqttInbox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DemoApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var analytics: DefaultAnalytics
        private set

    private lateinit var appLifecycleReporter: AppLifecycleReporter

    val pushGateway: PushGateway by lazy { PushClient(this) }
    val paymentMethodSheetController = PaymentMethodSheetController()

    val storePurchaseCoordinator: StorePurchaseCoordinator by lazy {
        val runtime = NetworkRuntime.get(this)
        StorePurchaseCoordinator(
            context = this,
            repository = runtime.billingRepository,
            orderStore = runtime.billingOrderStore,
            vipStatusStore = runtime.vipStatusStore,
            isFakePaymentEnabled = { runtime.appPrefs.isFakePaymentEnabled() },
            analyticsTracker = analytics.tracker,
            payAnalyticsReporter = analytics.payReporter,
            selectPaymentMethod = paymentMethodSheetController::select,
            onPaymentSucceeded = {
                runtime.coinRepository.getRechargePage()
                runtime.vipRepository.getVipPage()
                com.example.demoproject.platform.data.promotion.PromotionPurchasePageTracker.markPurchased()
                com.example.demoproject.platform.data.promotion.PromotionRechargeGuideTracker.markPurchased()
                com.example.demoproject.platform.data.promotion.PromotionTriggerBus.emit(
                    com.example.demoproject.platform.data.promotion.PromotionTrigger.PurchaseVerifiedSuccess,
                )
            },
        )
    }

    override fun onCreate() {
        super.onCreate()
        val runtime = NetworkRuntime.get(this)
        appScope.launch {
            runtime.sessionPrefs.migrateIfNeeded()
        }
        val mqtt = MqttRuntime.get(this)
        mqtt.connectionManager.start(appScope)

        analytics = DefaultAnalyticsFactory.create(this)
        AnalyticsHolder.tracker = analytics.tracker
        AnalyticsHolder.payReporter = analytics.payReporter
        // Always initialize; AdjustAnalyticsTracker no-ops when disabled / token blank.
        analytics.initializer.initialize(this)
        appScope.launch {
            analytics.payReporter.flushPending()
        }

        appLifecycleReporter = AppLifecycleReporter(
            application = this,
            analyticsTracker = analytics.tracker,
            adjustAttributionProvider = analytics.adjustAttributionProvider,
            appPrefs = AppPrefs.create(this),
        )
        // Each successful Adjust conversion event must also POST /adjust/add.
        analytics.adjustTracker.onConversionEventTracked = {
            appLifecycleReporter.requestAdjustAttributionUpload()
        }

        StorePurchaseLauncherHolder.launcher = StorePurchaseLauncher { activity, request, onResult ->
            storePurchaseCoordinator.launchPurchase(activity, request, onResult)
        }

        val signaling = MqttCallSignalingClient(
            mqttManager = mqtt.manager,
            callActions = HttpCallSignalingActions(runtime.callApi),
            currentUserIdProvider = object : CurrentUserIdProvider {
                override val currentUserId: String?
                    get() = runtime.sessionManager.currentUserId
            },
            json = runtime.json,
        )
        val agora = AgoraRtcClient(this)
        CallKitHolder.rtc = agora
        CallKitHolder.signaling = signaling
        CallKitHolder.coordinator = CallCoordinator(
            scope = appScope,
            signaling = signaling,
            rtc = agora,
        )
        ChatMqttInbox(
            mqttManager = mqtt.manager,
            messageRepository = runtime.messageRepository,
            sessionManager = runtime.sessionManager,
            json = runtime.json,
            scope = appScope,
        ).start()
        ChatMqttSyncCoordinator(
            mqttManager = mqtt.manager,
            messageRepository = runtime.messageRepository,
            sessionManager = runtime.sessionManager,
            activeConversationTracker = runtime.activeConversationTracker,
            json = runtime.json,
            scope = appScope,
        ).start()
        AccountMqttInbox(
            mqttManager = mqtt.manager,
            accountBalanceStore = runtime.accountBalanceStore,
            matchQuotaStore = runtime.matchQuotaStore,
            vipStatusStore = runtime.vipStatusStore,
            json = runtime.json,
            scope = appScope,
        ).start()
        PayEventMqttCoordinator(
            mqttManager = mqtt.manager,
            payEventStore = runtime.payEventStore,
            billingRepository = runtime.billingRepository,
            coinRepository = runtime.coinRepository,
            vipRepository = runtime.vipRepository,
            payAnalyticsReporter = analytics.payReporter,
            json = runtime.json,
            scope = appScope,
        ).start()

        appLifecycleReporter.start()
        BillingOrderRetryWorker.enqueuePeriodic(this)
    }
}
