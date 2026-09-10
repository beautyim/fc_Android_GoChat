package com.example.demoproject.platform.data.local.crypto

/**
 * Symmetric cipher used to protect session payload fields (user id, token) before
 * they land in DataStore. Implementations MUST be thread-safe — the encrypt/decrypt
 * calls are invoked from DataStore's upstream flow collector as well as from the
 * IO-bound `edit {}` transaction.
 *
 * The plaintext/ciphertext boundary is intentionally [String]: ciphertext is Base64
 * encoded so the backing store (currently DataStore Preferences) can keep treating
 * values as plain `stringPreferencesKey` without binary juggling.
 */
interface SessionCipher {

    /**
     * Encrypt [plaintext] into a Base64-encoded opaque string.
     *
     * @throws SessionCryptoException when the underlying key material cannot be
     * created or loaded (e.g. Android Keystore is in a permanently broken state
     * after an OS-level factory reset of encryption keys).
     */
    fun encrypt(plaintext: String): String

    /**
     * Decrypt a previously produced [ciphertext]. Returns `null` when the input is
     * malformed or was produced with a different keyset (e.g. leftover data from a
     * previous install whose keyset was wiped). Callers use `null` to trigger a
     * defensive session wipe rather than crash.
     *
     * Only catastrophic init failures are allowed to throw [SessionCryptoException].
     */
    fun decrypt(ciphertext: String): String?
}

/** Thrown when the session cipher cannot initialize its key material. */
class SessionCryptoException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
