package com.exploreUBC.cpen321application.ui.trips

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.exploreUBC.cpen321application.data.itineraries.ActivityItem
import com.exploreUBC.cpen321application.data.itineraries.CreateActivityInput
import com.exploreUBC.cpen321application.data.itineraries.CreateTransportationInput
import com.exploreUBC.cpen321application.data.itineraries.ItinerariesRepository
import com.exploreUBC.cpen321application.data.itineraries.Itinerary
import com.exploreUBC.cpen321application.data.itineraries.ItineraryDetails
import com.exploreUBC.cpen321application.data.itineraries.ItineraryItem
import com.exploreUBC.cpen321application.data.itineraries.TransportationItem
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val activityCategories = listOf(
    "",
    "food",
    "art",
    "outdoors",
    "markets",
    "live_music",
    "gaming",
    "museum",
    "other",
)

private val transportationModes = listOf(
    "walking",
    "cycling",
    "transit",
    "driving",
    "other",
)

private val pacificTimeZone = ZoneId.of("America/Vancouver")
private val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a z")

private enum class NewItemType {
    ACTIVITY,
    TRANSPORTATION,
}

@Composable
fun TripsRoute(
    userId: String?,
    itinerariesRepository: ItinerariesRepository,
    modifier: Modifier = Modifier,
) {
    var savedItineraries by remember(userId) { mutableStateOf<List<Itinerary>>(emptyList()) }
    var recommendedItineraries by remember(userId) { mutableStateOf<List<Itinerary>>(emptyList()) }
    var selectedItinerary by remember(userId) { mutableStateOf<Itinerary?>(null) }
    var selectedItem by remember(userId) { mutableStateOf<ItineraryItem?>(null) }
    var newItemType by remember(userId) { mutableStateOf<NewItemType?>(null) }
    var isLoading by remember(userId) { mutableStateOf(false) }
    var isSaving by remember(userId) { mutableStateOf(false) }
    var statusMessage by remember(userId) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(userId, itinerariesRepository) {
        if (userId.isNullOrBlank()) return@LaunchedEffect
        isLoading = true
        statusMessage = null
        runCatching {
            coroutineScope {
                val saved = async {
                    itinerariesRepository.getItineraries(userId, saved = true)
                }
                val recommended = async {
                    itinerariesRepository.getItineraries(
                        userId,
                        inRecommendation = true,
                    )
                }
                saved.await() to recommended.await()
            }
        }.onSuccess { (saved, recommended) ->
            savedItineraries = saved
            recommendedItineraries = recommended
        }.onFailure {
            statusMessage = it.message ?: "Could not load itineraries."
        }
        isLoading = false
    }

    val itinerary = selectedItinerary
    val item = selectedItem
    val itemType = newItemType
    if (itinerary == null) {
        TripsList(
            savedItineraries = savedItineraries,
            recommendedItineraries = recommendedItineraries,
            isLoading = isLoading,
            statusMessage = if (userId.isNullOrBlank()) {
                "A user ID is required to load itineraries."
            } else {
                statusMessage
            },
            onItineraryClick = { selected ->
                if (userId.isNullOrBlank()) return@TripsList
                scope.launch {
                    isLoading = true
                    statusMessage = null
                    runCatching {
                        itinerariesRepository.getItinerary(userId, selected.id)
                    }.onSuccess {
                        selectedItinerary = it
                    }.onFailure {
                        statusMessage = it.message ?: "Could not load the itinerary."
                    }
                    isLoading = false
                }
            },
            modifier = modifier,
        )
    } else if (item != null) {
        ItineraryItemScreen(
            item = item,
            onBack = { selectedItem = null },
            modifier = modifier,
        )
    } else if (itemType != null) {
        NewItineraryItemScreen(
            itemType = itemType,
            isSaving = isSaving,
            statusMessage = statusMessage,
            onBack = {
                newItemType = null
                statusMessage = null
            },
            onAddActivity = { activity ->
                if (userId.isNullOrBlank()) return@NewItineraryItemScreen
                scope.launch {
                    isSaving = true
                    statusMessage = null
                    runCatching {
                        itinerariesRepository.addActivity(
                            userId,
                            itinerary.id,
                            activity,
                        )
                    }.onSuccess { addedItem ->
                        val updated = itinerary.copy(items = itinerary.items + addedItem)
                        selectedItinerary = updated
                        savedItineraries = savedItineraries.replace(updated)
                        recommendedItineraries = recommendedItineraries.replace(updated)
                        newItemType = null
                        statusMessage = "Activity added."
                    }.onFailure {
                        statusMessage = it.message ?: "Could not add the activity."
                    }
                    isSaving = false
                }
            },
            onAddTransportation = { segment ->
                if (userId.isNullOrBlank()) return@NewItineraryItemScreen
                scope.launch {
                    isSaving = true
                    statusMessage = null
                    runCatching {
                        itinerariesRepository.addTransportationSegment(
                            userId,
                            itinerary.id,
                            segment,
                        )
                    }.onSuccess { addedItem ->
                        val updated = itinerary.copy(items = itinerary.items + addedItem)
                        selectedItinerary = updated
                        savedItineraries = savedItineraries.replace(updated)
                        recommendedItineraries = recommendedItineraries.replace(updated)
                        newItemType = null
                        statusMessage = "Transportation added."
                    }.onFailure {
                        statusMessage = it.message ?: "Could not add transportation."
                    }
                    isSaving = false
                }
            },
            modifier = modifier,
        )
    } else {
        ItineraryScreen(
            itinerary = itinerary,
            isSaving = isSaving,
            statusMessage = statusMessage,
            onBack = {
                selectedItem = null
                newItemType = null
                selectedItinerary = null
                statusMessage = null
            },
            onItemClick = { selectedItem = it },
            onAddActivity = {
                statusMessage = null
                newItemType = NewItemType.ACTIVITY
            },
            onAddTransportation = {
                statusMessage = null
                newItemType = NewItemType.TRANSPORTATION
            },
            onSave = { details ->
                if (userId.isNullOrBlank()) return@ItineraryScreen
                scope.launch {
                    isSaving = true
                    statusMessage = null
                    runCatching {
                        itinerariesRepository.updateItineraryDetails(
                            userId,
                            itinerary.id,
                            details,
                        )
                    }.onSuccess { updated ->
                        selectedItinerary = updated
                        savedItineraries = savedItineraries.replace(updated)
                        recommendedItineraries = recommendedItineraries.replace(updated)
                        statusMessage = "Itinerary details saved."
                    }.onFailure {
                        statusMessage = it.message ?: "Could not save itinerary details."
                    }
                    isSaving = false
                }
            },
            modifier = modifier,
        )
    }
}

