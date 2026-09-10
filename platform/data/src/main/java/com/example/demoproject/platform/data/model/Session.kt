package com.example.demoproject.platform.data.model

/**
 * Local session snapshot. Mirrors the subset of `/auth/login` `/auth/register` response
 * that the client needs to identify itself to the backend.
 *
 * The backend only returns a single opaque [token] (no refresh token, no expiry). The
 * `profile completed` flag is synthesised from `result.type` (1 = complete, 2 = needs
 * profile) at the edge of the repository layer.
 */
data class Session(
    /** Stringified `user_info.uid`. Empty when the backend did not echo a uid. */
    val userId: String,
    val token: String,
    val profileComplete: Boolean = false,
)
