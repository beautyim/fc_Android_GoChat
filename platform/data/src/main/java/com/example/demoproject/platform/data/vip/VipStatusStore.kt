package com.example.demoproject.platform.data.vip

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Global VIP membership snapshot for the signed-in user. Me home / VIP page loads and post-purchase
 * refreshes write here so Me, own Profile Home, and Discover Match Now can update without a full
 * page reload. Verified VIP purchases also flip this immediately so upsell entries hide before the
 * follow-up network refresh returns.
 */
data class VipStatus(
    val isVip: Boolean,
    val expiryText: String? = null,
)

class VipStatusStore {
    private val _status = MutableStateFlow<VipStatus?>(null)
    val status: StateFlow<VipStatus?> = _status.asStateFlow()

    fun update(isVip: Boolean, expiryText: String? = null) {
        _status.value = VipStatus(isVip = isVip, expiryText = expiryText)
    }

    fun update(status: VipStatus) {
        _status.value = status
    }

    /**
     * Optimistically flips [isVip] right after a purchase is verified, without touching whatever
     * [VipStatus.expiryText] is already cached. Only the follow-up authoritative refresh (Me home /
     * VIP page load) should ever set the expiry date, otherwise this flip would wipe a valid cached
     * date to null and show a blank/incorrect date until the user manually refreshes.
     */
    fun markVerifiedOptimistically(isVip: Boolean) {
        _status.update { current -> current?.copy(isVip = isVip) ?: VipStatus(isVip = isVip) }
    }

    fun clear() {
        _status.value = null
    }
}
