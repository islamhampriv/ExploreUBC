package com.exploreUBC.cpen321application.e2e.maps.mock

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.exploreUBC.cpen321application.data.itineraries.ActivityItem
import com.exploreUBC.cpen321application.data.itineraries.ActivityUpdate
import com.exploreUBC.cpen321application.data.itineraries.CreateActivityInput
import com.exploreUBC.cpen321application.data.itineraries.CreateItineraryInput
import com.exploreUBC.cpen321application.data.itineraries.CreateTransportationInput
import com.exploreUBC.cpen321application.data.itineraries.ItinerariesRepository
import com.exploreUBC.cpen321application.data.itineraries.Itinerary
import com.exploreUBC.cpen321application.data.itineraries.ItineraryHttpException
import com.exploreUBC.cpen321application.data.itineraries.ItineraryMap
import com.exploreUBC.cpen321application.data.itineraries.ItineraryUpdate
import com.exploreUBC.cpen321application.data.itineraries.MapCoordinates
import com.exploreUBC.cpen321application.data.itineraries.MapMarker
import com.exploreUBC.cpen321application.data.itineraries.MapRoute as MapLeg
import com.exploreUBC.cpen321application.data.itineraries.RouteStep
import com.exploreUBC.cpen321application.data.itineraries.TransportationItem
import com.exploreUBC.cpen321application.data.itineraries.TransportationSegmentUpdate
import com.exploreUBC.cpen321application.data.itineraries.TravelOption
import com.exploreUBC.cpen321application.data.itineraries.UnmappedActivity
import com.exploreUBC.cpen321application.testutil.E2E_USER_ID
import com.exploreUBC.cpen321application.ui.map.MapRoute
import java.io.IOException
import org.junit.Rule
import org.junit.Test

class ViewItineraryMapMockTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    // Section 4.2 — View Itinerary on Map success: steps 2–9.
    // Search a saved itinerary, show the leg, switch Walk/Drive, then hide and show directions.
    fun showsWalkingAndDrivingDirectionsForSelectedItinerary() {
        showMap(ScriptedMapRepository(mapFor = { mappedItinerary() }))

        composeRule.onNodeWithTag("map_itinerary_search").assertIsDisplayed()
        composeRule.onNodeWithText("Show popular locations").assertIsDisplayed()
        composeRule.onNodeWithTag("map_itinerary_search").performTextReplacement("UBC")
        composeRule.onNodeWithTag("map_itinerary_$ITINERARY_ID").performClick()

        waitForTag("map_travel_drive")
        composeRule.onNodeWithText("Museum → Nest").assertIsDisplayed()
        composeRule.onNodeWithText("Drive 4 min (1.3 km)").assertIsDisplayed()
        composeRule.onNodeWithText("1. Head northeast").assertIsDisplayed()

        composeRule.onNodeWithTag("map_travel_walk").performClick()
        composeRule.onNodeWithText("Walk 13 min (890 m)").assertIsDisplayed()
        composeRule.onNodeWithText("1. Head east").assertIsDisplayed()

        composeRule.onNodeWithTag("map_directions_visibility").performClick()
        assertTextAbsent("1. Head east")
        composeRule.onNodeWithText("Show directions").assertIsDisplayed()
        composeRule.onNodeWithTag("map_directions_visibility").performClick()
        composeRule.onNodeWithText("1. Head east").assertIsDisplayed()
    }

    @Test
    // Section 4.2 — View Itinerary on Map failure 1a: no signed-in user.
    fun promptsWhenUserIsMissing() {
        composeRule.setContent {
            MapRoute(
                userId = null,
                itinerariesRepository = ScriptedMapRepository(mapFor = { mappedItinerary() }),
                showMap = false,
            )
        }
        waitForText("A user ID is required to search saved itineraries.")
    }

    @Test
    // Section 4.2 — View Itinerary on Map failure 2a: nothing saved matches the search.
    fun reportsWhenNoSavedItineraryMatches() {
        showMap(ScriptedMapRepository(itineraries = emptyList(), mapFor = { mappedItinerary() }))

        composeRule.onNodeWithTag("map_itinerary_search").performTextReplacement("missing trip")
        composeRule.onNodeWithText("No saved itineraries match your search.").assertIsDisplayed()
    }

    @Test
    // Section 4.2 — View Itinerary on Map failure 4a: saved itineraries cannot be loaded.
    fun reportsWhenSavedItinerariesFailToLoad() {
        showMap(
            ScriptedMapRepository(
                listFailure = IOException("Could not load saved itineraries."),
                mapFor = { mappedItinerary() },
            ),
        )

        waitForText("Could not load saved itineraries.")
    }

    @Test
    // Section 4.2 — View Itinerary on Map failure 2b: nothing to plot.
    fun reportsWhenItineraryHasNothingToPlot() {
        showMap(
            ScriptedMapRepository(mapFor = { emptyItineraryMap() }),
            selectedItineraryId = ITINERARY_ID,
        )

        waitForText("No routes were returned. Pins are shown without route lines.")
    }

    @Test
    // Section 4.2 — View Itinerary on Map failure 5b: one leg cannot be drawn.
    fun reportsMissingWalkingLegAndKeepsDrivingDirections() {
        showMap(
            ScriptedMapRepository(mapFor = { mappedItinerary(includeWalking = false) }),
            selectedItineraryId = ITINERARY_ID,
        )

        waitForText("Drive 4 min (1.3 km)")
        composeRule.onNodeWithTag("map_travel_walk").performClick()
        composeRule.onNodeWithText("No walk route for this leg.").assertIsDisplayed()
        assertTextAbsent("1. Head northeast")
    }

    @Test
    // Section 4.2 — View Itinerary on Map failure 4a: map request fails, then retry succeeds.
    fun retriesAfterMapRequestFails() {
        var attempts = 0
        showMap(
            ScriptedMapRepository(mapFor = {
                attempts += 1
                if (attempts == 1) {
                    throw IOException("Could not load the map.")
                }
                mappedItinerary()
            }),
            selectedItineraryId = ITINERARY_ID,
        )

        waitForText("Could not load the map.")
        composeRule.onNodeWithText("Retry").assertIsDisplayed().performClick()
        waitForText("Drive 4 min (1.3 km)")
    }

    @Test
    // Section 4.2 — View Itinerary on Map failure: the itinerary no longer exists.
    fun reportsWhenItineraryIsMissing() {
        showMap(
            ScriptedMapRepository(mapFor = {
                throw ItineraryHttpException(404, "Itinerary not found")
            }),
            selectedItineraryId = ITINERARY_ID,
        )

        waitForText("This itinerary no longer exists.")
    }

    private fun showMap(
        repository: ItinerariesRepository,
        selectedItineraryId: String? = null,
    ) {
        composeRule.setContent {
            var selectedId by remember { mutableStateOf(selectedItineraryId) }
            MapRoute(
                userId = E2E_USER_ID,
                itinerariesRepository = repository,
                selectedItineraryId = selectedId,
                onItinerarySelected = { selectedId = it },
                showMap = false,
            )
        }
        waitForTag("map_itinerary_search")
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun assertTextAbsent(text: String) {
        org.junit.Assert.assertTrue(
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty(),
        )
    }
}

private const val ITINERARY_ID = "map-itinerary-1"
private const val MUSEUM_ID = "museum"
private const val NEST_ID = "nest"

private fun mappedItinerary(includeWalking: Boolean = true) = ItineraryMap(
    itineraryId = ITINERARY_ID,
    ownerId = E2E_USER_ID,
    itineraryUpdatedAt = "2026-11-07T00:00:00Z",
    markers = listOf(
        marker(MUSEUM_ID, "Museum", "6393 NW Marine Dr, Vancouver"),
        marker(NEST_ID, "Nest", "6138 Student Union Blvd, Vancouver"),
    ),
    routes = listOf(
        MapLeg(
            fromActivityId = MUSEUM_ID,
            toActivityId = NEST_ID,
            transportationMode = "driving",
            encodedPolyline = "drive",
            distanceMeters = 1334.0,
            durationSeconds = 226.0,
            walking = if (includeWalking) {
                travelOption(890.0, 789.0, "Head east")
            } else {
                null
            },
            driving = travelOption(1334.0, 226.0, "Head northeast"),
        ),
    ),
    unmappedActivities = listOf(
        UnmappedActivity("unclear", "Unclear stop", "Near the park"),
    ),
    generatedAt = "2026-11-07T00:00:01Z",
)

