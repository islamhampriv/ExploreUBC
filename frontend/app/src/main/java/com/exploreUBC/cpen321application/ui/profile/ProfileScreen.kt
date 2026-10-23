package com.exploreUBC.cpen321application.ui.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.TextButton
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
import com.exploreUBC.cpen321application.R
import com.exploreUBC.cpen321application.data.users.BudgetPreference
import com.exploreUBC.cpen321application.data.users.InterestPreference
import com.exploreUBC.cpen321application.data.users.TransportationPreference
import com.exploreUBC.cpen321application.data.users.TransportationMode
import com.exploreUBC.cpen321application.data.users.User
import com.exploreUBC.cpen321application.data.users.UserPreferences
import com.exploreUBC.cpen321application.data.users.UserUpdate
import com.exploreUBC.cpen321application.data.users.UsersRepository
import kotlinx.coroutines.launch

private val defaultPreferences = UserPreferences(
    transportation = TransportationPreference(TransportationMode.WALKING),
    budget = BudgetPreference.ECONOMICAL,
    interest = InterestPreference.FOOD,
)

@Composable
fun ProfileRoute(
    userId: String?,
    usersRepository: UsersRepository,
    modifier: Modifier = Modifier,
    displayName: String? = null,
    startLocation: String? = null,
) {
    var user by remember(userId) { mutableStateOf<User?>(null) }
    var fallbackDisplayName by remember(userId, displayName) { mutableStateOf(displayName) }
    var fallbackStartLocation by remember(userId, startLocation) { mutableStateOf(startLocation) }
    var preferences by remember(userId) { mutableStateOf(defaultPreferences) }
    var errorMessage by remember(userId) { mutableStateOf<String?>(null) }
    var isSaving by remember(userId) { mutableStateOf(false) }
    var isEditingProfile by remember(userId) { mutableStateOf(false) }
    var savedMessage by remember(userId) { mutableStateOf<String?>(null) }
    var isLoading by remember(userId) { mutableStateOf(!userId.isNullOrBlank()) }
    val scope = rememberCoroutineScope()

    fun saveUpdate(
        update: UserUpdate,
        successMessage: String,
        afterSave: () -> Unit = {},
    ) {
        val currentUserId = userId ?: return
        scope.launch {
            isSaving = true
            errorMessage = null
            savedMessage = null
            runCatching { usersRepository.updateUser(currentUserId, update) }
                .onSuccess {
                    user = it
                    preferences = it.preferences
                    savedMessage = successMessage
                    afterSave()
                }
                .onFailure {
                    errorMessage = it.message ?: "Could not save profile changes."
                }
            isSaving = false
        }
    }

    LaunchedEffect(userId, usersRepository) {
        if (userId.isNullOrBlank()) {
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        errorMessage = null
        runCatching { usersRepository.getUser(userId) }
            .onSuccess {
                user = it
                preferences = it.preferences
                errorMessage = null
            }
            .onFailure { errorMessage = it.message ?: "Could not load profile." }
        isLoading = false
    }

    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .testTag("profile_loading"),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (!userId.isNullOrBlank() && user == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = errorMessage ?: "Could not load profile.",
                modifier = Modifier.testTag("profile_status"),
            )
        }
        return
    }

    ProfilePreferencesForm(
        state = ProfileScreenState(
            displayName = user?.displayName ?: fallbackDisplayName,
            startLocation = user?.startLocation ?: fallbackStartLocation,
            preferences = preferences,
            isSaving = isSaving,
            isEditingProfile = isEditingProfile,
            statusMessage = errorMessage ?: savedMessage,
        ),
        actions = ProfileScreenActions(
            onEditProfile = { isEditingProfile = true },
            onCancelProfileEdit = { isEditingProfile = false },
            onSaveProfile = { update ->
                if (userId.isNullOrBlank()) {
                    fallbackDisplayName = update.displayName ?: fallbackDisplayName
                    fallbackStartLocation = update.startLocation ?: fallbackStartLocation
                    errorMessage = null
                    savedMessage = "Profile selected."
                    isEditingProfile = false
                } else {
                    saveUpdate(update, "Profile saved.") {
                        isEditingProfile = false
                    }
                }
            },
            onSavePreferences = { update ->
                if (userId.isNullOrBlank()) {
                    preferences = update.preferences ?: preferences
                    errorMessage = null
                    savedMessage = "Preferences selected."
                } else {
                    saveUpdate(update, "Preferences saved.")
                }
            },
        ),
        modifier = modifier,
    )
}

data class ProfileScreenState(
    val displayName: String? = null,
    val startLocation: String? = null,
    val preferences: UserPreferences,
    val isSaving: Boolean,
    val isEditingProfile: Boolean = false,
    val statusMessage: String? = null,
)

