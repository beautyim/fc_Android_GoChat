package com.example.demoproject.product.me

import androidx.annotation.StringRes

/**
 * Client-side limits for bind/change-email forms (aligned with auth policy).
 */
internal object EmailCredentialsRules {
    const val PasswordMinLength = 6
    const val PasswordMaxLength = 20
    const val EmailMaxLength = 254
    const val EmailLocalMaxLength = 64
    const val VerificationCodeLength = 4

    fun sanitizeEmail(raw: String): String = raw.take(EmailMaxLength)

    fun sanitizePassword(raw: String): String = raw.take(PasswordMaxLength)

    fun sanitizeVerificationCode(raw: String): String =
        raw.filter { it.isDigit() }.take(VerificationCodeLength)

    @StringRes
    fun emailErrorRes(email: String): Int? {
        if (email.isEmpty()) return null
        if (containsWhitespace(email)) return R.string.email_error_invalid
        if (email.length > EmailMaxLength) return R.string.email_error_too_long
        val atIndex = email.indexOf('@')
        val localPart = if (atIndex >= 0) email.substring(0, atIndex) else email
        if (localPart.length > EmailLocalMaxLength) return R.string.email_error_local_too_long
        if (atIndex <= 0 || atIndex == email.lastIndex || email.count { it == '@' } != 1) {
            return R.string.email_error_invalid
        }
        return null
    }

    @StringRes
    fun passwordErrorRes(password: String): Int? {
        if (password.isEmpty()) return null
        if (containsWhitespace(password)) return R.string.email_error_password_invalid
        if (password.length < PasswordMinLength || password.length > PasswordMaxLength) {
            return R.string.email_error_password_length
        }
        return null
    }

    fun parseEmailCode(code: String): Int? =
        sanitizeVerificationCode(code).takeIf { it.length == VerificationCodeLength }?.toIntOrNull()

    fun canSubmitBind(email: String, code: String, password: String): Boolean =
        email.isNotEmpty() &&
            code.length == VerificationCodeLength &&
            password.isNotEmpty() &&
            emailErrorRes(email) == null &&
            passwordErrorRes(password) == null &&
            parseEmailCode(code) != null

    fun canSubmitChange(newEmail: String, code: String): Boolean =
        newEmail.isNotEmpty() &&
            code.length == VerificationCodeLength &&
            emailErrorRes(newEmail) == null &&
            parseEmailCode(code) != null

    private fun containsWhitespace(value: String): Boolean = value.any { it.isWhitespace() }
}
