package com.exploreUBC.cpen321application

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import com.exploreUBC.cpen321application.data.auth.AuthRepository
import com.exploreUBC.cpen321application.data.auth.GoogleCredentialProvider
import com.exploreUBC.cpen321application.data.auth.SessionManager
import com.exploreUBC.cpen321application.data.auth.SessionStore
import com.exploreUBC.cpen321application.data.events.EventsApi
import com.exploreUBC.cpen321application.data.events.EventsRepository
import com.exploreUBC.cpen321application.data.itineraries.ItinerariesApi
import com.exploreUBC.cpen321application.data.itineraries.ItinerariesRepository
import com.exploreUBC.cpen321application.data.network.ApiClient
import com.exploreUBC.cpen321application.data.network.ApiException
import com.exploreUBC.cpen321application.data.network.SingleFlightSessionRenewer
import com.exploreUBC.cpen321application.data.users.UsersApi
import com.exploreUBC.cpen321application.data.users.UsersRepository
import com.exploreUBC.cpen321application.ui.events.LiveEventsScreen
import com.exploreUBC.cpen321application.ui.map.MapRoute
import com.exploreUBC.cpen321application.ui.profile.ProfileRoute
import com.exploreUBC.cpen321application.ui.theme.CPEN321ApplicationTheme
import com.exploreUBC.cpen321application.ui.trips.TripsRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity2 : ComponentActivity() {
    private var returningToLogin = false
    private var isLoggingOut = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        showLoading()
        lifecycleScope.launch { initializeAuthenticatedApp() }
    }

    private suspend fun initializeAuthenticatedApp() {
        val sessionStore = SessionStore(applicationContext)
        val storedSession = withContext(Dispatchers.IO) { sessionStore.load() }
        if (storedSession == null) {
            returnToLogin()
            return
        }

        val sessionManager = SessionManager(storedSession)
        val authTransport = ApiClient(
            apiBaseUrl = BuildConfig.API_BASE_URL,
            sessionManager = sessionManager,
        )
        val authRepository = AuthRepository(
            backendClient = authTransport,
            sessionManager = sessionManager,
            persistSession = { session ->
                withContext(Dispatchers.IO) { sessionStore.save(session) }
            },
        )
        val credentialProvider = GoogleCredentialProvider(
            activity = this,
            serverClientId = BuildConfig.GOOGLE_CLIENT_ID,
        )
        val renewer = SingleFlightSessionRenewer(
            sessionManager = sessionManager,
            persistSession = { session ->
                withContext(Dispatchers.IO) { sessionStore.save(session) }
            },
            requestReplacement = { failedSession ->
                try {
                    val replacement = authRepository.requestReplacementSession(
                        credentialProvider.requestAuthorizedIdToken(),
                    )
                    if (replacement.userId != failedSession.userId) {
                        clearSessionAndReturnToLogin(sessionStore, sessionManager)
                        null
                    } else {
                        replacement
                    }
                } catch (_: NoCredentialException) {
                    clearSessionAndReturnToLogin(sessionStore, sessionManager)
                    null
                } catch (_: GetCredentialException) {
                    clearSessionAndReturnToLogin(sessionStore, sessionManager)
                    null
                } catch (error: ApiException) {
                    if (error.statusCode == 401) {
                        clearSessionAndReturnToLogin(sessionStore, sessionManager)
                        null
                    } else {
                        throw error
                    }
                }
            },
        )
        val apiClient = ApiClient(
            apiBaseUrl = BuildConfig.API_BASE_URL,
            sessionManager = sessionManager,
            renewalHandler = renewer,
        )

        showAuthenticatedApp(
            userId = storedSession.userId,
            displayName = intent.getStringExtra(EXTRA_DISPLAY_NAME),
            usersRepository = UsersApi(apiClient),
            itinerariesRepository = ItinerariesApi(apiClient),
            eventsRepository = EventsApi(apiClient),
            onLogout = {
                if (!isLoggingOut) {
                    isLoggingOut = true
                    lifecycleScope.launch {
                        try {
                            authRepository.logout()
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Exception) {
                            Log.w("MainActivity2", "Backend logout failed", error)
                        } finally {
                            clearSessionAndReturnToLogin(sessionStore, sessionManager)
                        }
                    }
                }
            },
        )
    }

    private suspend fun clearSessionAndReturnToLogin(
        sessionStore: SessionStore,
        sessionManager: SessionManager,
    ) {
        withContext(Dispatchers.IO) { sessionStore.clear() }
        sessionManager.clear()
        withContext(Dispatchers.Main.immediate) { returnToLogin() }
    }

    private fun showLoading() {
        setContent {
            CPEN321ApplicationTheme {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    private fun showAuthenticatedApp(
        userId: String,
        displayName: String?,
        usersRepository: UsersRepository,
        itinerariesRepository: ItinerariesRepository,
        eventsRepository: EventsRepository,
        onLogout: () -> Unit,
    ) {
        setContent {
            CPEN321ApplicationTheme {
                CPEN321ApplicationApp(
                    userId = userId,
                    displayName = displayName,
                    usersRepository = usersRepository,
                    itinerariesRepository = itinerariesRepository,
                    eventsRepository = eventsRepository,
                    onLogout = onLogout,
                )
            }
        }
    }

    private fun returnToLogin() {
        if (returningToLogin) return
        returningToLogin = true
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
        finish()
    }
}

@Composable
fun CPEN321ApplicationApp(
    userId: String?,
    displayName: String?,
    usersRepository: UsersRepository,
    itinerariesRepository: ItinerariesRepository,
    eventsRepository: EventsRepository,
    onLogout: () -> Unit = {},
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.EVENTS) }
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            painterResource(it.icon),
                            contentDescription = it.label,
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it },
                )
            }
        },
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.PROFILE -> ProfileRoute(
                    userId = userId,
                    displayName = displayName,
                    startLocation = null,
                    usersRepository = usersRepository,
                    modifier = Modifier.padding(innerPadding),
                    onLogout = onLogout,
                )
                AppDestinations.TRIPS -> TripsRoute(
                    userId = userId,
                    itinerariesRepository = itinerariesRepository,
                    modifier = Modifier.padding(innerPadding),
                )
                AppDestinations.EVENTS -> LiveEventsScreen(
                    userId = userId,
                    eventsRepository = eventsRepository,
                    itinerariesRepository = itinerariesRepository,
                    modifier = Modifier.padding(innerPadding),
                )
                AppDestinations.MAP -> MapRoute(
                    modifier = Modifier.padding(innerPadding),
                )
                else -> Greeting2(
                    name = currentDestination.label,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    EVENTS("Events", R.drawable.notifications_24px),
    TRIPS("Trips", R.drawable.trip_24px),
    MAP("Map", R.drawable.map_24px),
    CALCULATOR("Calculator", R.drawable.calculate_24px),
    PROFILE("Profile", R.drawable.ic_account_box),
}

@Composable
fun Greeting2(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CPEN321ApplicationTheme {
        Greeting2("Android")
    }
}
