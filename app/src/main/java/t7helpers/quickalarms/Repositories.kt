package t7helpers.quickalarms

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime

private const val TAG = "QuickAlarms"

/**
 * Device-protected storage, readable before the phone is first unlocked after a reboot,
 * so alarms can be rescheduled and ring then.
 */
private fun prefs(context: Context): SharedPreferences =
    context.createDeviceProtectedStorageContext()
        .getSharedPreferences("quick_alarms", Context.MODE_PRIVATE)

/** The alarms, kept in step with AlarmManager. Every change is saved at once. */
class AlarmRepository private constructor(private val context: Context) {
    private val prefs = prefs(context)
    private val state = MutableStateFlow(decodeAlarms(prefs.getString(KEY_ALARMS, null)))

    /** Sorted by the time they ring. */
    val alarms: StateFlow<List<Alarm>> = state.asStateFlow()

    fun find(id: Int): Alarm? = state.value.find { it.id == id }

    /**
     * Creates, saves and schedules a new alarm.
     * @throws SecurityException if the app may not set exact alarms; nothing is saved then.
     */
    @Synchronized
    fun add(
        kind: AlarmKind,
        time: LocalDateTime,
        meetingStart: LocalDateTime? = null,
        timerMinutes: Int? = null,
    ): Alarm {
        val id = prefs.getInt(KEY_NEXT_ID, 1)
        prefs.edit().putInt(KEY_NEXT_ID, id + 1).commit()
        return put(Alarm(id, kind, time, time, meetingStart, timerMinutes))
    }

    /** Schedules and saves [alarm], replacing the one with the same id. */
    @Synchronized
    fun put(alarm: Alarm): Alarm {
        AlarmScheduler.schedule(context, alarm)
        save(state.value.filter { it.id != alarm.id } + alarm)
        return alarm
    }

    @Synchronized
    fun remove(id: Int): Alarm? {
        val alarm = find(id) ?: return null
        AlarmScheduler.cancel(context, alarm)
        save(state.value.filter { it.id != id })
        return alarm
    }

    @Synchronized
    fun removeAll() {
        state.value.forEach { AlarmScheduler.cancel(context, it) }
        save(emptyList())
    }

    /**
     * Registers every alarm with AlarmManager again, after a reboot, a clock change or a
     * force stop. Drops alarms that were missed long ago, and returns them; leaves
     * [ringingId] alone.
     */
    @Synchronized
    fun rescheduleAll(now: LocalDateTime, ringingId: Int?): List<Alarm> {
        val (stale, live) = state.value.partition { it.id != ringingId && AlarmTimes.isStale(it, now) }
        for (alarm in live) {
            if (alarm.id == ringingId) continue
            try {
                AlarmScheduler.schedule(context, alarm)
            } catch (e: SecurityException) {
                Log.w(TAG, "Cannot schedule alarm " + alarm.id, e)
            }
        }
        if (stale.isNotEmpty()) save(live)
        return stale
    }

    private fun save(alarms: List<Alarm>) {
        val sorted = alarms.sortedBy { it.ringAt }
        prefs.edit().putString(KEY_ALARMS, encodeAlarms(sorted)).commit()
        state.value = sorted
    }

    companion object {
        private const val KEY_ALARMS = "alarms"
        private const val KEY_NEXT_ID = "nextId"

        @Volatile
        private var instance: AlarmRepository? = null

        fun get(context: Context): AlarmRepository = instance ?: synchronized(this) {
            instance ?: AlarmRepository(context.applicationContext).also { instance = it }
        }
    }
}

class SettingsRepository private constructor(context: Context) {
    private val prefs = prefs(context)
    private val state = MutableStateFlow(decodeSettings(prefs.getString(KEY_SETTINGS, null)))

    val settings: StateFlow<Settings> = state.asStateFlow()
    val current: Settings get() = state.value

    @Synchronized
    fun update(change: (Settings) -> Settings) {
        val settings = change(state.value)
        prefs.edit().putString(KEY_SETTINGS, settings.toJson().toString()).commit()
        state.value = settings
    }

    companion object {
        private const val KEY_SETTINGS = "settings"

        @Volatile
        private var instance: SettingsRepository? = null

        fun get(context: Context): SettingsRepository = instance ?: synchronized(this) {
            instance ?: SettingsRepository(context.applicationContext).also { instance = it }
        }
    }
}
