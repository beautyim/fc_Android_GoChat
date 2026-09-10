package com.example.demoproject.product.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthCredentialsRulesTest {

    @Test
    fun password_rejectsShorterThanMin() {
        assertEquals(
            R.string.auth_error_password_length,
            AuthCredentialsRules.passwordErrorRes("12345"),
        )
    }

    @Test
    fun password_acceptsMinToMax() {
        assertNull(AuthCredentialsRules.passwordErrorRes("123456"))
        assertNull(AuthCredentialsRules.passwordErrorRes("a".repeat(20)))
    }

    @Test
    fun password_isSanitizedToMax() {
        val sanitized = AuthCredentialsRules.sanitizePassword("a".repeat(25))
        assertEquals(20, sanitized.length)
        assertNull(AuthCredentialsRules.passwordErrorRes(sanitized))
    }

    @Test
    fun email_rejectsLocalLongerThan64() {
        val local = "a".repeat(65)
        assertEquals(
            R.string.auth_error_email_local_too_long,
            AuthCredentialsRules.emailErrorRes("$local@example.com"),
        )
        assertEquals(
            R.string.auth_error_email_local_too_long,
            AuthCredentialsRules.emailErrorRes(local),
        )
    }

    @Test
    fun email_acceptsLocalAt64() {
        val local = "a".repeat(64)
        assertNull(AuthCredentialsRules.emailErrorRes("$local@example.com"))
    }

    @Test
    fun email_isSanitizedTo254() {
        val sanitized = AuthCredentialsRules.sanitizeEmail("a".repeat(300))
        assertEquals(254, sanitized.length)
    }

    @Test
    fun email_rejectsWhitespace() {
        assertEquals(
            R.string.auth_error_email_invalid,
            AuthCredentialsRules.emailErrorRes("a @b.com"),
        )
        assertEquals(
            R.string.auth_error_email_invalid,
            AuthCredentialsRules.emailErrorRes(" a@b.com"),
        )
    }

    @Test
    fun password_rejectsWhitespace() {
        assertEquals(
            R.string.auth_error_password_invalid,
            AuthCredentialsRules.passwordErrorRes("12 3456"),
        )
    }

    @Test
    fun canSubmit_requiresValidEmailAndPassword() {
        assertFalse(AuthCredentialsRules.canSubmit("", "123456"))
        assertFalse(AuthCredentialsRules.canSubmit("a@b.com", "123"))
        assertFalse(AuthCredentialsRules.canSubmit("a @b.com", "123456"))
        assertFalse(AuthCredentialsRules.canSubmit("a@b.com", "123 456"))
        assertTrue(AuthCredentialsRules.canSubmit("a@b.com", "123456"))
    }
}
