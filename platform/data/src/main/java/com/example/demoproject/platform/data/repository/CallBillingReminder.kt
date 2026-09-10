package com.example.demoproject.platform.data.repository

data class CallBillingReminder(
    val roomId: Long,
    val text: String,
    val coinsPerMinuteText: String?,
)
