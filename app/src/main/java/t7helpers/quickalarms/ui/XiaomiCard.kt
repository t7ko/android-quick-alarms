package t7helpers.quickalarms.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import t7helpers.quickalarms.Permissions
import t7helpers.quickalarms.SettingsRepository

/**
 * On Xiaomi phones only: the MIUI/HyperOS settings an alarm needs. The app cannot read
 * them, so the card stays until the user taps Done.
 */
@Composable
fun XiaomiCard() {
    val context = LocalContext.current
    val repo = remember { SettingsRepository.get(context) }
    val settings by repo.settings.collectAsState()
    if (!Permissions.isXiaomi || settings.xiaomiSetupDone) return

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp)) {
            Text(
                "Xiaomi phone: check these once",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            listOf(
                "App info → Autostart: on. Without it, alarms are lost when the phone restarts.",
                "App info → Battery saver: No restrictions.",
                "Permissions → Show on lock screen, and Display pop-up windows while running " +
                    "in the background: allow. Without them, the alarm screen may not appear.",
            ).forEach {
                Text(
                    "• $it",
                    Modifier.padding(top = 6.dp, end = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { Permissions.open(context, Permissions.appInfo(context)) }) {
                    Text("App info")
                }
                TextButton(onClick = { Permissions.open(context, Permissions.xiaomiOtherPermissions(context)) }) {
                    Text("Permissions")
                }
                TextButton(onClick = { repo.update { it.copy(xiaomiSetupDone = true) } }) { Text("Done") }
            }
        }
    }
}
