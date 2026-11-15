package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

object DateUtils {
  private val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
  private val shortDayFormat = SimpleDateFormat("EEE", Locale.US)
  private val prettyFormat = SimpleDateFormat("EEE, MMM d", Locale.US)

  fun todayKey(): String = keyFormat.format(Calendar.getInstance().time)

  fun offsetDateKey(daysOffset: Int): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, daysOffset)
    return keyFormat.format(cal.time)
  }

  fun shiftDateKey(baseKey: String, daysOffset: Int): String {
    val parsed = runCatching { keyFormat.parse(baseKey) }.getOrNull() ?: return todayKey()
    val cal = Calendar.getInstance().apply { time = parsed }
    cal.add(Calendar.DAY_OF_YEAR, daysOffset)
    return keyFormat.format(cal.time)
  }

  fun lastNDaysKeys(n: Int): List<String> {
    return (-(n - 1)..0).map { offsetDateKey(it) }
  }

  fun shortDayLabel(dateKey: String): String {
    val parsed = runCatching { keyFormat.parse(dateKey) }.getOrNull() ?: return dateKey
    return shortDayFormat.format(parsed)
  }

  fun formatDisplayDate(dateKey: String): String {
    if (dateKey == todayKey()) return "Today · ${prettyFormat.format(Calendar.getInstance().time)}"
    if (dateKey == offsetDateKey(-1)) return "Yesterday"
    val parsed = runCatching { keyFormat.parse(dateKey) }.getOrNull() ?: return dateKey
    return prettyFormat.format(parsed)
  }

  fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val displayHour =
      when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
      }
    return String.format(Locale.US, "%d:%02d %s", displayHour, minute, amPm)
  }

  fun formatMinutesOfDay(minutesOfDay: Int): String {
    val normalized = ((minutesOfDay % 1440) + 1440) % 1440
    return formatTime(normalized / 60, normalized % 60)
  }

  fun computeSleepHours(bedtimeMinutes: Int, wakeTimeMinutes: Int): Double {
    val diff =
      if (wakeTimeMinutes >= bedtimeMinutes) {
        wakeTimeMinutes - bedtimeMinutes
      } else {
        (1440 - bedtimeMinutes) + wakeTimeMinutes
      }
    return ((diff / 60.0) * 10.0).roundToInt() / 10.0
  }

  fun formatCountdown(totalSeconds: Int): String {
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", mins, secs)
  }
}
