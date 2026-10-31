package com.example.cpen321application.maps

import android.net.Uri
import com.google.android.gms.maps.model.LatLng
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

class GoogleMapsApiClient(
    private val mapsApiKey: String,
    private val placesApiKey: String = mapsApiKey,
) {
    fun autocompleteAddresses(query: String): AddressAutocompleteResult {
        val trimmed = query.trim()
        if (trimmed.length < 3) return AddressAutocompleteResult(emptyList())
        val keys = listOf(placesApiKey, mapsApiKey)
            .map { it.trim() }
            .filter { it.isNotBlank() && it != "YOUR_API_KEY" }
            .distinct()
        if (keys.isEmpty()) {
            return AddressAutocompleteResult(
                emptyList(),
                "Address suggestions are unavailable. Configure a Places API key.",
            )
        }
        var lastError: String? = null
        for (key in keys) {
            val result = autocompleteAddressesWithKey(trimmed, key) ?: continue
            if (result.errorMessage == null) return result
            lastError = result.errorMessage
        }
        return AddressAutocompleteResult(
            emptyList(),
            lastError ?: "Address suggestions are unavailable. Check your connection and Places API settings.",
        )
    }

    private fun autocompleteAddressesWithKey(
        query: String,
        key: String,
    ): AddressAutocompleteResult? {
        val uri = Uri.parse("https://places.googleapis.com/v1/places:autocomplete")
        val requestBody = JSONObject()
            .put("input", query)
            .put("includedRegionCodes", JSONArray().put("ca"))
            .put(
                "locationBias",
                JSONObject().put(
                    "circle",
                    JSONObject()
                        .put(
                            "center",
                            JSONObject()
                                .put("latitude", 49.2606)
                                .put("longitude", -123.2460),
                        )
                        .put("radius", 30_000),
                ),
            )
        val json = postJson(uri, key, requestBody) ?: return null
        val apiError = json.optJSONObject("error")
        if (apiError != null) {
            val responseStatus = apiError.optString("status").ifBlank { "unknown error" }
            val responseMessage = apiError.optString("message")
            val detail = responseMessage.takeIf(String::isNotBlank)?.let { ": $it" }.orEmpty()
            return AddressAutocompleteResult(
                emptyList(),
                "Places API error ($responseStatus)$detail",
            )
        }
        val predictions = json.optJSONArray("suggestions") ?: return AddressAutocompleteResult(emptyList())
        val suggestions = buildList {
            for (i in 0 until minOf(predictions.length(), 6)) {
                val prediction = predictions.optJSONObject(i)
                    ?.optJSONObject("placePrediction")
                    ?: continue
                val fullAddress = prediction.optJSONObject("text")
                    ?.optString("text")
                    .orEmpty()
                if (fullAddress.isBlank()) continue
                val primaryText = prediction.optJSONObject("structuredFormat")
                    ?.optJSONObject("mainText")
                    ?.optString("text")
                add(
                    AddressSuggestion(
                        primaryText = primaryText.orEmpty().ifBlank { fullAddress },
                        fullAddress = fullAddress,
                        placeId = prediction.optString("placeId"),
                    ),
                )
            }
        }
        return AddressAutocompleteResult(suggestions)
    }

    fun fetchPopularPlaces(center: LatLng): List<PopularPlace>? {
        val key = placesApiKey.ifBlank { mapsApiKey }
        if (key.isBlank() || key == "YOUR_API_KEY") return null

        val uri = Uri.parse("https://maps.googleapis.com/maps/api/place/nearbysearch/json").buildUpon()
            .appendQueryParameter("location", "${center.latitude},${center.longitude}")
            .appendQueryParameter("radius", "3000")
            .appendQueryParameter("type", "tourist_attraction")
            .appendQueryParameter("key", key)
            .build()

        val json = getJson(uri) ?: return null
        if (json.optString("status") != "OK") return null

        val results = json.optJSONArray("results") ?: return emptyList()
        return buildList {
            for (i in 0 until minOf(results.length(), 8)) {
                val place = results.optJSONObject(i) ?: continue
                val location = place.optJSONObject("geometry")?.optJSONObject("location") ?: continue
                val types = place.optJSONArray("types")
                add(
                    PopularPlace(
                        name = place.optString("name"),
                        category = types?.optString(0)?.replace('_', ' ').orEmpty().ifBlank { "Place" },
                        address = place.optString("vicinity"),
                        location = LatLng(location.getDouble("lat"), location.getDouble("lng"))
                    )
                )
            }
        }
    }

    private fun postJson(uri: Uri, apiKey: String, body: JSONObject): JSONObject? {
        val connection = (URL(uri.toString()).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 8_000
            readTimeout = 8_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("X-Goog-Api-Key", apiKey)
            setRequestProperty("X-Goog-FieldMask", "suggestions.placePrediction")
        }
        return try {
            connection.outputStream.bufferedWriter().use { writer ->
                writer.write(body.toString())
            }
            val responseCode = connection.responseCode
            val responseStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: return null
            }
            JSONObject(responseStream.bufferedReader().use { it.readText() })
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun getJson(uri: Uri): JSONObject? {
        val connection = (URL(uri.toString()).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 8_000
        }
        return try {
            val responseCode = connection.responseCode
            val responseStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: return null
            }
            JSONObject(responseStream.bufferedReader().use { it.readText() })
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }
}
