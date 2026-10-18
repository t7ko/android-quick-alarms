package t7helpers.quickalarms.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import t7helpers.quickalarms.AlarmTimes
import t7helpers.quickalarms.Fmt
import t7helpers.quickalarms.Settings
import java.time.LocalDateTime
import java.time.LocalTime

sealed interface HomeDialog {
    /** Type a wake-up time: save it as a slot, or set an alarm once. */
    data object WakeTime : HomeDialog

    /** Long press on a wake-up cell: remove the slot, or keep a one-off time as a slot. */
    data class WakeSlot(val time: LocalTime) : HomeDialog

    /** Type a meeting start that is not in the grid. */
    data object MeetingTime : HomeDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDialogs(dialog: HomeDialog, settings: Settings, now: LocalDateTime, actions: HomeActions, onClose: () -> Unit) {
    val context = LocalContext.current
    when (dialog) {
        HomeDialog.WakeTime -> TimeDialog("Wake-up time", LocalTime.of(7, 0), onDismiss = onClose) { picked ->
            TextButton(onClick = { actions.saveWakeSlot(picked()); onClose() }) { Text("Save slot") }
            TextButton(onClick = { actions.setWake(picked()); onClose() }) { Text("Set alarm") }
        }

        is HomeDialog.WakeSlot -> {
            val isSlot = dialog.time in settings.wakeSlots
            AlertDialog(
                onDismissRequest = onClose,
                title = { Text("Wake-up time " + Fmt.time(context, dialog.time)) },
                text = {
                    Text(
                        if (isSlot) "Remove it from the wake-up slots? An alarm set for it stays."
                        else "Keep it as a wake-up slot?",
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (isSlot) actions.removeWakeSlot(dialog.time) else actions.saveWakeSlot(dialog.time)
                        onClose()
                    }) { Text(if (isSlot) "Remove slot" else "Save slot") }
                },
                dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } },
            )
        }

        HomeDialog.MeetingTime -> {
            val leads = listOf(settings.tapLead, settings.longPressLead).distinct()
            var lead by remember { mutableIntStateOf(settings.tapLead) }
            TimeDialog(
                "Meeting start",
                AlarmTimes.nextQuarterHour(now),
                onDismiss = onClose,
                extra = {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        leads.forEachIndexed { index, value ->
                            SegmentedButton(
                                selected = lead == value,
                                onClick = { lead = value },
                                shape = SegmentedButtonDefaults.itemShape(index, leads.size),
                            ) { Text("$value min before") }
                        }
                    }
                },
            ) { picked ->
                TextButton(onClick = {
                    actions.setMeeting(now.toLocalDate().atTime(picked()), lead)
                    onClose()
                }) { Text("Add") }
            }
        }
    }
}
