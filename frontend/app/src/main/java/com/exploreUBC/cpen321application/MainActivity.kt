package com.exploreUBC.cpen321application

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import com.exploreUBC.cpen321application.data.auth.AuthRepository
import com.exploreUBC.cpen321application.data.auth.GoogleAuthenticationResult
import com.exploreUBC.cpen321application.data.auth.GoogleCredentialProvider
import com.exploreUBC.cpen321application.data.auth.SessionManager
import com.exploreUBC.cpen321application.data.auth.SessionStartupCoordinator
import com.exploreUBC.cpen321application.data.auth.SessionStartupResult
import com.exploreUBC.cpen321application.data.auth.SessionStore
import com.exploreUBC.cpen321application.data.network.ApiClient
import com.exploreUBC.cpen321application.data.network.ApiException
import com.exploreUBC.cpen321application.ui.theme.CPEN321ApplicationTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "MainActivity"
const val EXTRA_USER_ID = "userId"
const val EXTRA_DISPLAY_NAME = "displayName"
const val EXTRA_ONBOARDING_REQUIRED = "onboardingRequired"

private sealed interface AuthenticationUiState {
    data object Loading : AuthenticationUiState

    data class Login(
        val message: String? = null,
        val canRetryRestoration: Boolean = false,
    ) : AuthenticationUiState
}

class MainActivity : ComponentActivity() {
    private lateinit var authRepository: AuthRepository
    private lateinit var credentialProvider: GoogleCredentialProvider
    private lateinit var startupCoordinator: SessionStartupCoordinator
    private var uiState by mutableStateOf<AuthenticationUiState>(AuthenticationUiState.Loading)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sessionManager = SessionManager()
        val sessionStore = SessionStore(applicationContext)
        val authTransport = ApiClient(
            apiBaseUrl = BuildConfig.API_BASE_URL,
            sessionManager = sessionManager,
        )
        authRepository = AuthRepository(
            backendClient = authTransport,
            sessionManager = sessionManager,
            persistSession = { session ->
                withContext(Dispatchers.IO) { sessionStore.save(session) }
            },
        )
        credentialProvider = GoogleCredentialProvider(
            activity = this,
            serverClientId = BuildConfig.GOOGLE_CLIENT_ID,
        )
        startupCoordinator = SessionStartupCoordinator(
            sessionManager = sessionManager,
            loadSession = { withContext(Dispatchers.IO) { sessionStore.load() } },
            clearPersistedSession = {
                withContext(Dispatchers.IO) { sessionStore.clear() }
            },
            restoreSession = authRepository::restoreSession,
            renewSession = {
                try {
                    authRepository.authenticateWithGoogle(
                        credentialProvider.requestAuthorizedIdToken(),
                    )
                } catch (_: NoCredentialException) {
                    null
                }
            },
        )

        setContent {
            CPEN321ApplicationTheme {
                AuthenticationScreen(
                    state = uiState,
                    onLogin = { lifecycleScope.launch { loginInteractively() } },
                    onRetry = { lifecycleScope.launch { restoreOnStartup() } },
                )
            }
        }

        lifecycleScope.launch { restoreOnStartup() }
    }

    private suspend fun restoreOnStartup() {
        uiState = AuthenticationUiState.Loading
        when (val result = startupCoordinator.restore()) {
            is SessionStartupResult.Authenticated -> openAuthenticatedApp(
                userId = result.userId,
                displayName = result.displayName,
                onboardingRequired = result.onboardingRequired,
            )
            SessionStartupResult.SignedOut -> {
                uiState = AuthenticationUiState.Login()
            }
            SessionStartupResult.RetryableFailure -> {
                uiState = AuthenticationUiState.Login(
                    message = "Could not verify your saved session. Check your connection and retry.",
                    canRetryRestoration = true,
                )
            }
        }
    }

    private suspend fun loginInteractively() {
        uiState = AuthenticationUiState.Loading
        try {
            val authentication = authRepository.authenticateWithGoogle(
                credentialProvider.requestInteractiveIdToken(),
            )
            openAuthenticatedApp(authentication)
        } catch (_: GetCredentialCancellationException) {
            uiState = AuthenticationUiState.Login("Sign-in was cancelled.")
        } catch (_: NoCredentialException) {
            uiState = AuthenticationUiState.Login("No Google credential is available.")
        } catch (error: ApiException) {
            Log.e(TAG, "Backend authentication failed: ${error.statusCode} (${error.errorCode})")
            uiState = AuthenticationUiState.Login(error.message)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(TAG, "Authentication could not be completed", error)
            uiState = AuthenticationUiState.Login("Sign in could not be completed. Please retry.")
        }
    }

    private fun openAuthenticatedApp(authentication: GoogleAuthenticationResult) {
        openAuthenticatedApp(
            userId = authentication.userId,
            displayName = authentication.displayName,
            onboardingRequired = authentication.onboardingRequired,
        )
    }

    private fun openAuthenticatedApp(
        userId: String,
        displayName: String,
        onboardingRequired: Boolean,
    ) {
        startActivity(
            Intent(this, MainActivity2::class.java).apply {
                putExtra(EXTRA_USER_ID, userId)
                putExtra(EXTRA_DISPLAY_NAME, displayName)
                putExtra(EXTRA_ONBOARDING_REQUIRED, onboardingRequired)
            },
        )
        finish()
    }
}

@Composable
private fun AuthenticationScreen(
    state: AuthenticationUiState,
    onLogin: () -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (state) {
                AuthenticationUiState.Loading -> CircularProgressIndicator()
                is AuthenticationUiState.Login -> {
                    state.message?.let { Text(it, modifier = Modifier.padding(bottom = 16.dp)) }
                    if (state.canRetryRestoration) {
                        Button(
                            onClick = onRetry,
                            modifier = Modifier.padding(bottom = 8.dp),
                        ) {
                            Text("Retry")
                        }
                    }
                    Button(onClick = onLogin) {
                        Text("Login with Google")
                    }
                }
            }
        }
    }
}
