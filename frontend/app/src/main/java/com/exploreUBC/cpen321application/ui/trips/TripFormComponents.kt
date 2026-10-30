package com.exploreUBC.cpen321application.ui.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.exploreUBC.cpen321application.data.itineraries.CreateActivityInput
import com.exploreUBC.cpen321application.data.itineraries.CreateTransportationInput
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

internal val activityCategories = listOf(
    "food", "art", "outdoors", "markets", "live_music", "gaming", "museum", "other",
)
internal val transportationModes = listOf("walking", "cycling", "transit", "driving", "other")
private val pacificTimeZone = ZoneId.of("America/Vancouver")
private val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a z")
internal const val ITINERARY_NAME_MAX_LENGTH = 100
internal const val ACTIVITY_NAME_MAX_LENGTH = 200
internal const val IMAGE_URL_MAX_LENGTH = 2_000
internal const val DESCRIPTION_MAX_LENGTH = 1_000
internal const val LOCATION_MAX_LENGTH = 500

private data class ItemFormModel(
    val title: String,
    val isSaving: Boolean,
    val isValid: Boolean,
    val hasTimeOrderError: Boolean,
    val hasDistanceError: Boolean,
    val statusMessage: String?,
)

private data class ItemFormActions(
    val onBack: () -> Unit,
    val onCreate: () -> Unit,
)

@Composable
internal fun NewItineraryItemScreen(
    itemType: NewItemType,
    isSaving: Boolean,
    statusMessage: String?,
    onBack: () -> Unit,
    onAddActivity: (CreateActivityInput) -> Unit,
    onAddTransportation: (CreateTransportationInput) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (itemType) {
        NewItemType.ACTIVITY -> NewActivityScreen(
            isSaving, statusMessage, onBack, onAddActivity, modifier,
        )
        NewItemType.TRANSPORTATION -> NewTransportationScreen(
            isSaving, statusMessage, onBack, onAddTransportation, modifier,
        )
    }
}

@Composable
private fun NewActivityScreen(
    isSaving: Boolean,
    statusMessage: String?,
    onBack: () -> Unit,
    onCreate: (CreateActivityInput) -> Unit,
    modifier: Modifier,
) {
    var name by remember { mutableStateOf("") }
    var image by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    val showTimeOrderError = hasTimeOrderError(startTime, endTime)
    val isValid = name.isNotBlank() && name.length <= ACTIVITY_NAME_MAX_LENGTH &&
        image.length <= IMAGE_URL_MAX_LENGTH && category.isNotBlank() &&
        description.length <= DESCRIPTION_MAX_LENGTH &&
        location.isNotBlank() && location.length <= LOCATION_MAX_LENGTH &&
        timesAreOrdered(startTime, endTime)
    val create = {
        onCreate(
            CreateActivityInput(
                name,
                image,
                description.takeIf(String::isNotBlank),
                category.takeIf(String::isNotBlank),
                location,
                startTime,
                endTime,
            ),
        )
    }

    ItemFormPage(
        ItemFormModel(
            "New activity",
            isSaving,
            isValid,
            showTimeOrderError,
            false,
            statusMessage,
        ),
        ItemFormActions(onBack, create),
        modifier,
    ) {
        FormField(
            name, { name = it }, "Name", "new_activity_name",
            required = true, maxLength = ACTIVITY_NAME_MAX_LENGTH,
        )
        FormField(
            image, { image = it }, "Image URL (optional)", "new_activity_image",
            maxLength = IMAGE_URL_MAX_LENGTH,
        )
        FormField(
            description, { description = it }, "Description (optional)",
            "new_activity_description", maxLength = DESCRIPTION_MAX_LENGTH,
        )
        SelectionDropdown(
            "Category",
            activityCategories,
            category,
            { category = it },
            "new_activity_category",
            required = true,
        )
        AddressAutocompleteField(
            value = location,
            onValueChange = { location = it },
            label = "Location",
            tag = "new_activity_location",
            options = AddressAutocompleteOptions(
                required = true,
                maxLength = LOCATION_MAX_LENGTH,
            ),
        )
        DateTimePickerField(
            "Start time", startTime, { startTime = it }, "new_activity_start_time",
            required = true,
        )
        DateTimePickerField(
            "End time", endTime, { endTime = it }, "new_activity_end_time",
            required = true,
        )
    }
}

