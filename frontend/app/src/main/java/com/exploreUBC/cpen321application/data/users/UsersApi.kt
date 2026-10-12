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
    suspend fun getPreferences(userId: String): UserPreferences

    suspend fun updatePreferences(userId: String, update: PreferencesUpdate): UserPreferences
}

class UsersApi(private val apiBaseUrl: String) : UsersRepository {
    override suspend fun getPreferences(userId: String): UserPreferences = withContext(Dispatchers.IO) {
        execute(
            method = "GET",
            userId = userId,
        )
    }

    override suspend fun updatePreferences(
        userId: String,
        update: PreferencesUpdate,
    ): UserPreferences = withContext(Dispatchers.IO) {
        execute(
            method = "PATCH",
            userId = userId,
            requestBody = JSONObject()
                .put(
                    "transportation",
                    JSONObject().put("mode", update.transportationMode.apiValue),
                )
                .put("budget", update.budget.apiValue)
                .put("interest", update.interest.apiValue)
                .toString(),
        )
    }

    private fun execute(
        method: String,
        userId: String,
        requestBody: String? = null,
    ): UserPreferences {
        val encodedUserId = URLEncoder.encode(userId, StandardCharsets.UTF_8.toString())
        val endpoint =
            "${apiBaseUrl.trimEnd('/')}/api/v1/users/$encodedUserId/preferences"
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

            parsePreferences(JSONObject(responseText).getJSONObject("preferences"))
        } finally {
            connection.disconnect()
        }
    }

    private fun parsePreferences(json: JSONObject): UserPreferences {
        return UserPreferences(
            transportationMode = TransportationMode.fromApiValue(
                json.getJSONObject("transportation").getString("mode"),
            ),
            budget = BudgetPreference.fromApiValue(json.getString("budget")),
            interest = InterestPreference.fromApiValue(json.getString("interest")),
        )
    }
}
