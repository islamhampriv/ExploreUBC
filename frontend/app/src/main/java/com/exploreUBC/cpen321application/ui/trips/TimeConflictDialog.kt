package com.exploreUBC.cpen321application.ui.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.exploreUBC.cpen321application.data.itineraries.ItineraryTimeConflict
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun TimeConflictDialog(
    conflicts: List<ItineraryTimeConflict>,
    isWorking: Boolean,
    onKeepExisting: () -> Unit,
    onReplace: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onKeepExisting,
        title = { Text("Schedule conflict") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("The new item overlaps with:")
                conflicts.forEach { conflict ->
                    Text(
                        "${conflict.name}: " +
                            "${formatConflictTime(conflict.startTime)} to " +
                            formatConflictTime(conflict.endTime),
                    )
                }
                Text("Replacing will remove all listed items.")
            }
        },
        confirmButton = {
            TextButton(onClick = onReplace, enabled = !isWorking) {
                Text(if (isWorking) "Replacing..." else "Replace with new item")
            }
        },
        dismissButton = {
            TextButton(onClick = onKeepExisting, enabled = !isWorking) {
                Text("Keep existing schedule")
            }
        },
    )
}

private fun formatConflictTime(value: String): String = runCatching {
    Instant.parse(value)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))
}.getOrDefault(value)
