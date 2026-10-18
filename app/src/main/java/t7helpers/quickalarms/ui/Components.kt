package t7helpers.quickalarms.ui

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import t7helpers.quickalarms.Alarm
import t7helpers.quickalarms.AlarmRepository
import t7helpers.quickalarms.AlarmService
import t7helpers.quickalarms.Notifications
import java.time.LocalDateTime

/** The current time, updated every minute and whenever [key] changes. */
@Composable
fun rememberNow(key: Any? = Unit): LocalDateTime {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(key) {
        while (true) {
            val current = LocalDateTime.now()
            now = current
            delay(60_000L - current.second * 1000L - current.nano / 1_000_000L + 50)
        }
    }
    return now
}

/** Deletes an alarm; one that is ringing is dismissed. */
fun deleteAlarm(context: Context, alarm: Alarm) {
    if (AlarmService.ringing.value?.id == alarm.id) {
        AlarmService.dismiss(context, alarm.id)
    } else {
        AlarmRepository.get(context).remove(alarm.id)
        Notifications.cancelStatus(context, alarm.id)
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        Modifier.padding(top = 16.dp, bottom = 4.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

/** Lays [items] out in rows of [columns] equal cells. */
@Composable
fun <T> CellGrid(items: List<T>, columns: Int = 4, cell: @Composable (T, Modifier) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in items.chunked(columns)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (item in row) cell(item, Modifier.weight(1f))
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** A time button: filled when an alarm is set for it. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SlotCell(
    title: String,
    subtitle: String?,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val colors = MaterialTheme.colorScheme
    val background = if (active) colors.primary else colors.surfaceContainerHigh
    val content = when {
        active -> colors.onPrimary
        enabled -> colors.onSurface
        else -> colors.onSurface.copy(alpha = 0.38f)
    }
    Box(
        modifier
            .height(60.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick?.let { action ->
                    {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        action()
                    }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                color = content,
            )
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.labelSmall, color = content)
        }
    }
}