@Composable
private fun NewTransportationScreen(
    isSaving: Boolean,
    statusMessage: String?,
    onBack: () -> Unit,
    onCreate: (CreateTransportationInput) -> Unit,
    modifier: Modifier,
) {
    var mode by remember { mutableStateOf("") }
    var startLocation by remember { mutableStateOf("") }
    var endLocation by remember { mutableStateOf("") }
    var distanceMeters by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    val distance = distanceMeters.toDoubleOrNull()
    val showTimeOrderError = hasTimeOrderError(startTime, endTime)
    val showDistanceError = hasDistanceError(distanceMeters)
    val isValid = mode in transportationModes && startLocation.isNotBlank() &&
        startLocation.length <= LOCATION_MAX_LENGTH && endLocation.isNotBlank() &&
        endLocation.length <= LOCATION_MAX_LENGTH && distance != null && distance >= 0 &&
        timesAreOrdered(startTime, endTime)
    val create = {
        onCreate(
            CreateTransportationInput(
                mode,
                startLocation,
                endLocation,
                requireNotNull(distance),
                startTime,
                endTime,
            ),
        )
    }

    ItemFormPage(
        ItemFormModel(
            "New transportation",
            isSaving,
            isValid,
            showTimeOrderError,
            showDistanceError,
            statusMessage,
        ),
        ItemFormActions(onBack, create),
        modifier,
    ) {
        SelectionDropdown(
            "Mode", transportationModes, mode, { mode = it }, "new_transportation_mode",
            required = true,
        )
        AddressAutocompleteField(
            value = startLocation,
            onValueChange = { startLocation = it },
            label = "Start location",
            tag = "new_transportation_start_location",
            options = AddressAutocompleteOptions(
                required = true,
                maxLength = LOCATION_MAX_LENGTH,
            ),
        )
        AddressAutocompleteField(
            value = endLocation,
            onValueChange = { endLocation = it },
            label = "End location",
            tag = "new_transportation_end_location",
            options = AddressAutocompleteOptions(
                required = true,
                maxLength = LOCATION_MAX_LENGTH,
            ),
        )
        FormField(
            distanceMeters, { distanceMeters = it }, "Distance in meters",
            "new_transportation_distance", required = true,
        )
        DateTimePickerField(
            "Start time", startTime, { startTime = it }, "new_transportation_start_time",
            required = true,
        )
        DateTimePickerField(
            "End time", endTime, { endTime = it }, "new_transportation_end_time",
            required = true,
        )
    }
}

@Composable
private fun ItemFormPage(
    model: ItemFormModel,
    actions: ItemFormActions,
    modifier: Modifier = Modifier,
    fields: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = actions.onBack, modifier = Modifier.testTag("new_item_back")) {
            Text("Back to itinerary")
        }
        Text(
            model.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        fields()
        model.statusMessage?.let { Text(it, modifier = Modifier.testTag("new_item_status")) }
        DistanceErrorMessage(model.hasDistanceError)
        TimeOrderErrorMessage(model.hasTimeOrderError)
        Button(
            onClick = actions.onCreate,
            enabled = model.isValid && !model.isSaving,
            modifier = Modifier.fillMaxWidth().testTag("new_item_create"),
        ) { Text(if (model.isSaving) "Adding…" else "Add") }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
internal fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    tag: String,
    required: Boolean = false,
    maxLength: Int? = null,
) {
    val exceedsMaxLength = maxLength != null && value.length > maxLength
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(if (required) "$label *" else label) },
            isError = exceedsMaxLength,
            modifier = Modifier.fillMaxWidth().testTag(tag),
        )
        if (exceedsMaxLength) {
            Text(
                text = "${label.substringBefore(" (optional)")} must be at most $maxLength characters.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("${tag}_length_error"),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectionDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    tag: String,
    required: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected.displayOption(),
            onValueChange = {},
            readOnly = true,
            label = { Text(if (required) "$label *" else label) },
            placeholder = { Text("Select") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable).testTag(tag),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(if (option.isEmpty()) "None" else option.displayOption()) },
                    onClick = { onSelected(option); expanded = false },
                )
            }
        }
    }
}

