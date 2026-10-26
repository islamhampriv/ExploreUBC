package com.exploreUBC.cpen321application.data.auth

import com.exploreUBC.cpen321application.data.network.ApiRequest
import com.exploreUBC.cpen321application.data.network.BackendClient
import com.exploreUBC.cpen321application.data.AppConstants
import java.time.Instant
import org.json.JSONObject

data class GoogleAuthenticationResult(
    val userId: String,
    val displayName: String,
    val onboardingRequired: Boolean,
)

data class SessionRestorationResult(
    val userId: String,
    val displayName: String,
    val onboardingRequired: Boolean,
)

private data class GoogleAuthenticationResponse(
    val session: AuthenticatedSession,
    val displayName: String,
    val onboardingRequired: Boolean,
)

class AuthRepository(
    private val backendClient: BackendClient,
    private val sessionManager: SessionManager,
    private val persistSession: suspend (AuthenticatedSession) -> Unit,
) {
    suspend fun authenticateWithGoogle(idToken: String): GoogleAuthenticationResult {
        val response = exchangeGoogleIdToken(idToken)

        // Persist first so a storage failure cannot leave only a transient in-memory login.
        persistSession(response.session)
        sessionManager.replace(response.session)

        return GoogleAuthenticationResult(
            userId = response.session.userId,
            displayName = response.displayName,
            onboardingRequired = response.onboardingRequired,
        )
    }

    suspend fun requestReplacementSession(idToken: String): AuthenticatedSession =
        exchangeGoogleIdToken(idToken).session

    suspend fun logout() {
        backendClient.execute(
            ApiRequest(
                method = "DELETE",
                path = AppConstants.Auth.CURRENT_SESSION_PATH,
            ),
        )
    }

    suspend fun restoreSession(): SessionRestorationResult {
        val currentSession = checkNotNull(sessionManager.currentSession()) {
            "A session must be loaded before it can be restored."
        }
        val responseBody = backendClient.execute(
            ApiRequest(
                method = "GET",
                path = AppConstants.Auth.CURRENT_SESSION_PATH,
            ),
        )
        val response = parseSessionRestorationResponse(responseBody)
        check(response.userId == currentSession.userId) {
            "The restored user does not match the stored session."
        }
        val refreshedSession = currentSession.copy(
            expiresAtEpochMillis = response.expiresAtEpochMillis,
        )
        persistSession(refreshedSession)
        sessionManager.replace(refreshedSession)
        return SessionRestorationResult(
            userId = response.userId,
            displayName = response.displayName,
            onboardingRequired = response.onboardingRequired,
        )
    }

    private suspend fun exchangeGoogleIdToken(idToken: String): GoogleAuthenticationResponse {
        require(idToken.isNotBlank()) { "Google ID token must not be blank." }
        val responseBody = backendClient.execute(
            ApiRequest(
                method = "POST",
                path = AppConstants.Auth.GOOGLE_SIGN_IN_PATH,
                body = "{\"idToken\":${jsonString(idToken)}}",
                requiresAuthentication = false,
            ),
        )
        return parseGoogleAuthenticationResponse(responseBody)
    }
}

private data class SessionRestorationResponse(
    val userId: String,
    val displayName: String,
    val onboardingRequired: Boolean,
    val expiresAtEpochMillis: Long,
)

private fun jsonString(value: String): String = buildString {
    append('"')
    value.forEach { character ->
        when (character) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (character.code < 0x20) {
                append("\\u%04x".format(character.code))
            } else {
                append(character)
            }
        }
    }
    append('"')
}

private fun parseGoogleAuthenticationResponse(responseBody: String): GoogleAuthenticationResponse {
    val response = JSONObject(responseBody)
    val user = response.getJSONObject("user")
    val session = AuthenticatedSession(
        token = response.getString("sessionToken"),
        userId = user.getString("id"),
        expiresAtEpochMillis = Instant.parse(response.getString("expiresAt")).toEpochMilli(),
    )

    return GoogleAuthenticationResponse(
        session = session,
        displayName = user.getString("displayName"),
        onboardingRequired = response.getBoolean("onboardingRequired"),
    )
}

private fun parseSessionRestorationResponse(responseBody: String): SessionRestorationResponse {
    val response = JSONObject(responseBody)
    val user = response.getJSONObject("user")
    return SessionRestorationResponse(
        userId = user.getString("id"),
        displayName = user.getString("displayName"),
        onboardingRequired = response.getBoolean("onboardingRequired"),
        expiresAtEpochMillis = Instant.parse(response.getString("expiresAt")).toEpochMilli(),
    )
}
