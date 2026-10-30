package com.exploreUBC.cpen321application.e2e.users

import com.exploreUBC.cpen321application.data.users.BudgetPreference
import com.exploreUBC.cpen321application.data.users.InterestPreference
import com.exploreUBC.cpen321application.data.users.TransportationMode
import com.exploreUBC.cpen321application.data.users.TransportationPreference
import com.exploreUBC.cpen321application.data.users.User
import com.exploreUBC.cpen321application.data.users.UserPreferences
import com.exploreUBC.cpen321application.data.users.UserUpdate
import com.exploreUBC.cpen321application.data.users.UsersRepository

internal const val E2E_PROFILE_USER_ID = "6705f0000000000000000001"

internal class InMemoryUsersRepository : UsersRepository {
    var user = User(
        googleSubject = E2E_PROFILE_USER_ID,
        email = "explorer@ubc.ca",
        displayName = "ExploreUBC User",
        startLocation = "UBC Bus Exchange",
        preferences = UserPreferences(
            transportation = TransportationPreference(TransportationMode.WALKING),
            budget = BudgetPreference.ECONOMICAL,
            interest = InterestPreference.FOOD,
        ),
        createdAt = "2026-10-29T12:00:00Z",
        updatedAt = "2026-10-29T12:00:00Z",
    )
        private set

    override suspend fun getUser(userId: String): User {
        require(userId == E2E_PROFILE_USER_ID)
        return user
    }

    override suspend fun updateUser(userId: String, update: UserUpdate): User {
        require(userId == E2E_PROFILE_USER_ID)
        user = user.copy(
            displayName = update.displayName ?: user.displayName,
            startLocation = update.startLocation ?: user.startLocation,
            preferences = update.preferences ?: user.preferences,
            updatedAt = "2026-10-29T12:01:00Z",
        )
        return user
    }
}
