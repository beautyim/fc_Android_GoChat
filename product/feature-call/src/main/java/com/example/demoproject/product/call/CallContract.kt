package com.example.demoproject.product.call

data class CallUiState(
    val isLoading: Boolean = false,
    val status: String = "",
    val records: List<String> = emptyList(),
    val mediaHint: String = "",
    val coordinatorState: String = "Idle",
)

sealed interface CallIntent {
    data object LoadRecords : CallIntent
    data object StartVideoCall : CallIntent
    data object StartVoiceCall : CallIntent
    data object Hangup : CallIntent
}

sealed interface CallEffect {
    data class ShowMessage(val message: String) : CallEffect
}
