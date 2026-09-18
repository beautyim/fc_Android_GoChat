package com.example.demoproject.product.me

data class SettingsUiState(
    val isLoading: Boolean = true,
    /** True after the first successful apply (network or cache). */
    val hasLoaded: Boolean = false,
    val errorMessage: String? = null,
    val email: String? = null,
    val languageLabel: String = "",
    val isAuthVerified: Boolean = false,
    val showLogoutConfirm: Boolean = false,
    val isLoggingOut: Boolean = false,
    val showFakePaymentToggle: Boolean = false,
    val fakePaymentEnabled: Boolean = false,
) {
    val hasBoundEmail: Boolean get() = !email.isNullOrBlank()
}

sealed interface SettingsIntent {
    data object Refresh : SettingsIntent
    data object Back : SettingsIntent
    data object OpenEmail : SettingsIntent
    data object OpenLanguage : SettingsIntent
    data object OpenBlockedUsers : SettingsIntent
    data object OpenChangePassword : SettingsIntent
    data object OpenVerification : SettingsIntent
    data object OpenAbout : SettingsIntent
    data object RequestLogout : SettingsIntent
    data object DismissLogout : SettingsIntent
    data object ConfirmLogout : SettingsIntent
    data class ToggleFakePayment(val enabled: Boolean) : SettingsIntent
}

sealed interface SettingsEffect {
    data object NavigateBack : SettingsEffect
    data object OpenBlockedUsers : SettingsEffect
    data object OpenAbout : SettingsEffect
    data object OpenBindEmail : SettingsEffect
    data object OpenChangeEmail : SettingsEffect
    data object OpenVerification : SettingsEffect
    data class ShowMessage(val message: String) : SettingsEffect
}
