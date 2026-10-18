package t7helpers.quickalarms.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import t7helpers.quickalarms.Alarm
import t7helpers.quickalarms.AlarmKind
import t7helpers.quickalarms.AlarmTimes
import t7helpers.quickalarms.Fmt
import t7helpers.quickalarms.Settings
import java.time.LocalDateTime
import java.time.LocalTime

/** The wake-up slots, plus any one-off wake-up alarm, and a + cell to add a time. */
@Composable
fun WakeGrid(
    alarms: List<Alarm>,
    settings: Settings,
    now: LocalDateTime,
    actions: HomeActions,
    onAdd: () -> Unit,
    onLongPress: (LocalTime) -> Unit,
) {
    val context = LocalContext.current
    val wakeAlarms = alarms.filter { it.kind == AlarmKind.WAKE }
    val times: List<LocalTime?> =
        (settings.wakeSlots + wakeAlarms.map { it.time.toLocalTime() }).distinct().sorted() + null
    CellGrid(times) { time, modifier ->
        if (time == null) {
            SlotCell("+", null, active = false, enabled = true, onClick = onAdd, onLongClick = null, modifier = modifier)
        } else {
            val alarm = wakeAlarms.firstOrNull { it.time.toLocalTime() == time }
            SlotCell(
                title = Fmt.time(context, time),
                subtitle = alarm?.let {
                    if (it.isSnoozed) "snoozed" else Fmt.day(it.time.toLocalDate(), now.toLocalDate())
                },
                active = alarm != null,
                enabled = true,
                onClick = {
                    if (alarm != null) {
                        actions.remove(alarm, "Wake-up alarm at ${Fmt.time(context, time)} removed")
                    } else {
                        actions.setWake(time)
                    }
                },
                onLongClick = { onLongPress(time) },
                modifier = modifier,
            )
        }
    }
}

/**
 * Today's meeting starts from the grid settings, plus the starts of today's meeting alarms.
 * Starts that have passed drop out unless an alarm is still set for them.
 */
@Composable
fun MeetingGrid(alarms: List<Alarm>, settings: Settings, now: LocalDateTime, actions: HomeActions) {
    val context = LocalContext.current
    val today = now.toLocalDate()
    val meetingAlarms = alarms.filter { it.kind == AlarmKind.MEETING && it.meetingStart?.toLocalDate() == today }
    val starts = (
        AlarmTimes.gridSlots(settings.gridFromHour, settings.gridToHour, settings.gridStepMinutes) +
            meetingAlarms.mapNotNull { it.meetingStart?.toLocalTime() }
        ).distinct().sorted()
    val cells = starts.mapNotNull { time ->
        val start = today.atTime(time)
        val alarm = meetingAlarms.find { it.meetingStart == start }
        if (alarm == null && !start.isAfter(now)) null else start to alarm
    }
    if (cells.isEmpty()) {
        Text(
            "No meeting slots left today.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val shortestLead = minOf(settings.tapLead, settings.longPressLead).toLong()
    CellGrid(cells) { (start, alarm), modifier ->
        val startText = Fmt.time(context, start)
        SlotCell(
            title = startText,
            subtitle = alarm?.let { "−${it.leadMinutes} · ${Fmt.time(context, it.ringAt)}" },
            active = alarm != null,
            enabled = alarm != null || start.minusMinutes(shortestLead).isAfter(now),
            onClick = {
                if (alarm != null) {
                    actions.remove(alarm, "Alarm for the $startText meeting removed")
                } else {
                    actions.setMeeting(start, settings.tapLead)
                }
            },
            onLongClick = {
                if (alarm != null) actions.switchLead(alarm) else actions.setMeeting(start, settings.longPressLead)
            },
            modifier = modifier,
        )
    }
}
