package com.example.demoproject.product.me

data class AboutUsUiState(
    val showDeleteConfirm: Boolean = false,
    val isDeletingAccount: Boolean = false,
)

sealed interface AboutUsIntent {
    data object Back : AboutUsIntent
    data object OpenContact : AboutUsIntent
    data object OpenPrivacy : AboutUsIntent
    data object OpenTerms : AboutUsIntent
    data object RequestDeleteAccount : AboutUsIntent
    data object DismissDeleteAccount : AboutUsIntent
    data object ConfirmDeleteAccount : AboutUsIntent
}

sealed interface AboutUsEffect {
    data object NavigateBack : AboutUsEffect
    data object OpenContact : AboutUsEffect
    data object OpenPrivacy : AboutUsEffect
    data object OpenTerms : AboutUsEffect
    data class ShowMessage(val message: String) : AboutUsEffect
}
