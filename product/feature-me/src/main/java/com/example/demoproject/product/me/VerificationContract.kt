package com.example.demoproject.product.me

data class VerificationUiState(
    val cameraPermissionGranted: Boolean = false,
    val cameraPermissionDenied: Boolean = false,
)

sealed interface VerificationIntent {
    data object Back : VerificationIntent
    data object StartVerification : VerificationIntent
    data object RequestCameraPermission : VerificationIntent
    data class CameraPermissionResult(val granted: Boolean) : VerificationIntent
}

sealed interface VerificationEffect {
    data object NavigateBack : VerificationEffect
    data object OpenCapture : VerificationEffect
    data object RequestCameraPermission : VerificationEffect
    data class ShowMessage(val message: String) : VerificationEffect
}
