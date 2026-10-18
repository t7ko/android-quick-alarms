package t7helpers.quickalarms.ui

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import t7helpers.quickalarms.Alarm
import t7helpers.quickalarms.AlarmKind
import t7helpers.quickalarms.AlarmRepository
import t7helpers.quickalarms.AlarmTimes
import t7helpers.quickalarms.Fmt
import t7helpers.quickalarms.SettingsRepository
import java.time.LocalDateTime
import java.time.LocalTime

/** What the main screen does when tapped; it reports each result in a snackbar with Undo. */
class HomeActions(
    private val context: Context,
    private val snackbar: SnackbarHostState,
    private val scope: CoroutineScope,
) {
    private val alarms = AlarmRepository.get(context)
    private val settingsRepo = SettingsRepository.get(context)
    private val settings get() = settingsRepo.current

    /** A one-time alarm at the next [time]: today if still ahead, otherwise tomorrow. */
    fun setWake(time: LocalTime) {
        val now = LocalDateTime.now()
        val at = AlarmTimes.nextOccurrence(time, now)
        val alarm = add(AlarmKind.WAKE, at) ?: return
        val day = Fmt.day(at.toLocalDate(), now.toLocalDate())
        say("Wake-up alarm $day at ${t(at)}, ${AlarmTimes.untilText(now, at)}") { alarms.remove(alarm.id) }
    }

    /** An alarm [lead] minutes before a meeting that starts at [start]. */
    fun setMeeting(start: LocalDateTime, lead: Int) {
        val at = start.minusMinutes(lead.toLong())
        if (!at.isAfter(LocalDateTime.now())) return say("Too late for an alarm $lead min before ${t(start)}")
        val alarm = add(AlarmKind.MEETING, at, start) ?: return
        say("Alarm at ${t(at)}, $lead min before the ${t(start)} meeting") { alarms.remove(alarm.id) }
    }

    /** Moves a meeting alarm between the tap and the long-press lead time. */
    fun switchLead(alarm: Alarm) {
        val start = alarm.meetingStart ?: return
        val lead = if (alarm.leadMinutes == settings.tapLead.toLong()) settings.longPressLead else settings.tapLead
        val at = start.minusMinutes(lead.toLong())
        if (!at.isAfter(LocalDateTime.now())) return say("Too late for an alarm $lead min before ${t(start)}")
        if (!put(alarm.copy(time = at, ringAt = at))) return
        say("Alarm moved to ${t(at)}, $lead min before ${t(start)}") { put(alarm) }
    }

    fun remove(alarm: Alarm, message: String) {
        deleteAlarm(context, alarm)
        say(message) { if (alarm.ringAt.isAfter(LocalDateTime.now())) put(alarm) }
    }

    fun saveWakeSlot(time: LocalTime) =
        settingsRepo.update { it.copy(wakeSlots = (it.wakeSlots + time).distinct().sorted()) }

    fun removeWakeSlot(time: LocalTime) =
        settingsRepo.update { it.copy(wakeSlots = it.wakeSlots - time) }

    fun say(message: String, undo: (() -> Unit)? = null) {
        snackbar.currentSnackbarData?.dismiss()
        scope.launch {
            val result = snackbar.showSnackbar(
                message,
                actionLabel = if (undo != null) "Undo" else null,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) undo?.invoke()
        }
    }

    private fun t(time: LocalDateTime) = Fmt.time(context, time)

    private fun add(kind: AlarmKind, time: LocalDateTime, meetingStart: LocalDateTime? = null): Alarm? = try {
        alarms.add(kind, time, meetingStart)
    } catch (e: SecurityException) {
        say(NOT_ALLOWED)
        null
    }

    private fun put(alarm: Alarm): Boolean = try {
        alarms.put(alarm)
        true
    } catch (e: SecurityException) {
        say(NOT_ALLOWED)
        false
    }

    private companion object {
        const val NOT_ALLOWED = "The app may not set exact alarms: see the warning at the top"
    }
}
