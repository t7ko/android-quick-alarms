package t7helpers.quickalarms.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import t7helpers.quickalarms.Alarm
import t7helpers.quickalarms.AlarmService
import t7helpers.quickalarms.AlarmTimes
import t7helpers.quickalarms.Fmt
import t7helpers.quickalarms.Permissions
import java.time.LocalDateTime

/** One card for each permission an alarm needs to ring properly; checked again on resume. */
@Composable
fun PermissionWarnings(resumeTick: Int) {
    val context = LocalContext.current
    val exactOk = remember(resumeTick) { Permissions.exactAlarmsAllowed(context) }
    val notificationsOk = remember(resumeTick) { Permissions.notificationsAllowed(context) }
    val fullScreenOk = remember(resumeTick) { Permissions.fullScreenAllowed(context) }
    var volumeTick by remember { mutableIntStateOf(0) }
    val volumeOk = remember(resumeTick, volumeTick) { Permissions.alarmVolumeOn(context) }
    val requestNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) context.startActivity(Permissions.notificationSettings(context))
    }

    if (!exactOk && Build.VERSION.SDK_INT >= 31) {
        Warning("Exact alarms are not allowed, so no alarm can be set.") {
            context.startActivity(Permissions.exactAlarmSettings(context))
        }
    }
    if (!notificationsOk) {
        Warning("Notifications are off: a ringing alarm cannot show its screen.") {
            val canAsk = Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
            if (canAsk) {
                requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                context.startActivity(Permissions.notificationSettings(context))
            }
        }
    }
    if (!fullScreenOk && Build.VERSION.SDK_INT >= 34) {
        Warning("Full-screen alarms are off: alarms show only as a notification.") {
            context.startActivity(Permissions.fullScreenSettings(context))
        }
    }
    if (!volumeOk) {
        Warning("Alarm volume is off, so alarms make no sound.", button = "Raise") {
            Permissions.raiseAlarmVolume(context)
            volumeTick++
        }
    }
}

@Composable
private fun Warning(text: String, button: String = "Allow", onClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text,
                Modifier.weight(1f).padding(vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            TextButton(onClick = onClick) { Text(button) }
        }
    }
}

@Composable
fun RingingCard(alarm: Alarm) {
    val context = LocalContext.current
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Ringing: " + Fmt.title(context, alarm),
                Modifier.weight(1f).padding(vertical = 12.dp),
                style = MaterialTheme.typography.titleSmall,
            )
            TextButton(onClick = { AlarmService.snooze(context, alarm.id) }) { Text("Snooze") }
            Button(onClick = { AlarmService.dismiss(context, alarm.id) }) { Text("Dismiss") }
        }
    }
}

/** "Next: Wake up, tomorrow 7:00 (in 8 h 30 min)"; opens the alarm list. */
@Composable
fun NextAlarm(next: Alarm?, now: LocalDateTime, onClick: () -> Unit) {
    val context = LocalContext.current
    val text = if (next == null) {
        "No alarms set"
    } else {
        val day = Fmt.day(next.ringAt.toLocalDate(), now.toLocalDate())
        "Next: ${Fmt.title(context, next)}, $day ${Fmt.time(context, next.ringAt)} " +
            "(${AlarmTimes.untilText(now, next.ringAt)})"
    }
    Text(
        text,
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
