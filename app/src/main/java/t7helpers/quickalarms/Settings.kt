package t7helpers.quickalarms

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.LocalTime

data class Settings(
    /** Wake-up times offered on the main screen. */
    val wakeSlots: List<LocalTime> = listOf(
        LocalTime.of(6, 30), LocalTime.of(7, 0), LocalTime.of(7, 30), LocalTime.of(8, 0),
    ),
    /**
     * The meeting grid offers starts every [gridStepMinutes] from [gridFromHour]:00 to
     * [gridToHour]:00. The main screen can pick another step until the app is closed.
     */
    val gridFromHour: Int = 9,
    val gridToHour: Int = 19,
    val gridStepMinutes: Int = 30,
    /** Starts added with "Other start time", shown in the grid all their day, whatever the step. */
    val extraMeetingStarts: List<LocalDateTime> = emptyList(),
    /** Minutes before a meeting for a tap on its slot, and for a long press. */
    val tapLead: Int = 3,
    val longPressLead: Int = 5,
    /** Timer lengths offered on the main screen, in minutes. */
    val timerSlots: List<Int> = listOf(5, 10, 15, 30),
    val snoozeMinutes: Int = 5,
    /** A ringing alarm stops by itself after this many minutes. */
    val ringMinutes: Int = 10,
    val vibrate: Boolean = true,
    /** Alarm sound URI; null means the default alarm sound of the system. */
    val ringtone: String? = null,
    /** The user has been through the Xiaomi setup card and hid it. */
    val xiaomiSetupDone: Boolean = false,
) {
    companion object {
        /** The meeting grid steps to choose from, in minutes. */
        val GRID_STEPS = listOf(15, 20, 30, 60)
    }
}

fun Settings.toJson(): JSONObject = JSONObject()
    .put("wakeSlots", JSONArray(wakeSlots.map { it.toString() }))
    .put("gridFromHour", gridFromHour)
    .put("gridToHour", gridToHour)
    .put("gridStepMinutes", gridStepMinutes)
    .put("extraMeetingStarts", JSONArray(extraMeetingStarts.map { it.toString() }))
    .put("tapLead", tapLead)
    .put("longPressLead", longPressLead)
    .put("timerSlots", JSONArray(timerSlots))
    .put("snoozeMinutes", snoozeMinutes)
    .put("ringMinutes", ringMinutes)
    .put("vibrate", vibrate)
    .putOpt("ringtone", ringtone)
    .put("xiaomiSetupDone", xiaomiSetupDone)

/** Missing or unreadable values fall back to the defaults. */
fun decodeSettings(json: String?): Settings {
    val d = Settings()
    if (json.isNullOrEmpty()) return d
    val o = runCatching { JSONObject(json) }.getOrNull() ?: return d
    return Settings(
        wakeSlots = o.optList("wakeSlots") { LocalTime.parse(getString(it)) } ?: d.wakeSlots,
        gridFromHour = o.optInt("gridFromHour", d.gridFromHour),
        gridToHour = o.optInt("gridToHour", d.gridToHour),
        gridStepMinutes = o.optInt("gridStepMinutes", d.gridStepMinutes),
        extraMeetingStarts = o.optList("extraMeetingStarts") { LocalDateTime.parse(getString(it)) }
            ?: d.extraMeetingStarts,
        tapLead = o.optInt("tapLead", d.tapLead),
        longPressLead = o.optInt("longPressLead", d.longPressLead),
        timerSlots = o.optList("timerSlots") { getInt(it) } ?: d.timerSlots,
        snoozeMinutes = o.optInt("snoozeMinutes", d.snoozeMinutes),
        ringMinutes = o.optInt("ringMinutes", d.ringMinutes),
        vibrate = o.optBoolean("vibrate", d.vibrate),
        ringtone = if (o.has("ringtone")) o.getString("ringtone") else null,
        xiaomiSetupDone = o.optBoolean("xiaomiSetupDone", d.xiaomiSetupDone),
    )
}

/** The array under [key], or null if it is missing or any entry is unreadable. */
private fun <T> JSONObject.optList(key: String, read: JSONArray.(Int) -> T): List<T>? =
    optJSONArray(key)?.let { a -> runCatching { (0 until a.length()).map { a.read(it) } }.getOrNull() }
