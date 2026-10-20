package t7helpers.quickalarms

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime

class StorageFormatTest {
    @Test
    fun alarmsSurviveARoundTrip() {
        val start = LocalDateTime.of(2026, 10, 17, 10, 0)
        val alarms = listOf(
            Alarm(1, AlarmKind.WAKE, LocalDateTime.of(2026, 10, 18, 7, 0)),
            Alarm(2, AlarmKind.MEETING, start.minusMinutes(5), start.plusMinutes(1), start),
            Alarm(3, AlarmKind.TIMER, start.plusSeconds(907).plusNanos(123_000_000), timerMinutes = 15),
        )
        assertEquals(alarms, decodeAlarms(encodeAlarms(alarms)))
    }

    @Test
    fun unreadableAlarmsAreSkipped() {
        val json = "[{\"id\":1,\"kind\":\"WAKE\",\"time\":\"2026-10-18T07:00\",\"ringAt\":\"2026-10-18T07:00\"}," +
            "{\"id\":2,\"kind\":\"NAP\"}]"
        assertEquals(listOf(1), decodeAlarms(json).map { it.id })
        assertEquals(emptyList<Alarm>(), decodeAlarms("not json"))
        assertEquals(emptyList<Alarm>(), decodeAlarms(null))
    }

    @Test
    fun settingsSurviveARoundTrip() {
        val settings = Settings(
            wakeSlots = listOf(LocalTime.of(5, 45), LocalTime.of(9, 0)),
            gridFromHour = 8,
            gridStepMinutes = 15,
            extraMeetingStarts = listOf(LocalDateTime.of(2026, 10, 20, 10, 15)),
            longPressLead = 7,
            timerSlots = listOf(3, 45, 90),
            vibrate = false,
            ringtone = "content://media/internal/audio/media/42",
            xiaomiSetupDone = true,
        )
        assertEquals(settings, decodeSettings(settings.toJson().toString()))
        assertEquals(Settings(), decodeSettings(Settings().toJson().toString()))
    }

    @Test
    fun missingSettingsFallBackToDefaults() {
        assertEquals(Settings(), decodeSettings(null))
        assertEquals(Settings(tapLead = 4), decodeSettings("{\"tapLead\":4}"))
        assertEquals(Settings(), decodeSettings("{\"timerSlots\":[5,\"ten\"]}"))
    }
}
