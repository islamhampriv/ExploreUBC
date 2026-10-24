package com.exploreUBC.cpen321application.data.auth

import com.exploreUBC.cpen321application.data.network.ApiException
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionStartupCoordinatorTest {
    private val now = 1_000_000L
    private val validSession = AuthenticatedSession(
        token = "stored-token",
        userId = "user-id",
        expiresAtEpochMillis = now + 3_600_000L,
    )

    @Test
    fun missingStoredSessionShowsLogin() = runBlocking {
        val manager = SessionManager()
        val fixture = fixture(manager = manager, savedSession = null)

        assertSame(SessionStartupResult.SignedOut, fixture.coordinator.restore())
        assertNull(manager.currentSession())
        assertEquals(0, fixture.restoreCount)
        assertEquals(0, fixture.renewCount)
    }

    @Test
    fun validStoredSessionIsValidatedWithBackend() = runBlocking {
        val manager = SessionManager()
        val fixture = fixture(manager = manager, savedSession = validSession)

        val result = fixture.coordinator.restore()

        assertTrue(result is SessionStartupResult.Authenticated)
        assertSame(validSession, manager.currentSession())
        assertEquals(1, fixture.restoreCount)
        assertEquals(0, fixture.renewCount)
    }

    @Test
    fun nearExpirySessionRenewsInsteadOfRestoring() = runBlocking {
        val manager = SessionManager()
        val nearExpiry = validSession.copy(expiresAtEpochMillis = now + 60_000L)
        val fixture = fixture(manager = manager, savedSession = nearExpiry)

        val result = fixture.coordinator.restore()

        assertTrue(result is SessionStartupResult.Authenticated)
        assertEquals(0, fixture.restoreCount)
        assertEquals(1, fixture.renewCount)
    }

    @Test
    fun unavailableAuthorizedCredentialClearsSession() = runBlocking {
        val manager = SessionManager()
        val nearExpiry = validSession.copy(expiresAtEpochMillis = now)
        val fixture = fixture(
            manager = manager,
            savedSession = nearExpiry,
            renewalResult = null,
        )

        assertSame(SessionStartupResult.SignedOut, fixture.coordinator.restore())
        assertNull(manager.currentSession())
        assertEquals(1, fixture.clearCount)
    }

    @Test
    fun revokedStoredSessionClearsSession() = runBlocking {
        val manager = SessionManager()
        val fixture = fixture(
            manager = manager,
            savedSession = validSession,
            restorationFailure = ApiException.fromHttpStatus(401, "INVALID_SESSION"),
        )

        assertSame(SessionStartupResult.SignedOut, fixture.coordinator.restore())
        assertNull(manager.currentSession())
        assertEquals(1, fixture.clearCount)
    }

    @Test
    fun temporaryFailurePreservesSessionForRetry() = runBlocking {
        val manager = SessionManager()
        val fixture = fixture(
            manager = manager,
            savedSession = validSession,
            restorationFailure = IOException("offline"),
        )

        assertSame(SessionStartupResult.RetryableFailure, fixture.coordinator.restore())
        assertSame(validSession, manager.currentSession())
        assertEquals(0, fixture.clearCount)
    }

    private fun fixture(
        manager: SessionManager,
        savedSession: AuthenticatedSession?,
        renewalResult: GoogleAuthenticationResult? = GoogleAuthenticationResult(
            userId = "user-id",
            displayName = "Example User",
            onboardingRequired = false,
        ),
        restorationFailure: Exception? = null,
    ): StartupFixture = StartupFixture(
        manager = manager,
        savedSession = savedSession,
        renewalResult = renewalResult,
        restorationFailure = restorationFailure,
        now = now,
    )
}

private class StartupFixture(
    manager: SessionManager,
    savedSession: AuthenticatedSession?,
    renewalResult: GoogleAuthenticationResult?,
    restorationFailure: Exception?,
    now: Long,
) {
    var clearCount = 0
    var restoreCount = 0
    var renewCount = 0

    val coordinator = SessionStartupCoordinator(
        sessionManager = manager,
        loadSession = { savedSession },
        clearPersistedSession = { clearCount += 1 },
        restoreSession = {
            restoreCount += 1
            restorationFailure?.let { throw it }
            SessionRestorationResult(
                userId = "user-id",
                displayName = "Example User",
                onboardingRequired = false,
            )
        },
        renewSession = {
            renewCount += 1
            renewalResult
        },
        nowEpochMillis = { now },
    )
}
