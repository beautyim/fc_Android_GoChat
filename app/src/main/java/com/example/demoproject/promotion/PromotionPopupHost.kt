package com.example.demoproject.promotion

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.demoproject.DemoApplication
import com.example.demoproject.DemoRoutes
import com.example.demoproject.platform.data.billing.StorePurchaseLauncherHolder
import com.example.demoproject.platform.data.promotion.PromotionPopupSelector
import com.example.demoproject.platform.data.repository.BillingProductType
import com.example.demoproject.platform.data.repository.StorePurchaseRequest
import com.example.demoproject.platform.data.repository.StorePurchaseResult
import com.example.demoproject.ui.designsystem.FloatingTreasureEntry
import com.example.demoproject.ui.designsystem.FreeMatchWelfareDialog
import com.example.demoproject.ui.designsystem.FreeVideoWelfareDialog
import com.example.demoproject.ui.designsystem.PrizeWinDialog
import com.example.demoproject.ui.designsystem.TreasureOfferDialog
import com.example.demoproject.ui.foundation.ComponentSize

/**
 * Global promotion overlay host — welfare / treasure / winning + floating treasure entry.
 */
@Composable
fun PromotionPopupHost(
    navController: NavHostController,
    viewModel: PromotionPopupViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.updateGate { it.copy(hostResumed = true) }
                Lifecycle.Event.ON_PAUSE -> viewModel.updateGate { it.copy(hostResumed = false) }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            val route = entry.destination.route.orEmpty()
            // Exact main-tab routes only — nested pages (profile/user, settings, …) are not tabs.
            val mainTab = when (route) {
                DemoRoutes.Home -> PromotionPopupSelector.TAB_HOME
                DemoRoutes.Match -> PromotionPopupSelector.TAB_MATCH
                DemoRoutes.Chat -> DemoRoutes.Chat
                DemoRoutes.CallRecords -> DemoRoutes.CallRecords
                DemoRoutes.Profile -> DemoRoutes.Profile
                else -> null
            }
            val excluded = mainTab == null
            val matchImmersive = false
            viewModel.updateGate {
                it.copy(
                    excludedRoute = excluded,
                    currentMainTab = mainTab,
                    matchImmersive = matchImmersive,
                )
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                PromotionPopupEffect.NavigateToMatch -> {
                    navController.navigate(DemoRoutes.Match) {
                        launchSingleTop = true
                    }
                }
                PromotionPopupEffect.NavigateToHome -> {
                    navController.navigate(DemoRoutes.Home) {
                        launchSingleTop = true
                    }
                }
                PromotionPopupEffect.NavigateToStore -> {
                    navController.navigate(DemoRoutes.Store)
                }
                PromotionPopupEffect.StartFreeMatch -> {
                    // Match screen starts free match when opened with free quota; no extra flag yet.
                }
                is PromotionPopupEffect.LaunchPurchase -> {
                    val act = activity ?: return@collect
                    val launcher = StorePurchaseLauncherHolder.launcher ?: return@collect
                    val request = StorePurchaseRequest(
                        uiId = "promo_${effect.goodsId}",
                        goodsId = effect.goodsId,
                        productId = effect.sku,
                        productType = BillingProductType.fromApi(effect.productTypeApi),
                        fromType = effect.fromType,
                        fromId = effect.fromId,
                        orderFrom = effect.orderFrom,
                    )
                    launcher.launch(act, request) { result ->
                        when (result) {
                            is StorePurchaseResult.Success -> {
                                viewModel.onPurchaseFinished(success = true, failedMessage = null)
                                (context.applicationContext as? DemoApplication)
                                    ?.let { /* purchase success also via coordinator hook */ }
                            }
                            StorePurchaseResult.Canceled,
                            StorePurchaseResult.ExternalCheckoutOpened,
                            -> viewModel.onPurchaseFinished(success = false, failedMessage = null)
                            is StorePurchaseResult.Failed ->
                                viewModel.onPurchaseFinished(
                                    success = false,
                                    failedMessage = result.message,
                                )
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(state.toastMessage) {
        val msg = state.toastMessage ?: return@LaunchedEffect
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        viewModel.onIntent(PromotionPopupIntent.ConsumeToast)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (state.treasureEntryVisible) {
            FloatingTreasureEntry(
                remainSeconds = state.treasureRemainSeconds,
                showSpecialOffer = state.treasureEntryShowSpecialOffer,
                onClick = { viewModel.onIntent(PromotionPopupIntent.TreasureEntryClick) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(
                        bottom = ComponentSize.onlineTabBarBodyHeight +
                            ComponentSize.onlineTreasureAboveTab,
                    ),
            )
        }

        when (val popup = state.activePopup) {
            is PromotionActivePopup.FreeMatch -> FreeMatchWelfareDialog(
                count = popup.count,
                onStart = { viewModel.onIntent(PromotionPopupIntent.FreeMatchStart) },
                onDismiss = { viewModel.onIntent(PromotionPopupIntent.Dismiss) },
            )
            is PromotionActivePopup.FreeCall -> FreeVideoWelfareDialog(
                count = popup.count,
                onStart = { viewModel.onIntent(PromotionPopupIntent.FreeCallStart) },
                onDismiss = { viewModel.onIntent(PromotionPopupIntent.Dismiss) },
            )
            is PromotionActivePopup.Treasure -> TreasureOfferDialog(
                variant = popup.offer.content.toTreasureVariant(),
                originalPrice = popup.offer.originalPrice,
                salePrice = popup.offer.salePrice,
                onGetOffer = { viewModel.onIntent(PromotionPopupIntent.TreasureGetOffer) },
                onDismiss = { viewModel.onIntent(PromotionPopupIntent.Dismiss) },
            )
            is PromotionActivePopup.Winning -> PrizeWinDialog(
                baseCoins = popup.offer.baseCoins,
                bonusCoins = popup.offer.bonusCoins,
                originalPrice = popup.offer.originalPrice,
                salePrice = popup.offer.salePrice,
                baseCoinIconUrl = popup.offer.baseCoinIconUrl,
                bonusCoinIconUrl = popup.offer.bonusCoinIconUrl,
                onClaim = { viewModel.onIntent(PromotionPopupIntent.WinningClaim) },
                onDismiss = { viewModel.onIntent(PromotionPopupIntent.Dismiss) },
            )
            null -> Unit
        }
    }
}
