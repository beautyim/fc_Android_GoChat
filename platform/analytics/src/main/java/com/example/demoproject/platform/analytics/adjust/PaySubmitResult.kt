package com.example.demoproject.platform.analytics.adjust

/**
 * Outcome of handing a `pay` event to the Adjust SDK (or deciding not to).
 * [SkippedTerminal] should clear the outbox entry; [SkippedRetryable]/[Failed] keep it.
 */
enum class PaySubmitResult {
    Submitted,
    SkippedTerminal,
    SkippedRetryable,
    Failed,
}
