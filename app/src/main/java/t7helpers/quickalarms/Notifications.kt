package t7helpers.quickalarms

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import t7helpers.quickalarms.ui.MainActivity
import t7helpers.quickalarms.ui.RingActivity

object Notifications {
    const val ID_RINGING = 1
    const val CHANNEL_RINGING = "ringing"
    private const val CHANNEL_STATUS = "status"

    /** Status notifications of an alarm (snoozed, missed) use this id plus the alarm id. */
    private const val STATUS_BASE = 1000

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_RINGING, "Ringing alarm", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Shows the alarm screen. The app plays the alarm sound itself."
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_STATUS, "Snoozed and missed alarms", NotificationManager.IMPORTANCE_LOW),
        )
    }

    /** The notification of the foreground service while an alarm rings. */
    fun ringing(context: Context, alarm: Alarm): Notification {
        ensureChannels(context)
        val screen = PendingIntent.getActivity(
            context, 0, RingActivity.intent(context),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(context, CHANNEL_RINGING)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(Fmt.title(context, alarm))
            .setContentText(Fmt.detail(context, alarm))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(screen)
            .setFullScreenIntent(screen, true)
            .addAction(0, "Snooze", serviceIntent(context, AlarmService.ACTION_SNOOZE, alarm.id))
            .addAction(0, "Dismiss", serviceIntent(context, AlarmService.ACTION_DISMISS, alarm.id))
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    /** For the moment a service start finds no alarm to ring. */
    fun placeholder(context: Context): Notification {
        ensureChannels(context)
        return NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle("Checking alarms")
            .build()
    }

    fun showSnoozed(context: Context, alarm: Alarm) {
        val dismiss = PendingIntent.getBroadcast(
            context, alarm.id,
            Intent(context, AlarmReceiver::class.java)
                .setAction(AlarmReceiver.ACTION_CANCEL)
                .putExtra(AlarmReceiver.EXTRA_ID, alarm.id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        notify(
            context, alarm.id,
            status(context)
                .setContentTitle("Snoozed until " + Fmt.time(context, alarm.ringAt))
                .setContentText(Fmt.title(context, alarm))
                .addAction(0, "Dismiss", dismiss),
        )
    }

    fun showMissed(context: Context, alarm: Alarm) {
        notify(
            context, alarm.id,
            status(context)
                .setContentTitle("Missed alarm " + Fmt.time(context, alarm.ringAt))
                .setContentText(Fmt.title(context, alarm))
                .setAutoCancel(true),
        )
    }

    fun cancelStatus(context: Context, alarmId: Int) {
        NotificationManagerCompat.from(context).cancel(STATUS_BASE + alarmId)
    }

    private fun status(context: Context): NotificationCompat.Builder {
        ensureChannels(context)
        val open = PendingIntent.getActivity(
            context, 0, MainActivity.alarmsIntent(context), PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(open)
    }

    @SuppressLint("MissingPermission") // Checked by areNotificationsEnabled().
    private fun notify(context: Context, alarmId: Int, builder: NotificationCompat.Builder) {
        val manager = NotificationManagerCompat.from(context)
        if (manager.areNotificationsEnabled()) manager.notify(STATUS_BASE + alarmId, builder.build())
    }

    private fun serviceIntent(context: Context, action: String, id: Int): PendingIntent =
        PendingIntent.getService(
            context, id, AlarmService.intent(context, action, id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
