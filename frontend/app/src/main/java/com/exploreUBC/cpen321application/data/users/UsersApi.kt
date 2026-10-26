package com.exploreUBC.cpen321application.data.users

import com.exploreUBC.cpen321application.data.network.ApiRequest
import com.exploreUBC.cpen321application.data.network.BackendClient
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.json.JSONObject

interface UsersRepository {
    suspend fun getUser(userId: String): User

    suspend fun updateUser(userId: String, update: UserUpdate): User
}

class UsersApi(private val backendClient: BackendClient) : UsersRepository {
    override suspend fun getUser(userId: String): User {
        val encodedUserId = encode(userId)
        val response = backendClient.execute(
            ApiRequest(
                method = "GET",
                path = "/users/$encodedUserId",
            ),
        )
        return parseUser(JSONObject(response).getJSONObject("user"))
    }

    override suspend fun updateUser(userId: String, update: UserUpdate): User {
        val requestBody = JSONObject().apply {
            update.displayName?.let { put("displayName", it) }
            update.startLocation?.let { put("start_location", it) }
            update.preferences?.let { preferences ->
                put(
                    "preferences",
                    JSONObject().apply {
                        put(
                            "transportation",
                            JSONObject().put(
                                "mode",
                                preferences.transportation.mode.apiValue,
                            ),
                        )
                        put("budget", preferences.budget.apiValue)
                        put("interest", preferences.interest.apiValue)
                    },
                )
            }
        }
        val response = backendClient.execute(
            ApiRequest(
                method = "PATCH",
                path = "/users/${encode(userId)}",
                body = requestBody.toString(),
            ),
        )
        return parseUser(JSONObject(response).getJSONObject("user"))
    }

    private fun parseUser(json: JSONObject): User = User(
        googleSubject = json.getString("googleSubject"),
        email = json.getString("email"),
        displayName = json.getString("displayName"),
        startLocation = json.getString("start_location"),
        preferences = parsePreferences(json.getJSONObject("preferences")),
        createdAt = json.getString("create_at"),
        updatedAt = json.getString("updated_at"),
    )

    private fun parsePreferences(json: JSONObject): UserPreferences {
        return UserPreferences(
            transportation = TransportationPreference(
                mode = TransportationMode.fromApiValue(
                    json.getJSONObject("transportation").getString("mode"),
                ),
            ),
            budget = BudgetPreference.fromApiValue(json.getString("budget")),
            interest = InterestPreference.fromApiValue(json.getString("interest")),
        )
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
}
