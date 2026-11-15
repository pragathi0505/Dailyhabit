package com.example

import com.example.notifications.ReminderScheduler
import com.example.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class VitalPrepUnitTest {

  @Test
  fun computeSleepHours_overnight_returnsAccurateHours() {
    // 10:30 PM (22*60+30) to 6:00 AM (6*60) = 7.5 hours
    val hours = DateUtils.computeSleepHours(22 * 60 + 30, 6 * 60)
    assertEquals(7.5, hours, 0.01)
  }

  @Test
  fun formatTime_handlesMidnightNoonAndAmPm() {
    assertEquals("12:00 AM", DateUtils.formatTime(0, 0))
    assertEquals("6:30 AM", DateUtils.formatTime(6, 30))
    assertEquals("12:15 PM", DateUtils.formatTime(12, 15))
    assertEquals("6:45 PM", DateUtils.formatTime(18, 45))
  }

  @Test
  fun formatCountdown_formatsMinutesAndSeconds() {
    assertEquals("60:00", DateUtils.formatCountdown(3600))
    assertEquals("09:05", DateUtils.formatCountdown(545))
    assertEquals("00:00", DateUtils.formatCountdown(0))
  }

  @Test
  fun computeNextTriggerMillis_returnsFutureTimeOnMatchingDay() {
    val now =
      Calendar.getInstance().apply {
        set(2026, Calendar.NOVEMBER, 15, 5, 0, 0)
        set(Calendar.MILLISECOND, 0)
      }
    val next =
      ReminderScheduler.computeNextTriggerMillis(
        hour = 6,
        minute = 30,
        daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
        nowMillis = now.timeInMillis,
      )
    assertNotNull(next)
    assertTrue(next!! > now.timeInMillis)
    val diffMinutes = (next - now.timeInMillis) / 60000L
    assertEquals(90L, diffMinutes)
  }
}
