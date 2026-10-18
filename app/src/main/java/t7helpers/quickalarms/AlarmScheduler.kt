package t7helpers.quickalarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import t7helpers.quickalarms.ui.MainActivity
import java.time.ZoneId

/**
 * Registers alarms with AlarmManager.setAlarmClock(), the call Clock apps use: it fires on
 * time even in Doze, and the system shows the next alarm in the status bar.
 */
object AlarmScheduler {
    /** @throws SecurityException if the app may not set exact alarms. */
    fun schedule(context: Context, alarm: Alarm) {
        val millis = alarm.ringAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val show = PendingIntent.getActivity(
            context, 0, MainActivity.alarmsIntent(context), PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager(context).setAlarmClock(
            AlarmManager.AlarmClockInfo(millis, show), fireIntent(context, alarm.id),
        )
    }

    fun cancel(context: Context, alarm: Alarm) {
        alarmManager(context).cancel(fireIntent(context, alarm.id))
    }

    private fun alarmManager(context: Context) = context.getSystemService(AlarmManager::class.java)

    private fun fireIntent(context: Context, id: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        id,
        Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_FIRE)
            .putExtra(AlarmReceiver.EXTRA_ID, id),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
