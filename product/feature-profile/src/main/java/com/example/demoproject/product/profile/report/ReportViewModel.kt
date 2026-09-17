package com.example.demoproject.product.profile.report

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.demoproject.platform.data.network.NetworkRuntime
import com.example.demoproject.platform.network.result.AppResult
import com.example.demoproject.product.profile.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReportViewModel(
    application: Application,
    private val targetUserId: String,
    private val initialAge: Int = 0,
    private val initialIsOnline: Boolean? = null,
) : AndroidViewModel(application) {
    private val runtime = NetworkRuntime.get(application)
    private val _uiState = MutableStateFlow(
        ReportUiState(
            age = initialAge.coerceAtLeast(0),
            isOnline = initialIsOnline,
        ),
    )
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<ReportEffect>(extraBufferCapacity = 1)
    val effect: SharedFlow<ReportEffect> = _effect.asSharedFlow()

    init {
        onIntent(ReportIntent.Load)
    }

    fun onIntent(intent: ReportIntent) {
        when (intent) {
            ReportIntent.Load, ReportIntent.Retry -> load()
            ReportIntent.Back -> emit(ReportEffect.NavigateBack)
            is ReportIntent.SelectReason -> _uiState.update {
                it.copy(selectedReasonId = intent.reasonId)
            }
            is ReportIntent.AddPhotos -> addPhotos(intent.uris)
            is ReportIntent.RemovePhoto -> _uiState.update { state ->
                state.copy(photos = state.photos.filterNot { it.localUri == intent.localUri })
            }
            ReportIntent.Submit -> submit()
        }
    }

    private fun load() {
        val uid = targetUserId.toLongOrNull()?.takeIf { it > 0L }
        if (uid == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = str(R.string.report_error_invalid_user),
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, targetUid = uid)
            }
            when (val result = runtime.reportRepository.loadReportInit(uid)) {
                is AppResult.Success -> {
                    val data = result.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            targetUid = data.targetUid,
                            nickname = data.nickname.ifBlank { it.nickname },
                            avatarUrl = data.avatarUrl ?: it.avatarUrl,
                            reasons = data.reasons,
                            selectedReasonId = it.selectedReasonId
                                ?.takeIf { id -> data.reasons.any { reason -> reason.id == id } },
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }
        }
    }

    private fun addPhotos(uris: List<String>) {
        if (uris.isEmpty()) return
        _uiState.update { state ->
            val remaining = MAX_PHOTOS - state.photos.size
            if (remaining <= 0) return@update state
            val incoming = uris
                .asSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .filterNot { uri -> state.photos.any { it.localUri == uri } }
                .take(remaining)
                .map { ReportPhotoUi(localUri = it) }
                .toList()
            if (incoming.isEmpty()) state else state.copy(photos = state.photos + incoming)
        }
    }

    private fun submit() {
        val state = _uiState.value
        val reasonId = state.selectedReasonId ?: return
        if (!state.canSubmit || state.targetUid <= 0L) return
        val reasonTitle = state.reasons.firstOrNull { it.id == reasonId }?.title.orEmpty()
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            when (
                val result = runtime.reportRepository.submitReport(
                    targetUid = state.targetUid,
                    content = reasonTitle,
                    reasonIds = listOf(reasonId),
                    photoUris = state.photos.map { it.localUri },
                )
            ) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    emit(ReportEffect.ShowMessage(str(R.string.report_submit_success)))
                    emit(ReportEffect.SubmitSucceeded)
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    emit(ReportEffect.ShowMessage(result.message))
                }
            }
        }
    }

    private fun emit(effect: ReportEffect) {
        _effect.tryEmit(effect)
    }

    private fun str(@StringRes id: Int): String = getApplication<Application>().getString(id)

    companion object {
        const val MAX_PHOTOS = 3
    }
}
