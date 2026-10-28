package com.exploreUBC.cpen321application.ui.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.exploreUBC.cpen321application.data.itineraries.MapRoute
import com.exploreUBC.cpen321application.data.itineraries.TravelOption
import kotlin.math.roundToInt

@Composable
fun TravelLegCard(
    route: MapRoute,
    fromTitle: String,
    toTitle: String,
    modifier: Modifier = Modifier,
) {
    val walking = route.walking
    val driving = route.driving
    var showWalking by remember { mutableStateOf(false) }
    var showDriving by remember { mutableStateOf(false) }
    if (walking == null && driving == null) {
        return
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("travel_leg_${route.fromActivityId}"),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "$fromTitle → $toTitle",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            if (walking != null) {
                TravelModeDetails(
                    label = "Walk",
                    option = walking,
                    expanded = showWalking,
                    onToggle = { showWalking = !showWalking },
                )
            }
            if (driving != null) {
                TravelModeDetails(
                    label = "Drive",
                    option = driving,
                    expanded = showDriving,
                    onToggle = { showDriving = !showDriving },
                )
            }
        }
    }
}

@Composable
private fun TravelModeDetails(
    label: String,
    option: TravelOption,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Text(
        text = "$label ${formatTravelDuration(option.durationSeconds)}" +
            " (${formatTravelDistance(option.distanceMeters)})",
        style = MaterialTheme.typography.bodyMedium,
    )
    if (option.steps.isNotEmpty()) {
        TextButton(
            onClick = onToggle,
            modifier = Modifier.testTag("travel_${label.lowercase()}_toggle"),
        ) {
            Text(if (expanded) "Hide $label directions" else "Show $label directions")
        }
        if (expanded) {
            option.steps.forEachIndexed { index, step ->
                Text(
                    text = "${index + 1}. ${step.instruction.replace('\n', ' ')}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

fun formatTravelDuration(seconds: Double): String {
    val totalMinutes = (seconds / 60.0).roundToInt().coerceAtLeast(1)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        "${hours} hr ${minutes} min"
    } else {
        "$totalMinutes min"
    }
}

fun formatTravelDistance(meters: Double): String {
    return if (meters >= 1000) {
        val tenths = (meters / 100.0).roundToInt()
        "${tenths / 10.0} km"
    } else {
        "${meters.roundToInt()} m"
    }
}
