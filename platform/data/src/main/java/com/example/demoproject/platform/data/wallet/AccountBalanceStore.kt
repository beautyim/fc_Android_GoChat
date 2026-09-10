package com.example.demoproject.platform.data.wallet

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global coin-balance snapshot for the signed-in user. `/app/init`, `/app/open`, recharge and
 * spend paths write here so Online / Me / Store stay consistent without private page copies.
 */
class AccountBalanceStore {
    private val _coins = MutableStateFlow(0)
    val coins: StateFlow<Int> = _coins.asStateFlow()

    fun update(coins: Int) {
        _coins.value = coins.coerceAtLeast(0)
    }

    fun clear() {
        _coins.value = 0
    }
}
