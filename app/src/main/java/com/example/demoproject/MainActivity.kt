package com.example.demoproject

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.demoproject.BuildConfig
import com.example.demoproject.platform.callkit.CallKitHolder
import com.example.demoproject.platform.callkit.CallState
import com.example.demoproject.platform.data.model.Session
import com.example.demoproject.platform.data.network.NetworkRuntime
import com.example.demoproject.platform.data.session.SessionManager
import com.example.demoproject.product.auth.AuthScreen
import com.example.demoproject.product.auth.AuthViewModel
import com.example.demoproject.product.call.CallRecordsEffect
import com.example.demoproject.product.call.CallRecordsScreen
import com.example.demoproject.product.call.CallRecordsViewModel
import com.example.demoproject.product.call.CallScreen
import com.example.demoproject.product.call.CallViewModel
import com.example.demoproject.product.chat.ChatDetailScreen
import com.example.demoproject.product.chat.ChatDetailViewModel
import com.example.demoproject.product.chat.ChatListIntent
import com.example.demoproject.product.chat.ChatListScreen
import com.example.demoproject.product.chat.ChatListViewModel
import com.example.demoproject.product.home.HomeEffect
import com.example.demoproject.product.home.HomeScreen
import com.example.demoproject.product.home.HomeViewModel
import com.example.demoproject.product.match.MatchScreen
import com.example.demoproject.product.match.MatchViewModel
import com.example.demoproject.product.me.AboutUsScreen
import com.example.demoproject.product.me.AboutUsViewModel
import com.example.demoproject.product.me.BlockedUsersScreen
import com.example.demoproject.product.me.BlockedUsersViewModel
import com.example.demoproject.product.me.MeEffect
import com.example.demoproject.product.me.MeScreen
import com.example.demoproject.product.me.MeViewModel
import com.example.demoproject.product.me.RelationshipListEffect
import com.example.demoproject.product.me.RelationshipListScreen
import com.example.demoproject.product.me.RelationshipListType
import com.example.demoproject.product.me.RelationshipListViewModel
import com.example.demoproject.product.me.SettingsScreen
import com.example.demoproject.product.me.SettingsViewModel
import com.example.demoproject.product.profile.ProfileScreen
import com.example.demoproject.product.profile.ProfileViewModel
import com.example.demoproject.product.store.StoreScreen
import com.example.demoproject.product.store.StoreViewModel
import com.example.demoproject.product.vip.VipPurchaseScreen
import com.example.demoproject.product.vip.VipPurchaseViewModel
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.foundation.DemoWindowSize
import com.example.demoproject.ui.foundation.ProvideWindowSize
import com.example.demoproject.ui.foundation.WindowHeightClass
import com.example.demoproject.ui.foundation.WindowWidthClass
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull

object DemoRoutes {
    const val Home = "home"
    const val Auth = "auth"
    const val Chat = "chat"
    const val ChatDetail = "chat/detail/{conversationId}/{nickname}"
    const val Match = "match"
    const val Call = "call/{userId}/{nickname}/{age}/{avatar}/{video}/{cover}"
    const val CallRecords = "call-records"
    const val Store = "store"
    const val Vip = "vip"
    const val Profile = "profile"
    const val Settings = "settings"
    const val AboutUs = "settings/about-us"
    const val BlockedUsers = "settings/blocked-users"
    const val RelationshipList = "profile/relationships/{type}/{count}"
    const val ProfileUser = "profile/user/{userId}"

    fun chatDetail(conversationId: String, nickname: String): String {
        val id = URLEncoder.encode(conversationId, StandardCharsets.UTF_8.toString())
        val name = URLEncoder.encode(nickname.ifBlank { "_" }, StandardCharsets.UTF_8.toString())
        return "chat/detail/$id/$name"
    }

    fun call(
        userId: String = "",
        nickname: String = "",
        age: Int = 0,
        avatarUrl: String = "",
        videoUrl: String = "",
        coverUrl: String = "",
    ): String {
        fun enc(raw: String): String =
            URLEncoder.encode(raw.ifBlank { "_" }, StandardCharsets.UTF_8.toString())
        return "call/${enc(userId)}/${enc(nickname)}/$age/${enc(avatarUrl)}/${enc(videoUrl)}/${enc(coverUrl)}"
    }

