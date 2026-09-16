package com.example.demoproject.platform.data.billing

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class PayEvent(
    val eventId: Long,
    val transactionId: String,
    val currency: String?,
    val value: Double?,
    val goodsId: Long,
    val itemId: String,
)

class PayEventStore {
    private val _events = MutableSharedFlow<PayEvent>(extraBufferCapacity = 32)
    val events = _events.asSharedFlow()

    fun publish(events: Iterable<PayEvent>) {
        events.forEach(_events::tryEmit)
    }
}
