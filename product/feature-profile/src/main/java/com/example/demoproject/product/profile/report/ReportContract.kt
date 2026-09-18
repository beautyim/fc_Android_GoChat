package com.example.demoproject.product.profile.report

import com.example.demoproject.platform.data.repository.ReportReason

data class ReportPhotoUi(
    val localUri: String,
)

data class ReportUiState(
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val targetUid: Long = 0L,
    val nickname: String = "",
    val age: Int = 0,
    val avatarUrl: String? = null,
    /** null = unknown (hide status row). */
    val isOnline: Boolean? = null,
    val reasons: List<ReportReason> = emptyList(),
    val selectedReasonId: Int? = null,
    val photos: List<ReportPhotoUi> = emptyList(),
) {
    val canSubmit: Boolean
        get() = selectedReasonId != null && !isSubmitting && !isLoading

    val displayName: String
        get() = if (age > 0 && nickname.isNotBlank()) {
            "$nickname, $age"
        } else {
            nickname
        }
}

sealed interface ReportIntent {
    data object Load : ReportIntent
    data object Back : ReportIntent
    data object Retry : ReportIntent
    data class SelectReason(val reasonId: Int) : ReportIntent
    data class AddPhotos(val uris: List<String>) : ReportIntent
    data class RemovePhoto(val localUri: String) : ReportIntent
    data object Submit : ReportIntent
}

sealed interface ReportEffect {
    data object NavigateBack : ReportEffect
    /** Report submitted; UI should toast then leave the page. */
    data class SubmitSucceeded(val message: String) : ReportEffect
    data class ShowMessage(val message: String) : ReportEffect
}
