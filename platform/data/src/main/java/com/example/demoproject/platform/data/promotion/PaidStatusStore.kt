package com.example.demoproject.platform.data.promotion

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Lifetime recharge money (`recharge_money`) for unpaid/paid treasure eligibility.
 * Updated from `/app/init` user snapshot — not from free welcome coins.
 */
class PaidStatusStore {
    private val _rechargeMoney = MutableStateFlow(0)
    val rechargeMoney: StateFlow<Int> = _rechargeMoney.asStateFlow()

    val isPaidUser: Boolean
        get() = _rechargeMoney.value > 0

    fun update(rechargeMoney: Int) {
        _rechargeMoney.value = rechargeMoney.coerceAtLeast(0)
    }

    fun markPaidFromPurchase() {
        if (_rechargeMoney.value <= 0) {
            _rechargeMoney.value = 1
        }
    }

    fun clear() {
        _rechargeMoney.value = 0
    }
}
