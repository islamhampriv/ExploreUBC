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
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import com.exploreUBC.cpen321application.data.auth.AuthComponentFactory
import com.exploreUBC.cpen321application.data.auth.LoginAuthComponents
import com.exploreUBC.cpen321application.data.auth.SessionStartupResult
import com.exploreUBC.cpen321application.data.network.ApiException
import com.exploreUBC.cpen321application.ui.theme.CPEN321ApplicationTheme
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import java.io.IOException
import java.security.GeneralSecurityException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private const val TAG = "MainActivity"

private sealed interface AuthenticationUiState {
    data object Loading : AuthenticationUiState

    data class Login(
        val message: String? = null,
        val canRetryRestoration: Boolean = false,
    ) : AuthenticationUiState
}

class MainActivity : ComponentActivity() {
    private lateinit var authComponents: LoginAuthComponents
    private var uiState by mutableStateOf<AuthenticationUiState>(AuthenticationUiState.Loading)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        authComponents = AuthComponentFactory(
            context = applicationContext,
            apiBaseUrl = BuildConfig.API_BASE_URL,
            googleClientId = BuildConfig.GOOGLE_CLIENT_ID,
        ).createLoginComponents(this)

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
        when (authComponents.restoreSession()) {
            is SessionStartupResult.Authenticated -> openAuthenticatedApp()
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
            authComponents.authenticateInteractively()
            openAuthenticatedApp()
        } catch (_: GetCredentialCancellationException) {
            uiState = AuthenticationUiState.Login("Sign-in was cancelled.")
        } catch (_: NoCredentialException) {
            uiState = AuthenticationUiState.Login("No Google credential is available.")
        } catch (error: ApiException) {
            Log.e(TAG, "Backend authentication failed: ${error.statusCode} (${error.errorCode})")
            uiState = AuthenticationUiState.Login(error.message)
        } catch (error: CancellationException) {
            throw error
        } catch (error: GetCredentialException) {
            showUnexpectedLoginFailure(error)
        } catch (error: GoogleIdTokenParsingException) {
            showUnexpectedLoginFailure(error)
        } catch (error: IOException) {
            showUnexpectedLoginFailure(error)
        } catch (error: GeneralSecurityException) {
            showUnexpectedLoginFailure(error)
        }
    }

    private fun showUnexpectedLoginFailure(error: Exception) {
        Log.e(TAG, "Authentication could not be completed", error)
        uiState = AuthenticationUiState.Login("Sign in could not be completed. Please retry.")
    }

    private fun openAuthenticatedApp() {
        startActivity(Intent(this, MainActivity2::class.java))
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
