package t7helpers.quickalarms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime

class AlarmTimesTest {
    private val evening = LocalDateTime.of(2026, 10, 17, 22, 30)
    private val morning = LocalDateTime.of(2026, 10, 17, 6, 0)

    @Test
    fun nextOccurrenceIsTomorrowOnceTheTimeHasPassed() {
        assertEquals(LocalDateTime.of(2026, 10, 18, 7, 0), AlarmTimes.nextOccurrence(LocalTime.of(7, 0), evening))
    }

    @Test
    fun nextOccurrenceIsTodayWhileTheTimeIsAhead() {
        assertEquals(LocalDateTime.of(2026, 10, 17, 7, 0), AlarmTimes.nextOccurrence(LocalTime.of(7, 0), morning))
    }

    @Test
    fun nextOccurrenceOfTheCurrentMinuteIsTomorrow() {
        assertEquals(LocalDateTime.of(2026, 10, 18, 6, 0), AlarmTimes.nextOccurrence(LocalTime.of(6, 0), morning))
    }

    @Test
    fun gridIncludesBothEnds() {
        val slots = AlarmTimes.gridSlots(9, 11, 30)
        assertEquals(listOf("09:00", "09:30", "10:00", "10:30", "11:00"), slots.map { it.toString() })
    }

    @Test
    fun gridStopsBeforeMidnight() {
        assertEquals(LocalTime.of(23, 0), AlarmTimes.gridSlots(22, 23, 60).last())
        assertEquals(1, AlarmTimes.gridSlots(23, 23, 15).size)
    }

    @Test
    fun nextQuarterHourRoundsUpAndWraps() {
        assertEquals(LocalTime.of(10, 15), AlarmTimes.nextQuarterHour(LocalDateTime.of(2026, 10, 17, 10, 0)))
        assertEquals(LocalTime.of(10, 15), AlarmTimes.nextQuarterHour(LocalDateTime.of(2026, 10, 17, 10, 14)))
        assertEquals(LocalTime.of(0, 0), AlarmTimes.nextQuarterHour(LocalDateTime.of(2026, 10, 17, 23, 50)))
    }

    @Test
    fun roundToStepGoesToTheNearestStep() {
        assertEquals(LocalTime.of(7, 0), AlarmTimes.roundToStep(LocalTime.of(7, 0), 5))
        assertEquals(LocalTime.of(10, 15), AlarmTimes.roundToStep(LocalTime.of(10, 17), 5))
        assertEquals(LocalTime.of(10, 20), AlarmTimes.roundToStep(LocalTime.of(10, 18), 5))
        assertEquals(LocalTime.of(11, 0), AlarmTimes.roundToStep(LocalTime.of(10, 58), 5))
        assertEquals(LocalTime.of(0, 0), AlarmTimes.roundToStep(LocalTime.of(23, 58), 5))
    }

    @Test
    fun staleOnlyAfterTheGracePeriod() {
        val alarm = Alarm(1, AlarmKind.WAKE, morning)
        assertFalse(AlarmTimes.isStale(alarm, morning.plusMinutes(15)))
        assertTrue(AlarmTimes.isStale(alarm, morning.plusMinutes(16)))
    }

    @Test
    fun untilTextRoundsUpToMinutes() {
        assertEquals("now", AlarmTimes.untilText(morning, morning))
        assertEquals("in 1 min", AlarmTimes.untilText(morning, morning.plusSeconds(20)))
        assertEquals("in 3 min", AlarmTimes.untilText(morning, morning.plusMinutes(3)))
        assertEquals("in 2 h", AlarmTimes.untilText(morning, morning.plusHours(2)))
        assertEquals("in 8 h 30 min", AlarmTimes.untilText(evening, LocalDateTime.of(2026, 10, 18, 7, 0)))
    }

    @Test
    fun leadMinutesOfAMeetingAlarm() {
        val start = LocalDateTime.of(2026, 10, 17, 10, 0)
        val alarm = Alarm(1, AlarmKind.MEETING, start.minusMinutes(3), meetingStart = start)
        assertEquals(3L, alarm.leadMinutes)
        assertFalse(alarm.isSnoozed)
        assertTrue(alarm.copy(ringAt = start.plusMinutes(2)).isSnoozed)
    }
}
