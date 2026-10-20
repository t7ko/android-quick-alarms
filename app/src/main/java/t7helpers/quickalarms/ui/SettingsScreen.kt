package t7helpers.quickalarms.ui

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings as SystemSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import t7helpers.quickalarms.Permissions
import t7helpers.quickalarms.SettingsRepository

/** [onGridStep] runs when the user picks a new meeting grid step. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onGridStep: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { SettingsRepository.get(context) }
    val s by repo.settings.collectAsState()
    val pickSound = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            val uri = IntentCompat.getParcelableExtra(data, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            repo.update { it.copy(ringtone = uri?.takeIf { u -> u != SystemSettings.System.DEFAULT_ALARM_ALERT_URI }?.toString()) }
        }
    }
    val soundName = remember(s.ringtone) {
        s.ringtone?.let { runCatching { RingtoneManager.getRingtone(context, Uri.parse(it))?.getTitle(context) }.getOrNull() }
            ?: "Default alarm sound"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        ) {
            SectionTitle("Meeting grid")
            Stepper("First start", s.gridFromHour, 0..23, { "$it:00" }) { v ->
                repo.update { it.copy(gridFromHour = v, gridToHour = maxOf(it.gridToHour, v)) }
            }
            Stepper("Last start", s.gridToHour, 0..23, { "$it:00" }) { v ->
                repo.update { it.copy(gridToHour = v, gridFromHour = minOf(it.gridFromHour, v)) }
            }
            Text(
                "Interval when the app opens",
                Modifier.padding(top = 8.dp, bottom = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
            IntervalChoice(s.gridStepMinutes, Modifier.padding(bottom = 4.dp)) { step ->
                repo.update { it.copy(gridStepMinutes = step) }
                onGridStep()
            }

            SectionTitle("Alarm before a meeting, minutes")
            Stepper("Tap", s.tapLead, 1..60) { v -> repo.update { it.copy(tapLead = v) } }
            Stepper("Long press", s.longPressLead, 1..60) { v -> repo.update { it.copy(longPressLead = v) } }

            SectionTitle("Ringing")
            Stepper("Snooze, minutes", s.snoozeMinutes, 1..30) { v -> repo.update { it.copy(snoozeMinutes = v) } }
            Stepper("Stop after, minutes", s.ringMinutes, 1..60) { v -> repo.update { it.copy(ringMinutes = v) } }
            Row(
                Modifier.fillMaxWidth().clickable { repo.update { it.copy(vibrate = !it.vibrate) } }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Vibrate", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = s.vibrate, onCheckedChange = { v -> repo.update { it.copy(vibrate = v) } })
            }
            Column(
                Modifier.fillMaxWidth().clickable {
                    val current = s.ringtone?.let(Uri::parse) ?: SystemSettings.System.DEFAULT_ALARM_ALERT_URI
                    pickSound.launch(
                        Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, SystemSettings.System.DEFAULT_ALARM_ALERT_URI)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, current),
                    )
                }.padding(vertical = 12.dp),
            ) {
                Text("Sound", style = MaterialTheme.typography.bodyLarge)
                Text(soundName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (Permissions.isXiaomi && s.xiaomiSetupDone) {
                SectionTitle("Xiaomi")
                Text(
                    "Show the Xiaomi setup card again",
                    Modifier.fillMaxWidth()
                        .clickable { repo.update { it.copy(xiaomiSetupDone = false) } }
                        .padding(vertical = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            SectionTitle("Wake-up times and timers")
            Text(
                "Add one with + on the main screen; long-press it there to remove it.",
                Modifier.padding(bottom = 24.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
