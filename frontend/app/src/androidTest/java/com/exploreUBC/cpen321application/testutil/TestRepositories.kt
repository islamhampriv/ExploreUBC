package com.exploreUBC.cpen321application.testutil

import com.exploreUBC.cpen321application.data.events.EventsRepository
import com.exploreUBC.cpen321application.data.events.RecommendedLiveEvent
import com.exploreUBC.cpen321application.data.events.SyncResult
import com.exploreUBC.cpen321application.data.events.EventRecommendationOptions
import com.exploreUBC.cpen321application.data.itineraries.ItinerariesRepository
import com.exploreUBC.cpen321application.data.itineraries.Itinerary
import com.exploreUBC.cpen321application.data.itineraries.ItineraryDetails
import com.exploreUBC.cpen321application.data.users.UsersRepository
import com.exploreUBC.cpen321application.data.users.User
import com.exploreUBC.cpen321application.data.users.UserUpdate

object TestRepositories {
    val fakeUsersRepository = object : UsersRepository {
        override suspend fun getUser(userId: String): User {
            return User(
                googleSubject = "test-subject",
                email = "test@example.com",
                displayName = "Test User",
                createdAt = "2026-01-01T00:00:00.000Z",
                updatedAt = "2026-01-01T00:00:00.000Z"
            )
        }

        override suspend fun updateUser(userId: String, update: UserUpdate): User {
            return getUser(userId)
        }
    }

    val fakeItinerariesRepository = object : ItinerariesRepository {
        override suspend fun getItineraries(userId: String): List<Itinerary> {
            return emptyList()
        }

        override suspend fun createItinerary(userId: String, details: ItineraryDetails): Itinerary {
            return Itinerary(
                id = "test-id",
                ownerId = userId,
                name = "Test Itinerary",
                createdAt = "2026-01-01T00:00:00.000Z",
                updatedAt = "2026-01-01T00:00:00.000Z"
            )
        }

        override suspend fun getItinerary(userId: String, itineraryId: String): Itinerary {
            return createItinerary(userId, ItineraryDetails("", ""))
        }
    }

    val fakeEventsRepository = object : EventsRepository {
        override suspend fun getRecommendedEvents(
            userId: String,
            options: EventRecommendationOptions
        ): List<RecommendedLiveEvent> {
            return emptyList()
        }

        override suspend fun syncEvents(): SyncResult {
            return SyncResult(success = true, syncedCount = 0)
        }
    }
}
