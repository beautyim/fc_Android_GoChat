package com.example.demoproject.product.vip

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
import com.example.demoproject.platform.data.repository.VipPageData
import com.example.demoproject.platform.data.repository.VipPlan
import com.example.demoproject.platform.network.result.AppResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VipPurchaseViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val runtime = NetworkRuntime.get(application)
    private val _uiState = MutableStateFlow(VipPurchaseUiState(isLoading = true))
    val uiState: StateFlow<VipPurchaseUiState> = _uiState.asStateFlow()

    private val _effects = Channel<VipPurchaseEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var cachedPage: VipPageData? = null

    @Volatile
    private var hostActivity: Activity? = null

    init {
        viewModelScope.launch {
            runtime.vipStatusStore.status.collect { status ->
                if (status == null) return@collect
                _uiState.update {
                    it.copy(
                        isVip = status.isVip,
                        expiryText = status.expiryText ?: it.expiryText,
                    )
                }
            }
        }
        onIntent(VipPurchaseIntent.Refresh)
    }

    fun bindActivity(activity: Activity?) {
        hostActivity = activity
    }

    fun onIntent(intent: VipPurchaseIntent) {
        when (intent) {
            VipPurchaseIntent.Refresh -> loadPage()
            is VipPurchaseIntent.SelectPlan -> selectPlan(intent.planId)
            VipPurchaseIntent.PurchaseSelected -> purchaseSelected()
        }
    }

    private fun loadPage() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = runtime.vipRepository.getVipPage()) {
                is AppResult.Success -> applyPage(result.data)
                is AppResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    private fun applyPage(page: VipPageData) {
        cachedPage = page
        val plans = page.plans.map { it.toUi() }
        val selectedId = resolveDefaultPlanId(plans) ?: plans.firstOrNull()?.id
        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = null,
                isVip = page.user.isVip,
                expiryText = page.user.expiryText,
                plans = plans,
                selectedPlanId = selectedId,
                benefits = benefitsFor(page, selectedId, plans),
            )
        }
    }

    private fun selectPlan(planId: Long) {
        val page = cachedPage
        val plans = _uiState.value.plans
        _uiState.update {
            it.copy(
                selectedPlanId = planId,
                benefits = if (page != null) {
                    benefitsFor(page, planId, plans)
                } else {
                    it.benefits
                },
            )
        }
    }

    private fun purchaseSelected() {
        if (_uiState.value.isPurchasing) return
        val plan = _uiState.value.plans.firstOrNull { it.id == _uiState.value.selectedPlanId }
            ?: return
        val activity = hostActivity
        if (activity == null) {
            viewModelScope.launch {
                _effects.send(VipPurchaseEffect.ShowMessage(str(R.string.vip_status_no_activity)))
            }
            return
        }
        val launcher = StorePurchaseLauncherHolder.launcher
        if (launcher == null) {
            viewModelScope.launch {
                _effects.send(
                    VipPurchaseEffect.ShowMessage(str(R.string.vip_status_launcher_missing)),
                )
            }
            return
        }
        val request = StorePurchaseRequest(
            uiId = "vip-${plan.id}",
            goodsId = plan.id,
            productId = plan.sku,
            productType = BillingProductType.Vip,
            paymentType = BillingPaymentType.GooglePlay,
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isPurchasing = true) }
            launcher.launch(activity, request) { result ->
                _uiState.update { it.copy(isPurchasing = false) }
                when (result) {
                    is StorePurchaseResult.Success -> {
                        viewModelScope.launch {
                            _effects.send(
                                VipPurchaseEffect.ShowMessage(
                                    str(R.string.vip_status_purchase_verified),
                                ),
                            )
                        }
                        loadPage()
                    }
                    is StorePurchaseResult.Canceled -> {
                        viewModelScope.launch {
                            _effects.send(
                                VipPurchaseEffect.ShowMessage(
                                    str(R.string.vip_status_purchase_canceled),
                                ),
                            )
                        }
                    }
                    is StorePurchaseResult.Failed -> {
                        viewModelScope.launch {
                            _effects.send(
                                VipPurchaseEffect.ShowMessage(
                                    str(R.string.vip_status_purchase_failed_fmt, result.message),
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun str(@StringRes id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)
}

private fun benefitsFor(
    page: VipPageData,
    selectedId: Long?,
    plans: List<VipPlanUi>,
): List<VipBenefitUi> {
    val sku = plans.firstOrNull { it.id == selectedId }?.sku
    return page.benefitsForPlan(sku).map { benefit ->
        VipBenefitUi(
            id = benefit.id,
            title = benefit.title,
            iconUrl = benefit.iconUrl,
        )
    }
}

private fun resolveDefaultPlanId(plans: List<VipPlanUi>): Long? {
    if (plans.isEmpty()) return null
    plans.firstOrNull { plan ->
        plan.label.contains("popular", ignoreCase = true)
    }?.let { return it.id }
    return plans.getOrNull(plans.size / 2)?.id ?: plans.first().id
}

private fun VipPlan.toUi(): VipPlanUi =
    VipPlanUi(
        id = id,
        sku = sku,
        title = composePlanTitle(month = month, title = title),
        price = price,
        originalPrice = originalPrice,
        dayDesc = planPeriodLabel(dayDesc),
        saleText = saleText,
        label = label,
    )

/** Display title for a VIP purchase plan: `"$month $title"` (e.g. `1` + `Month` → `1 Month`). */
private fun composePlanTitle(month: Int, title: String): String {
    val titlePart = title.trim()
    return when {
        month > 0 && titlePart.isNotBlank() -> {
            val prefix = month.toString()
            if (titlePart == prefix || titlePart.startsWith("$prefix ")) {
                titlePart
            } else {
                "$prefix $titlePart"
            }
        }
        titlePart.isNotBlank() -> titlePart
        month > 0 -> month.toString()
        else -> ""
    }
}

/**
 * API `day_desc` is often `"$1.89/Week"`. The plan card already shows [VipPlan.price],
 * so keep only the period segment to match Figma unit copy (e.g. `Week`).
 */
private fun planPeriodLabel(dayDesc: String): String {
    val trimmed = dayDesc.trim()
    if (trimmed.isBlank()) return ""
    val sep = trimmed.lastIndexOf('/')
    if (sep in 0 until trimmed.lastIndex) {
        return trimmed.substring(sep + 1).trim()
    }
    return trimmed
}
