package t7helpers.quickalarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.LocalDateTime

/** AlarmManager fires here, and so does Dismiss in the notification of a snoozed alarm. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(EXTRA_ID, -1)
        when (intent.action) {
            ACTION_FIRE -> if (AlarmRepository.get(context).find(id) != null) AlarmService.ring(context, id)
            ACTION_CANCEL -> {
                AlarmRepository.get(context).remove(id)
                Notifications.cancelStatus(context, id)
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "t7helpers.quickalarms.FIRE"
        const val ACTION_CANCEL = "t7helpers.quickalarms.CANCEL"
        const val EXTRA_ID = "id"
    }
}

/** Registers the alarms again after a reboot, an app update or a clock change. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AlarmRepository.get(context).rescheduleAll(LocalDateTime.now(), AlarmService.ringing.value?.id)
    }
}
