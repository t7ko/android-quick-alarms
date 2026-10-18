package t7helpers.quickalarms.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import t7helpers.quickalarms.AlarmRepository
import t7helpers.quickalarms.AlarmService
import t7helpers.quickalarms.SettingsRepository

/** The main screen: one tap sets a wake-up or meeting alarm. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(resumeTick: Int, onOpenAlarms: () -> Unit, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val alarms by remember { AlarmRepository.get(context) }.alarms.collectAsState()
    val settings by remember { SettingsRepository.get(context) }.settings.collectAsState()
    val ringing by AlarmService.ringing.collectAsState()
    val now = rememberNow(resumeTick)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val actions = remember { HomeActions(context, snackbar, scope) }
    var dialog by remember { mutableStateOf<HomeDialog?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quick Alarms") },
                actions = {
                    IconButton(onClick = onOpenAlarms) {
                        BadgedBox(badge = { if (alarms.isNotEmpty()) Badge { Text(alarms.size.toString()) } }) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "All alarms")
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PermissionWarnings(resumeTick)
            XiaomiCard()
            ringing?.let { RingingCard(it) }
            NextAlarm(alarms.firstOrNull(), now, onOpenAlarms)

            SectionTitle("Wake up")
            WakeGrid(
                alarms, settings, now, actions,
                onAdd = { dialog = HomeDialog.WakeTime },
                onLongPress = { dialog = HomeDialog.WakeSlot(it) },
            )

            SectionTitle("Meetings today")
            Text(
                "Tap a start time for an alarm ${settings.tapLead} min before, " +
                    "long-press for ${settings.longPressLead} min. Tap a set alarm to remove it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MeetingGrid(alarms, settings, now, actions)
            OutlinedButton(onClick = { dialog = HomeDialog.MeetingTime }, modifier = Modifier.fillMaxWidth()) {
                Text("Other start time…")
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    dialog?.let { HomeDialogs(it, settings, now, actions, onClose = { dialog = null }) }
}
