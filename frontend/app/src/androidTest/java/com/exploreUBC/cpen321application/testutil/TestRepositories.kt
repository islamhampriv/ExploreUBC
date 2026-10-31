package com.exploreUBC.cpen321application.testutil

import androidx.compose.runtime.Composable
import com.exploreUBC.cpen321application.CPEN321ApplicationApp
import com.exploreUBC.cpen321application.data.events.Event
import com.exploreUBC.cpen321application.data.events.EventsRepository
import com.exploreUBC.cpen321application.data.itineraries.ActivityItem
import com.exploreUBC.cpen321application.data.itineraries.ActivityUpdate
import com.exploreUBC.cpen321application.data.itineraries.CreateActivityInput
import com.exploreUBC.cpen321application.data.itineraries.CreateItineraryInput
import com.exploreUBC.cpen321application.data.itineraries.CreateTransportationInput
import com.exploreUBC.cpen321application.data.itineraries.ItinerariesRepository
import com.exploreUBC.cpen321application.data.itineraries.Itinerary
import com.exploreUBC.cpen321application.data.itineraries.ItineraryDetails
import com.exploreUBC.cpen321application.data.itineraries.TransportationItem
import com.exploreUBC.cpen321application.data.itineraries.TransportationSegmentUpdate
import com.exploreUBC.cpen321application.data.users.BudgetPreference
import com.exploreUBC.cpen321application.data.users.InterestPreference
import com.exploreUBC.cpen321application.data.users.TransportationMode
import com.exploreUBC.cpen321application.data.users.TransportationPreference
import com.exploreUBC.cpen321application.data.users.User
import com.exploreUBC.cpen321application.data.users.UserPreferences
import com.exploreUBC.cpen321application.data.users.UserUpdate
import com.exploreUBC.cpen321application.data.users.UsersRepository

@Composable
fun AccessibilityTestApp() {
    CPEN321ApplicationApp(
        userId = "test-user",
        displayName = "Test User",
        usersRepository = TestRepositories.users,
        itinerariesRepository = TestRepositories.itineraries,
        eventsRepository = TestRepositories.events,
    )
}

object TestRepositories {
    val users: UsersRepository = object : UsersRepository {
        override suspend fun getUser(userId: String): User = testUser

        override suspend fun updateUser(userId: String, update: UserUpdate): User = testUser
    }

    val itineraries: ItinerariesRepository = object : ItinerariesRepository {
        override suspend fun createItinerary(
            userId: String,
            input: CreateItineraryInput,
        ): Itinerary = testItinerary(userId, input)

        override suspend fun getItineraries(
            userId: String,
            saved: Boolean?,
            inRecommendation: Boolean?,
        ): List<Itinerary> = emptyList()

        override suspend fun getItinerary(
            userId: String,
            itineraryId: String,
        ): Itinerary = testItinerary(userId)

        override suspend fun updateItineraryDetails(
            userId: String,
            itineraryId: String,
            details: ItineraryDetails,
        ): Itinerary = testItinerary(
            userId = userId,
            input = CreateItineraryInput(
                name = details.name,
                description = details.description,
                startingLocation = details.startingLocation,
            ),
        )

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

        override suspend fun deleteActivity(
            userId: String,
            itineraryId: String,
            activityId: String,
        ): Unit = unsupported()

        override suspend fun deleteTransportationSegment(
            userId: String,
            itineraryId: String,
            segmentId: String,
        ): Unit = unsupported()
    }

    val events: EventsRepository = object : EventsRepository {
        override suspend fun getEvents(
            userId: String,
            latitude: Double,
            longitude: Double,
            page: Int,
            limit: Int,
        ): List<Event> = emptyList()
    }

    private val testUser = User(
        googleSubject = "test-subject",
        email = "test@example.com",
        displayName = "Test User",
        startLocation = "UBC Bus Exchange",
        preferences = UserPreferences(
            transportation = TransportationPreference(TransportationMode.WALKING),
            budget = BudgetPreference.ECONOMICAL,
            interest = InterestPreference.FOOD,
        ),
        createdAt = "2026-01-01T00:00:00.000Z",
        updatedAt = "2026-01-01T00:00:00.000Z",
    )

    private fun testItinerary(
        userId: String,
        input: CreateItineraryInput = CreateItineraryInput(
            name = "Test itinerary",
            description = "Test description",
            startingLocation = "UBC Bus Exchange",
        ),
    ): Itinerary = Itinerary(
        id = "test-itinerary",
        ownerId = userId,
        name = input.name,
        description = input.description,
        startingLocation = input.startingLocation,
        items = emptyList(),
        createdAt = "2026-01-01T00:00:00.000Z",
        updatedAt = "2026-01-01T00:00:00.000Z",
        saved = true,
        inRecommendation = false,
        score = emptyMap(),
    )

    private fun <T> unsupported(): T =
        throw UnsupportedOperationException("Not required by accessibility tests")
}
