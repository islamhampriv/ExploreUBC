package com.example.cpen321application.maps

import android.net.Uri
import com.google.android.gms.maps.model.LatLng
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class GoogleMapsApiClient(
    private val mapsApiKey: String,
    private val placesApiKey: String = mapsApiKey,
) {
    fun autocompleteAddresses(query: String): List<AddressSuggestion> {
        val trimmed = query.trim()
        if (trimmed.length < 3) return emptyList()
        val keys = listOf(placesApiKey, mapsApiKey)
            .map { it.trim() }
            .filter { it.isNotBlank() && it != "YOUR_API_KEY" }
            .distinct()
        for (key in keys) {
            val suggestions = autocompleteAddressesWithKey(trimmed, key) ?: continue
            return suggestions
        }
        return emptyList()
    }

    private fun autocompleteAddressesWithKey(
        query: String,
        key: String,
    ): List<AddressSuggestion>? {
        val uri = Uri.parse("https://maps.googleapis.com/maps/api/place/autocomplete/json").buildUpon()
            .appendQueryParameter("input", query)
            .appendQueryParameter("types", "address")
            .appendQueryParameter("components", "country:ca")
            .appendQueryParameter("location", "49.2606,-123.2460")
            .appendQueryParameter("radius", "30000")
            .appendQueryParameter("key", key)
            .build()

        val json = getJson(uri) ?: return null
        val status = json.optString("status")
        if (status != "OK" && status != "ZERO_RESULTS") return null
        val predictions = json.optJSONArray("predictions") ?: return emptyList()
        return buildList {
            for (i in 0 until minOf(predictions.length(), 6)) {
                val prediction = predictions.optJSONObject(i) ?: continue
                val fullAddress = prediction.optString("description")
                if (fullAddress.isBlank()) continue
                val structured = prediction.optJSONObject("structured_formatting")
                add(
                    AddressSuggestion(
                        primaryText = structured?.optString("main_text").orEmpty().ifBlank { fullAddress },
                        fullAddress = fullAddress,
                        placeId = prediction.optString("place_id"),
                    ),
                )
            }
        }
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

    private fun getJson(uri: Uri): JSONObject? {
        val connection = (URL(uri.toString()).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 8_000
        }
        return try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }
}
