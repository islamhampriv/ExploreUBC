package com.exploreUBC.cpen321application.data.auth

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.exploreUBC.cpen321application.data.network.ApiException
import com.exploreUBC.cpen321application.data.network.ApiRequest
import com.exploreUBC.cpen321application.data.network.BackendClient
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthRepositoryTest {
    private val backendSession = AuthenticatedSession(
        token = "exploreubc-session-token",
        userId = "backend-user-id",
        expiresAtEpochMillis = 123_456L,
    )

    @Test
    fun parsesBackendAuthenticationContractAndStoresOnlyExploreUbcSession() = runBlocking {
        lateinit var request: ApiRequest
        var persisted: AuthenticatedSession? = null
        val sessionManager = SessionManager()
        val repository = AuthRepository(
            backendClient = BackendClient {
                request = it
                """
                    {
                      "sessionToken": "exploreubc-session-token",
                      "expiresAt": "2026-10-25T08:00:00.000Z",
                      "onboardingRequired": true,
                      "user": {
                        "id": "backend-user-id",
                        "displayName": "Backend Name"
                      }
                    }
                """.trimIndent()
            },
            sessionManager = sessionManager,
            persistSession = { persisted = it },
        )

        val result = repository.authenticateWithGoogle("google-id-token")

        assertEquals("google-id-token", JSONObject(request.body!!).getString("idToken"))
        assertEquals("backend-user-id", result.userId)
        assertEquals("Backend Name", result.displayName)
        assertEquals("exploreubc-session-token", persisted?.token)
        assertEquals("backend-user-id", persisted?.userId)
        assertFalse(persisted?.token.orEmpty().contains("google-id-token"))
        assertSame(persisted, sessionManager.currentSession())
    }

    @Test
    fun backendFailureDoesNotPersistOrAuthenticate() {
        var persistedSession: AuthenticatedSession? = null
        val sessionManager = SessionManager()
        val repository = AuthRepository(
            backendClient = BackendClient {
                throw ApiException.fromHttpStatus(401, "INVALID_GOOGLE_TOKEN")
            },
            sessionManager = sessionManager,
            persistSession = { persistedSession = it },
        )

        assertThrows(ApiException::class.java) {
            runBlocking { repository.authenticateWithGoogle("invalid-google-token") }
        }

        assertNull(persistedSession)
        assertNull(sessionManager.currentSession())
    }

    @Test
    fun restoreSessionValidatesAndRefreshesStoredExpiry() = runBlocking {
        lateinit var request: ApiRequest
        var persistedSession: AuthenticatedSession? = null
        val sessionManager = SessionManager(backendSession)
        val repository = AuthRepository(
            backendClient = BackendClient { received ->
                request = received
                """
                    {
                      "expiresAt": "1970-01-01T00:16:39.999Z",
                      "onboardingRequired": false,
                      "user": {
                        "id": "backend-user-id",
                        "displayName": "Backend Name"
                      }
                    }
                """.trimIndent()
            },
            sessionManager = sessionManager,
            persistSession = { persistedSession = it },
        )

        val result = repository.restoreSession()

        assertEquals("GET", request.method)
        assertEquals("/auth/session", request.path)
        assertTrue(request.requiresAuthentication)
        assertEquals(999_999L, persistedSession?.expiresAtEpochMillis)
        assertSame(persistedSession, sessionManager.currentSession())
        assertEquals("Backend Name", result.displayName)
        assertFalse(result.onboardingRequired)
    }

    @Test
    fun logoutCallsTheBackendSessionDeletionEndpoint() = runBlocking {
        lateinit var request: ApiRequest
        val repository = AuthRepository(
            backendClient = BackendClient {
                request = it
                ""
            },
            sessionManager = SessionManager(backendSession),
            persistSession = {},
        )

        repository.logout()

        assertEquals("DELETE", request.method)
        assertEquals("/auth/session", request.path)
        assertTrue(request.requiresAuthentication)
    }
}
