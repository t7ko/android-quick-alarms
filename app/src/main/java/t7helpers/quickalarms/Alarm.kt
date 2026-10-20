package t7helpers.quickalarms

import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.LocalDateTime

enum class AlarmKind { WAKE, MEETING, TIMER }

/**
 * A one-time alarm. Times are local wall-clock times, so an alarm set for 7:00 still rings
 * at 7:00 after the time zone changes.
 */
data class Alarm(
    /** Also the request code of the alarm's PendingIntent. */
    val id: Int,
    val kind: AlarmKind,
    /** The time the alarm was set for. */
    val time: LocalDateTime,
    /** The time it rings next: [time], or later once snoozed. */
    val ringAt: LocalDateTime = time,
    /** Start of the meeting a [AlarmKind.MEETING] alarm warns about. */
    val meetingStart: LocalDateTime? = null,
    /** Length of a [AlarmKind.TIMER], which rings this many minutes after it was started. */
    val timerMinutes: Int? = null,
) {
    val isSnoozed: Boolean get() = ringAt != time

    /** Minutes from the alarm to the meeting start. */
    val leadMinutes: Long? get() = meetingStart?.let { Duration.between(time, it).toMinutes() }
}

fun Alarm.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("kind", kind.name)
    .put("time", time.toString())
    .put("ringAt", ringAt.toString())
    .putOpt("meetingStart", meetingStart?.toString())
    .putOpt("timerMinutes", timerMinutes)

fun alarmFromJson(o: JSONObject) = Alarm(
    id = o.getInt("id"),
    kind = AlarmKind.valueOf(o.getString("kind")),
    time = LocalDateTime.parse(o.getString("time")),
    ringAt = LocalDateTime.parse(o.getString("ringAt")),
    meetingStart = if (o.has("meetingStart")) LocalDateTime.parse(o.getString("meetingStart")) else null,
    timerMinutes = if (o.has("timerMinutes")) o.getInt("timerMinutes") else null,
)

fun encodeAlarms(alarms: List<Alarm>): String = JSONArray(alarms.map { it.toJson() }).toString()

/** Skips entries it cannot read rather than losing all alarms. */
fun decodeAlarms(json: String?): List<Alarm> {
    if (json.isNullOrEmpty()) return emptyList()
    val array = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
    return (0 until array.length()).mapNotNull { i ->
        runCatching { alarmFromJson(array.getJSONObject(i)) }.getOrNull()
    }
}
