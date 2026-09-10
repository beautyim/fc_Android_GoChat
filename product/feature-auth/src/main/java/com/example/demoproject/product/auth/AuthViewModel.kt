package com.example.demoproject.product.auth

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.demoproject.platform.analytics.AnalyticsEvent
import com.example.demoproject.platform.analytics.AnalyticsHolder
import com.example.demoproject.platform.data.network.NetworkRuntime
import com.example.demoproject.platform.data.repository.AuthLoginResult
import com.example.demoproject.platform.network.result.AppResult
import com.example.demoproject.ui.foundation.R as FoundationR
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val runtime = NetworkRuntime.get(application)
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<AuthEffect>(extraBufferCapacity = 1)
    val effect: SharedFlow<AuthEffect> = _effect.asSharedFlow()

    private var sendCodeCooldownJob: Job? = null

    init {
        onIntent(AuthIntent.Refresh)
    }

    fun onIntent(intent: AuthIntent) {
        when (intent) {
            AuthIntent.ShowLanding -> _uiState.update {
                it.copy(
                    step = AuthStep.Landing,
                    status = "",
                    emailErrorRes = null,
                    passwordErrorRes = null,
                    showCreateAccountDialog = false,
                )
            }
            AuthIntent.ShowEmailForm -> _uiState.update {
                it.copy(
                    step = AuthStep.EmailForm,
                    status = "",
                    emailErrorRes = null,
                    passwordErrorRes = null,
                    codeErrorRes = null,
                    newPasswordErrorRes = null,
                    confirmPasswordErrorRes = null,
                    showCreateAccountDialog = false,
                )
            }
            is AuthIntent.EmailChanged -> {
                val email = AuthCredentialsRules.sanitizeEmail(intent.value)
                _uiState.update {
                    it.copy(
                        email = email,
                        emailErrorRes = AuthCredentialsRules.emailErrorRes(email),
                    )
                }
            }
            is AuthIntent.PasswordChanged -> {
                val password = AuthCredentialsRules.sanitizePassword(intent.value)
                _uiState.update {
                    it.copy(
                        password = password,
                        passwordErrorRes = AuthCredentialsRules.passwordErrorRes(password),
                    )
                }
            }
            AuthIntent.TogglePasswordVisibility -> _uiState.update {
                it.copy(isPasswordVisible = !it.isPasswordVisible)
            }
            is AuthIntent.VerificationCodeChanged -> {
                val code = AuthCredentialsRules.sanitizeVerificationCode(intent.value)
                _uiState.update {
                    it.copy(
                        verificationCode = code,
                        codeErrorRes = null,
                    )
                }
            }
            is AuthIntent.NewPasswordChanged -> {
                val password = AuthCredentialsRules.sanitizePassword(intent.value)
                _uiState.update { state ->
                    state.copy(
                        newPassword = password,
                        newPasswordErrorRes = AuthCredentialsRules.resetPasswordErrorRes(password),
                        confirmPasswordErrorRes = AuthCredentialsRules.confirmPasswordErrorRes(
                            password = password,
                            confirmPassword = state.confirmPassword,
                        ),
                    )
                }
            }
            is AuthIntent.ConfirmPasswordChanged -> {
                val confirm = AuthCredentialsRules.sanitizePassword(intent.value)
                _uiState.update { state ->
                    state.copy(
                        confirmPassword = confirm,
                        confirmPasswordErrorRes = AuthCredentialsRules.confirmPasswordErrorRes(
                            password = state.newPassword,
                            confirmPassword = confirm,
                        ),
                    )
                }
            }
            AuthIntent.ToggleResetPasswordVisibility -> _uiState.update {
                it.copy(isResetPasswordVisible = !it.isResetPasswordVisible)
            }
            AuthIntent.GuestLogin -> guestLogin()
            AuthIntent.EmailLogin -> emailLogin()
            AuthIntent.GoogleLogin -> googleLogin()
            AuthIntent.ForgotPassword -> openForgetPassword()
            AuthIntent.SendResetCode -> sendResetCode()
            AuthIntent.ConfirmResetPassword -> confirmResetPassword()
            AuthIntent.DismissCreateAccountDialog -> {
                if (_uiState.value.loadingAction == AuthLoadingAction.ConfirmSignUp) return
                _uiState.update { it.copy(showCreateAccountDialog = false) }
            }
            AuthIntent.ConfirmSignUp -> confirmSignUp()
            AuthIntent.OpenTerms -> emit(AuthEffect.OpenTerms)
            AuthIntent.OpenPrivacy -> emit(AuthEffect.OpenPrivacy)
            AuthIntent.Logout -> logout()
            AuthIntent.Refresh -> refresh()
        }
    }

    private fun openForgetPassword() {
        sendCodeCooldownJob?.cancel()
        sendCodeCooldownJob = null
        val email = AuthCredentialsRules.sanitizeEmail(_uiState.value.email)
        _uiState.update {
            it.copy(
                step = AuthStep.ForgetPassword,
                email = email,
                emailErrorRes = AuthCredentialsRules.emailErrorRes(email),
                verificationCode = "",
                newPassword = "",
                confirmPassword = "",
                isResetPasswordVisible = false,
                sendCodeCooldownSec = 0,
                codeErrorRes = null,
                newPasswordErrorRes = null,
                confirmPasswordErrorRes = null,
                status = "",
                loadingAction = null,
                showCreateAccountDialog = false,
            )
        }
    }

    private fun refresh() {
        val session = runtime.sessionManager.currentSessionSnapshot
        _uiState.update {
            it.copy(
                sessionLabel = session?.let { s ->
                    str(FoundationR.string.label_session_uid_fmt, s.userId)
                } ?: str(FoundationR.string.label_session_logged_out),
                status = if (session != null) {
                    str(FoundationR.string.status_signed_in)
                } else {
                    ""
                },
            )
        }
    }

    private fun guestLogin() {
        viewModelScope.launch {
            if (_uiState.value.isLoading) return@launch
            _uiState.update {
                it.copy(
                    loadingAction = AuthLoadingAction.GuestLogin,
                    status = str(R.string.auth_status_guest_login),
                )
            }
            when (val result = runtime.authRepository.loginAsGuest()) {
                is AppResult.Success -> {
                    trackRegisterIfNeeded(result)
                    val uid = result.data.user?.id ?: runtime.sessionManager.currentUserId.orEmpty()
                    _uiState.update {
                        it.copy(
                            loadingAction = null,
                            status = str(R.string.auth_status_guest_ok_fmt, result.data.isRegister.toString()),
                            sessionLabel = str(FoundationR.string.label_session_uid_fmt, uid),
                        )
                    }
                    emitMessage(R.string.auth_status_login_ok)
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(loadingAction = null, status = result.message) }
                    emit(AuthEffect.ShowMessage(result.message))
                }
            }
        }
    }

    private fun emailLogin() {
        viewModelScope.launch {
            if (_uiState.value.isLoading) return@launch
            val credentials = validatedEmailCredentials() ?: return@launch
            _uiState.update {
                it.copy(
                    email = credentials.email,
                    password = credentials.password,
                    loadingAction = AuthLoadingAction.EmailSubmit,
                    status = str(R.string.auth_status_checking_email),
                    emailErrorRes = null,
                    passwordErrorRes = null,
                    showCreateAccountDialog = false,
                )
            }
            when (val check = runtime.authRepository.isEmailRegistered(credentials.email)) {
                is AppResult.Success -> {
                    if (check.data) {
                        performEmailLogin(credentials.email, credentials.password)
                    } else {
                        _uiState.update {
                            it.copy(
                                loadingAction = null,
                                status = "",
                                showCreateAccountDialog = true,
                            )
                        }
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(loadingAction = null, status = check.message) }
                    emit(AuthEffect.ShowMessage(check.message))
                }
            }
        }
    }

    private fun confirmSignUp() {
        viewModelScope.launch {
            if (_uiState.value.isLoading) return@launch
            val credentials = validatedEmailCredentials() ?: return@launch
            _uiState.update {
                it.copy(
                    email = credentials.email,
                    password = credentials.password,
                    loadingAction = AuthLoadingAction.ConfirmSignUp,
                    status = str(R.string.auth_status_email_login),
                    emailErrorRes = null,
                    passwordErrorRes = null,
                    showCreateAccountDialog = true,
                )
            }
            performEmailLogin(credentials.email, credentials.password)
        }
    }

    private fun sendResetCode() {
        viewModelScope.launch {
            if (_uiState.value.sendCodeCooldownSec > 0 || _uiState.value.isLoading) return@launch
            val email = AuthCredentialsRules.sanitizeEmail(_uiState.value.email)
            val emailErrorRes = when {
                email.isEmpty() -> R.string.auth_error_email_required
                else -> AuthCredentialsRules.emailErrorRes(email)
            }
            if (emailErrorRes != null) {
                val message = str(emailErrorRes)
                _uiState.update {
                    it.copy(
                        email = email,
                        emailErrorRes = emailErrorRes,
                        status = message,
                    )
                }
                emit(AuthEffect.ShowMessage(message))
                return@launch
            }
            _uiState.update {
                it.copy(
                    email = email,
                    emailErrorRes = null,
                    loadingAction = AuthLoadingAction.SendResetCode,
                    status = str(R.string.auth_status_sending_code),
                )
            }
            when (val result = runtime.authRepository.sendResetPasswordCode(email)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            loadingAction = null,
                            status = "",
                        )
                    }
                    startSendCodeCooldown()
                    emitMessage(R.string.auth_status_code_sent)
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(loadingAction = null, status = result.message) }
                    emit(AuthEffect.ShowMessage(result.message))
                }
            }
        }
    }

    private fun confirmResetPassword() {
        viewModelScope.launch {
            if (_uiState.value.isLoading) return@launch
            val form = validatedResetForm() ?: return@launch
            _uiState.update {
                it.copy(
                    email = form.email,
                    verificationCode = form.code,
                    newPassword = form.password,
                    confirmPassword = form.confirmPassword,
                    loadingAction = AuthLoadingAction.ConfirmResetPassword,
                    status = str(R.string.auth_status_resetting_password),
                    emailErrorRes = null,
                    codeErrorRes = null,
                    newPasswordErrorRes = null,
                    confirmPasswordErrorRes = null,
                )
            }
            when (val check = runtime.authRepository.checkEmailCode(form.email, form.code)) {
                is AppResult.Failure -> {
                    val message = check.message.ifBlank { str(R.string.auth_error_verification_code) }
                    _uiState.update {
                        it.copy(
                            loadingAction = null,
                            status = message,
                            codeErrorRes = R.string.auth_error_verification_code,
                        )
                    }
                    emit(AuthEffect.ShowMessage(message))
                    return@launch
                }
                is AppResult.Success -> Unit
            }
            when (val result = runtime.authRepository.resetPassword(form.email, form.password, form.code)) {
                is AppResult.Success -> {
                    sendCodeCooldownJob?.cancel()
                    sendCodeCooldownJob = null
                    _uiState.update {
                        it.copy(
                            step = AuthStep.EmailForm,
                            password = "",
                            verificationCode = "",
                            newPassword = "",
                            confirmPassword = "",
                            isResetPasswordVisible = false,
                            sendCodeCooldownSec = 0,
                            loadingAction = null,
                            status = "",
                            passwordErrorRes = null,
                            codeErrorRes = null,
                            newPasswordErrorRes = null,
                            confirmPasswordErrorRes = null,
                        )
                    }
                    emitMessage(R.string.auth_status_reset_password_ok)
                }
                is AppResult.Failure -> {
                    val message = result.message
                    val codeError = message.contains("code", ignoreCase = true) ||
                        message.contains("verification", ignoreCase = true)
                    _uiState.update {
                        it.copy(
                            loadingAction = null,
                            status = message,
                            codeErrorRes = if (codeError) R.string.auth_error_verification_code else null,
                        )
                    }
                    emit(AuthEffect.ShowMessage(message))
                }
            }
        }
    }

    private data class EmailCredentials(
        val email: String,
        val password: String,
    )

    private data class ResetForm(
        val email: String,
        val code: String,
        val password: String,
        val confirmPassword: String,
    )

    private fun validatedEmailCredentials(): EmailCredentials? {
        val email = AuthCredentialsRules.sanitizeEmail(_uiState.value.email)
        val password = AuthCredentialsRules.sanitizePassword(_uiState.value.password)
        val emailErrorRes = when {
            email.isEmpty() -> R.string.auth_error_email_required
            else -> AuthCredentialsRules.emailErrorRes(email)
        }
        val passwordErrorRes = when {
            password.isEmpty() -> R.string.auth_error_password_required
            else -> AuthCredentialsRules.passwordErrorRes(password)
        }
        if (emailErrorRes != null || passwordErrorRes != null) {
            val messageRes = when {
                email.isEmpty() || password.isEmpty() -> R.string.auth_status_email_required
                emailErrorRes != null -> emailErrorRes
                else -> passwordErrorRes!!
            }
            val message = str(messageRes)
            _uiState.update {
                it.copy(
                    email = email,
                    password = password,
                    emailErrorRes = emailErrorRes,
                    passwordErrorRes = passwordErrorRes,
                    status = message,
                    showCreateAccountDialog = false,
                    loadingAction = null,
                )
            }
            emit(AuthEffect.ShowMessage(message))
            return null
        }
        return EmailCredentials(email = email, password = password)
    }

    private fun validatedResetForm(): ResetForm? {
        val email = AuthCredentialsRules.sanitizeEmail(_uiState.value.email)
        val code = AuthCredentialsRules.sanitizeVerificationCode(_uiState.value.verificationCode)
        val password = AuthCredentialsRules.sanitizePassword(_uiState.value.newPassword)
        val confirmPassword = AuthCredentialsRules.sanitizePassword(_uiState.value.confirmPassword)
        val emailErrorRes = when {
            email.isEmpty() -> R.string.auth_error_email_required
            else -> AuthCredentialsRules.emailErrorRes(email)
        }
        val codeErrorRes = when {
            code.isEmpty() -> R.string.auth_error_code_required
            else -> null
        }
        val newPasswordErrorRes = when {
            password.isEmpty() -> R.string.auth_error_password_required
            else -> AuthCredentialsRules.resetPasswordErrorRes(password)
        }
        val confirmPasswordErrorRes = when {
            confirmPassword.isEmpty() -> R.string.auth_error_password_required
            else -> AuthCredentialsRules.confirmPasswordErrorRes(password, confirmPassword)
        }
        if (emailErrorRes != null ||
            codeErrorRes != null ||
            newPasswordErrorRes != null ||
            confirmPasswordErrorRes != null
        ) {
            val messageRes = emailErrorRes
                ?: codeErrorRes
                ?: newPasswordErrorRes
                ?: confirmPasswordErrorRes!!
            val message = str(messageRes)
            _uiState.update {
                it.copy(
                    email = email,
                    verificationCode = code,
                    newPassword = password,
                    confirmPassword = confirmPassword,
                    emailErrorRes = emailErrorRes,
                    codeErrorRes = codeErrorRes,
                    newPasswordErrorRes = newPasswordErrorRes,
                    confirmPasswordErrorRes = confirmPasswordErrorRes,
                    status = message,
                    loadingAction = null,
                )
            }
            emit(AuthEffect.ShowMessage(message))
            return null
        }
        return ResetForm(
            email = email,
            code = code,
            password = password,
            confirmPassword = confirmPassword,
        )
    }

    private fun startSendCodeCooldown(seconds: Int = SendCodeCooldownSeconds) {
        sendCodeCooldownJob?.cancel()
        sendCodeCooldownJob = viewModelScope.launch {
            for (remaining in seconds downTo 1) {
                _uiState.update { it.copy(sendCodeCooldownSec = remaining) }
                delay(1_000)
            }
            _uiState.update { it.copy(sendCodeCooldownSec = 0) }
        }
    }

    private suspend fun performEmailLogin(email: String, password: String) {
        _uiState.update {
            it.copy(
                status = str(R.string.auth_status_email_login),
            )
        }
        when (val result = runtime.authRepository.loginWithEmail(email, password)) {
            is AppResult.Success -> {
                trackRegisterIfNeeded(result)
                val uid = result.data.user?.id ?: runtime.sessionManager.currentUserId.orEmpty()
                _uiState.update {
                    it.copy(
                        loadingAction = null,
                        showCreateAccountDialog = false,
                        status = str(R.string.auth_status_login_ok),
                        sessionLabel = str(FoundationR.string.label_session_uid_fmt, uid),
                    )
                }
                emitMessage(R.string.auth_status_login_ok)
            }
            is AppResult.Failure -> {
                _uiState.update {
                    it.copy(
                        loadingAction = null,
                        status = result.message,
                    )
                }
                emit(AuthEffect.ShowMessage(result.message))
            }
        }
    }

    private fun googleLogin() {
        emitMessage(R.string.auth_status_google_unavailable)
    }

    private fun logout() {
        viewModelScope.launch {
            runtime.sessionManager.clearSession()
            _uiState.update {
                it.copy(
                    sessionLabel = str(FoundationR.string.label_session_logged_out),
                    status = str(FoundationR.string.status_logged_out),
                )
            }
        }
    }

    private fun trackRegisterIfNeeded(result: AppResult.Success<AuthLoginResult>) {
        if (!result.data.isRegister) return
        AnalyticsHolder.tracker?.track(
            AnalyticsEvent.Register(userId = result.data.user?.id.orEmpty()),
        )
    }

    private fun emitMessage(@StringRes id: Int) {
        emit(AuthEffect.ShowMessage(str(id)))
    }

    private fun emit(effect: AuthEffect) {
        viewModelScope.launch { _effect.emit(effect) }
    }

    private fun str(@StringRes id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)

    companion object {
        private const val SendCodeCooldownSeconds = 60
    }
}
