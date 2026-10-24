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
import androidx.lifecycle.lifecycleScope
import com.exploreUBC.cpen321application.data.auth.AuthComponentFactory
import com.exploreUBC.cpen321application.data.auth.AuthenticatedAuthComponents
import com.exploreUBC.cpen321application.data.events.EventsApi
import com.exploreUBC.cpen321application.data.events.EventsRepository
import com.exploreUBC.cpen321application.data.itineraries.ItinerariesApi
import com.exploreUBC.cpen321application.data.itineraries.ItinerariesRepository
import com.exploreUBC.cpen321application.data.users.UsersApi
import com.exploreUBC.cpen321application.data.users.UsersRepository
import com.exploreUBC.cpen321application.ui.events.LiveEventsScreen
import com.exploreUBC.cpen321application.ui.map.MapRoute
import com.exploreUBC.cpen321application.ui.profile.ProfileRoute
import com.exploreUBC.cpen321application.ui.theme.CPEN321ApplicationTheme
import com.exploreUBC.cpen321application.ui.trips.TripsRoute
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
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
        val auth = AuthComponentFactory(
            context = applicationContext,
            apiBaseUrl = BuildConfig.API_BASE_URL,
            googleClientId = BuildConfig.GOOGLE_CLIENT_ID,
        ).createAuthenticatedComponents(
            activity = this,
            onSessionInvalidated = {
                withContext(Dispatchers.Main.immediate) {
                    returnToLogin()
                }
            },
        )
        if (auth == null) {
            returnToLogin()
            return
        }

        showAuthenticatedApp(
            userId = auth.userId,
            usersRepository = UsersApi(auth.apiClient),
            itinerariesRepository = ItinerariesApi(auth.apiClient),
            eventsRepository = EventsApi(auth.apiClient),
            onLogout = createLogoutAction(auth),
        )
    }

    private fun createLogoutAction(
        auth: AuthenticatedAuthComponents,
    ): () -> Unit = {
        if (!isLoggingOut) {
            isLoggingOut = true
            lifecycleScope.launch {
                try {
                    auth.logout()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: IOException) {
                    Log.w(TAG, "Backend logout failed", error)
                } finally {
                    returnToLogin()
                }
            }
        }
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
        usersRepository: UsersRepository,
        itinerariesRepository: ItinerariesRepository,
        eventsRepository: EventsRepository,
        onLogout: () -> Unit,
    ) {
        setContent {
            CPEN321ApplicationTheme {
                CPEN321ApplicationApp(
                    userId = userId,
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

    private companion object {
        const val TAG = "MainActivity2"
    }
}

@Composable
fun CPEN321ApplicationApp(
    userId: String?,
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
