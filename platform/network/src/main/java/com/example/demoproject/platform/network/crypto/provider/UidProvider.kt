package com.example.demoproject.platform.network.crypto.provider

/**
 * Returns the uid fed into [com.example.demoproject.platform.network.crypto.ApiKeyDeriver].
 *
 * The MVP contract always uses the anonymous `0` so both pre-login and post-login
 * traffic shares a single AES key; this interface exists so a future rollout can
 * bind uids to per-user keys without touching the interceptor.
 */
fun interface UidProvider {
    fun currentUid(): Long

    companion object {
        const val ANONYMOUS: Long = 0L
    }
}
