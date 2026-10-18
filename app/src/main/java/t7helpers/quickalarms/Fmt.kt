package t7helpers.quickalarms

import android.content.Context
import android.text.format.DateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Texts shown to the user. Times follow the 12/24-hour setting of the phone. */
object Fmt {
    fun time(context: Context, time: LocalTime): String =
        time.format(DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(context)) "H:mm" else "h:mm a"))

    fun time(context: Context, time: LocalDateTime): String = time(context, time.toLocalTime())

    /** "today", "tomorrow" or a short date. */
    fun day(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "today"
        today.plusDays(1) -> "tomorrow"
        today.minusDays(1) -> "yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("EEE d MMM"))
    }

    /** "Wake up" or "Meeting at 10:00". */
    fun title(context: Context, alarm: Alarm): String = when (alarm.kind) {
        AlarmKind.WAKE -> "Wake up"
        AlarmKind.MEETING -> "Meeting at " + time(context, alarm.meetingStart ?: alarm.time)
    }

    /** "Alarm 9:57, 3 min before", and when snoozed ", snoozed to 10:02". */
    fun detail(context: Context, alarm: Alarm): String = buildString {
        append("Alarm ").append(time(context, alarm.time))
        alarm.leadMinutes?.let { append(", ").append(it).append(" min before") }
        if (alarm.isSnoozed) append(", snoozed to ").append(time(context, alarm.ringAt))
    }
}
