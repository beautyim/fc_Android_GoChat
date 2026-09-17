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

class ChangeEmailViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val runtime = NetworkRuntime.get(application)
    private val _uiState = MutableStateFlow(ChangeEmailUiState())
    val uiState: StateFlow<ChangeEmailUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ChangeEmailEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var sendCodeCooldownJob: Job? = null

    init {
        loadCurrentEmail()
    }

    fun onIntent(intent: ChangeEmailIntent) {
        when (intent) {
            ChangeEmailIntent.Back -> emit(ChangeEmailEffect.NavigateBack)
            is ChangeEmailIntent.NewEmailChanged -> _uiState.update {
                it.copy(
                    newEmail = EmailCredentialsRules.sanitizeEmail(intent.value),
                    newEmailErrorRes = null,
                )
            }
            is ChangeEmailIntent.CodeChanged -> _uiState.update {
                it.copy(
                    verificationCode = EmailCredentialsRules.sanitizeVerificationCode(intent.value),
                    codeErrorRes = null,
                )
            }
            ChangeEmailIntent.SendCode -> sendCode()
            ChangeEmailIntent.Submit -> submit()
        }
    }

    private fun loadCurrentEmail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCurrent = true) }
            when (val result = runtime.profileRepository.getMyHomeDetail()) {
                is AppResult.Success -> {
                    val email = result.data.user.email?.trim().orEmpty()
                    _uiState.update {
                        it.copy(currentEmail = email, isLoadingCurrent = false)
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isLoadingCurrent = false) }
                    emit(ChangeEmailEffect.ShowMessage(result.message))
                }
            }
        }
    }

    private fun sendCode() {
        viewModelScope.launch {
            if (_uiState.value.sendCodeCooldownSec > 0 || _uiState.value.isLoading) return@launch
            val newEmail = EmailCredentialsRules.sanitizeEmail(_uiState.value.newEmail)
            val emailErrorRes = when {
                newEmail.isEmpty() -> R.string.email_error_new_required
                else -> EmailCredentialsRules.emailErrorRes(newEmail)
            }
            if (emailErrorRes != null) {
                val message = str(emailErrorRes)
                _uiState.update { it.copy(newEmail = newEmail, newEmailErrorRes = emailErrorRes) }
                emit(ChangeEmailEffect.ShowMessage(message))
                return@launch
            }
            _uiState.update {
                it.copy(
                    newEmail = newEmail,
                    newEmailErrorRes = null,
                    loadingAction = EmailLoadingAction.SendCode,
                )
            }
            when (val result = runtime.profileRepository.sendUpdateEmailCode(newEmail)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(loadingAction = null) }
                    startSendCodeCooldown()
                    emit(ChangeEmailEffect.ShowMessage(str(R.string.email_message_code_sent)))
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(loadingAction = null) }
                    emit(ChangeEmailEffect.ShowMessage(result.message))
                }
            }
        }
    }

    private fun submit() {
        viewModelScope.launch {
            if (_uiState.value.isLoading) return@launch
            val newEmail = EmailCredentialsRules.sanitizeEmail(_uiState.value.newEmail)
            val code = EmailCredentialsRules.sanitizeVerificationCode(_uiState.value.verificationCode)
            val emailErrorRes = when {
                newEmail.isEmpty() -> R.string.email_error_new_required
                else -> EmailCredentialsRules.emailErrorRes(newEmail)
            }
            val codeErrorRes = when {
                code.isEmpty() -> R.string.email_error_code_required
                EmailCredentialsRules.parseEmailCode(code) == null -> R.string.email_error_code_invalid
                else -> null
            }
            if (emailErrorRes != null || codeErrorRes != null) {
                _uiState.update {
                    it.copy(
                        newEmail = newEmail,
                        verificationCode = code,
                        newEmailErrorRes = emailErrorRes,
                        codeErrorRes = codeErrorRes,
                    )
                }
                val messageRes = emailErrorRes ?: codeErrorRes
                if (messageRes != null) emit(ChangeEmailEffect.ShowMessage(str(messageRes)))
                return@launch
            }
            val emailCode = EmailCredentialsRules.parseEmailCode(code) ?: return@launch
            _uiState.update {
                it.copy(
                    newEmail = newEmail,
                    verificationCode = code,
                    newEmailErrorRes = null,
                    codeErrorRes = null,
                    loadingAction = EmailLoadingAction.Submit,
                )
            }
            when (
                val result = runtime.profileRepository.modifyEmail(
                    email = newEmail,
                    emailCode = emailCode,
                )
            ) {
                is AppResult.Success -> {
                    sendCodeCooldownJob?.cancel()
                    sendCodeCooldownJob = null
                    _uiState.update { it.copy(loadingAction = null, sendCodeCooldownSec = 0) }
                    emit(ChangeEmailEffect.ShowMessage(str(R.string.email_message_change_ok)))
                    emit(ChangeEmailEffect.ChangeSucceeded)
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(loadingAction = null) }
                    emit(ChangeEmailEffect.ShowMessage(result.message))
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

    private fun emit(effect: ChangeEmailEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    private fun str(@StringRes id: Int): String =
        getApplication<Application>().getString(id)

    private companion object {
        const val SendCodeCooldownSeconds = 60
    }
}
