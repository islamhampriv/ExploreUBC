package com.exploreUBC.cpen321application.data.users

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

interface UsersRepository {
    suspend fun getUser(userId: String): User

    suspend fun updateUser(userId: String, update: UserUpdate): User
}

class UsersApi(private val apiBaseUrl: String) : UsersRepository {
    override suspend fun getUser(userId: String): User = withContext(Dispatchers.IO) {
        val encodedUserId = encode(userId)
        val response = execute(
            method = "GET",
            endpoint = "${apiBaseUrl.trimEnd('/')}/api/v1/users/$encodedUserId",
        )
        parseUser(JSONObject(response).getJSONObject("user"))
    }

    override suspend fun updateUser(userId: String, update: UserUpdate): User =
        withContext(Dispatchers.IO) {
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
            val response = execute(
                method = "PATCH",
                endpoint = "${apiBaseUrl.trimEnd('/')}/api/v1/users/${encode(userId)}",
                requestBody = requestBody.toString(),
            )
            parseUser(JSONObject(response).getJSONObject("user"))
        }

    private fun execute(
        method: String,
        endpoint: String,
        requestBody: String? = null,
    ): String {
        val connection = URL(endpoint).openConnection() as HttpURLConnection

        return try {
            connection.requestMethod = method
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            connection.setRequestProperty("Accept", "application/json")

            if (requestBody != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.bufferedWriter().use { writer ->
                    writer.write(requestBody)
                }
            }

            val responseCode = connection.responseCode
            val responseText = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }

            if (responseCode !in 200..299) {
                throw IOException("Profile request failed with HTTP $responseCode: $responseText")
            }

            responseText
        } finally {
            connection.disconnect()
        }
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
