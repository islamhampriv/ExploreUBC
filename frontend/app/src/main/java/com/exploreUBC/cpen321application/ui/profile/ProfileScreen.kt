package com.exploreUBC.cpen321application.ui.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.exploreUBC.cpen321application.data.users.BudgetPreference
import com.exploreUBC.cpen321application.data.users.InterestPreference
import com.exploreUBC.cpen321application.data.users.PlaceSnapshot
import com.exploreUBC.cpen321application.data.users.PreferencesUpdate
import com.exploreUBC.cpen321application.data.users.TransportationMode
import com.exploreUBC.cpen321application.data.users.UserPreferences
import com.exploreUBC.cpen321application.data.users.UsersRepository
import com.exploreUBC.cpen321application.R
import kotlinx.coroutines.launch

private val defaultPreferences = UserPreferences(
    transportationMode = TransportationMode.WALKING,
    budget = BudgetPreference.ECONOMICAL,
    interest = InterestPreference.FOOD,
)

@Composable
fun ProfileRoute(
    userId: String?,
    displayName: String? = null,
    startLocation: PlaceSnapshot? = null,
    usersRepository: UsersRepository,
    modifier: Modifier = Modifier,
) {
    var preferences by remember(userId) { mutableStateOf(defaultPreferences) }
    var errorMessage by remember(userId) { mutableStateOf<String?>(null) }
    var isSaving by remember(userId) { mutableStateOf(false) }
    var savedMessage by remember(userId) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(userId, usersRepository) {
        if (userId.isNullOrBlank()) {
            return@LaunchedEffect
        }

        runCatching { usersRepository.getPreferences(userId) }
            .onSuccess {
                preferences = it
                errorMessage = null
            }
            .onFailure { errorMessage = it.message ?: "Could not load preferences." }
    }

    ProfilePreferencesForm(
        displayName = displayName,
        startLocation = startLocation,
        preferences = preferences,
        isSaving = isSaving,
        statusMessage = errorMessage ?: savedMessage,
        onSave = { update ->
            if (userId.isNullOrBlank()) {
                preferences = update.toUserPreferences()
                errorMessage = null
                savedMessage = "Preferences selected."
            } else {
                val currentUserId = userId
                scope.launch {
                    isSaving = true
                    errorMessage = null
                    savedMessage = null
                    runCatching { usersRepository.updatePreferences(currentUserId, update) }
                        .onSuccess {
                            preferences = it
                            savedMessage = "Preferences saved."
                        }
                        .onFailure {
                            errorMessage = it.message ?: "Could not save preferences."
                        }
                    isSaving = false
                }
            }
        },
        modifier = modifier,
    )
}

private fun PreferencesUpdate.toUserPreferences() = UserPreferences(
    transportationMode = transportationMode,
    budget = budget,
    interest = interest,
)

@Composable
fun ProfilePreferencesForm(
    displayName: String? = null,
    startLocation: PlaceSnapshot? = null,
    preferences: UserPreferences,
    isSaving: Boolean,
    statusMessage: String?,
    onSave: (PreferencesUpdate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var transportationMode by remember(preferences) {
        mutableStateOf(preferences.transportationMode)
    }
    var budget by remember(preferences) { mutableStateOf(preferences.budget) }
    var interest by remember(preferences) { mutableStateOf(preferences.interest) }
    var startLocationText by remember(startLocation) {
        mutableStateOf(startLocation.editableText())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        ProfileSummary(
            displayName = displayName,
            startLocationText = startLocationText,
            onStartLocationChanged = { startLocationText = it },
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Profile preferences", fontWeight = FontWeight.Bold)
        Text(text = "These defaults are used for itinerary recommendations.")

        PreferenceDropdown(
            title = "Transportation",
            options = TransportationMode.entries,
            selected = transportationMode,
            label = { it.displayName },
            dropdownTag = "profile_transportation_dropdown",
            tag = { "profile_transportation_${it.apiValue}" },
            onSelected = { transportationMode = it },
        )
        PreferenceDropdown(
            title = "Budget",
            options = BudgetPreference.entries,
            selected = budget,
            label = { it.displayName },
            dropdownTag = "profile_budget_dropdown",
            tag = { "profile_budget_${it.apiValue}" },
            onSelected = { budget = it },
        )
        PreferenceDropdown(
            title = "Interest",
            options = InterestPreference.entries,
            selected = interest,
            label = { it.displayName },
            dropdownTag = "profile_interest_dropdown",
            tag = { "profile_interest_${it.apiValue}" },
            onSelected = { interest = it },
        )

        statusMessage?.let {
            Text(
                text = it,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .testTag("profile_status"),
            )
        }

        Button(
            onClick = {
                onSave(
                    PreferencesUpdate(
                        transportationMode = transportationMode,
                        budget = budget,
                        interest = interest,
                    ),
                )
            },
            enabled = !isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_save"),
        ) {
            Text(if (isSaving) "Saving…" else "Save preferences")
        }
    }
}

@Composable
private fun ProfileSummary(
    displayName: String?,
    startLocationText: String,
    onStartLocationChanged: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_account_box),
                    contentDescription = "Profile image",
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("profile_image"),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
        ) {
            Text(
                text = displayName?.takeIf { it.isNotBlank() } ?: "ExploreUBC User",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("profile_display_name"),
            )
            OutlinedTextField(
                value = startLocationText,
                onValueChange = onStartLocationChanged,
                label = { Text("Start location") },
                placeholder = { Text("Enter a starting location") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_start_location"),
            )
        }
    }
}

private fun PlaceSnapshot?.editableText(): String = when (this) {
    null -> ""
    else -> address?.takeIf { it.isNotBlank() }?.let { "$name — $it" } ?: name
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> PreferenceDropdown(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    dropdownTag: String,
    tag: (T) -> String,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Spacer(modifier = Modifier.height(20.dp))
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            value = label(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(title) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .testTag(dropdownTag),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(label(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    modifier = Modifier.testTag(tag(option)),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfilePreferencesFormPreview() {
    ProfilePreferencesForm(
        preferences = UserPreferences(
            transportationMode = TransportationMode.TRANSIT,
            budget = BudgetPreference.BALANCED,
            interest = InterestPreference.OUTDOORS,
        ),
        isSaving = false,
        statusMessage = null,
        onSave = {},
    )
}
