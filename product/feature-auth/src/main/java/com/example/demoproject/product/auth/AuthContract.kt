package com.example.demoproject.product.auth

import androidx.annotation.StringRes

enum class AuthStep {
    Landing,
    EmailForm,
    ForgetPassword,
}

/** Which auth CTA is currently in-flight; drives button-level loading only. */
enum class AuthLoadingAction {
    GuestLogin,
    EmailSubmit,
    ConfirmSignUp,
    SendResetCode,
    ConfirmResetPassword,
}

data class AuthUiState(
    val step: AuthStep = AuthStep.Landing,
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val verificationCode: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    /** Shared visibility for new-password + confirm-password fields. */
    val isResetPasswordVisible: Boolean = false,
    val sendCodeCooldownSec: Int = 0,
    val loadingAction: AuthLoadingAction? = null,
    val status: String = "",
    val sessionLabel: String = "",
    @StringRes val emailErrorRes: Int? = null,
    @StringRes val passwordErrorRes: Int? = null,
    @StringRes val codeErrorRes: Int? = null,
    @StringRes val newPasswordErrorRes: Int? = null,
    @StringRes val confirmPasswordErrorRes: Int? = null,
    val showCreateAccountDialog: Boolean = false,
) {
    val isLoading: Boolean get() = loadingAction != null
}

sealed interface AuthIntent {
    data object ShowLanding : AuthIntent
    data object ShowEmailForm : AuthIntent
    data class EmailChanged(val value: String) : AuthIntent
    data class PasswordChanged(val value: String) : AuthIntent
    data object TogglePasswordVisibility : AuthIntent
    data class VerificationCodeChanged(val value: String) : AuthIntent
    data class NewPasswordChanged(val value: String) : AuthIntent
    data class ConfirmPasswordChanged(val value: String) : AuthIntent
    data object ToggleResetPasswordVisibility : AuthIntent
    data object GuestLogin : AuthIntent
    data object EmailLogin : AuthIntent
    data object GoogleLogin : AuthIntent
    data object ForgotPassword : AuthIntent
    data object SendResetCode : AuthIntent
    data object ConfirmResetPassword : AuthIntent
    data object DismissCreateAccountDialog : AuthIntent
    data object ConfirmSignUp : AuthIntent
    data object OpenTerms : AuthIntent
    data object OpenPrivacy : AuthIntent
    data object Logout : AuthIntent
    data object Refresh : AuthIntent
}

sealed interface AuthEffect {
    data class ShowMessage(val message: String) : AuthEffect
    data object OpenTerms : AuthEffect
    data object OpenPrivacy : AuthEffect
}
