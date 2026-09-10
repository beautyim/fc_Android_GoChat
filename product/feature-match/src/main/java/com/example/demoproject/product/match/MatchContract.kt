package com.example.demoproject.product.match

data class MatchUiState(
    val isLoading: Boolean = false,
    val status: String = "",
    val infoSummary: String = "",
    val startSummary: String = "",
)

sealed interface MatchIntent {
    data object LoadInfo : MatchIntent
    data object Start : MatchIntent
}

sealed interface MatchEffect {
    data class ShowMessage(val message: String) : MatchEffect
}
