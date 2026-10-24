package com.exploreUBC.cpen321application.data.network

import com.exploreUBC.cpen321application.data.auth.AuthenticatedSession
import com.exploreUBC.cpen321application.data.auth.SessionManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun interface SessionRenewalHandler {
    suspend fun renew(failedSession: AuthenticatedSession): AuthenticatedSession?
}

class SingleFlightSessionRenewer(
    private val sessionManager: SessionManager,
    private val persistSession: suspend (AuthenticatedSession) -> Unit,
    private val requestReplacement: suspend (AuthenticatedSession) -> AuthenticatedSession?,
) : SessionRenewalHandler {
    private val stateMutex = Mutex()
    private var inFlight: CompletableDeferred<AuthenticatedSession?>? = null

    override suspend fun renew(failedSession: AuthenticatedSession): AuthenticatedSession? {
        sessionManager.currentSession()?.let { current ->
            if (current.token != failedSession.token) return current
        }

        var leadsRenewal = false
        val sharedResult: CompletableDeferred<AuthenticatedSession?> = stateMutex.withLock {
            sessionManager.currentSession()?.let { current ->
                if (current.token != failedSession.token) {
                    return@withLock CompletableDeferred<AuthenticatedSession?>().also {
                        it.complete(current)
                    }
                }
            }
            inFlight ?: CompletableDeferred<AuthenticatedSession?>().also {
                inFlight = it
                leadsRenewal = true
            }
        }

        if (leadsRenewal) {
            try {
                val replacement = requestReplacement(failedSession)
                if (replacement != null) {
                    persistSession(replacement)
                    sessionManager.replace(replacement)
                }
                sharedResult.complete(replacement)
            } catch (error: Throwable) {
                sharedResult.completeExceptionally(error)
            } finally {
                stateMutex.withLock {
                    if (inFlight === sharedResult) inFlight = null
                }
            }
        }

        return sharedResult.await()
    }
}
