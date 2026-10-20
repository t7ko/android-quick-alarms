package t7helpers.quickalarms.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import t7helpers.quickalarms.Alarm
import t7helpers.quickalarms.AlarmTimes
import t7helpers.quickalarms.Fmt
import t7helpers.quickalarms.Settings

/** The saved timer lengths and a + cell. A tap always starts another timer, even while some run. */
@Composable
fun TimerGrid(settings: Settings, actions: HomeActions, onAdd: () -> Unit, onLongPress: (Int) -> Unit) {
    val lengths: List<Int?> = settings.timerSlots.sorted() + null
    CellGrid(lengths) { minutes, modifier ->
        if (minutes == null) {
            SlotCell("+", null, active = false, enabled = true, onClick = onAdd, onLongClick = null, modifier = modifier)
        } else {
            SlotCell(
                title = AlarmTimes.durationText(minutes),
                subtitle = null,
                active = false,
                enabled = true,
                onClick = { actions.startTimer(minutes) },
                onLongClick = { onLongPress(minutes) },
                modifier = modifier,
            )
        }
    }
}

/** A row for each running timer, soonest first, counting down by the second. */
@Composable
fun RunningTimers(timers: List<Alarm>, actions: HomeActions) {
    if (timers.isEmpty()) return
    val context = LocalContext.current
    val now = rememberNow(seconds = true)
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (timer in timers) {
            val title = Fmt.title(context, timer)
            val detail = when {
                !timer.ringAt.isAfter(now) -> "Time's up"
                timer.isSnoozed -> "Snoozed, rings at " + Fmt.time(context, timer.ringAt)
                else -> "Rings at " + Fmt.time(context, timer.ringAt)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.primary)
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    AlarmTimes.countdownText(now, timer.ringAt),
                    Modifier.widthIn(min = 72.dp),
                    // Digits of one width, so the text does not shift every second.
                    style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                    color = colors.onPrimary,
                )
                Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, color = colors.onPrimary)
                    Text(detail, style = MaterialTheme.typography.labelSmall, color = colors.onPrimary)
                }
                IconButton(onClick = { actions.remove(timer, "$title cancelled") }) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel $title", tint = colors.onPrimary)
                }
            }
        }
    }
}