    fun profileUser(externalUserId: String): String {
        val id = URLEncoder.encode(externalUserId, StandardCharsets.UTF_8.toString())
        return "profile/user/$id"
    }

    fun relationshipList(type: RelationshipListType, count: Int): String =
        "profile/relationships/${type.name}/${count.coerceAtLeast(0)}"
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProvideWindowSize(calculateWindowSizeClass(this).toDemoWindowSize()) {
                DemoTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        DemoNavHost()
                    }
                }
            }
        }
    }
}

/** Collapse Material's experimental size classes into `:ui:foundation` types for feature code. */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
private fun WindowSizeClass.toDemoWindowSize(): DemoWindowSize = DemoWindowSize(
    widthClass = when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> WindowWidthClass.Compact
        WindowWidthSizeClass.Medium -> WindowWidthClass.Medium
        else -> WindowWidthClass.Expanded
    },
    heightClass = when (heightSizeClass) {
        WindowHeightSizeClass.Compact -> WindowHeightClass.Compact
        WindowHeightSizeClass.Medium -> WindowHeightClass.Medium
        else -> WindowHeightClass.Expanded
    },
)

@Composable
private fun DemoNavHost() {
    val app = LocalContext.current.applicationContext as Application
    val context = LocalContext.current
    val sessionManager = remember(app) { NetworkRuntime.get(app).sessionManager }
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(sessionManager) {
        val session = sessionManager.awaitInitialHydration()
        startDestination = if (session != null) DemoRoutes.Home else DemoRoutes.Auth
    }

    val destination = startDestination ?: return

    val navController = rememberNavController()
    val factory = remember(app) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return when {
                    modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(app) as T
                    modelClass.isAssignableFrom(AuthViewModel::class.java) -> AuthViewModel(app) as T
                    modelClass.isAssignableFrom(ChatListViewModel::class.java) -> ChatListViewModel(app) as T
                    modelClass.isAssignableFrom(MatchViewModel::class.java) -> MatchViewModel(app) as T
                    modelClass.isAssignableFrom(CallRecordsViewModel::class.java) -> CallRecordsViewModel(app) as T
                    modelClass.isAssignableFrom(StoreViewModel::class.java) -> StoreViewModel(app) as T
                    modelClass.isAssignableFrom(VipPurchaseViewModel::class.java) -> VipPurchaseViewModel(app) as T
                    modelClass.isAssignableFrom(MeViewModel::class.java) -> MeViewModel(app) as T
                    modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(app) as T
                    modelClass.isAssignableFrom(AboutUsViewModel::class.java) -> AboutUsViewModel(app) as T
                    modelClass.isAssignableFrom(BlockedUsersViewModel::class.java) -> BlockedUsersViewModel(app) as T
                    modelClass.isAssignableFrom(ProfileViewModel::class.java) -> ProfileViewModel(app) as T
                    else -> error("Unknown ViewModel: ${modelClass.name}")
                }
            }
        }
    }

    LaunchedEffect(navController, sessionManager) {
        observeSessionNavigation(navController, sessionManager)
    }

    LaunchedEffect(navController) {
        val coordinator = CallKitHolder.coordinator ?: return@LaunchedEffect
        coordinator.state
            .mapNotNull { state -> state as? CallState.IncomingRinging }
            .distinctUntilChanged { a, b -> a.inviteId == b.inviteId }
            .collect { ringing ->
                val route = navController.currentBackStackEntry?.destination?.route.orEmpty()
                if (route.startsWith("call/")) return@collect
                // Leave userId blank so CallViewModel does not start an outgoing /call/create.
                navController.navigate(
                    DemoRoutes.call(
                        userId = "",
                        nickname = ringing.callerName,
                        age = ringing.callerAge,
                        avatarUrl = ringing.callerAvatar,
                        videoUrl = ringing.peerVideoUrl,
                        coverUrl = ringing.peerCoverUrl,
                    ),
                ) {
                    launchSingleTop = true
                }
            }
    }

    NavHost(navController = navController, startDestination = destination) {
        composable(DemoRoutes.Home) {
            val vm: HomeViewModel = viewModel(factory = factory)
            val state by vm.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(vm) {
                vm.effects.collect { effect ->
                    when (effect) {
                        is HomeEffect.OpenProfile -> {
                            navController.navigate(DemoRoutes.profileUser(effect.externalUserId))
                        }
                        is HomeEffect.OpenChatDetail -> {
                            navController.navigate(
                                DemoRoutes.chatDetail(effect.conversationId, effect.nickname),
                            )
                        }
                        is HomeEffect.StartVideoCall -> {
                            navController.navigate(
                                DemoRoutes.call(
                                    userId = effect.userId,
                                    nickname = effect.nickname,
                                    age = effect.age,
                                    avatarUrl = effect.avatarUrl,
                                    videoUrl = effect.videoUrl,
                                    coverUrl = effect.coverUrl,
                                ),
                            )
                        }
                        HomeEffect.OpenStore -> navController.navigate(DemoRoutes.Store)
                        is HomeEffect.ShowMessage -> {
                            Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            HomeScreen(
                state = state,
                onIntent = vm::onIntent,
                onNavigateTab = { route ->
                    navigateMainTab(navController, route)
                },
            )
        }
        composable(DemoRoutes.CallRecords) {
            val vm: CallRecordsViewModel = viewModel(factory = factory)
            val state by vm.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(vm) {
                vm.effects.collect { effect ->
                    when (effect) {
                        is CallRecordsEffect.OpenProfile -> {
                            navController.navigate(DemoRoutes.profileUser(effect.externalUserId))
                        }
                        is CallRecordsEffect.StartVideoCall -> {
                            navController.navigate(
                                DemoRoutes.call(
                                    userId = effect.userId,
                                    nickname = effect.nickname,
                                    age = effect.age,
                                    avatarUrl = effect.avatarUrl,
                                    videoUrl = effect.videoUrl,
                                    coverUrl = effect.coverUrl,
                                ),
                            )
                        }
                        CallRecordsEffect.OpenStore -> navController.navigate(DemoRoutes.Store)
                        CallRecordsEffect.OpenMatch -> navController.navigate(DemoRoutes.Match)
                        is CallRecordsEffect.ShowMessage -> {
                            Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            CallRecordsScreen(
                state = state,
                onIntent = vm::onIntent,
                onNavigateTab = { route ->
                    navigateMainTab(navController, route)
                },
            )
        }
        composable(DemoRoutes.Auth) {
            val vm: AuthViewModel = viewModel(factory = factory)
            AuthScreen(
                viewModel = vm,
                onBack = {
                    if (!navController.popBackStack()) {
                        (context as? Activity)?.finish()
                    }
                },
                termsUrl = BuildConfig.TERMS_URL,
                privacyUrl = BuildConfig.PRIVACY_URL,
            )
        }
        composable(DemoRoutes.Chat) {
            val vm: ChatListViewModel = viewModel(factory = factory)
            val activity = context as ComponentActivity
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) {
                vm.onIntent(ChatListIntent.NotificationPermissionResult)
            }
            ChatListScreen(
                viewModel = vm,
                onNavigateTab = { route -> navigateMainTab(navController, route) },
                onOpenChatDetail = { conversationId, nickname ->
                    navController.navigate(DemoRoutes.chatDetail(conversationId, nickname))
                },
                onOpenHome = { navigateMainTab(navController, "home") },
                onRequestNotificationPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        activity.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
                            },
                        )
                        vm.onIntent(ChatListIntent.NotificationPermissionResult)
                    }
                },
            )
        }
        composable(
            route = DemoRoutes.ChatDetail,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType },
                navArgument("nickname") { type = NavType.StringType },
            ),
        ) { entry ->
            val conversationId = URLDecoder.decode(
                entry.arguments?.getString("conversationId").orEmpty(),
                StandardCharsets.UTF_8.toString(),
            )
            val nicknameRaw = URLDecoder.decode(
                entry.arguments?.getString("nickname").orEmpty(),
                StandardCharsets.UTF_8.toString(),
            )
            val nickname = nicknameRaw.takeUnless { it == "_" }.orEmpty()
            val chatDetailFactory = remember(app, conversationId, nickname) {
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        require(modelClass.isAssignableFrom(ChatDetailViewModel::class.java))
                        return ChatDetailViewModel(
                            application = app,
                            conversationId = conversationId,
                            initialNickname = nickname,
                        ) as T
                    }
                }
            }
            val chatDetailVm: ChatDetailViewModel = viewModel(
                key = "chat-detail-$conversationId",
                factory = chatDetailFactory,
            )
            ChatDetailScreen(
                viewModel = chatDetailVm,
                onBack = { navController.popBackStack() },
                onOpenPeerProfile = { externalUserId ->
                    navController.navigate(DemoRoutes.profileUser(externalUserId))
                },
                onStartVideoCall = { peerUserId ->
                    navController.navigate(DemoRoutes.call(userId = peerUserId))
                },
                onOpenStore = { navController.navigate(DemoRoutes.Store) },
            )
        }
        composable(DemoRoutes.Match) {
            val vm: MatchViewModel = viewModel(factory = factory)
            MatchScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
        composable(
            route = DemoRoutes.Call,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("nickname") { type = NavType.StringType },
                navArgument("age") { type = NavType.IntType },
                navArgument("avatar") { type = NavType.StringType },
                navArgument("video") { type = NavType.StringType },
                navArgument("cover") { type = NavType.StringType },
            ),
        ) { entry ->
            fun decodeArg(key: String): String {
                val raw = URLDecoder.decode(
                    entry.arguments?.getString(key).orEmpty(),
                    StandardCharsets.UTF_8.toString(),
                )
                return raw.takeUnless { it == "_" }.orEmpty()
            }
            val userId = decodeArg("userId")
            val nickname = decodeArg("nickname")
            val age = entry.arguments?.getInt("age") ?: 0
            val avatarUrl = decodeArg("avatar")
            val videoUrl = decodeArg("video")
            val coverUrl = decodeArg("cover")
            val callFactory = remember(app, userId, nickname, age, avatarUrl, videoUrl, coverUrl) {
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        require(modelClass.isAssignableFrom(CallViewModel::class.java))
                        return CallViewModel(
                            application = app,
                            targetUserId = userId,
                            initialNickname = nickname,
                            initialAvatarUrl = avatarUrl,
                            initialAge = age,
                            initialVideoUrl = videoUrl,
                            initialCoverUrl = coverUrl,
                        ) as T
                    }
                }
            }
            val vm: CallViewModel = viewModel(
                key = "call-$userId-$nickname-$age",
                factory = callFactory,
            )
            CallScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onOpenStore = { navController.navigate(DemoRoutes.Store) },
            )
        }
        composable(DemoRoutes.Store) {
            val vm: StoreViewModel = viewModel(factory = factory)
            StoreScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
        composable(DemoRoutes.Vip) {
            val vm: VipPurchaseViewModel = viewModel(factory = factory)
            VipPurchaseScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
        composable(DemoRoutes.Profile) {
            val vm: MeViewModel = viewModel(factory = factory)
            val state by vm.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(vm) {
                vm.effects.collect { effect ->
                    when (effect) {
                        is MeEffect.OpenPublicProfile -> {
                            navController.navigate(DemoRoutes.profileUser(effect.externalUserId))
                        }
                        is MeEffect.OpenRelationshipList -> {
                            navController.navigate(
                                DemoRoutes.relationshipList(effect.type, effect.count),
                            )
                        }
                        MeEffect.OpenStore -> navController.navigate(DemoRoutes.Store)
                        MeEffect.OpenVip -> navController.navigate(DemoRoutes.Vip)
                        MeEffect.OpenSettings -> navController.navigate(DemoRoutes.Settings)
                        is MeEffect.ShowMessage -> {
                            Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            MeScreen(
                state = state,
                onIntent = vm::onIntent,
                onNavigateTab = { route -> navigateMainTab(navController, route) },
            )
        }
        composable(
            route = DemoRoutes.RelationshipList,
            arguments = listOf(
                navArgument("type") { type = NavType.StringType },
                navArgument("count") { type = NavType.IntType },
            ),
        ) { entry ->
            val type = runCatching {
                RelationshipListType.valueOf(
                    entry.arguments?.getString("type").orEmpty(),
                )
            }.getOrDefault(RelationshipListType.Following)
            val count = entry.arguments?.getInt("count") ?: 0
            val relationshipFactory = remember(app, type, count) {
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        require(modelClass.isAssignableFrom(RelationshipListViewModel::class.java))
                        return RelationshipListViewModel(
                            application = app,
                            type = type,
                            expectedCount = count,
                        ) as T
                    }
                }
            }
            val vm: RelationshipListViewModel = viewModel(
                key = "relationship-${type.name}-$count",
                factory = relationshipFactory,
            )
            val state by vm.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(vm) {
                vm.effects.collect { effect ->
                    when (effect) {
                        is RelationshipListEffect.OpenProfile -> {
                            navController.navigate(DemoRoutes.profileUser(effect.externalUserId))
                        }
                        is RelationshipListEffect.OpenChat -> {
                            navController.navigate(
                                DemoRoutes.chatDetail(effect.conversationId, effect.nickname),
                            )
                        }
                        is RelationshipListEffect.StartVideoCall -> {
                            navController.navigate(DemoRoutes.call(userId = effect.userId))
                        }
                    }
                }
            }
            RelationshipListScreen(
                state = state,
                onIntent = vm::onIntent,
                onBack = { navController.popBackStack() },
            )
        }
        composable(DemoRoutes.Settings) {
            val vm: SettingsViewModel = viewModel(factory = factory)
            SettingsScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onOpenBlockedUsers = { navController.navigate(DemoRoutes.BlockedUsers) },
                onOpenAbout = { navController.navigate(DemoRoutes.AboutUs) },
            )
        }
        composable(DemoRoutes.AboutUs) {
            val vm: AboutUsViewModel = viewModel(factory = factory)
            val unavailableMessage = context.getString(
                com.example.demoproject.product.me.R.string.about_link_unavailable,
            )
            AboutUsScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onOpenContact = {
                    Toast.makeText(context, unavailableMessage, Toast.LENGTH_SHORT).show()
                },
                onOpenPrivacy = {
                    if (!context.openExternalLink(BuildConfig.PRIVACY_URL)) {
                        Toast.makeText(context, unavailableMessage, Toast.LENGTH_SHORT).show()
                    }
                },
                onOpenTerms = {
                    if (!context.openExternalLink(BuildConfig.TERMS_URL)) {
                        Toast.makeText(context, unavailableMessage, Toast.LENGTH_SHORT).show()
                    }
                },
            )
        }
        composable(DemoRoutes.BlockedUsers) {
            val vm: BlockedUsersViewModel = viewModel(factory = factory)
            BlockedUsersScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
        composable(
            route = DemoRoutes.ProfileUser,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
            ),
        ) { entry ->
            val userId = URLDecoder.decode(
                entry.arguments?.getString("userId").orEmpty(),
                StandardCharsets.UTF_8.toString(),
            )
            val userProfileFactory = remember(app, userId) {
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        require(modelClass.isAssignableFrom(ProfileViewModel::class.java))
                        return ProfileViewModel(app, externalUserId = userId) as T
                    }
                }
            }
            val vm: ProfileViewModel = viewModel(
                key = "profile-user-$userId",
                factory = userProfileFactory,
            )
            ProfileScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onOpenStore = { navController.navigate(DemoRoutes.Store) },
                onOpenChatDetail = { conversationId, nickname ->
                    navController.navigate(DemoRoutes.chatDetail(conversationId, nickname))
                },
            )
        }
    }
}

