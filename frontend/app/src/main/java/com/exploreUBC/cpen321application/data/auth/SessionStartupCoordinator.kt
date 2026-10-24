package com.exploreUBC.cpen321application.data.auth

import com.exploreUBC.cpen321application.data.network.ApiException
import com.exploreUBC.cpen321application.data.AppConstants
import kotlinx.coroutines.CancellationException

sealed interface SessionStartupResult {
    data object SignedOut : SessionStartupResult

    data class Authenticated(
        val userId: String,
        val displayName: String,
        val onboardingRequired: Boolean,
    ) : SessionStartupResult

    data object RetryableFailure : SessionStartupResult
}

class SessionStartupCoordinator(
    private val sessionManager: SessionManager,
    private val loadSession: suspend () -> AuthenticatedSession?,
    private val clearPersistedSession: suspend () -> Unit,
    private val restoreSession: suspend () -> SessionRestorationResult,
    private val renewSession: suspend () -> GoogleAuthenticationResult?,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
    private val renewalWindowMillis: Long = AppConstants.Session.RENEWAL_WINDOW_MILLIS,
) {
    suspend fun restore(): SessionStartupResult {
        val savedSession = loadSession() ?: return SessionStartupResult.SignedOut
        sessionManager.replace(savedSession)

        return try {
            if (savedSession.expiresWithin(renewalWindowMillis, nowEpochMillis())) {
                val renewed = renewSession() ?: return clearAndSignOut()
                SessionStartupResult.Authenticated(
                    userId = renewed.userId,
                    displayName = renewed.displayName,
                    onboardingRequired = renewed.onboardingRequired,
                )
            } else {
                val restored = restoreSession()
                SessionStartupResult.Authenticated(
                    userId = restored.userId,
                    displayName = restored.displayName,
                    onboardingRequired = restored.onboardingRequired,
                )
            }
        } catch (error: ApiException) {
            if (error.statusCode == 401) clearAndSignOut()
            else SessionStartupResult.RetryableFailure
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            SessionStartupResult.RetryableFailure
        }
    }

    private suspend fun clearAndSignOut(): SessionStartupResult {
        clearPersistedSession()
        sessionManager.clear()
        return SessionStartupResult.SignedOut
    }
}
