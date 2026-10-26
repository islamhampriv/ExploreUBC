package com.example.cpen321application.maps

import android.net.Uri
import com.google.android.gms.maps.model.LatLng
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

class GoogleMapsApiClient(
    private val mapsApiKey: String,
    private val geocodingApiKey: String = mapsApiKey,
    private val routesApiKey: String = mapsApiKey,
    private val placesApiKey: String = mapsApiKey,
) {

    fun geocode(address: String): LatLng? {
        val trimmed = address.trim()
        val key = geocodingApiKey.ifBlank { mapsApiKey }
        if (trimmed.isEmpty() || key.isBlank() || key == "YOUR_API_KEY") return null
        parseLatLng(trimmed)?.let { return it }

        val uri = Uri.parse("https://maps.googleapis.com/maps/api/geocode/json").buildUpon()
            .appendQueryParameter("address", trimmed)
            .appendQueryParameter("region", "ca")
            .appendQueryParameter("key", key)
            .build()

        val json = getJson(uri) ?: return null
        if (json.optString("status") != "OK") return null
        val location = json.optJSONArray("results")
            ?.optJSONObject(0)
            ?.optJSONObject("geometry")
            ?.optJSONObject("location")
            ?: return null
        return LatLng(location.getDouble("lat"), location.getDouble("lng"))
    }

    fun fetchDrivingRoute(stops: List<ItineraryStop>): List<LatLng>? {
        if (stops.size < 2) return null
        fetchRoutesApiPolyline(stops)?.let { return it }
        return fetchDirectionsApiPolyline(stops, travelMode = "driving")
    }

    fun fetchRoutePolyline(stops: List<ItineraryStop>): List<LatLng>? =
        fetchDrivingRoute(stops)

    private fun fetchRoutesApiPolyline(stops: List<ItineraryStop>): List<LatLng>? {
        val key = routesApiKey.ifBlank { mapsApiKey }
        if (key.isBlank() || key == "YOUR_API_KEY") return null

        val origin = stops.first().location
        val destination = stops.last().location
        val body = JSONObject()
            .put("origin", latLngWaypoint(origin))
            .put("destination", latLngWaypoint(destination))
            .put("travelMode", "DRIVE")
            .put("routingPreference", "TRAFFIC_UNAWARE")
            .apply {
                if (stops.size > 2) {
                    val intermediates = JSONArray()
                    stops.drop(1).dropLast(1).forEach { stop ->
                        intermediates.put(latLngWaypoint(stop.location))
                    }
                    put("intermediates", intermediates)
                }
            }

        val connection = (URL("https://routes.googleapis.com/directions/v2:computeRoutes")
            .openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 8_000
            readTimeout = 8_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("X-Goog-Api-Key", key)
            setRequestProperty("X-Goog-FieldMask", "routes.polyline.encodedPolyline")
        }
        return try {
            connection.outputStream.bufferedWriter().use { it.write(body.toString()) }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val encoded = json.optJSONArray("routes")
                ?.optJSONObject(0)
                ?.optJSONObject("polyline")
                ?.optString("encodedPolyline")
                .orEmpty()
            encoded.takeIf { it.isNotBlank() }?.let { decodePolyline(it) }
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun fetchDirectionsApiPolyline(
        stops: List<ItineraryStop>,
        travelMode: String,
    ): List<LatLng>? {
        val key = routesApiKey.ifBlank { mapsApiKey }
        if (key.isBlank() || key == "YOUR_API_KEY") return null

        val origin = stops.first().location
        val destination = stops.last().location
        val waypoints = stops.drop(1).dropLast(1).joinToString("|") {
            "${it.location.latitude},${it.location.longitude}"
        }

        val uri = Uri.parse("https://maps.googleapis.com/maps/api/directions/json").buildUpon()
            .appendQueryParameter("origin", "${origin.latitude},${origin.longitude}")
            .appendQueryParameter("destination", "${destination.latitude},${destination.longitude}")
            .appendQueryParameter("mode", travelMode)
            .apply {
                if (waypoints.isNotBlank()) {
                    appendQueryParameter("waypoints", waypoints)
                }
            }
            .appendQueryParameter("key", key)
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

    fun autocompleteAddresses(query: String): List<AddressSuggestion> {
        val trimmed = query.trim()
        if (trimmed.length < 3) return emptyList()
        val keys = listOf(placesApiKey, geocodingApiKey, mapsApiKey)
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

    private fun latLngWaypoint(point: LatLng): JSONObject =
        JSONObject().put(
            "location",
            JSONObject().put(
                "latLng",
                JSONObject()
                    .put("latitude", point.latitude)
                    .put("longitude", point.longitude),
            ),
        )

    private fun parseLatLng(value: String): LatLng? {
        val parts = value.split(',').map { it.trim() }
        if (parts.size != 2) return null
        val lat = parts[0].toDoubleOrNull() ?: return null
        val lng = parts[1].toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return null
        return LatLng(lat, lng)
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
