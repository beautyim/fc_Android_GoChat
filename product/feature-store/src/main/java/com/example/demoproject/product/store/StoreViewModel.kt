package com.example.demoproject.product.store

import android.app.Activity
import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.demoproject.platform.data.billing.StorePurchaseLauncherHolder
import com.example.demoproject.platform.data.network.NetworkRuntime
import com.example.demoproject.platform.data.repository.BillingPaymentType
import com.example.demoproject.platform.data.repository.BillingProductType
import com.example.demoproject.platform.data.repository.StorePurchaseRequest
import com.example.demoproject.platform.data.repository.StorePurchaseResult
import com.example.demoproject.platform.network.result.AppResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StoreViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val runtime = NetworkRuntime.get(application)
    private val _uiState = MutableStateFlow(StoreUiState(isLoading = true))
    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    private val _effects = Channel<StoreEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    @Volatile
    private var hostActivity: Activity? = null

    init {
        viewModelScope.launch {
            runtime.accountBalanceStore.coins.collect { coins ->
                _uiState.update { it.copy(balance = coins) }
            }
        }
        onIntent(StoreIntent.Refresh)
    }

    fun bindActivity(activity: Activity?) {
        hostActivity = activity
    }

    fun onIntent(intent: StoreIntent) {
        when (intent) {
            StoreIntent.Refresh -> loadCatalog()
            is StoreIntent.PurchaseCoin -> purchaseCoin(intent.offerId)
            is StoreIntent.PurchaseVip -> purchaseVip(intent.offerId)
            is StoreIntent.PurchaseSale -> purchaseSale(intent.offerId)
        }
    }

    private fun loadCatalog() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val page = runtime.coinRepository.getRechargePage()) {
                is AppResult.Success -> {
                    val data = page.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            balance = data.balance,
                            vipOffers = data.vipCarouselItems().map { item -> item.toUi() },
                            saleOffers = data.saleItems.map { item ->
                                item.toSaleUi(fallbackSuperDiscountLabel())
                            },
                            coinOffers = data.toCoinOffers(),
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = page.message,
                        )
                    }
                }
            }
        }
    }

    private fun purchaseCoin(offerId: Long) {
        if (_uiState.value.purchasingOfferId != null) return
        val offer = _uiState.value.coinOffers.firstOrNull { it.id == offerId } ?: return
        launchPurchase(
            offerId = offer.id,
            request = StorePurchaseRequest(
                uiId = "store-coin-${offer.id}",
                goodsId = offer.id,
                productId = offer.sku,
                productType = BillingProductType.Coins,
                paymentType = BillingPaymentType.GooglePlay,
            ),
        )
    }

    private fun purchaseSale(offerId: Long) {
        if (_uiState.value.purchasingOfferId != null) return
        val offer = _uiState.value.saleOffers.firstOrNull { it.id == offerId } ?: return
        launchPurchase(
            offerId = offer.id,
            request = StorePurchaseRequest(
                uiId = "store-sale-${offer.id}",
                goodsId = offer.id,
                productId = offer.sku,
                productType = BillingProductType.Coins,
                paymentType = BillingPaymentType.GooglePlay,
            ),
        )
    }

    private fun purchaseVip(offerId: Long) {
        if (_uiState.value.purchasingOfferId != null) return
        val offer = _uiState.value.vipOffers.firstOrNull { it.id == offerId } ?: return
        launchPurchase(
            offerId = offer.id,
            request = StorePurchaseRequest(
                uiId = "store-vip-${offer.id}",
                goodsId = offer.id,
                productId = offer.sku,
                productType = BillingProductType.Vip,
                paymentType = BillingPaymentType.GooglePlay,
            ),
        )
    }

    private fun launchPurchase(offerId: Long, request: StorePurchaseRequest) {
        val activity = hostActivity
        if (activity == null) {
            viewModelScope.launch {
                _effects.send(StoreEffect.ShowMessage(str(R.string.store_status_no_activity)))
            }
            return
        }
        val launcher = StorePurchaseLauncherHolder.launcher
        if (launcher == null) {
            viewModelScope.launch {
                _effects.send(StoreEffect.ShowMessage(str(R.string.store_status_launcher_missing)))
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(purchasingOfferId = offerId) }
            launcher.launch(activity, request) { result ->
                _uiState.update { it.copy(purchasingOfferId = null) }
                when (result) {
                    is StorePurchaseResult.Success -> {
                        viewModelScope.launch {
                            _effects.send(
                                StoreEffect.ShowMessage(str(R.string.store_status_purchase_verified)),
                            )
                        }
                        loadCatalog()
                    }
                    is StorePurchaseResult.Canceled -> {
                        viewModelScope.launch {
                            _effects.send(
                                StoreEffect.ShowMessage(str(R.string.store_status_purchase_canceled)),
                            )
                        }
                    }
                    is StorePurchaseResult.ExternalCheckoutOpened -> {
                        viewModelScope.launch {
                            _effects.send(
                                StoreEffect.ShowMessage(
                                    str(R.string.store_status_external_checkout_opened),
                                ),
                            )
                        }
                    }
                    is StorePurchaseResult.Failed -> {
                        viewModelScope.launch {
                            _effects.send(
                                StoreEffect.ShowMessage(
                                    str(R.string.store_status_purchase_failed_fmt, result.message),
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun fallbackSuperDiscountLabel(): String = str(R.string.store_super_discount)

    private fun str(@StringRes id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)
}