data class ProfileScreenActions(
    val onEditProfile: () -> Unit = {},
    val onCancelProfileEdit: () -> Unit = {},
    val onSaveProfile: (UserUpdate) -> Unit = {},
    val onSavePreferences: (UserUpdate) -> Unit = {},
)

@Composable
fun ProfilePreferencesForm(
    state: ProfileScreenState,
    actions: ProfileScreenActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        ProfileDetailsSection(state = state, actions = actions)
        Spacer(modifier = Modifier.height(24.dp))
        PreferencesSection(state = state, onSave = actions.onSavePreferences)
    }
}

@Composable
private fun ProfileDetailsSection(
    state: ProfileScreenState,
    actions: ProfileScreenActions,
) {
    var displayNameText by remember(state.displayName) {
        mutableStateOf(state.displayName.orEmpty())
    }
    var startLocationText by remember(state.startLocation) {
        mutableStateOf(state.startLocation.orEmpty())
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        ProfileHeader(
            isEditing = state.isEditingProfile,
            isSaving = state.isSaving,
            onEdit = actions.onEditProfile,
            onCancel = {
                displayNameText = state.displayName.orEmpty()
                startLocationText = state.startLocation.orEmpty()
                actions.onCancelProfileEdit()
            },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileImage()
            if (state.isEditingProfile) {
                ProfileEditFields(
                    displayName = displayNameText,
                    startLocation = startLocationText,
                    isSaving = state.isSaving,
                    onDisplayNameChanged = { displayNameText = it },
                    onStartLocationChanged = { startLocationText = it },
                    onSave = {
                        actions.onSaveProfile(
                            UserUpdate(
                                displayName = displayNameText,
                                startLocation = startLocationText,
                            ),
                        )
                    },
                )
            } else {
                ProfileDisplay(
                    displayName = state.displayName,
                    startLocation = state.startLocation,
                )
            }
        }
    }
}

@Composable
private fun PreferencesSection(
    state: ProfileScreenState,
    onSave: (UserUpdate) -> Unit,
) {
    var transportationMode by remember(state.preferences) {
        mutableStateOf(state.preferences.transportation.mode)
    }
    var budget by remember(state.preferences) { mutableStateOf(state.preferences.budget) }
    var interest by remember(state.preferences) { mutableStateOf(state.preferences.interest) }

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

    PreferenceSaveControls(
        statusMessage = state.statusMessage,
        isSaving = state.isSaving,
        onSave = {
            onSave(
                UserUpdate(
                    preferences = UserPreferences(
                        transportation = TransportationPreference(transportationMode),
                        budget = budget,
                        interest = interest,
                    ),
                ),
            )
        },
    )
}

@Composable
private fun PreferenceSaveControls(
    statusMessage: String?,
    isSaving: Boolean,
    onSave: () -> Unit,
) {
    statusMessage?.let {
        Text(
            text = it,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .testTag("profile_status"),
        )
    }

    Button(
        onClick = onSave,
        enabled = !isSaving,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_save"),
    ) {
        Text(if (isSaving) "Saving…" else "Save preferences")
    }
}

@Composable
private fun ProfileHeader(
    isEditing: Boolean,
    isSaving: Boolean,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Profile",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = if (isEditing) onCancel else onEdit,
            enabled = !isSaving,
            modifier = Modifier.testTag("profile_edit"),
        ) {
            Text(if (isEditing) "Cancel" else "Edit")
        }
    }
}

@Composable
private fun ProfileImage() {
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
}

@Composable
private fun RowScope.ProfileDisplay(
    displayName: String?,
    startLocation: String?,
) {
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .testTag("profile_start_location"),
        ) {
            Text(text = "Start location", style = MaterialTheme.typography.labelMedium)
            Text(
                text = startLocation?.takeIf { it.isNotBlank() } ?: "Not set",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun RowScope.ProfileEditFields(
    displayName: String,
    startLocation: String,
    isSaving: Boolean,
    onDisplayNameChanged: (String) -> Unit,
    onStartLocationChanged: (String) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(start = 16.dp),
    ) {
        OutlinedTextField(
            value = displayName,
            onValueChange = onDisplayNameChanged,
            label = { Text("Display name") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_display_name_input"),
        )
        OutlinedTextField(
            value = startLocation,
            onValueChange = onStartLocationChanged,
            label = { Text("Start location") },
            placeholder = { Text("Enter a starting location") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_start_location"),
        )
        Button(
            onClick = onSave,
            enabled = !isSaving && displayName.isNotBlank() && startLocation.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .testTag("profile_details_save"),
        ) {
            Text(if (isSaving) "Saving…" else "Save profile")
        }
    }
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
        state = ProfileScreenState(
            preferences = UserPreferences(
                transportation = TransportationPreference(TransportationMode.TRANSIT),
                budget = BudgetPreference.BALANCED,
                interest = InterestPreference.OUTDOORS,
            ),
            isSaving = false,
        ),
        actions = ProfileScreenActions(),
    )
}
