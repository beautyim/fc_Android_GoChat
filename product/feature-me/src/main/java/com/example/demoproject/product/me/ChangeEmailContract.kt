package com.example.demoproject.product.me

data class ChangeEmailUiState(
    val currentEmail: String = "",
    val newEmail: String = "",
    val verificationCode: String = "",
    val isLoadingCurrent: Boolean = true,
    val sendCodeCooldownSec: Int = 0,
    val loadingAction: EmailLoadingAction? = null,
    val newEmailErrorRes: Int? = null,
    val codeErrorRes: Int? = null,
) {
    val isLoading: Boolean get() = loadingAction != null
}

sealed interface ChangeEmailIntent {
    data object Back : ChangeEmailIntent
    data class NewEmailChanged(val value: String) : ChangeEmailIntent
    data class CodeChanged(val value: String) : ChangeEmailIntent
    data object SendCode : ChangeEmailIntent
    data object Submit : ChangeEmailIntent
}

sealed interface ChangeEmailEffect {
    data object NavigateBack : ChangeEmailEffect
    data object ChangeSucceeded : ChangeEmailEffect
    data class ShowMessage(val message: String) : ChangeEmailEffect
}
