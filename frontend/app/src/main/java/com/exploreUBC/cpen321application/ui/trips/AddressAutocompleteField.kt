package com.exploreUBC.cpen321application.ui.trips

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.cpen321application.maps.AddressSuggestion
import com.example.cpen321application.maps.GoogleMapsApiClient
import com.exploreUBC.cpen321application.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun AddressAutocompleteField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    tag: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    placeholder: String = "Start typing an address",
) {
    val mapsApi = remember {
        GoogleMapsApiClient(
            mapsApiKey = BuildConfig.MAPS_API_KEY,
            geocodingApiKey = BuildConfig.GEOCODING_API_KEY,
            routesApiKey = BuildConfig.ROUTES_API_KEY,
            placesApiKey = BuildConfig.PLACES_API_KEY,
        )
    }
    var suggestions by remember { mutableStateOf<List<AddressSuggestion>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var selectedSuggestion by remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        if (selectedSuggestion || value.trim().length < 3) {
            suggestions = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(300)
        suggestions = withContext(Dispatchers.IO) { mapsApi.autocompleteAddresses(value) }
        isSearching = false
    }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                selectedSuggestion = false
                onValueChange(it)
            },
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            supportingText = {
                Text(supportingText ?: "Recommended addresses appear as you type")
            },
            trailingIcon = {
                if (isSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(tag),
        )
        suggestions.forEach { suggestion ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .clickable {
                        selectedSuggestion = true
                        suggestions = emptyList()
                        onValueChange(suggestion.fullAddress)
                    }
                    .testTag("${tag}_suggestion"),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(suggestion.primaryText, style = MaterialTheme.typography.bodyLarge)
                    if (suggestion.fullAddress != suggestion.primaryText) {
                        Text(
                            suggestion.fullAddress,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
