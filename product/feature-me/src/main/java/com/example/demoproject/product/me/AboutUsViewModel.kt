package com.example.demoproject.product.me

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.demoproject.platform.data.network.NetworkRuntime
import com.example.demoproject.platform.network.result.AppResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AboutUsViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val runtime = NetworkRuntime.get(application)
    private val _uiState = MutableStateFlow(AboutUsUiState())
    val uiState: StateFlow<AboutUsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<AboutUsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onIntent(intent: AboutUsIntent) {
        when (intent) {
            AboutUsIntent.Back -> emit(AboutUsEffect.NavigateBack)
            AboutUsIntent.OpenContact -> emit(AboutUsEffect.OpenContact)
            AboutUsIntent.OpenPrivacy -> emit(AboutUsEffect.OpenPrivacy)
            AboutUsIntent.OpenTerms -> emit(AboutUsEffect.OpenTerms)
            AboutUsIntent.RequestDeleteAccount -> {
                if (_uiState.value.isDeletingAccount) return
                _uiState.update { it.copy(showDeleteConfirm = true) }
            }
            AboutUsIntent.DismissDeleteAccount -> {
                if (_uiState.value.isDeletingAccount) return
                _uiState.update { it.copy(showDeleteConfirm = false) }
            }
            AboutUsIntent.ConfirmDeleteAccount -> deleteAccount()
        }
    }

    private fun deleteAccount() {
        if (_uiState.value.isDeletingAccount) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingAccount = true) }
            when (val result = runtime.profileRepository.disbandAccount()) {
                is AppResult.Success -> {
                    runtime.sessionManager.clearSession()
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isDeletingAccount = false,
                            showDeleteConfirm = false,
                        )
                    }
                    _effects.send(
                        AboutUsEffect.ShowMessage(
                            result.message.ifBlank { str(R.string.about_delete_failed) },
                        ),
                    )
                }
            }
        }
    }

    private fun emit(effect: AboutUsEffect) {
        viewModelScope.launch {
            _effects.send(effect)
        }
    }

    private fun str(@StringRes id: Int): String =
        getApplication<Application>().getString(id)
}
