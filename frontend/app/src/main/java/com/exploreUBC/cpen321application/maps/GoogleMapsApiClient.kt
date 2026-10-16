package com.example.cpen321application.maps

import android.net.Uri
import com.google.android.gms.maps.model.LatLng
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class GoogleMapsApiClient(private val apiKey: String) {

    fun fetchRoutePolyline(stops: List<ItineraryStop>): List<LatLng>? {
        if (stops.size < 2 || apiKey.isBlank() || apiKey == "YOUR_API_KEY") return null

        val origin = stops.first().location
        val destination = stops.last().location
        val waypoints = stops.drop(1).dropLast(1).joinToString("|") { "${it.location.latitude},${it.location.longitude}" }

        val uri = Uri.parse("https://maps.googleapis.com/maps/api/directions/json").buildUpon()
            .appendQueryParameter("origin", "${origin.latitude},${origin.longitude}")
            .appendQueryParameter("destination", "${destination.latitude},${destination.longitude}")
            .appendQueryParameter("mode", "walking")
            .apply {
                if (waypoints.isNotBlank()) {
                    appendQueryParameter("waypoints", waypoints)
                }
            }
            .appendQueryParameter("key", apiKey)
            .build()

        val json = getJson(uri) ?: return null
        if (json.optString("status") != "OK") return null

        val encoded = json.getJSONArray("routes")
            .optJSONObject(0)
            ?.getJSONObject("overview_polyline")
            ?.optString("points")
            .orEmpty()

        return encoded.takeIf { it.isNotBlank() }?.let { decodePolyline(it) }
    }

    fun fetchPopularPlaces(center: LatLng): List<PopularPlace>? {
        if (apiKey.isBlank() || apiKey == "YOUR_API_KEY") return null

        val uri = Uri.parse("https://maps.googleapis.com/maps/api/place/nearbysearch/json").buildUpon()
            .appendQueryParameter("location", "${center.latitude},${center.longitude}")
            .appendQueryParameter("radius", "3000")
            .appendQueryParameter("type", "tourist_attraction")
            .appendQueryParameter("key", apiKey)
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

    private fun decodePolyline(encoded: String): List<LatLng> {
        val points = mutableListOf<LatLng>()
        var index = 0
        var lat = 0
        var lng = 0

        while (index < encoded.length) {
            var result = 0
            var shift = 0
            var byte: Int
            do {
                byte = encoded[index++].code - 63
                result = result or ((byte and 0x1f) shl shift)
                shift += 5
            } while (byte >= 0x20)
            lat += if (result and 1 != 0) (result shr 1).inv() else result shr 1

            result = 0
            shift = 0
            do {
                byte = encoded[index++].code - 63
                result = result or ((byte and 0x1f) shl shift)
                shift += 5
            } while (byte >= 0x20)
            lng += if (result and 1 != 0) (result shr 1).inv() else result shr 1

            points.add(LatLng(lat / 1e5, lng / 1e5))
        }
        return points
    }
}
