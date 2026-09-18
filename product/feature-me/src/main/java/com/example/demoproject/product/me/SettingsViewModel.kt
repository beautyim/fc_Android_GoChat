package com.example.demoproject.product.me

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.demoproject.platform.data.BuildConfig
import com.example.demoproject.platform.data.model.User
import com.example.demoproject.platform.data.network.NetworkRuntime
import com.example.demoproject.platform.network.result.AppResult
import kotlinx.coroutines.Job
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

    private var loadJob: Job? = null

    init {
        if (BuildConfig.DEBUG) {
            _uiState.update { it.copy(showFakePaymentToggle = true) }
            viewModelScope.launch {
                runtime.appPrefs.fakePaymentEnabled.collect { enabled ->
                    _uiState.update { it.copy(fakePaymentEnabled = enabled) }
                }
            }
        }
        // Initial load is triggered by SettingsScreen RESUMED Refresh so we do not
        // double-fetch (and flash skeleton twice) on first enter.
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
                viewModelScope.launch {
                    _effects.send(SettingsEffect.OpenVerification)
                }
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
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (!_uiState.value.hasLoaded) {
                val uid = runtime.sessionManager.currentUserId?.takeIf { it.isNotBlank() }
                if (uid != null) {
                    runtime.profileRepository.getCachedUser(uid)?.let(::applyUser)
                }
            }
            _uiState.update {
                it.copy(
                    // Only show skeleton before the first successful apply; refresh
                    // after bind/change email must not blank the list (esp. unbound).
                    isLoading = !it.hasLoaded,
                    errorMessage = null,
                    languageLabel = str(R.string.settings_language_value),
                )
            }
            when (val result = runtime.profileRepository.getMyProfile()) {
                is AppResult.Success -> applyUser(result.data)
                is AppResult.Failure -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = if (it.hasLoaded) null else result.message,
                    )
                }
            }
        }
    }

    private fun applyUser(user: User) {
        _uiState.update {
            it.copy(
                isLoading = false,
                hasLoaded = true,
                errorMessage = null,
                email = user.email?.trim()?.takeIf(String::isNotEmpty),
                isAuthVerified = user.isAuthVerified,
                languageLabel = str(R.string.settings_language_value),
            )
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
