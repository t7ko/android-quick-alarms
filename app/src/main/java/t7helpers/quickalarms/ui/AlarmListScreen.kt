package t7helpers.quickalarms.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import t7helpers.quickalarms.AlarmRepository
import t7helpers.quickalarms.AlarmTimes
import t7helpers.quickalarms.Fmt

/** Every alarm that is set, soonest first. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val alarms by remember { AlarmRepository.get(context) }.alarms.collectAsState()
    val now = rememberNow()
    var confirmDeleteAll by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alarms") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (alarms.isNotEmpty()) TextButton(onClick = { confirmDeleteAll = true }) { Text("Delete all") }
                },
            )
        },
    ) { padding ->
        if (alarms.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No alarms", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize()) {
                items(alarms, key = { it.id }) { alarm ->
                    val day = Fmt.day(alarm.ringAt.toLocalDate(), now.toLocalDate())
                    ListItem(
                        headlineContent = {
                            Text(day.replaceFirstChar { it.uppercase() } + " " + Fmt.time(context, alarm.ringAt))
                        },
                        supportingContent = {
                            Text(
                                listOfNotNull(
                                    Fmt.title(context, alarm),
                                    alarm.leadMinutes?.let { "$it min before" },
                                    if (alarm.isSnoozed) "snoozed" else null,
                                    AlarmTimes.untilText(now, alarm.ringAt),
                                ).joinToString(" · "),
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = { deleteAlarm(context, alarm) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text("Delete all alarms?") },
            confirmButton = {
                TextButton(onClick = {
                    alarms.forEach { deleteAlarm(context, it) }
                    confirmDeleteAll = false
                }) { Text("Delete all") }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteAll = false }) { Text("Cancel") } },
        )
    }
}
