package com.exploreUBC.cpen321application.ui.trips

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.exploreUBC.cpen321application.R
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
    var errorMessage by mutableStateOf<String?>(null)
    var isDropdownExpanded by mutableStateOf(false)
    var noResults by mutableStateOf(false)

    fun clearSearch() {
        suggestions = emptyList()
        isSearching = false
        errorMessage = null
        isDropdownExpanded = false
        noResults = false
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
            state.errorMessage = null
            state.isDropdownExpanded = false
            state.noResults = false
            delay(300)
            val result = withContext(Dispatchers.IO) {
                mapsApi.autocompleteAddresses(value)
            }
            state.suggestions = result.suggestions
            state.errorMessage = result.errorMessage
            state.noResults = result.errorMessage == null && result.suggestions.isEmpty()
            state.isDropdownExpanded = result.suggestions.isNotEmpty()
            state.isSearching = false
        }
    }

    AddressAutocompleteContent(value, onValueChange, label, tag, options, state)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
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
        ExposedDropdownMenuBox(
            expanded = state.isDropdownExpanded && state.suggestions.isNotEmpty(),
            onExpandedChange = { state.isDropdownExpanded = it },
            modifier = Modifier.fillMaxWidth(),
        ) {
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
                    when {
                        state.isSearching -> CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                        state.selectedSuggestion -> Icon(
                            painter = painterResource(R.drawable.place_verified_24px),
                            contentDescription = "Address selected from Places",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(20.dp)
                                .testTag("${tag}_places_verified"),
                        )
                        state.suggestions.isNotEmpty() -> ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = state.isDropdownExpanded,
                        )
                        else -> Unit
                    }
                },
                isError = exceedsMaxLength,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryEditable)
                    .testTag(tag),
            )
            ExposedDropdownMenu(
                expanded = state.isDropdownExpanded && state.suggestions.isNotEmpty(),
                onDismissRequest = { state.isDropdownExpanded = false },
            ) {
                state.suggestions.forEach { suggestion ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(suggestion.primaryText)
                                if (suggestion.fullAddress != suggestion.primaryText) {
                                    Text(
                                        suggestion.fullAddress,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        },
                        onClick = {
                            state.selectedSuggestion = true
                            state.suggestions = emptyList()
                            state.isDropdownExpanded = false
                            onValueChange(suggestion.fullAddress)
                        },
                        modifier = Modifier.testTag("${tag}_suggestion"),
                    )
                }
            }
        }
        if (exceedsMaxLength) {
            Text(
                text = "$label must be at most ${options.maxLength} characters.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("${tag}_length_error"),
            )
        }
        state.errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .testTag("${tag}_suggestion_error"),
            )
        }
        if (state.noResults) {
            Text(
                text = "No matching address recommendations.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .testTag("${tag}_suggestion_empty"),
            )
        }
    }
}
