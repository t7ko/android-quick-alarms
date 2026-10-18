package t7helpers.quickalarms

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalTime

data class Settings(
    /** Wake-up times offered on the main screen. */
    val wakeSlots: List<LocalTime> = listOf(
        LocalTime.of(6, 30), LocalTime.of(7, 0), LocalTime.of(7, 30), LocalTime.of(8, 0),
    ),
    /** The meeting grid offers starts every [gridStepMinutes] from [gridFromHour]:00 to [gridToHour]:00. */
    val gridFromHour: Int = 9,
    val gridToHour: Int = 19,
    val gridStepMinutes: Int = 30,
    /** Minutes before a meeting for a tap on its slot, and for a long press. */
    val tapLead: Int = 3,
    val longPressLead: Int = 5,
    val snoozeMinutes: Int = 5,
    /** A ringing alarm stops by itself after this many minutes. */
    val ringMinutes: Int = 10,
    val vibrate: Boolean = true,
    /** Alarm sound URI; null means the default alarm sound of the system. */
    val ringtone: String? = null,
    /** The user has been through the Xiaomi setup card and hid it. */
    val xiaomiSetupDone: Boolean = false,
)

fun Settings.toJson(): JSONObject = JSONObject()
    .put("wakeSlots", JSONArray(wakeSlots.map { it.toString() }))
    .put("gridFromHour", gridFromHour)
    .put("gridToHour", gridToHour)
    .put("gridStepMinutes", gridStepMinutes)
    .put("tapLead", tapLead)
    .put("longPressLead", longPressLead)
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
    val slots = o.optJSONArray("wakeSlots")?.let { a ->
        runCatching { (0 until a.length()).map { LocalTime.parse(a.getString(it)) } }.getOrNull()
    }
    return Settings(
        wakeSlots = slots ?: d.wakeSlots,
        gridFromHour = o.optInt("gridFromHour", d.gridFromHour),
        gridToHour = o.optInt("gridToHour", d.gridToHour),
        gridStepMinutes = o.optInt("gridStepMinutes", d.gridStepMinutes),
        tapLead = o.optInt("tapLead", d.tapLead),
        longPressLead = o.optInt("longPressLead", d.longPressLead),
        snoozeMinutes = o.optInt("snoozeMinutes", d.snoozeMinutes),
        ringMinutes = o.optInt("ringMinutes", d.ringMinutes),
        vibrate = o.optBoolean("vibrate", d.vibrate),
        ringtone = if (o.has("ringtone")) o.getString("ringtone") else null,
        xiaomiSetupDone = o.optBoolean("xiaomiSetupDone", d.xiaomiSetupDone),
    )
}
