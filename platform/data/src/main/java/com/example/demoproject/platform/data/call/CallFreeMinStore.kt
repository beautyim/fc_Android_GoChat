package com.example.demoproject.platform.data.call

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global remaining free video-call quota (`user_info.call_free_min`) for the signed-in user.
 * `/app/open`, login and post-call refresh write here so Online cards stay consistent
 * without page-private copies.
 */
class CallFreeMinStore {
    private val _callFreeMin = MutableStateFlow(0)
    val callFreeMin: StateFlow<Int> = _callFreeMin.asStateFlow()

    fun update(callFreeMin: Int) {
        _callFreeMin.value = callFreeMin.coerceAtLeast(0)
    }

    fun clear() {
        _callFreeMin.value = 0
    }
}
