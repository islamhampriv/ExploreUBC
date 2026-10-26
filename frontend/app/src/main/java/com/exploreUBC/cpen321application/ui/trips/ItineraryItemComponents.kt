package com.exploreUBC.cpen321application.ui.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.exploreUBC.cpen321application.R
import com.exploreUBC.cpen321application.data.itineraries.ActivityItem
import com.exploreUBC.cpen321application.data.itineraries.ActivityUpdate
import com.exploreUBC.cpen321application.data.itineraries.ItineraryItem
import com.exploreUBC.cpen321application.data.itineraries.TransportationItem
import com.exploreUBC.cpen321application.data.itineraries.TransportationSegmentUpdate

data class ItineraryItemScreenModel(
    val item: ItineraryItem,
    val isReadOnly: Boolean,
    val isSaving: Boolean,
    val statusMessage: String?,
)

data class ItineraryItemScreenActions(
    val onBack: () -> Unit,
    val onUpdateActivity: (ActivityUpdate) -> Unit,
    val onUpdateTransportation: (TransportationSegmentUpdate) -> Unit,
    val onDelete: () -> Unit,
)

@Composable
internal fun ItineraryItemCard(item: ItineraryItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag("itinerary_item_${item.id}"),
    ) { ItineraryItemCardContent(item) }
}

@Composable
private fun ItineraryItemCardContent(item: ItineraryItem) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = when (item) {
                    is ActivityItem -> item.name
                    is TransportationItem -> item.mode.displayOption()
                },
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            ItemTypeLabel(item)
        }
        Text(
            "Start: ${item.startTime.toPacificDisplayDateTime()}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag("itinerary_item_start_time_${item.id}"),
        )
        Text(
            "End: ${item.endTime.toPacificDisplayDateTime()}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag("itinerary_item_end_time_${item.id}"),
        )
    }
}

@Composable
private fun ItemTypeLabel(item: ItineraryItem) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.testTag("itinerary_item_type_${item.id}"),
    ) {
        val type = if (item is ActivityItem) "Activity" else "Transportation"
        Text(type, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(8.dp, 4.dp))
    }
}

@Composable
fun ItineraryItemScreen(
    model: ItineraryItemScreenModel,
    actions: ItineraryItemScreenActions,
    modifier: Modifier = Modifier,
) {
    var isEditing by remember(model.item) { mutableStateOf(false) }
    var showDeleteConfirmation by remember(model.item.id) { mutableStateOf(false) }
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(20.dp).testTag("itinerary_item_detail"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ItemScreenHeader(model, actions.onBack, isEditing) { isEditing = !isEditing }
        ItemScreenContent(model, actions, isEditing)
        model.statusMessage?.let {
            Text(it, modifier = Modifier.testTag("itinerary_item_status"))
        }
        if (!model.isReadOnly) {
            DeleteItemButton(model.isSaving) { showDeleteConfirmation = true }
        }
    }
    if (!model.isReadOnly && showDeleteConfirmation) {
        DeleteItemDialog(
            onConfirm = { showDeleteConfirmation = false; actions.onDelete() },
            onDismiss = { showDeleteConfirmation = false },
        )
    }
}

@Composable
private fun ItemScreenHeader(
    model: ItineraryItemScreenModel,
    onBack: () -> Unit,
    isEditing: Boolean,
    onToggleEdit: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack, modifier = Modifier.testTag("itinerary_item_back")) {
            Text("Back to itinerary")
        }
        Spacer(Modifier.weight(1f))
        if (!model.isReadOnly) {
            TextButton(
                onClick = onToggleEdit,
                enabled = !model.isSaving,
                modifier = Modifier.testTag("itinerary_item_edit"),
            ) { Text(if (isEditing) "Cancel" else "Edit") }
        }
    }
}

@Composable
private fun ItemScreenContent(
    model: ItineraryItemScreenModel,
    actions: ItineraryItemScreenActions,
    isEditing: Boolean,
) {
    when (val item = model.item) {
        is ActivityItem -> if (isEditing) {
            EditActivityForm(item, model.isSaving, actions.onUpdateActivity)
        } else {
            ActivityDetails(item)
        }
        is TransportationItem -> if (isEditing) {
            EditTransportationForm(item, model.isSaving, actions.onUpdateTransportation)
        } else {
            TransportationDetails(item)
        }
    }
}

@Composable
private fun DeleteItemButton(isSaving: Boolean, onClick: () -> Unit) {
    Spacer(Modifier.height(12.dp))
    OutlinedButton(
        onClick = onClick,
        enabled = !isSaving,
        modifier = Modifier.fillMaxWidth().testTag("itinerary_item_delete"),
    ) {
        Icon(painterResource(R.drawable.delete_24px), contentDescription = "Delete item")
        Spacer(Modifier.width(8.dp))
        Text("Delete")
    }
}

