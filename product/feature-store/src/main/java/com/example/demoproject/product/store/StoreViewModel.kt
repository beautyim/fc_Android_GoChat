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
import com.example.demoproject.ui.foundation.R as FoundationR
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StoreViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val runtime = NetworkRuntime.get(application)
    private val _uiState = MutableStateFlow(
        StoreUiState(status = str(FoundationR.string.status_ready)),
    )
    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    @Volatile
    private var hostActivity: Activity? = null

    init { onIntent(StoreIntent.LoadCatalog) }

    fun bindActivity(activity: Activity?) {
        hostActivity = activity
    }

    fun onIntent(intent: StoreIntent) {
        when (intent) {
            StoreIntent.LoadCatalog -> loadCatalog()
            StoreIntent.CreateOrder -> createOrder()
            StoreIntent.LaunchGooglePlayPurchase -> launchGooglePlayPurchase()
        }
    }

    private fun loadCatalog() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, status = str(R.string.store_status_loading_catalog)) }
            when (val page = runtime.coinRepository.getRechargePage()) {
                is AppResult.Success -> {
                    val products = (page.data.hotProducts + page.data.products).distinctBy { it.id }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            status = str(R.string.store_status_catalog_fmt, products.size),
                            products = products.map { p ->
                                str(
                                    R.string.store_product_row_fmt,
                                    p.id,
                                    p.coinAmount,
                                    p.sku,
                                    p.price,
                                )
                            },
                        )
                    }
                }
                is AppResult.Failure -> _uiState.update { it.copy(isLoading = false, status = page.message) }
            }
        }
    }

    private fun createOrder() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, status = str(R.string.store_status_creating_order)) }
            val request = firstCoinRequest()
            if (request == null) {
                _uiState.update { it.copy(isLoading = false, status = str(R.string.store_status_empty_catalog)) }
                return@launch
            }
            when (val created = runtime.billingCheckout.createAndStoreOrder(request)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        status = str(R.string.store_status_order_created_http),
                        lastOrder = str(
                            R.string.store_order_fmt,
                            created.data.tranNo,
                            created.data.productId,
                        ),
                    )
                }
                is AppResult.Failure -> _uiState.update { it.copy(isLoading = false, status = created.message) }
            }
        }
    }

    private fun launchGooglePlayPurchase() {
        val activity = hostActivity
        if (activity == null) {
            _uiState.update { it.copy(status = str(R.string.store_status_no_activity)) }
            return
        }
        val launcher = StorePurchaseLauncherHolder.launcher
        if (launcher == null) {
            _uiState.update { it.copy(status = str(R.string.store_status_launcher_missing)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, status = str(R.string.store_status_launching_play)) }
            val request = firstCoinRequest()
            if (request == null) {
                _uiState.update { it.copy(isLoading = false, status = str(R.string.store_status_empty_catalog)) }
                return@launch
            }
            launcher.launch(activity, request) { result ->
                _uiState.update {
                    when (result) {
                        is StorePurchaseResult.Success -> it.copy(
                            isLoading = false,
                            status = str(R.string.store_status_purchase_verified),
                            lastOrder = str(R.string.store_purchased_fmt, result.purchasedId),
                        )
                        is StorePurchaseResult.Canceled -> it.copy(
                            isLoading = false,
                            status = str(R.string.store_status_purchase_canceled),
                        )
                        is StorePurchaseResult.Failed -> it.copy(
                            isLoading = false,
                            status = str(R.string.store_status_purchase_failed_fmt, result.message),
                        )
                    }
                }
            }
        }
    }

    private suspend fun firstCoinRequest(): StorePurchaseRequest? {
        return when (val page = runtime.coinRepository.getRechargePage()) {
            is AppResult.Failure -> null
            is AppResult.Success -> {
                val product = page.data.hotProducts.firstOrNull() ?: page.data.products.firstOrNull()
                    ?: return null
                StorePurchaseRequest(
                    uiId = "store-${product.id}",
                    goodsId = product.id,
                    productId = product.sku,
                    productType = BillingProductType.Coins,
                    paymentType = BillingPaymentType.GooglePlay,
                )
            }
        }
    }

    private fun str(@StringRes id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)
}
