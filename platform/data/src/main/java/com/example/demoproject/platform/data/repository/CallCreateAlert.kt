package com.example.demoproject.platform.data.repository

/**
 * Business alert for `/call/create` failures.
 *
 * - Backend `callback.func_name == "alert"` (e.g. offline) supplies [message] /
 *   [confirmLabel].
 * - Local `ok = 4` (peer unavailable / busy) sets [showSendMessageAction] so the UI
 *   can offer "Send message" + "Got it" with localized copy when [message] is blank.
 */
data class CallCreateAlert(
    val message: String,
    val title: String = "",
    val confirmLabel: String = "",
    val showSendMessageAction: Boolean = false,
)

/**
 * Prefers a backend callback alert for generic failures. For bare / callback
 * `ok = 4` (peer unavailable), always enable the Send-message action and reuse
 * any backend message when present.
 */
fun resolveCallCreateAlert(
    failureCode: Int,
    callbackAlert: CallCreateAlert?,
): CallCreateAlert? {
    if (failureCode == CallSessionRepository.CREATE_PEER_UNAVAILABLE) {
        return CallCreateAlert(
            message = callbackAlert?.message.orEmpty(),
            title = callbackAlert?.title.orEmpty(),
            confirmLabel = callbackAlert?.confirmLabel.orEmpty(),
            showSendMessageAction = true,
        )
    }
    return callbackAlert
}
