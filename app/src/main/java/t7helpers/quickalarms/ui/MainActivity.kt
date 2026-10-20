package t7helpers.quickalarms.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import t7helpers.quickalarms.Notifications
import t7helpers.quickalarms.restoreAlarms

private enum class Screen { HOME, ALARMS, SETTINGS }

class MainActivity : ComponentActivity() {
    private var screen by mutableStateOf(Screen.HOME)

    /** Changes on every resume, so permission checks and the clock refresh. */
    private var resumeTick by mutableIntStateOf(0)

    /**
     * The meeting grid step picked on the main screen; null means the one from Settings. It
     * lasts until the app goes to the background or Settings gets a new step.
     */
    private var meetingStep by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Notifications.ensureChannels(this)
        // A force stop clears the alarms from AlarmManager; opening the app restores them.
        restoreAlarms(this)
        screen = savedInstanceState?.getString(KEY_SCREEN)?.let { name -> Screen.entries.find { it.name == name } }
            ?: if (intent.getBooleanExtra(EXTRA_ALARMS, false)) Screen.ALARMS else Screen.HOME
        meetingStep = savedInstanceState?.takeIf { it.containsKey(KEY_MEETING_STEP) }?.getInt(KEY_MEETING_STEP)

        setContent {
            QuickAlarmsTheme {
                BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }
                when (screen) {
                    Screen.HOME -> HomeScreen(
                        resumeTick,
                        meetingStep,
                        onMeetingStep = { meetingStep = it },
                        onOpenAlarms = { screen = Screen.ALARMS },
                        onOpenSettings = { screen = Screen.SETTINGS },
                    )
                    Screen.ALARMS -> AlarmListScreen(onBack = { screen = Screen.HOME })
                    Screen.SETTINGS -> SettingsScreen(
                        onBack = { screen = Screen.HOME },
                        onGridStep = { meetingStep = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_ALARMS, false)) screen = Screen.ALARMS
    }

    override fun onResume() {
        super.onResume()
        resumeTick++
    }

    override fun onStop() {
        super.onStop()
        // Leaving the app: the next visit starts with the step from Settings again.
        if (!isChangingConfigurations) meetingStep = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_SCREEN, screen.name)
        meetingStep?.let { outState.putInt(KEY_MEETING_STEP, it) }
    }

    companion object {
        private const val EXTRA_ALARMS = "alarms"
        private const val KEY_SCREEN = "screen"
        private const val KEY_MEETING_STEP = "meetingStep"

        /** Opens the alarm list: from the status bar alarm icon and from notifications. */
        fun alarmsIntent(context: Context): Intent = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_ALARMS, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}