internal fun String.displayOption(): String =
    replace('_', ' ').replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

internal fun timesAreOrdered(startTime: String, endTime: String): Boolean =
    runCatching { Instant.parse(startTime).isBefore(Instant.parse(endTime)) }.getOrDefault(false)

internal fun hasTimeOrderError(startTime: String, endTime: String): Boolean =
    startTime.isNotBlank() && endTime.isNotBlank() && !timesAreOrdered(startTime, endTime)

internal fun hasDistanceError(distance: String): Boolean =
    distance.isNotBlank() && distance.toDoubleOrNull() == null

@Composable
internal fun DistanceErrorMessage(isVisible: Boolean) {
    if (isVisible) {
        Text(
            text = "Distance must be a number.",
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.testTag("distance_error"),
        )
    }
}

@Composable
internal fun TimeOrderErrorMessage(isVisible: Boolean) {
    if (isVisible) {
        Text(
            text = "End time must be after start time.",
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.testTag("time_order_error"),
        )
    }
}

@Composable
internal fun DateTimePickerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    tag: String,
    required: Boolean = false,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    OutlinedButton(
        onClick = { showDatePicker = true },
        modifier = Modifier.fillMaxWidth().testTag(tag),
    ) {
        val display = if (value.isBlank()) "Select date and time" else value.toPacificDisplayDateTime()
        Text("$label${if (required) " *" else ""}: $display")
    }
    if (showDatePicker) {
        DateSelectionDialog(value, { showDatePicker = false }) {
            selectedDateMillis = it
            showDatePicker = false
        }
    }
    selectedDateMillis?.let { dateMillis ->
        TimeSelectionDialog(value, dateMillis, onValueChange) { selectedDateMillis = null }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSelectionDialog(
    value: String,
    onDismiss: () -> Unit,
    onSelected: (Long) -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = value.toDatePickerMillis() ?: System.currentTimeMillis(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let(onSelected) },
                enabled = state.selectedDateMillis != null,
            ) { Text("Next") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) { DatePicker(state = state) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeSelectionDialog(
    value: String,
    dateMillis: Long,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val currentTime = value.toPacificDateTime()
    val state = rememberTimePickerState(
        initialHour = currentTime?.hour ?: 12,
        initialMinute = currentTime?.minute ?: 0,
        is24Hour = false,
    )
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Select time", style = MaterialTheme.typography.titleLarge)
                Box(modifier = Modifier.fillMaxWidth()) { TimePicker(state = state) }
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = {
                        onValueChange(dateMillis.toIsoTime(state.hour, state.minute))
                        onDismiss()
                    }) { Text("OK") }
                }
            }
        }
    }
}

private fun Long.toIsoTime(hour: Int, minute: Int): String =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
        .atTime(hour, minute).atZone(pacificTimeZone).toInstant().toString()

private fun String.toPacificDateTime() = runCatching {
    Instant.parse(this).atZone(pacificTimeZone)
}.getOrNull()

private fun String.toDatePickerMillis(): Long? = toPacificDateTime()?.toLocalDate()
    ?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()

internal fun String.toPacificDisplayDateTime(): String =
    toPacificDateTime()?.format(dateTimeFormatter) ?: this
