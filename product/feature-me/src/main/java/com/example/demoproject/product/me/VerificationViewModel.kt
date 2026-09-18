package com.example.demoproject.product.me

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VerificationViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(VerificationUiState())
    val uiState: StateFlow<VerificationUiState> = _uiState.asStateFlow()

    private val _effects = Channel<VerificationEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onIntent(intent: VerificationIntent) {
        when (intent) {
            VerificationIntent.Back -> viewModelScope.launch {
                _effects.send(VerificationEffect.NavigateBack)
            }
            VerificationIntent.StartVerification -> viewModelScope.launch {
                _effects.send(VerificationEffect.OpenCapture)
            }
            VerificationIntent.RequestCameraPermission -> viewModelScope.launch {
                _effects.send(VerificationEffect.RequestCameraPermission)
            }
            is VerificationIntent.CameraPermissionResult -> {
                _uiState.update {
                    it.copy(
                        cameraPermissionGranted = intent.granted,
                        cameraPermissionDenied = !intent.granted,
                    )
                }
                if (!intent.granted) {
                    viewModelScope.launch {
                        _effects.send(
                            VerificationEffect.ShowMessage(
                                getApplication<Application>()
                                    .getString(R.string.verify_camera_permission_denied),
                            ),
                        )
                    }
                }
            }
        }
    }

    fun markCameraPermission(granted: Boolean) {
        _uiState.update {
            it.copy(
                cameraPermissionGranted = granted,
                cameraPermissionDenied = !granted && it.cameraPermissionDenied,
            )
        }
    }
}