@Composable
private fun DeleteItemDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete item?") },
        text = { Text("This activity or transportation segment will be permanently deleted.") },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag("confirm_item_delete")) {
                Text("Delete")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun EditActivityForm(
    activity: ActivityItem,
    isSaving: Boolean,
    onSave: (ActivityUpdate) -> Unit,
) {
    var name by remember(activity) { mutableStateOf(activity.name) }
    var image by remember(activity) { mutableStateOf(activity.image) }
    var description by remember(activity) { mutableStateOf(activity.description.orEmpty()) }
    var category by remember(activity) { mutableStateOf(activity.category.orEmpty()) }
    var location by remember(activity) { mutableStateOf(activity.location) }
    var startTime by remember(activity) { mutableStateOf(activity.startTime) }
    var endTime by remember(activity) { mutableStateOf(activity.endTime) }
    val isValid = name.isNotBlank() && name.length <= ACTIVITY_NAME_MAX_LENGTH &&
        image.length <= IMAGE_URL_MAX_LENGTH && category.isNotBlank() &&
        description.length <= DESCRIPTION_MAX_LENGTH &&
        location.isNotBlank() && location.length <= LOCATION_MAX_LENGTH &&
        timesAreOrdered(startTime, endTime)
    Text(
        "Edit activity",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )
    FormField(
        name, { name = it }, "Name", "edit_activity_name",
        required = true, maxLength = ACTIVITY_NAME_MAX_LENGTH,
    )
    FormField(
        image, { image = it }, "Image URL (optional)", "edit_activity_image",
        maxLength = IMAGE_URL_MAX_LENGTH,
    )
    FormField(
        description, { description = it }, "Description (optional)",
        "edit_activity_description", maxLength = DESCRIPTION_MAX_LENGTH,
    )
    SelectionDropdown(
        "Category", activityCategories, category, { category = it },
        "edit_activity_category", required = true,
    )
    AddressAutocompleteField(
        value = location,
        onValueChange = { location = it },
        label = "Location",
        tag = "edit_activity_location",
        required = true,
        maxLength = LOCATION_MAX_LENGTH,
    )
    DateTimePickerField(
        "Start time", startTime, { startTime = it }, "edit_activity_start_time",
        required = true,
    )
    DateTimePickerField(
        "End time", endTime, { endTime = it }, "edit_activity_end_time",
        required = true,
    )
    TimeOrderErrorMessage(hasTimeOrderError(startTime, endTime))
    Button(
        onClick = {
            onSave(ActivityUpdate(name, image, description, category, location, startTime, endTime))
        },
        enabled = isValid && !isSaving,
        modifier = Modifier.fillMaxWidth().testTag("edit_activity_save"),
    ) { Text(if (isSaving) "Saving…" else "Save") }
}

@Composable
private fun EditTransportationForm(
    segment: TransportationItem,
    isSaving: Boolean,
    onSave: (TransportationSegmentUpdate) -> Unit,
) {
    var mode by remember(segment) { mutableStateOf(segment.mode) }
    var startLocation by remember(segment) { mutableStateOf(segment.startLocation) }
    var endLocation by remember(segment) { mutableStateOf(segment.endLocation) }
    var distanceMeters by remember(segment) { mutableStateOf(segment.distanceMeters.toString()) }
    var startTime by remember(segment) { mutableStateOf(segment.startTime) }
    var endTime by remember(segment) { mutableStateOf(segment.endTime) }
    val distance = distanceMeters.toDoubleOrNull()
    val isValid = mode in transportationModes && startLocation.isNotBlank() &&
        startLocation.length <= LOCATION_MAX_LENGTH && endLocation.isNotBlank() &&
        endLocation.length <= LOCATION_MAX_LENGTH && distance != null && distance >= 0 &&
        timesAreOrdered(startTime, endTime)
    Text(
        "Edit transportation",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )
    SelectionDropdown(
        "Mode", transportationModes, mode, { mode = it }, "edit_transportation_mode",
        required = true,
    )
    AddressAutocompleteField(
        value = startLocation,
        onValueChange = { startLocation = it },
        label = "Start location",
        tag = "edit_transportation_start_location",
        required = true,
        maxLength = LOCATION_MAX_LENGTH,
    )
    AddressAutocompleteField(
        value = endLocation,
        onValueChange = { endLocation = it },
        label = "End location",
        tag = "edit_transportation_end_location",
        required = true,
        maxLength = LOCATION_MAX_LENGTH,
    )
    FormField(
        distanceMeters, { distanceMeters = it }, "Distance in meters",
        "edit_transportation_distance", required = true,
    )
    DateTimePickerField(
        "Start time", startTime, { startTime = it }, "edit_transportation_start_time",
        required = true,
    )
    DateTimePickerField(
        "End time", endTime, { endTime = it }, "edit_transportation_end_time",
        required = true,
    )
    DistanceErrorMessage(hasDistanceError(distanceMeters))
    TimeOrderErrorMessage(hasTimeOrderError(startTime, endTime))
    Button(
        onClick = {
            onSave(
                TransportationSegmentUpdate(
                    mode, startLocation, endLocation, requireNotNull(distance), startTime, endTime,
                ),
            )
        },
        enabled = isValid && !isSaving,
        modifier = Modifier.fillMaxWidth().testTag("edit_transportation_save"),
    ) { Text(if (isSaving) "Saving…" else "Save") }
}

@Composable
private fun ActivityDetails(activity: ActivityItem) {
    Text(activity.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text("Activity")
    activity.category?.takeIf(String::isNotBlank)?.let { DetailField("Category", it) }
    activity.description?.takeIf(String::isNotBlank)?.let { DetailField("Description", it) }
    DetailField("Location", activity.location)
    DetailField("Start time", activity.startTime.toPacificDisplayDateTime())
    DetailField("End time", activity.endTime.toPacificDisplayDateTime())
    DetailField("Image", activity.image)
}

@Composable
private fun TransportationDetails(segment: TransportationItem) {
    Text(
        segment.mode.displayOption(),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )
    DetailField("From", segment.startLocation)
    DetailField("To", segment.endLocation)
    DetailField("Distance", "${segment.distanceMeters.toInt()} m")
    DetailField("Start time", segment.startTime.toPacificDisplayDateTime())
    DetailField("End time", segment.endTime.toPacificDisplayDateTime())
}

@Composable
internal fun DetailField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(value)
    }
}
