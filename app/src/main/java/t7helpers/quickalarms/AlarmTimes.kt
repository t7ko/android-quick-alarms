package t7helpers.quickalarms

import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/** Time arithmetic, free of Android so unit tests can cover it. */
object AlarmTimes {
    /**
     * After a reboot or a clock change, alarms whose ring time passed less than this long ago
     * still ring (at once); older ones are dropped.
     */
    val MISSED_GRACE: Duration = Duration.ofMinutes(15)

    /** The next [time] after [now]: today if it is still ahead, otherwise tomorrow. */
    fun nextOccurrence(time: LocalTime, now: LocalDateTime): LocalDateTime {
        val today = now.toLocalDate().atTime(time)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    /** Meeting starts every [stepMinutes] from [fromHour]:00 to [toHour]:00, both included. */
    fun gridSlots(fromHour: Int, toHour: Int, stepMinutes: Int): List<LocalTime> {
        val step = stepMinutes.coerceAtLeast(5)
        val last = toHour.coerceIn(0, 23) * 60
        return generateSequence(fromHour.coerceIn(0, 23) * 60) { it + step }
            .takeWhile { it <= last }
            .map { LocalTime.of(it / 60, it % 60) }
            .toList()
    }

    /** The start preselected in the "other time" dialog: the next quarter hour. */
    fun nextQuarterHour(now: LocalDateTime): LocalTime {
        val minutes = now.hour * 60 + now.minute
        val next = (minutes / 15 + 1) * 15 % (24 * 60)
        return LocalTime.of(next / 60, next % 60)
    }

    /** [time] rounded to the nearest multiple of [stepMinutes], wrapping past midnight. */
    fun roundToStep(time: LocalTime, stepMinutes: Int): LocalTime {
        val minutes = time.hour * 60 + time.minute
        val rounded = (minutes + stepMinutes / 2) / stepMinutes * stepMinutes % (24 * 60)
        return LocalTime.of(rounded / 60, rounded % 60)
    }

    fun isStale(alarm: Alarm, now: LocalDateTime): Boolean =
        alarm.ringAt.isBefore(now.minus(MISSED_GRACE))

    /** "in 8 h 5 min", "in 2 h", "in 3 min" or "now", rounding up to whole minutes. */
    fun untilText(now: LocalDateTime, then: LocalDateTime): String {
        val seconds = Duration.between(now, then).seconds
        if (seconds <= 0) return "now"
        val minutes = (seconds + 59) / 60
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h == 0L -> "in $m min"
            m == 0L -> "in $h h"
            else -> "in $h h $m min"
        }
    }
}
