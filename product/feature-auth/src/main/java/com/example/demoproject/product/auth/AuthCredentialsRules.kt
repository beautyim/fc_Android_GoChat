package com.example.demoproject.product.auth

import androidx.annotation.StringRes

/**
 * Client-side credential limits for the email login form.
 *
 * Email length follows RFC 5321 (max 254 total; local-part max 64).
 * Password length is product policy: 6–20 characters.
 * Neither field may contain whitespace.
 */
internal object AuthCredentialsRules {
    const val PasswordMinLength = 6
    const val PasswordMaxLength = 20
    const val EmailMaxLength = 254
    const val EmailLocalMaxLength = 64
    const val VerificationCodeMaxLength = 8

    fun sanitizeEmail(raw: String): String = raw.take(EmailMaxLength)

    fun sanitizePassword(raw: String): String = raw.take(PasswordMaxLength)

    fun sanitizeVerificationCode(raw: String): String =
        raw.filter { !it.isWhitespace() }.take(VerificationCodeMaxLength)

    @StringRes
    fun emailErrorRes(email: String): Int? {
        if (email.isEmpty()) return null
        if (containsWhitespace(email)) {
            return R.string.auth_error_email_invalid
        }
        if (email.length > EmailMaxLength) {
            return R.string.auth_error_email_too_long
        }
        val atIndex = email.indexOf('@')
        val localPart = if (atIndex >= 0) email.substring(0, atIndex) else email
        if (localPart.length > EmailLocalMaxLength) {
            return R.string.auth_error_email_local_too_long
        }
        return null
    }

    @StringRes
    fun passwordErrorRes(password: String): Int? {
        if (password.isEmpty()) return null
        if (containsWhitespace(password)) {
            return R.string.auth_error_password_invalid
        }
        if (password.length < PasswordMinLength || password.length > PasswordMaxLength) {
            return R.string.auth_error_password_length
        }
        return null
    }

    /** Forget-password field uses Figma short-password copy. */
    @StringRes
    fun resetPasswordErrorRes(password: String): Int? {
        if (password.isEmpty()) return null
        if (containsWhitespace(password)) {
            return R.string.auth_error_password_invalid
        }
        if (password.length < PasswordMinLength) {
            return R.string.auth_error_password_min_length
        }
        if (password.length > PasswordMaxLength) {
            return R.string.auth_error_password_length
        }
        return null
    }

    @StringRes
    fun confirmPasswordErrorRes(password: String, confirmPassword: String): Int? {
        if (confirmPassword.isEmpty()) return null
        if (password != confirmPassword) {
            return R.string.auth_error_password_mismatch
        }
        return null
    }

    fun canSubmit(email: String, password: String): Boolean =
        email.isNotEmpty() &&
            password.isNotEmpty() &&
            emailErrorRes(email) == null &&
            passwordErrorRes(password) == null

    fun canSubmitReset(
        email: String,
        code: String,
        password: String,
        confirmPassword: String,
    ): Boolean =
        email.isNotEmpty() &&
            code.isNotEmpty() &&
            password.isNotEmpty() &&
            confirmPassword.isNotEmpty() &&
            emailErrorRes(email) == null &&
            resetPasswordErrorRes(password) == null &&
            confirmPasswordErrorRes(password, confirmPassword) == null

    private fun containsWhitespace(value: String): Boolean = value.any { it.isWhitespace() }
}