private fun emptyItineraryMap() = ItineraryMap(
    itineraryId = ITINERARY_ID,
    ownerId = E2E_USER_ID,
    itineraryUpdatedAt = "2026-11-07T00:00:00Z",
    markers = emptyList(),
    routes = emptyList(),
    unmappedActivities = emptyList(),
    generatedAt = "2026-11-07T00:00:01Z",
)

private fun marker(id: String, title: String, address: String) = MapMarker(
    activityId = id,
    title = title,
    address = address,
    placeId = id,
    coordinates = MapCoordinates(49.26, -123.25),
    isApproximate = false,
)

private fun travelOption(meters: Double, seconds: Double, instruction: String) = TravelOption(
    encodedPolyline = instruction,
    distanceMeters = meters,
    durationSeconds = seconds,
    steps = listOf(RouteStep(instruction)),
)

private class ScriptedMapRepository(
    private val itineraries: List<Itinerary> = listOf(sampleItinerary()),
    private val listFailure: Throwable? = null,
    private val mapFor: () -> ItineraryMap,
) : ItinerariesRepository {
    override suspend fun getItineraries(
        userId: String,
        saved: Boolean?,
        inRecommendation: Boolean?,
    ): List<Itinerary> {
        listFailure?.let { throw it }
        return itineraries
    }

    override suspend fun getItinerary(userId: String, itineraryId: String): Itinerary =
        itineraries.first { it.id == itineraryId }

    override suspend fun getItineraryMap(userId: String, itineraryId: String): ItineraryMap =
        mapFor()

    override suspend fun createItinerary(userId: String, input: CreateItineraryInput): Itinerary =
        unsupported()

    override suspend fun generateRecommendations(userId: String): List<Itinerary> = unsupported()

    override suspend fun updateItinerary(
        userId: String,
        itineraryId: String,
        update: ItineraryUpdate,
    ): Itinerary = unsupported()

    override suspend fun deleteItinerary(userId: String, itineraryId: String) = Unit

    override suspend fun addActivity(
        userId: String,
        itineraryId: String,
        activity: CreateActivityInput,
    ): ActivityItem = unsupported()

    override suspend fun addTransportationSegment(
        userId: String,
        itineraryId: String,
        segment: CreateTransportationInput,
    ): TransportationItem = unsupported()

    override suspend fun updateActivity(
        userId: String,
        itineraryId: String,
        activityId: String,
        update: ActivityUpdate,
    ): ActivityItem = unsupported()

    override suspend fun updateTransportationSegment(
        userId: String,
        itineraryId: String,
        segmentId: String,
        update: TransportationSegmentUpdate,
    ): TransportationItem = unsupported()

    override suspend fun deleteActivity(userId: String, itineraryId: String, activityId: String) =
        Unit

    override suspend fun deleteTransportationSegment(
        userId: String,
        itineraryId: String,
        segmentId: String,
    ) = Unit

    private fun <T> unsupported(): T =
        throw UnsupportedOperationException("Not used by map scenarios")
}

private fun sampleItinerary() = Itinerary(
    id = ITINERARY_ID,
    ownerId = E2E_USER_ID,
    name = "UBC Map Day",
    description = "Mock itinerary for map scenarios.",
    startingLocation = "UBC Bus Exchange",
    items = listOf(
        ActivityItem(
            id = MUSEUM_ID,
            name = "Museum",
            image = "",
            description = null,
            category = "museum",
            location = "6393 NW Marine Dr, Vancouver",
            startTime = "2027-11-30T18:00:00Z",
            endTime = "2027-11-30T19:00:00Z",
        ),
    ),
    createdAt = "2026-11-07T00:00:00Z",
    updatedAt = "2026-11-07T00:00:00Z",
    saved = true,
    inRecommendation = false,
    score = emptyMap(),
)
