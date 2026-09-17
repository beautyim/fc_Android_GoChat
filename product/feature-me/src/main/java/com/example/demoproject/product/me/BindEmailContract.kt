package com.example.demoproject.product.me

data class BindEmailUiState(
    val email: String = "",
    val verificationCode: String = "",
    val password: String = "",
    val sendCodeCooldownSec: Int = 0,
    val loadingAction: EmailLoadingAction? = null,
    val emailErrorRes: Int? = null,
    val codeErrorRes: Int? = null,
    val passwordErrorRes: Int? = null,
) {
    val isLoading: Boolean get() = loadingAction != null
}

sealed interface BindEmailIntent {
    data object Back : BindEmailIntent
    data class EmailChanged(val value: String) : BindEmailIntent
    data class CodeChanged(val value: String) : BindEmailIntent
    data class PasswordChanged(val value: String) : BindEmailIntent
    data object SendCode : BindEmailIntent
    data object Submit : BindEmailIntent
}

sealed interface BindEmailEffect {
    data object NavigateBack : BindEmailEffect
    data object BindSucceeded : BindEmailEffect
    data class ShowMessage(val message: String) : BindEmailEffect
}

enum class EmailLoadingAction {
    SendCode,
    Submit,
}
