package t7helpers.quickalarms

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings as SystemSettings
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat

/** What the app needs from the user to ring reliably, and where to grant it. */
object Permissions {
    /** Notifications are on for the app and for the ringing channel. */
    fun notificationsAllowed(context: Context): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        val channel = manager.getNotificationChannel(Notifications.CHANNEL_RINGING) ?: return true
        return channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun exactAlarmsAllowed(context: Context): Boolean =
        Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    /** Without it a ringing alarm shows as a notification, not over the lock screen. */
    fun fullScreenAllowed(context: Context): Boolean =
        Build.VERSION.SDK_INT < 34 || context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

    fun alarmVolumeOn(context: Context): Boolean =
        context.getSystemService(AudioManager::class.java).getStreamVolume(AudioManager.STREAM_ALARM) > 0

    /** One step up, with the system volume panel so the user can go further. */
    fun raiseAlarmVolume(context: Context) {
        context.getSystemService(AudioManager::class.java)
            .adjustStreamVolume(AudioManager.STREAM_ALARM, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
    }

    fun notificationSettings(context: Context): Intent =
        Intent(SystemSettings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(SystemSettings.EXTRA_APP_PACKAGE, context.packageName)

    @RequiresApi(31)
    fun exactAlarmSettings(context: Context): Intent =
        Intent(SystemSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri(context))

    @RequiresApi(34)
    fun fullScreenSettings(context: Context): Intent =
        Intent(SystemSettings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri(context))

    /**
     * Xiaomi, Redmi and POCO phones. MIUI and HyperOS add restrictions of their own that
     * apps cannot read: Autostart (without it, no BOOT_COMPLETED), battery saver, and
     * "Show on lock screen" / "Display pop-up windows while running in the background".
     */
    val isXiaomi: Boolean get() = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)

    /** On MIUI and HyperOS this page has Autostart, Battery saver and Other permissions. */
    fun appInfo(context: Context): Intent =
        Intent(SystemSettings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(context))

    /** The MIUI "Other permissions" page of this app. */
    fun xiaomiOtherPermissions(context: Context): Intent =
        Intent("miui.intent.action.APP_PERM_EDITOR")
            .setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
            .putExtra("extra_pkgname", context.packageName)

    /** Opens [intent], or App info where the phone has no such page or does not let apps open it. */
    fun open(context: Context, intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            context.startActivity(appInfo(context))
        } catch (e: SecurityException) {
            context.startActivity(appInfo(context))
        }
    }

    private fun packageUri(context: Context) = Uri.parse("package:" + context.packageName)
}
