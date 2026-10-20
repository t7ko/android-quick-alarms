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
        restoreAlarms(context)
    }
}

/**
 * Registers the alarms with AlarmManager again and brings their notifications up to date:
 * running timers get back the countdowns that a reboot or a force stop cleared, and alarms
 * missed long ago lose theirs.
 */
fun restoreAlarms(context: Context) {
    val now = LocalDateTime.now()
    val repo = AlarmRepository.get(context)
    repo.rescheduleAll(now, AlarmService.ringing.value?.id).forEach { Notifications.cancelStatus(context, it.id) }
    repo.alarms.value
        .filter { it.kind == AlarmKind.TIMER && !it.isSnoozed && it.ringAt.isAfter(now) }
        .forEach { Notifications.showTimer(context, it) }
}
