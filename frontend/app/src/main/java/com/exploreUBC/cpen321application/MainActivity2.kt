package com.exploreUBC.cpen321application

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.exploreUBC.cpen321application.data.events.EventsApi
import com.exploreUBC.cpen321application.data.events.EventsRepository
import com.exploreUBC.cpen321application.data.users.UsersApi
import com.exploreUBC.cpen321application.data.users.UsersRepository
import com.exploreUBC.cpen321application.data.itineraries.ItinerariesApi
import com.exploreUBC.cpen321application.data.itineraries.ItinerariesRepository
import com.exploreUBC.cpen321application.ui.events.LiveEventsScreen
import com.exploreUBC.cpen321application.ui.map.MapRoute
import com.exploreUBC.cpen321application.ui.profile.ProfileRoute
import com.exploreUBC.cpen321application.ui.trips.TripsRoute
import com.exploreUBC.cpen321application.ui.theme.CPEN321ApplicationTheme

class MainActivity2 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CPEN321ApplicationTheme {
                val usersRepository = remember { UsersApi(BuildConfig.API_BASE_URL) }
                val itinerariesRepository = remember { ItinerariesApi(BuildConfig.API_BASE_URL) }
                val eventsRepository = remember { EventsApi(BuildConfig.API_BASE_URL) }
                CPEN321ApplicationApp(
//                    userId = intent.getStringExtra("userId"),
                    userId = "6acd519c76376cefc61e97f1",
                    displayName = intent.getStringExtra("displayName"),
                    usersRepository = usersRepository,
                    itinerariesRepository = itinerariesRepository,
                    eventsRepository = eventsRepository,
                )
            }
        }
    }
}

@Composable
fun CPEN321ApplicationApp(
    userId: String?,
    displayName: String?,
    usersRepository: UsersRepository,
    itinerariesRepository: ItinerariesRepository,
    eventsRepository: EventsRepository,
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.EVENTS) }
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            painterResource(it.icon),
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.PROFILE -> ProfileRoute(
                        userId = userId,
                        displayName = displayName,
                        startLocation = null,
                        usersRepository = usersRepository,
                        modifier = Modifier.padding(innerPadding),
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
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CPEN321ApplicationTheme {
        Greeting2("Android")
    }
}
