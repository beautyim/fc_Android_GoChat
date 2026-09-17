package com.example.demoproject.product.me

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.demoproject.platform.data.BuildConfig
import com.example.demoproject.platform.data.network.NetworkRuntime
import com.example.demoproject.platform.network.result.AppResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val runtime = NetworkRuntime.get(application)
    private val _uiState = MutableStateFlow(
        SettingsUiState(languageLabel = str(R.string.settings_language_value)),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        if (BuildConfig.DEBUG) {
            _uiState.update { it.copy(showFakePaymentToggle = true) }
            viewModelScope.launch {
                runtime.appPrefs.fakePaymentEnabled.collect { enabled ->
                    _uiState.update { it.copy(fakePaymentEnabled = enabled) }
                }
            }
        }
        loadSettings()
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            SettingsIntent.Refresh -> loadSettings()
            SettingsIntent.Back -> viewModelScope.launch {
                _effects.send(SettingsEffect.NavigateBack)
            }
            SettingsIntent.OpenEmail -> viewModelScope.launch {
                if (_uiState.value.hasBoundEmail) {
                    _effects.send(SettingsEffect.OpenChangeEmail)
                } else {
                    _effects.send(SettingsEffect.OpenBindEmail)
                }
            }
            SettingsIntent.OpenLanguage -> emitComingSoon(R.string.settings_message_language_soon)
            SettingsIntent.OpenBlockedUsers -> viewModelScope.launch {
                _effects.send(SettingsEffect.OpenBlockedUsers)
            }
            SettingsIntent.OpenChangePassword -> emitComingSoon(R.string.settings_message_password_soon)
            SettingsIntent.OpenVerification -> {
                if (_uiState.value.isAuthVerified) return
                emitComingSoon(R.string.me_message_verify_soon)
            }
            SettingsIntent.OpenAbout -> viewModelScope.launch {
                _effects.send(SettingsEffect.OpenAbout)
            }
            SettingsIntent.RequestLogout -> _uiState.update { it.copy(showLogoutConfirm = true) }
            SettingsIntent.DismissLogout -> _uiState.update {
                it.copy(showLogoutConfirm = false, isLoggingOut = false)
            }
            SettingsIntent.ConfirmLogout -> logout()
            is SettingsIntent.ToggleFakePayment -> setFakePaymentEnabled(intent.enabled)
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = it.email == null,
                    errorMessage = null,
                    languageLabel = str(R.string.settings_language_value),
                )
            }
            when (val result = runtime.profileRepository.getMyHomeDetail()) {
                is AppResult.Success -> {
                    val user = result.data.user
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            email = user.email?.trim()?.takeIf(String::isNotEmpty),
                            isAuthVerified = user.isAuthVerified,
                            languageLabel = str(R.string.settings_language_value),
                        )
                    }
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.message,
                    )
                }
            }
        }
    }

    private fun setFakePaymentEnabled(enabled: Boolean) {
        if (!BuildConfig.DEBUG) return
        viewModelScope.launch {
            runtime.appPrefs.setFakePaymentEnabled(enabled)
        }
    }

    private fun logout() {
        if (_uiState.value.isLoggingOut) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true) }
            runtime.sessionManager.clearSession()
        }
    }

    private fun emitComingSoon(@StringRes id: Int) {
        viewModelScope.launch {
            _effects.send(SettingsEffect.ShowMessage(str(id)))
        }
    }

    private fun str(@StringRes id: Int): String =
        getApplication<Application>().getString(id)
}
