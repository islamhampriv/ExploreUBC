package com.exploreUBC.cpen321application.data.network

import com.exploreUBC.cpen321application.data.auth.AuthenticatedSession
import com.exploreUBC.cpen321application.data.auth.SessionManager
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.ArrayDeque
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ApiClientTest {
    private val validSession = AuthenticatedSession(
        token = "old-token",
        userId = "user-id",
        expiresAtEpochMillis = 20_000L,
    )

    @Test
    fun protectedRequestAddsBearerHeader() = runBlocking {
        val connection = FakeHttpURLConnection(200, "ok")
        val client = client(
            sessionManager = SessionManager(validSession),
            connections = listOf(connection),
        )

        assertEquals("ok", client.execute(ApiRequest("GET", "/protected")))
        assertEquals("Bearer old-token", connection.getRequestProperty("Authorization"))
    }

    @Test
    fun apiClientAddsVersionPrefixToResourcePath() = runBlocking {
        val connection = FakeHttpURLConnection(200, "ok")
        lateinit var openedUrl: URL
        val client = ApiClient(
            apiBaseUrl = "https://example.test/",
            sessionManager = SessionManager(validSession),
            ioDispatcher = Dispatchers.Unconfined,
            openConnection = { url ->
                openedUrl = url
                connection
            },
        )

        client.execute(ApiRequest("GET", "/users/user-id"))

        assertEquals("https://example.test/api/v1/users/user-id", openedUrl.toString())
    }

    @Test
    fun publicRequestDoesNotRequireOrSendSession() = runBlocking {
        val connection = FakeHttpURLConnection(200, "ok")
        val client = client(
            sessionManager = SessionManager(),
            connections = listOf(connection),
        )

        client.execute(
            ApiRequest(
                method = "POST",
                path = "/auth/google",
                body = "{}",
                requiresAuthentication = false,
            ),
        )

        assertNull(connection.getRequestProperty("Authorization"))
    }

    @Test
    fun expiredSessionRenewsAndReplaysOnce() = runBlocking {
        val first = FakeHttpURLConnection(401, "expired")
        val retry = FakeHttpURLConnection(200, "renewed")
        val sessionManager = SessionManager(validSession)
        val replacement = validSession.copy(token = "replacement-token")
        var renewalCount = 0
        val client = client(
            sessionManager = sessionManager,
            connections = listOf(first, retry),
            renewalHandler = SessionRenewalHandler {
                renewalCount += 1
                sessionManager.replace(replacement)
                replacement
            },
            error = ApiErrorPayload("SESSION_EXPIRED"),
        )

        assertEquals("renewed", client.execute(ApiRequest("GET", "/protected")))
        assertEquals(1, renewalCount)
        assertEquals("Bearer old-token", first.getRequestProperty("Authorization"))
        assertEquals("Bearer replacement-token", retry.getRequestProperty("Authorization"))
    }

    @Test
    fun failedReplayDoesNotStartAnotherRenewal() {
        val sessionManager = SessionManager(validSession)
        val replacement = validSession.copy(token = "replacement-token")
        var renewalCount = 0
        val client = client(
            sessionManager = sessionManager,
            connections = listOf(
                FakeHttpURLConnection(401, "expired"),
                FakeHttpURLConnection(401, "expired-again"),
            ),
            renewalHandler = SessionRenewalHandler {
                renewalCount += 1
                sessionManager.replace(replacement)
                replacement
            },
            error = ApiErrorPayload("SESSION_EXPIRED"),
        )

        val error = assertThrows(ApiException::class.java) {
            runBlocking { client.execute(ApiRequest("GET", "/protected")) }
        }
        assertEquals(401, error.statusCode)
        assertEquals("SESSION_EXPIRED", error.errorCode)
        assertEquals("Your session is no longer valid. Please sign in again.", error.message)
        assertEquals(1, renewalCount)
    }

    @Test
    fun revokedNonExpiredSessionDoesNotRenew() {
        var renewalCount = 0
        val client = client(
            sessionManager = SessionManager(validSession),
            connections = listOf(FakeHttpURLConnection(401, "revoked")),
            renewalHandler = SessionRenewalHandler {
                renewalCount += 1
                null
            },
            error = ApiErrorPayload("INVALID_SESSION"),
            nowEpochMillis = 10_000L,
        )

        val error = assertThrows(ApiException::class.java) {
            runBlocking { client.execute(ApiRequest("GET", "/protected")) }
        }
        assertEquals(401, error.statusCode)
        assertEquals("INVALID_SESSION", error.errorCode)
        assertEquals(0, renewalCount)
    }

    @Test
    fun forbiddenResponseUsesFrontendSafeMessage() {
        val client = client(
            sessionManager = SessionManager(validSession),
            connections = listOf(FakeHttpURLConnection(403, "sensitive backend details")),
            error = ApiErrorPayload("USER_SCOPE_VIOLATION"),
        )

        val error = assertThrows(ApiException::class.java) {
            runBlocking { client.execute(ApiRequest("GET", "/protected")) }
        }

        assertEquals(403, error.statusCode)
        assertEquals("USER_SCOPE_VIOLATION", error.errorCode)
        assertEquals("You do not have permission to perform this action.", error.message)
    }

    private fun client(
        sessionManager: SessionManager,
        connections: List<FakeHttpURLConnection>,
        renewalHandler: SessionRenewalHandler? = null,
        error: ApiErrorPayload = ApiErrorPayload(null),
        nowEpochMillis: Long = 10_000L,
    ): ApiClient {
        val remaining = ArrayDeque(connections)
        return ApiClient(
            apiBaseUrl = "https://example.test",
            sessionManager = sessionManager,
            renewalHandler = renewalHandler,
            nowEpochMillis = { nowEpochMillis },
            ioDispatcher = Dispatchers.Unconfined,
            openConnection = { remaining.removeFirst() },
            parseError = { error },
        )
    }
}

private class FakeHttpURLConnection(
    private val status: Int,
    private val responseBody: String,
) : HttpURLConnection(URL("https://example.test")) {
    private val requestBody = ByteArrayOutputStream()

    override fun getResponseCode(): Int = status

    override fun getInputStream(): InputStream =
        ByteArrayInputStream(responseBody.toByteArray())

    override fun getErrorStream(): InputStream =
        ByteArrayInputStream(responseBody.toByteArray())

    override fun getOutputStream(): OutputStream = requestBody

    override fun connect() = Unit

    override fun disconnect() = Unit

    override fun usingProxy(): Boolean = false
}
