package com.exploreUBC.cpen321application.ui.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.exploreUBC.cpen321application.R
import com.exploreUBC.cpen321application.data.itineraries.ItineraryMap
import com.exploreUBC.cpen321application.data.itineraries.MapRoute
import com.exploreUBC.cpen321application.data.itineraries.TravelOption
import com.exploreUBC.cpen321application.ui.trips.formatTravelDistance
import com.exploreUBC.cpen321application.ui.trips.formatTravelDuration

@Composable
fun TripTravelOverlay(
    itineraryMap: ItineraryMap,
    travelMode: TripTravelMode,
    selectedRouteKey: String?,
    onTravelModeChange: (TripTravelMode) -> Unit,
    onRouteSelected: (String) -> Unit,
    onHideMap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDirections by remember(itineraryMap.itineraryId) { mutableStateOf(true) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TravelModeToggleRow(travelMode, onTravelModeChange)
                DirectionsVisibilityButton(
                    visible = showDirections,
                    onToggle = { showDirections = !showDirections },
                )
                if (showDirections) {
                    SelectableTravelDirections(
                        itineraryMap = itineraryMap,
                        travelMode = travelMode,
                        selectedRouteKey = selectedRouteKey,
                        onRouteSelected = onRouteSelected,
                    )
                }
            }
        }
        Button(
            onClick = onHideMap,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("itinerary_hide_map"),
        ) {
            Text(stringResource(R.string.map_hide))
        }
    }
}

@Composable
private fun TravelModeToggleRow(
    travelMode: TripTravelMode,
    onTravelModeChange: (TripTravelMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TravelModeChoice(
            label = stringResource(R.string.map_travel_walk),
            selected = travelMode == TripTravelMode.WALKING,
            onClick = { onTravelModeChange(TripTravelMode.WALKING) },
            modifier = Modifier
                .weight(1f)
                .testTag("map_travel_walk"),
        )
        TravelModeChoice(
            label = stringResource(R.string.map_travel_drive),
            selected = travelMode == TripTravelMode.DRIVING,
            onClick = { onTravelModeChange(TripTravelMode.DRIVING) },
            modifier = Modifier
                .weight(1f)
                .testTag("map_travel_drive"),
        )
    }
}

@Composable
private fun TravelModeChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(if (selected) 3.dp else 1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Text(
            text = label,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun DirectionsVisibilityButton(
    visible: Boolean,
    onToggle: () -> Unit,
) {
    val label = if (visible) {
        stringResource(R.string.map_hide_directions)
    } else {
        stringResource(R.string.map_show_directions)
    }
    OutlinedButton(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("map_directions_visibility"),
    ) {
        Text(label)
    }
}

@Composable
private fun SelectableTravelDirections(
    itineraryMap: ItineraryMap,
    travelMode: TripTravelMode,
    selectedRouteKey: String?,
    onRouteSelected: (String) -> Unit,
) {
    if (itineraryMap.routes.isEmpty()) {
        Text(
            text = stringResource(R.string.map_routes_empty),
            style = MaterialTheme.typography.bodySmall,
        )
        return
    }
    RouteSelectionRow(
        itineraryMap = itineraryMap,
        selectedRouteKey = selectedRouteKey,
        onRouteSelected = onRouteSelected,
    )
    val selectedRoute = itineraryMap.routes.firstOrNull {
        it.routeKey() == selectedRouteKey
    } ?: itineraryMap.routes.first()
    val modeLabel = if (travelMode == TripTravelMode.WALKING) {
        stringResource(R.string.map_travel_walk)
    } else {
        stringResource(R.string.map_travel_drive)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 180.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TravelLegDirections(
            route = selectedRoute,
            fromTitle = activityTitle(itineraryMap, selectedRoute.fromActivityId),
            toTitle = activityTitle(itineraryMap, selectedRoute.toActivityId),
            option = selectedOption(selectedRoute, travelMode),
            modeLabel = modeLabel,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RouteSelectionRow(
    itineraryMap: ItineraryMap,
    selectedRouteKey: String?,
    onRouteSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedIndex = itineraryMap.routes.indexOfFirst {
        it.routeKey() == selectedRouteKey
    }.takeIf { it >= 0 } ?: 0
    val selectedRoute = itineraryMap.routes[selectedIndex]
    val selectedLabel = routeLabel(itineraryMap, selectedIndex, selectedRoute)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .testTag("map_route_dropdown"),
            maxLines = 2,
            textStyle = MaterialTheme.typography.bodyMedium,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            itineraryMap.routes.forEachIndexed { index, route ->
                val key = route.routeKey()
                DropdownMenuItem(
                    text = {
                        Text(
                            text = routeLabel(itineraryMap, index, route),
                            fontWeight = if (key == selectedRouteKey) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    onClick = {
                        onRouteSelected(key)
                        expanded = false
                    },
                    modifier = Modifier.testTag("map_route_$index"),
                )
            }
        }
    }
}

private fun routeLabel(itineraryMap: ItineraryMap, index: Int, route: MapRoute): String =
    "${index + 1}. ${activityTitle(itineraryMap, route.fromActivityId)} → " +
        activityTitle(itineraryMap, route.toActivityId)

@Composable
private fun TravelLegDirections(
    route: MapRoute,
    fromTitle: String,
    toTitle: String,
    option: TravelOption?,
    modeLabel: String,
) {
    Text(
        text = "$fromTitle → $toTitle",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.testTag("map_travel_leg_${route.fromActivityId}"),
    )
    if (option == null) {
        Text(
            text = stringResource(R.string.map_no_travel_option, modeLabel.lowercase()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Text(
            text = "$modeLabel ${formatTravelDuration(option.durationSeconds)}" +
                " (${formatTravelDistance(option.distanceMeters)})",
            style = MaterialTheme.typography.bodyMedium,
        )
        option.steps.forEachIndexed { index, step ->
            Text(
                text = "${index + 1}. ${step.instruction.replace('\n', ' ')}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun selectedOption(route: MapRoute, travelMode: TripTravelMode): TravelOption? =
    when (travelMode) {
        TripTravelMode.WALKING -> route.walking
        TripTravelMode.DRIVING -> route.driving
    }

internal fun MapRoute.routeKey(): String = "$fromActivityId:$toActivityId"

private fun activityTitle(itineraryMap: ItineraryMap, activityId: String): String =
    itineraryMap.markers.firstOrNull { marker -> marker.activityId == activityId }?.title
        ?: "Activity"
