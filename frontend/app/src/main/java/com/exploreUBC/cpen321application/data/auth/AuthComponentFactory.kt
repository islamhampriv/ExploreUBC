package com.exploreUBC.cpen321application.data.auth

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.exploreUBC.cpen321application.data.network.ApiClient
import com.exploreUBC.cpen321application.data.network.ApiException
import com.exploreUBC.cpen321application.data.network.SingleFlightSessionRenewer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoginAuthComponents internal constructor(
    private val authRepository: AuthRepository,
    private val credentialProvider: GoogleCredentialProvider,
    private val startupCoordinator: SessionStartupCoordinator,
) {
    suspend fun restoreSession(): SessionStartupResult = startupCoordinator.restore()

    suspend fun authenticateInteractively() {
        authRepository.authenticateWithGoogle(
            credentialProvider.requestInteractiveIdToken(),
        )
    }
}

class AuthenticatedAuthComponents internal constructor(
    val userId: String,
    val apiClient: ApiClient,
    private val logoutAction: suspend () -> Unit,
) {
    suspend fun logout() = logoutAction()
}

class AuthComponentFactory(
    context: Context,
    private val apiBaseUrl: String,
    private val googleClientId: String,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val applicationContext = context.applicationContext

    fun createLoginComponents(activity: ComponentActivity): LoginAuthComponents {
        val core = createCore(activity, SessionStore(applicationContext), null)
        val startupCoordinator = SessionStartupCoordinator(
            sessionManager = core.sessionManager,
            loadSession = { loadSession(core.sessionStore) },
            clearPersistedSession = { clearStoredSession(core.sessionStore) },
            restoreSession = core.authRepository::restoreSession,
            renewSession = {
                try {
                    core.authRepository.authenticateWithGoogle(
                        core.credentialProvider.requestAuthorizedIdToken(),
                    )
                } catch (_: NoCredentialException) {
                    null
                }
            },
        )
        return LoginAuthComponents(
            authRepository = core.authRepository,
            credentialProvider = core.credentialProvider,
            startupCoordinator = startupCoordinator,
        )
    }

    suspend fun createAuthenticatedComponents(
        activity: ComponentActivity,
        onSessionInvalidated: suspend () -> Unit,
    ): AuthenticatedAuthComponents? {
        val sessionStore = SessionStore(applicationContext)
        val storedSession = loadSession(sessionStore) ?: return null
        val core = createCore(activity, sessionStore, storedSession)
        val renewer = createSessionRenewer(core, onSessionInvalidated)
        val apiClient = ApiClient(
            apiBaseUrl = apiBaseUrl,
            sessionManager = core.sessionManager,
            renewalHandler = renewer,
        )

        return AuthenticatedAuthComponents(
            userId = storedSession.userId,
            apiClient = apiClient,
            logoutAction = {
                try {
                    core.authRepository.logout()
                } finally {
                    clearSession(core)
                }
            },
        )
    }

    private fun createCore(
        activity: ComponentActivity,
        sessionStore: SessionStore,
        initialSession: AuthenticatedSession?,
    ): CoreAuthComponents {
        val sessionManager = SessionManager(initialSession)
        val authClient = ApiClient(
            apiBaseUrl = apiBaseUrl,
            sessionManager = sessionManager,
        )
        val authRepository = AuthRepository(
            backendClient = authClient,
            sessionManager = sessionManager,
            persistSession = { session -> saveSession(sessionStore, session) },
        )
        return CoreAuthComponents(
            sessionStore = sessionStore,
            sessionManager = sessionManager,
            authRepository = authRepository,
            credentialProvider = GoogleCredentialProvider(
                activity = activity,
                serverClientId = googleClientId,
            ),
        )
    }

    private fun createSessionRenewer(
        core: CoreAuthComponents,
        onSessionInvalidated: suspend () -> Unit,
    ) = SingleFlightSessionRenewer(
        sessionManager = core.sessionManager,
        persistSession = { session -> saveSession(core.sessionStore, session) },
        requestReplacement = { failedSession ->
            try {
                val replacement = core.authRepository.requestReplacementSession(
                    core.credentialProvider.requestAuthorizedIdToken(),
                )
                if (replacement.userId == failedSession.userId) {
                    replacement
                } else {
                    invalidateSession(core, onSessionInvalidated)
                    null
                }
            } catch (_: NoCredentialException) {
                invalidateSession(core, onSessionInvalidated)
                null
            } catch (_: GetCredentialException) {
                invalidateSession(core, onSessionInvalidated)
                null
            } catch (error: ApiException) {
                if (error.statusCode != 401) throw error
                invalidateSession(core, onSessionInvalidated)
                null
            }
        },
    )

    private suspend fun invalidateSession(
        core: CoreAuthComponents,
        onSessionInvalidated: suspend () -> Unit,
    ) {
        clearSession(core)
        onSessionInvalidated()
    }

    private suspend fun clearSession(core: CoreAuthComponents) {
        clearStoredSession(core.sessionStore)
        core.sessionManager.clear()
    }

    private suspend fun loadSession(sessionStore: SessionStore): AuthenticatedSession? =
        withContext(ioDispatcher) { sessionStore.load() }

    private suspend fun saveSession(
        sessionStore: SessionStore,
        session: AuthenticatedSession,
    ) {
        withContext(ioDispatcher) { sessionStore.save(session) }
    }

    private suspend fun clearStoredSession(sessionStore: SessionStore) {
        withContext(ioDispatcher) { sessionStore.clear() }
    }

    private class CoreAuthComponents(
        val sessionStore: SessionStore,
        val sessionManager: SessionManager,
        val authRepository: AuthRepository,
        val credentialProvider: GoogleCredentialProvider,
    )
}
