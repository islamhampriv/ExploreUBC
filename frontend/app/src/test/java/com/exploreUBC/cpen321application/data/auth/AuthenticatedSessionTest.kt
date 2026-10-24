package com.exploreUBC.cpen321application.data.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthenticatedSessionTest {
    private val session = AuthenticatedSession(
        token = "token",
        userId = "user-id",
        expiresAtEpochMillis = 10_000L,
    )

    @Test
    fun expiryBoundaryIsInclusive() {
        assertFalse(session.isExpired(9_999L))
        assertTrue(session.isExpired(10_000L))
    }

    @Test
    fun detectsRenewalWindow() {
        assertFalse(session.expiresWithin(windowMillis = 1_000L, nowEpochMillis = 8_999L))
        assertTrue(session.expiresWithin(windowMillis = 1_000L, nowEpochMillis = 9_000L))
    }
}
