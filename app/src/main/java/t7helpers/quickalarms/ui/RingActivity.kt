package t7helpers.quickalarms.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import t7helpers.quickalarms.Alarm
import t7helpers.quickalarms.AlarmKind
import t7helpers.quickalarms.AlarmService
import t7helpers.quickalarms.AlarmTimes
import t7helpers.quickalarms.Fmt
import t7helpers.quickalarms.SettingsRepository

/** The screen of the ringing alarm, shown over the lock screen. Closes when it stops. */
class RingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val snoozeMinutes = SettingsRepository.get(this).current.snoozeMinutes

        setContent {
            QuickAlarmsTheme {
                // Back does nothing: the alarm stays until dismissed or snoozed.
                BackHandler {}
                val alarm by AlarmService.ringing.collectAsState()
                LaunchedEffect(alarm) { if (alarm == null) finish() }
                alarm?.let { a ->
                    RingScreen(
                        a,
                        snoozeMinutes,
                        onSnooze = { AlarmService.snooze(this, a.id) },
                        onDismiss = { AlarmService.dismiss(this, a.id) },
                    )
                }
            }
        }
    }

    companion object {
        fun intent(context: Context): Intent = Intent(context, RingActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
    }
}

@Composable
private fun RingScreen(alarm: Alarm, snoozeMinutes: Int, onSnooze: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val now = rememberNow()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Fmt.time(context, now.toLocalTime()), fontSize = 80.sp, style = MaterialTheme.typography.displayLarge)
                Text(Fmt.title(context, alarm), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                alarm.meetingStart?.let { start ->
                    Text(
                        if (start.isAfter(now)) "starts " + AlarmTimes.untilText(now, start) else "has started",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (alarm.kind == AlarmKind.TIMER) {
                    Text("Time's up", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(80.dp)) {
                    Text("Dismiss", style = MaterialTheme.typography.headlineSmall)
                }
                FilledTonalButton(onClick = onSnooze, modifier = Modifier.fillMaxWidth().height(64.dp)) {
                    Text("Snooze $snoozeMinutes min", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