private fun List<Itinerary>.replace(updated: Itinerary): List<Itinerary> =
    map { if (it.id == updated.id) updated else it }

@Composable
fun TripsList(
    savedItineraries: List<Itinerary>,
    recommendedItineraries: List<Itinerary>,
    isLoading: Boolean,
    statusMessage: String?,
    onItineraryClick: (Itinerary) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                text = "Trips",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 20.dp),
            )
        }
        if (isLoading) {
            item { CircularProgressIndicator(modifier = Modifier.testTag("trips_loading")) }
        }
        statusMessage?.let { message ->
            item { Text(message, modifier = Modifier.testTag("trips_status")) }
        }
        itinerarySection(
            title = "Saved Itineraries",
            emptyMessage = "No saved itineraries.",
            itineraries = savedItineraries,
            tagPrefix = "saved",
            onItineraryClick = onItineraryClick,
        )
        itinerarySection(
            title = "Recommended Itineraries",
            emptyMessage = "No recommended itineraries.",
            itineraries = recommendedItineraries,
            tagPrefix = "recommended",
            onItineraryClick = onItineraryClick,
        )
        item { Spacer(Modifier.height(16.dp)) }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.itinerarySection(
    title: String,
    emptyMessage: String,
    itineraries: List<Itinerary>,
    tagPrefix: String,
    onItineraryClick: (Itinerary) -> Unit,
) {
    item {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
    if (itineraries.isEmpty()) {
        item { Text(emptyMessage) }
    } else {
        items(itineraries, key = { "$tagPrefix-${it.id}" }) { itinerary ->
            Card(
                onClick = { onItineraryClick(itinerary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${tagPrefix}_itinerary_${itinerary.id}"),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(itinerary.name, fontWeight = FontWeight.Bold)
                    itinerary.description?.takeIf { it.isNotBlank() }?.let { Text(it) }
                    Text("${itinerary.items.size} items")
                }
            }
        }
    }
}

@Composable
fun ItineraryScreen(
    itinerary: Itinerary,
    isSaving: Boolean,
    statusMessage: String?,
    onBack: () -> Unit,
    onItemClick: (ItineraryItem) -> Unit,
    onAddActivity: () -> Unit,
    onAddTransportation: () -> Unit,
    onSave: (ItineraryDetails) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember(itinerary.id, itinerary.updatedAt) { mutableStateOf(itinerary.name) }
    var description by remember(itinerary.id, itinerary.updatedAt) {
        mutableStateOf(itinerary.description.orEmpty())
    }
    var startingLocation by remember(itinerary.id, itinerary.updatedAt) {
        mutableStateOf(itinerary.startingLocation.orEmpty())
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack, modifier = Modifier.testTag("itinerary_back")) {
            Text("Back to itineraries")
        }
        Text(
            text = "Itinerary details",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("itinerary_name"),
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth().testTag("itinerary_description"),
        )
        OutlinedTextField(
            value = startingLocation,
            onValueChange = { startingLocation = it },
            label = { Text("Starting location") },
            modifier = Modifier.fillMaxWidth().testTag("itinerary_starting_location"),
        )
        Button(
            onClick = {
                onSave(
                    ItineraryDetails(
                        name = name,
                        description = description,
                        startingLocation = startingLocation,
                    ),
                )
            },
            enabled = !isSaving && name.isNotBlank(),
            modifier = Modifier.fillMaxWidth().testTag("itinerary_save"),
        ) {
            Text(if (isSaving) "Saving…" else "Save details")
        }
        statusMessage?.let { Text(it, modifier = Modifier.testTag("itinerary_status")) }

        Text(
            text = "Activities and transportation",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (itinerary.items.isEmpty()) {
            Text("No activities or transportation.")
        }
        itinerary.items.forEach { item ->
            ItineraryItemCard(
                item = item,
                onClick = { onItemClick(item) },
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onAddActivity,
                enabled = itinerary.saved,
                modifier = Modifier
                    .weight(1f)
                    .testTag("itinerary_add_activity"),
            ) {
                Text("+ Activity")
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = onAddTransportation,
                enabled = itinerary.saved,
                modifier = Modifier
                    .weight(1f)
                    .testTag("itinerary_add_transportation"),
            ) {
                Text("+ Transportation")
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun NewItineraryItemScreen(
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
            isSaving = isSaving,
            statusMessage = statusMessage,
            onBack = onBack,
            onCreate = onAddActivity,
            modifier = modifier,
        )
        NewItemType.TRANSPORTATION -> NewTransportationScreen(
            isSaving = isSaving,
            statusMessage = statusMessage,
            onBack = onBack,
            onCreate = onAddTransportation,
            modifier = modifier,
        )
    }
}

@Composable
private fun NewActivityScreen(
    isSaving: Boolean,
    statusMessage: String?,
    onBack: () -> Unit,
    onCreate: (CreateActivityInput) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf("") }
    var image by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    val isValid = name.isNotBlank() && image.isNotBlank() &&
        location.isNotBlank() && startTime.isNotBlank() && endTime.isNotBlank()

    ItemFormPage(
        title = "New activity",
        isSaving = isSaving,
        isValid = isValid,
        statusMessage = statusMessage,
        onBack = onBack,
        onCreate = {
            onCreate(
                CreateActivityInput(
                    name = name,
                    image = image,
                    description = description.takeIf { it.isNotBlank() },
                    category = category.takeIf { it.isNotBlank() },
                    location = location,
                    startTime = startTime,
                    endTime = endTime,
                ),
            )
        },
        modifier = modifier,
    ) {
        FormField(name, { name = it }, "Name", "new_activity_name")
        FormField(image, { image = it }, "Image URL", "new_activity_image")
        FormField(description, { description = it }, "Description (optional)", "new_activity_description")
        SelectionDropdown(
            label = "Category (optional)",
            options = activityCategories,
            selected = category,
            onSelected = { category = it },
            tag = "new_activity_category",
        )
        FormField(location, { location = it }, "Location", "new_activity_location")
        DateTimePickerField(
            label = "Start time",
            value = startTime,
            onValueChange = { startTime = it },
            tag = "new_activity_start_time",
        )
        DateTimePickerField(
            label = "End time",
            value = endTime,
            onValueChange = { endTime = it },
            tag = "new_activity_end_time",
        )
    }
}

@Composable
private fun NewTransportationScreen(
    isSaving: Boolean,
    statusMessage: String?,
    onBack: () -> Unit,
    onCreate: (CreateTransportationInput) -> Unit,
    modifier: Modifier = Modifier,
) {
    var mode by remember { mutableStateOf("") }
    var startLocation by remember { mutableStateOf("") }
    var endLocation by remember { mutableStateOf("") }
    var distanceMeters by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    val parsedDistance = distanceMeters.toDoubleOrNull()
    val isValid = mode in transportationModes && startLocation.isNotBlank() &&
        endLocation.isNotBlank() && parsedDistance != null && parsedDistance >= 0 &&
        startTime.isNotBlank() && endTime.isNotBlank()

    ItemFormPage(
        title = "New transportation",
        isSaving = isSaving,
        isValid = isValid,
        statusMessage = statusMessage,
        onBack = onBack,
        onCreate = {
            onCreate(
                CreateTransportationInput(
                    mode = mode,
                    startLocation = startLocation,
                    endLocation = endLocation,
                    distanceMeters = requireNotNull(parsedDistance),
                    startTime = startTime,
                    endTime = endTime,
                ),
            )
        },
        modifier = modifier,
    ) {
        SelectionDropdown(
            label = "Mode",
            options = transportationModes,
            selected = mode,
            onSelected = { mode = it },
            tag = "new_transportation_mode",
        )
        FormField(startLocation, { startLocation = it }, "Start location", "new_transportation_start_location")
        FormField(endLocation, { endLocation = it }, "End location", "new_transportation_end_location")
        FormField(distanceMeters, { distanceMeters = it }, "Distance in meters", "new_transportation_distance")
        DateTimePickerField(
            label = "Start time",
            value = startTime,
            onValueChange = { startTime = it },
            tag = "new_transportation_start_time",
        )
        DateTimePickerField(
            label = "End time",
            value = endTime,
            onValueChange = { endTime = it },
            tag = "new_transportation_end_time",
        )
    }
}

@Composable
private fun ItemFormPage(
    title: String,
    isSaving: Boolean,
    isValid: Boolean,
    statusMessage: String?,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
    fields: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack, modifier = Modifier.testTag("new_item_back")) {
            Text("Back to itinerary")
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        fields()
        statusMessage?.let { Text(it, modifier = Modifier.testTag("new_item_status")) }
        Button(
            onClick = onCreate,
            enabled = isValid && !isSaving,
            modifier = Modifier.fillMaxWidth().testTag("new_item_create"),
        ) {
            Text(if (isSaving) "Adding…" else "Add")
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    tag: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().testTag(tag),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    tag: String,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            value = selected.displayOption(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text("Select") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .testTag(tag),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(if (option.isEmpty()) "None" else option.displayOption())
                    },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun String.displayOption(): String =
    replace('_', ' ').replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimePickerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    tag: String,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }

    OutlinedButton(
        onClick = { showDatePicker = true },
        modifier = Modifier.fillMaxWidth().testTag(tag),
    ) {
        Text(
            if (value.isBlank()) "$label: Select date and time"
            else "$label: ${value.toPacificDisplayDateTime()}",
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = value.toDatePickerMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedDateMillis = datePickerState.selectedDateMillis
                        showDatePicker = false
                        if (selectedDateMillis != null) showTimePicker = true
                    },
                    enabled = datePickerState.selectedDateMillis != null,
                ) {
                    Text("Next")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val currentTime = value.toPacificDateTime()
        val timePickerState = rememberTimePickerState(
            initialHour = currentTime?.hour ?: 12,
            initialMinute = currentTime?.minute ?: 0,
            is24Hour = false,
        )
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(shape = MaterialTheme.shapes.large) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Select time", style = MaterialTheme.typography.titleLarge)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        TimePicker(state = timePickerState)
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("Cancel")
                        }
                        Spacer(Modifier.weight(1f))
                        TextButton(
                            onClick = {
                                val dateMillis = selectedDateMillis
                                if (dateMillis != null) {
                                    val date = Instant.ofEpochMilli(dateMillis)
                                        .atZone(ZoneOffset.UTC)
                                        .toLocalDate()
                                    onValueChange(
                                        date.atTime(
                                            timePickerState.hour,
                                            timePickerState.minute,
                                        ).atZone(pacificTimeZone)
                                            .toInstant()
                                            .toString(),
                                    )
                                }
                                showTimePicker = false
                            },
                        ) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
}

private fun String.toPacificDateTime() = runCatching {
    Instant.parse(this).atZone(pacificTimeZone)
}.getOrNull()

private fun String.toDatePickerMillis(): Long? = toPacificDateTime()
    ?.toLocalDate()
    ?.atStartOfDay(ZoneOffset.UTC)
    ?.toInstant()
    ?.toEpochMilli()

private fun String.toPacificDisplayDateTime(): String =
    toPacificDateTime()?.format(dateTimeFormatter) ?: this

@Composable
private fun ItineraryItemCard(
    item: ItineraryItem,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("itinerary_item_${item.id}"),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            when (item) {
                is ActivityItem -> {
                    Text(item.name, fontWeight = FontWeight.Bold)
                    Text("Activity")
                }
                is TransportationItem -> {
                    Text(item.mode.replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold)
                    Text("Transportation")
                }
            }
        }
    }
}

@Composable
fun ItineraryItemScreen(
    item: ItineraryItem,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("itinerary_item_detail"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(
            onClick = onBack,
            modifier = Modifier.testTag("itinerary_item_back"),
        ) {
            Text("Back to itinerary")
        }

        when (item) {
            is ActivityItem -> ActivityDetails(item)
            is TransportationItem -> TransportationDetails(item)
        }
    }
}

@Composable
private fun ActivityDetails(activity: ActivityItem) {
    Text(
        text = activity.name,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )
    Text("Activity")
    activity.category?.takeIf { it.isNotBlank() }?.let {
        DetailField("Category", it)
    }
    activity.description?.takeIf { it.isNotBlank() }?.let {
        DetailField("Description", it)
    }
    DetailField("Location", activity.location)
    DetailField("Start time", activity.startTime.toPacificDisplayDateTime())
    DetailField("End time", activity.endTime.toPacificDisplayDateTime())
    DetailField("Image", activity.image)
}

@Composable
private fun TransportationDetails(segment: TransportationItem) {
    Text(
        text = "${segment.mode.replaceFirstChar { it.uppercase() }} transportation",
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
private fun DetailField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(value)
    }
}
