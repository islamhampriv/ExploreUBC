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

data class AddressAutocompleteOptions(
    val modifier: Modifier = Modifier,
    val supportingText: String? = null,
    val placeholder: String = "Start typing an address",
    val required: Boolean = false,
    val maxLength: Int? = null,
)

private class AddressAutocompleteState {
    var suggestions by mutableStateOf<List<AddressSuggestion>>(emptyList())
    var isSearching by mutableStateOf(false)
    var selectedSuggestion by mutableStateOf(false)

    fun clearSearch() {
        suggestions = emptyList()
        isSearching = false
    }
}

@Composable
fun AddressAutocompleteField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    tag: String,
    options: AddressAutocompleteOptions = AddressAutocompleteOptions(),
) {
    val mapsApi = remember {
        GoogleMapsApiClient(
            mapsApiKey = BuildConfig.MAPS_API_KEY,
            placesApiKey = BuildConfig.PLACES_API_KEY,
        )
    }
    val state = remember { AddressAutocompleteState() }

    LaunchedEffect(value) {
        if (state.selectedSuggestion || value.trim().length < 3) {
            state.clearSearch()
        } else {
            state.isSearching = true
            delay(300)
            state.suggestions = withContext(Dispatchers.IO) {
                mapsApi.autocompleteAddresses(value)
            }
            state.isSearching = false
        }
    }

    AddressAutocompleteContent(value, onValueChange, label, tag, options, state)
}

@Composable
private fun AddressAutocompleteContent(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    tag: String,
    options: AddressAutocompleteOptions,
    state: AddressAutocompleteState,
) {
    val exceedsMaxLength = options.maxLength != null && value.length > options.maxLength
    Column(modifier = options.modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                state.selectedSuggestion = false
                onValueChange(it)
            },
            label = { Text(if (options.required) "$label *" else label) },
            placeholder = { Text(options.placeholder) },
            supportingText = {
                Text(options.supportingText ?: "Recommended addresses appear as you type")
            },
            trailingIcon = {
                if (state.isSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                }
            },
            isError = exceedsMaxLength,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(tag),
        )
        if (exceedsMaxLength) {
            Text(
                text = "$label must be at most ${options.maxLength} characters.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("${tag}_length_error"),
            )
        }
        state.suggestions.forEach { suggestion ->
            AddressSuggestionCard(suggestion, tag) {
                state.selectedSuggestion = true
                state.suggestions = emptyList()
                onValueChange(suggestion.fullAddress)
            }
        }
    }
}

@Composable
private fun AddressSuggestionCard(
    suggestion: AddressSuggestion,
    tag: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .clickable(onClick = onClick)
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
