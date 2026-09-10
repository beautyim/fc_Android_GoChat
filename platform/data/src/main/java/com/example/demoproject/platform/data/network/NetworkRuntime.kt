package com.example.demoproject.platform.data.network

import android.content.Context
import com.example.demoproject.platform.data.device.DefaultDeviceFingerprint
import com.example.demoproject.platform.data.local.cache.ChatStore
import com.example.demoproject.platform.data.local.cache.RoomChatStore
import com.example.demoproject.platform.data.local.cache.RoomUserCache
import com.example.demoproject.platform.data.local.cache.UserCache
import com.example.demoproject.platform.data.local.db.DemoDatabase
import com.example.demoproject.platform.data.local.db.LocalDatabaseFactory
import com.example.demoproject.platform.data.local.db.dao.ConversationDao
import com.example.demoproject.platform.data.local.db.dao.MessageDao
import com.example.demoproject.platform.data.local.db.dao.PostDao
import com.example.demoproject.platform.data.local.db.dao.UserDao
import com.example.demoproject.platform.data.blocked.BlockedUsersStore
import com.example.demoproject.platform.data.repository.BlockRepository
import com.example.demoproject.platform.data.repository.BlockRepositoryImpl
import com.example.demoproject.platform.data.repository.ReportRepository
import com.example.demoproject.platform.data.repository.ReportRepositoryImpl
import com.example.demoproject.platform.data.repository.PostRepository
import com.example.demoproject.platform.data.repository.PostRepositoryImpl
import com.example.demoproject.platform.data.local.pref.SessionPrefs
import com.example.demoproject.platform.data.locale.DefaultAppLocaleProvider
import com.example.demoproject.platform.data.network.api.AppApi
import com.example.demoproject.platform.data.network.api.AuthApi
import com.example.demoproject.platform.data.network.api.CallApi
import com.example.demoproject.platform.data.network.api.CoinApi
import com.example.demoproject.platform.data.network.api.FeedApi
import com.example.demoproject.platform.data.network.api.FirebaseApi
import com.example.demoproject.platform.data.network.api.GooglePayApi
import com.example.demoproject.platform.data.network.api.MatchApi
import com.example.demoproject.platform.data.network.api.MessageApi
import com.example.demoproject.platform.data.network.api.NotificationApi
import com.example.demoproject.platform.data.network.api.PostApi
import com.example.demoproject.platform.data.network.api.ProfileApi
import com.example.demoproject.platform.data.network.api.ReportApi
import com.example.demoproject.platform.data.network.api.TranslationApi
import com.example.demoproject.platform.data.network.api.VipApi
import com.example.demoproject.platform.data.repository.AppSessionRepository
import com.example.demoproject.platform.data.repository.AppSessionRepositoryImpl
import com.example.demoproject.platform.data.repository.BillingCheckout
import com.example.demoproject.platform.data.repository.BillingOrderStore
import com.example.demoproject.platform.data.repository.BillingRepository
import com.example.demoproject.platform.data.repository.BillingRepositoryImpl
import com.example.demoproject.platform.data.repository.AuthRepository
import com.example.demoproject.platform.data.repository.PushTokenRepositoryImpl
import com.example.demoproject.platform.data.repository.PushTokenRepository
import com.example.demoproject.platform.data.repository.AuthRepositoryImpl
import com.example.demoproject.platform.data.repository.CoinRepository
import com.example.demoproject.platform.data.repository.CoinRepositoryImpl
import com.example.demoproject.platform.data.repository.VipRepositoryImpl
import com.example.demoproject.platform.data.repository.VipRepository
import com.example.demoproject.platform.data.repository.NotificationRepositoryImpl
import com.example.demoproject.platform.data.repository.NotificationRepository
import com.example.demoproject.platform.data.repository.CallSessionRepositoryImpl
import com.example.demoproject.platform.data.repository.CallSessionRepository
import com.example.demoproject.platform.data.repository.CallRepositoryImpl
import com.example.demoproject.platform.data.repository.CallRepository
import com.example.demoproject.platform.data.repository.FeedRepository
import com.example.demoproject.platform.data.repository.FeedRepositoryImpl
import com.example.demoproject.platform.data.repository.MatchRepository
import com.example.demoproject.platform.data.repository.MatchRepositoryImpl
import com.example.demoproject.platform.data.repository.MessageRepository
import com.example.demoproject.platform.data.repository.MessageRepositoryNetworkImpl
import com.example.demoproject.platform.data.repository.ProfileRepository
import com.example.demoproject.platform.data.repository.ProfileRepositoryImpl
import com.example.demoproject.platform.data.session.SessionManager
import com.example.demoproject.platform.data.session.SessionUidProvider
import com.example.demoproject.platform.s3.MediaUploadService
import com.example.demoproject.platform.s3.S3Runtime
import com.example.demoproject.platform.data.local.crypto.TinkAeadSessionCipher
import com.example.demoproject.platform.data.local.pref.AppPrefs
import com.example.demoproject.platform.data.vip.VipStatusStore
import com.example.demoproject.platform.data.wallet.AccountBalanceStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.example.demoproject.platform.network.config.DefaultNetworkConfig
import com.example.demoproject.platform.network.config.NetworkConfig
import com.example.demoproject.platform.network.constants.NetworkHeaders
import com.example.demoproject.platform.network.crypto.ApiBodyCipher
import com.example.demoproject.platform.network.crypto.ApiKeyDeriver
import com.example.demoproject.platform.network.crypto.ApiSigner
import com.example.demoproject.platform.network.crypto.ApiUserAgentEncoder
import com.example.demoproject.platform.network.interceptor.AuthInterceptor
import com.example.demoproject.platform.network.interceptor.ClientKeyInterceptor
import com.example.demoproject.platform.network.interceptor.LocaleHeaderInterceptor
import com.example.demoproject.platform.network.interceptor.SigningEncryptionInterceptor
import com.example.demoproject.platform.network.provider.RetrofitProvider
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class NetworkRuntime private constructor(
    val networkConfig: NetworkConfig,
    val sessionManager: SessionManager,
    val json: Json,
    val retrofit: Retrofit,
    val appApi: AppApi,
    val authApi: AuthApi,
    val feedApi: FeedApi,
    val postApi: PostApi,
    val profileApi: ProfileApi,
    val messageApi: MessageApi,
    val notificationApi: NotificationApi,
    val callApi: CallApi,
    val matchApi: MatchApi,
    val reportApi: ReportApi,
    val vipApi: VipApi,
    val coinApi: CoinApi,
    val googlePayApi: GooglePayApi,
    val firebaseApi: FirebaseApi,
    val translationApi: TranslationApi,
    val authRepository: AuthRepository,
    val appSessionRepository: AppSessionRepository,
    val profileRepository: ProfileRepository,
    val messageRepository: MessageRepository,
    val feedRepository: FeedRepository,
    val matchRepository: MatchRepository,
    val coinRepository: CoinRepository,
    val callRepository: CallRepository,
    val callSessionRepository: CallSessionRepository,
    val vipRepository: VipRepository,
    val notificationRepository: NotificationRepository,
    val billingRepository: BillingRepository,
    val billingOrderStore: BillingOrderStore,
    val billingCheckout: BillingCheckout,
    val pushTokenRepository: PushTokenRepository,
    val postRepository: PostRepository,
    val database: DemoDatabase,
    val userCache: UserCache,
    val userDao: UserDao,
    val messageDao: MessageDao,
    val conversationDao: ConversationDao,
    val postDao: PostDao,
    val blockedUsersStore: BlockedUsersStore,
    val blockRepository: BlockRepository,
    val reportRepository: ReportRepository,
    val appPrefs: AppPrefs,
    val vipStatusStore: VipStatusStore,
    val accountBalanceStore: AccountBalanceStore,
    val sessionPrefs: SessionPrefs,
) {
    companion object {
        @Volatile
        private var instance: NetworkRuntime? = null

        fun get(context: Context): NetworkRuntime {
            return instance ?: synchronized(this) {
                instance ?: create(context.applicationContext).also { instance = it }
            }
        }

        fun create(context: Context): NetworkRuntime {
            val appContext = context.applicationContext
            val networkConfig: NetworkConfig = DefaultNetworkConfig()
            val json = Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
                explicitNulls = false
                encodeDefaults = true
            }
            val sessionPrefs = SessionPrefs(appContext, TinkAeadSessionCipher(appContext))
            val sessionManager = SessionManager(sessionPrefs)
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                sessionPrefs.migrateIfNeeded()
            }
            val fingerprint = DefaultDeviceFingerprint(appContext)
            val cipher = ApiBodyCipher()
            val signing = SigningEncryptionInterceptor(
                networkConfig = networkConfig,
                keyDeriver = ApiKeyDeriver(),
                signer = ApiSigner(),
                cipher = cipher,
                uaEncoder = ApiUserAgentEncoder(cipher, fingerprint),
                uidProvider = SessionUidProvider(),
                deviceFingerprint = fingerprint,
            )
            val logging = HttpLoggingInterceptor().apply {
                level = if (networkConfig.verboseHttpLogging) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
                redactHeader(NetworkHeaders.AUTHORIZATION)
            }
            val client = OkHttpClient.Builder()
                .connectTimeout(networkConfig.connectTimeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(networkConfig.readTimeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(networkConfig.writeTimeoutSeconds, TimeUnit.SECONDS)
                .addInterceptor(ClientKeyInterceptor(networkConfig))
                .addInterceptor(AuthInterceptor(sessionManager))
                .addInterceptor(LocaleHeaderInterceptor(DefaultAppLocaleProvider()))
                .addInterceptor(signing)
                .addInterceptor(logging)
                .build()
            val retrofit = RetrofitProvider.create(networkConfig.baseUrl, client, json)
            val authApi = retrofit.create(AuthApi::class.java)
            val appApi = retrofit.create(AppApi::class.java)
            val profileApi = retrofit.create(ProfileApi::class.java)
            val messageApi = retrofit.create(MessageApi::class.java)
            val feedApi = retrofit.create(FeedApi::class.java)
            val matchApi = retrofit.create(MatchApi::class.java)
            val callApi = retrofit.create(CallApi::class.java)
            val coinApi = retrofit.create(CoinApi::class.java)
            val translationApi = retrofit.create(TranslationApi::class.java)
            val mediaUploadService = S3Runtime.createMediaUploadService(
                context = appContext,
                retrofit = retrofit,
                networkConfig = networkConfig,
            )
            val postApi = retrofit.create(PostApi::class.java)
            val reportApi = retrofit.create(ReportApi::class.java)
            val database = LocalDatabaseFactory.create(appContext)
            val userDao = database.userDao()
            val userCache: UserCache = RoomUserCache(userDao)
            val authRepository: AuthRepository = AuthRepositoryImpl(authApi, sessionManager)
            val appSessionRepository: AppSessionRepository = AppSessionRepositoryImpl(appApi)
            val profileRepository: ProfileRepository = ProfileRepositoryImpl(
                profileApi = profileApi,
                userDao = userCache,
                mediaUploadService = mediaUploadService,
            )
            val chatStore: ChatStore = RoomChatStore(
                conversationDao = database.conversationDao(),
                messageDao = database.messageDao(),
                sessionManager = sessionManager,
            )
            val blockedUsersStore = BlockedUsersStore(appContext)
            val appPrefs = AppPrefs.create(appContext)
            val vipStatusStore = VipStatusStore()
            val accountBalanceStore = AccountBalanceStore()
            val blockRepository: BlockRepository = BlockRepositoryImpl(
                profileApi = profileApi,
                blockedUsersStore = blockedUsersStore,
            )
            val reportRepository: ReportRepository = ReportRepositoryImpl(reportApi)
            val messageRepository: MessageRepository = MessageRepositoryNetworkImpl(
                messageApi = messageApi,
                translationApi = translationApi,
                sessionManager = sessionManager,
                mediaUploadService = mediaUploadService,
                chatStore = chatStore,
                blockedUsersStore = blockedUsersStore,
                accountBalanceStore = accountBalanceStore,
            )
            val feedRepository: FeedRepository = FeedRepositoryImpl(feedApi)
            val matchRepository: MatchRepository = MatchRepositoryImpl(matchApi)
            val coinRepository: CoinRepository = CoinRepositoryImpl(coinApi)
            val callRepository: CallRepository = CallRepositoryImpl(callApi)
            val callSessionRepository: CallSessionRepository = CallSessionRepositoryImpl(
                callApi = callApi,
                chatStore = chatStore,
                sessionManager = sessionManager,
            )
            val vipRepository: VipRepository = VipRepositoryImpl(retrofit.create(VipApi::class.java))
            val notificationRepository: NotificationRepository = NotificationRepositoryImpl(
                retrofit.create(NotificationApi::class.java),
            )
            val billingRepository: BillingRepository = BillingRepositoryImpl(
                retrofit.create(GooglePayApi::class.java),
            )
            val billingOrderStore = BillingOrderStore(appContext)
            val billingCheckout = BillingCheckout(billingRepository, billingOrderStore)
            val pushTokenRepository: PushTokenRepository = PushTokenRepositoryImpl(
                retrofit.create(FirebaseApi::class.java),
            )
            val postRepository: PostRepository = PostRepositoryImpl(postApi)
            return NetworkRuntime(
                networkConfig = networkConfig,
                sessionManager = sessionManager,
                json = json,
                retrofit = retrofit,
                appApi = appApi,
                authApi = authApi,
                feedApi = feedApi,
                postApi = postApi,
                profileApi = profileApi,
                messageApi = messageApi,
                notificationApi = retrofit.create(NotificationApi::class.java),
                callApi = callApi,
                matchApi = matchApi,
                reportApi = reportApi,
                vipApi = retrofit.create(VipApi::class.java),
                coinApi = coinApi,
                googlePayApi = retrofit.create(GooglePayApi::class.java),
                firebaseApi = retrofit.create(FirebaseApi::class.java),
                translationApi = translationApi,
                authRepository = authRepository,
                appSessionRepository = appSessionRepository,
                profileRepository = profileRepository,
                messageRepository = messageRepository,
                feedRepository = feedRepository,
                matchRepository = matchRepository,
                coinRepository = coinRepository,
                callRepository = callRepository,
                callSessionRepository = callSessionRepository,
                vipRepository = vipRepository,
                notificationRepository = notificationRepository,
                billingRepository = billingRepository,
                billingOrderStore = billingOrderStore,
                billingCheckout = billingCheckout,
                pushTokenRepository = pushTokenRepository,
                postRepository = postRepository,
                database = database,
                userCache = userCache,
                userDao = userDao,
                messageDao = database.messageDao(),
                conversationDao = database.conversationDao(),
                postDao = database.postDao(),
                blockedUsersStore = blockedUsersStore,
                blockRepository = blockRepository,
                reportRepository = reportRepository,
                appPrefs = appPrefs,
                vipStatusStore = vipStatusStore,
                accountBalanceStore = accountBalanceStore,
                sessionPrefs = sessionPrefs,
            )
        }
    }
}
