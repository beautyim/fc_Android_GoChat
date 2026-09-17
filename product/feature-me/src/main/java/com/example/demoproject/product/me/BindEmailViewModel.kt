package com.example.demoproject.product.me

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.demoproject.platform.data.network.NetworkRuntime
import com.example.demoproject.platform.network.result.AppResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BindEmailViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val runtime = NetworkRuntime.get(application)
    private val _uiState = MutableStateFlow(BindEmailUiState())
    val uiState: StateFlow<BindEmailUiState> = _uiState.asStateFlow()

    private val _effects = Channel<BindEmailEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var sendCodeCooldownJob: Job? = null

    fun onIntent(intent: BindEmailIntent) {
        when (intent) {
            BindEmailIntent.Back -> emit(BindEmailEffect.NavigateBack)
            is BindEmailIntent.EmailChanged -> _uiState.update {
                it.copy(
                    email = EmailCredentialsRules.sanitizeEmail(intent.value),
                    emailErrorRes = null,
                )
            }
            is BindEmailIntent.CodeChanged -> _uiState.update {
                it.copy(
                    verificationCode = EmailCredentialsRules.sanitizeVerificationCode(intent.value),
                    codeErrorRes = null,
                )
            }
            is BindEmailIntent.PasswordChanged -> _uiState.update {
                it.copy(
                    password = EmailCredentialsRules.sanitizePassword(intent.value),
                    passwordErrorRes = null,
                )
            }
            BindEmailIntent.SendCode -> sendCode()
            BindEmailIntent.Submit -> submit()
        }
    }

    private fun sendCode() {
        viewModelScope.launch {
            if (_uiState.value.sendCodeCooldownSec > 0 || _uiState.value.isLoading) return@launch
            val email = EmailCredentialsRules.sanitizeEmail(_uiState.value.email)
            val emailErrorRes = when {
                email.isEmpty() -> R.string.email_error_required
                else -> EmailCredentialsRules.emailErrorRes(email)
            }
            if (emailErrorRes != null) {
                val message = str(emailErrorRes)
                _uiState.update { it.copy(email = email, emailErrorRes = emailErrorRes) }
                emit(BindEmailEffect.ShowMessage(message))
                return@launch
            }
            _uiState.update {
                it.copy(email = email, emailErrorRes = null, loadingAction = EmailLoadingAction.SendCode)
            }
            when (val result = runtime.profileRepository.sendUpdateEmailCode(email)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(loadingAction = null) }
                    startSendCodeCooldown()
                    emit(BindEmailEffect.ShowMessage(str(R.string.email_message_code_sent)))
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(loadingAction = null) }
                    emit(BindEmailEffect.ShowMessage(result.message))
                }
            }
        }
    }

    private fun submit() {
        viewModelScope.launch {
            if (_uiState.value.isLoading) return@launch
            val email = EmailCredentialsRules.sanitizeEmail(_uiState.value.email)
            val code = EmailCredentialsRules.sanitizeVerificationCode(_uiState.value.verificationCode)
            val password = EmailCredentialsRules.sanitizePassword(_uiState.value.password)
            val emailErrorRes = when {
                email.isEmpty() -> R.string.email_error_required
                else -> EmailCredentialsRules.emailErrorRes(email)
            }
            val codeErrorRes = when {
                code.isEmpty() -> R.string.email_error_code_required
                EmailCredentialsRules.parseEmailCode(code) == null -> R.string.email_error_code_invalid
                else -> null
            }
            val passwordErrorRes = when {
                password.isEmpty() -> R.string.email_error_password_required
                else -> EmailCredentialsRules.passwordErrorRes(password)
            }
            if (emailErrorRes != null || codeErrorRes != null || passwordErrorRes != null) {
                _uiState.update {
                    it.copy(
                        email = email,
                        verificationCode = code,
                        password = password,
                        emailErrorRes = emailErrorRes,
                        codeErrorRes = codeErrorRes,
                        passwordErrorRes = passwordErrorRes,
                    )
                }
                val messageRes = emailErrorRes ?: codeErrorRes ?: passwordErrorRes
                if (messageRes != null) emit(BindEmailEffect.ShowMessage(str(messageRes)))
                return@launch
            }
            val emailCode = EmailCredentialsRules.parseEmailCode(code) ?: return@launch
            _uiState.update {
                it.copy(
                    email = email,
                    verificationCode = code,
                    password = password,
                    emailErrorRes = null,
                    codeErrorRes = null,
                    passwordErrorRes = null,
                    loadingAction = EmailLoadingAction.Submit,
                )
            }
            when (
                val result = runtime.profileRepository.bindEmail(
                    email = email,
                    password = password,
                    emailCode = emailCode,
                )
            ) {
                is AppResult.Success -> {
                    sendCodeCooldownJob?.cancel()
                    sendCodeCooldownJob = null
                    _uiState.update { it.copy(loadingAction = null, sendCodeCooldownSec = 0) }
                    emit(BindEmailEffect.ShowMessage(str(R.string.email_message_bind_ok)))
                    emit(BindEmailEffect.BindSucceeded)
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(loadingAction = null) }
                    emit(BindEmailEffect.ShowMessage(result.message))
                }
            }
        }
    }

    private fun startSendCodeCooldown() {
        sendCodeCooldownJob?.cancel()
        sendCodeCooldownJob = viewModelScope.launch {
            for (remaining in SendCodeCooldownSeconds downTo 1) {
                _uiState.update { it.copy(sendCodeCooldownSec = remaining) }
                delay(1_000)
            }
            _uiState.update { it.copy(sendCodeCooldownSec = 0) }
        }
    }

    private fun emit(effect: BindEmailEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    private fun str(@StringRes id: Int): String =
        getApplication<Application>().getString(id)

    private companion object {
        const val SendCodeCooldownSeconds = 60
    }
}
