package t7helpers.quickalarms

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime

/**
 * Rings an alarm: a foreground service that plays the sound and vibrates, with a
 * notification that opens the alarm screen. One alarm rings at a time; a newer one
 * replaces it, and the older one counts as missed.
 */
class AlarmService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val timeout = Runnable { onTimeout() }
    private var ringer: Ringer? = null

    private val settings get() = SettingsRepository.get(this).current
    private val repo get() = AlarmRepository.get(this)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getIntExtra(EXTRA_ID, -1) ?: -1
        when (intent?.action) {
            ACTION_RING -> ring(id)
            ACTION_DISMISS -> dismiss(id)
            ACTION_SNOOZE -> snooze(id)
        }
        stopIfIdle()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        if (state.value != null) stopRinging()
        super.onDestroy()
    }

    private fun ring(id: Int) {
        val alarm = repo.find(id)
        val current = state.value
        if (alarm == null) {
            // Started with startForegroundService(): it must enter the foreground regardless.
            if (current == null) enterForeground(Notifications.placeholder(this))
            return
        }
        if (current?.id == id) return
        if (current != null) {
            stopRinging()
            repo.remove(current.id)
            Notifications.showMissed(this, current)
        }
        state.value = alarm
        Notifications.cancelStatus(this, id)
        enterForeground(Notifications.ringing(this, alarm))
        ringer = Ringer(this, settings).also { it.start() }
        handler.postDelayed(timeout, settings.ringMinutes * 60_000L)
    }

    private fun dismiss(id: Int) {
        if (state.value?.id == id) stopRinging()
        repo.remove(id)
        Notifications.cancelStatus(this, id)
    }

    private fun snooze(id: Int) {
        if (state.value?.id == id) stopRinging()
        val alarm = repo.find(id) ?: return
        val snoozed = alarm.copy(ringAt = LocalDateTime.now().plusMinutes(settings.snoozeMinutes.toLong()))
        try {
            repo.put(snoozed)
            Notifications.showSnoozed(this, snoozed)
        } catch (e: SecurityException) {
            Log.w("QuickAlarms", "Cannot snooze alarm $id", e)
            repo.remove(id)
        }
    }

    private fun onTimeout() {
        val alarm = state.value ?: return
        stopRinging()
        repo.remove(alarm.id)
        Notifications.showMissed(this, alarm)
        stopIfIdle()
    }

    private fun stopRinging() {
        handler.removeCallbacks(timeout)
        ringer?.stop()
        ringer = null
        state.value = null
    }

    private fun stopIfIdle() {
        if (state.value != null) return
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun enterForeground(notification: Notification) {
        val type = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        ServiceCompat.startForeground(this, Notifications.ID_RINGING, notification, type)
    }

    companion object {
        const val ACTION_RING = "t7helpers.quickalarms.RING"
        const val ACTION_DISMISS = "t7helpers.quickalarms.DISMISS"
        const val ACTION_SNOOZE = "t7helpers.quickalarms.SNOOZE"
        private const val EXTRA_ID = "id"

        private val state = MutableStateFlow<Alarm?>(null)

        /** The alarm ringing now, if any. */
        val ringing: StateFlow<Alarm?> = state.asStateFlow()

        fun intent(context: Context, action: String, id: Int): Intent =
            Intent(context, AlarmService::class.java).setAction(action).putExtra(EXTRA_ID, id)

        fun ring(context: Context, id: Int) {
            ContextCompat.startForegroundService(context, intent(context, ACTION_RING, id))
        }

        fun dismiss(context: Context, id: Int) {
            context.startService(intent(context, ACTION_DISMISS, id))
        }

        fun snooze(context: Context, id: Int) {
            context.startService(intent(context, ACTION_SNOOZE, id))
        }
    }
}