private fun Context.openExternalLink(url: String): Boolean {
    if (url.isBlank()) return false
    return runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }.isSuccess
}

private fun navigateMainTab(navController: NavHostController, route: String) {
    val destination = when (route) {
        "home" -> DemoRoutes.Home
        "feed", "call-records" -> DemoRoutes.CallRecords
        "match" -> DemoRoutes.Match
        "chat" -> DemoRoutes.Chat
        "profile" -> DemoRoutes.Profile
        else -> route
    }
    val current = navController.currentDestination?.route
    if (current == destination) return
    if (destination == DemoRoutes.Home) {
        navController.popBackStack(DemoRoutes.Home, inclusive = false)
        return
    }
    navController.navigate(destination) {
        popUpTo(DemoRoutes.Home) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Keep root navigation aligned with [SessionManager]: login → Home, logout → Auth,
 * clearing the back stack so protected screens are unreachable without a session.
 */
private suspend fun observeSessionNavigation(
    navController: NavHostController,
    sessionManager: SessionManager,
) {
    var previous: Session? = sessionManager.currentSessionSnapshot
    sessionManager.sessionFlow.collect { session ->
        if (session == previous) return@collect
        val wasLoggedIn = previous != null
        previous = session
        when {
            wasLoggedIn && session == null -> {
                navController.navigate(DemoRoutes.Auth) {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
            }
            !wasLoggedIn && session != null -> {
                navController.navigate(DemoRoutes.Home) {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }
}
