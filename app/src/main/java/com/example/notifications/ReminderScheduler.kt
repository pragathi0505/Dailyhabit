package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.DailyReminder
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

object ReminderScheduler {
  const val CHANNEL_ID = "vitalprep_daily_reminders"
  private const val PREFS_NAME = "vitalprep_reminder_cache"
  private const val KEY_REMINDERS_JSON = "reminders_json"

  const val EXTRA_REMINDER_ID = "extra_reminder_id"
  const val EXTRA_TITLE = "extra_title"
  const val EXTRA_MESSAGE = "extra_message"
  const val EXTRA_HOUR = "extra_hour"
  const val EXTRA_MINUTE = "extra_minute"
  const val EXTRA_DAYS = "extra_days"

  fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val existing = manager.getNotificationChannel(CHANNEL_ID)
      if (existing == null) {
        val channel =
          NotificationChannel(
            CHANNEL_ID,
            "Daily Wellness & APPSC Reminders",
            NotificationManager.IMPORTANCE_HIGH,
          ).apply {
            description =
              "Morning exercise, APPSC Group 1 focus study, mindful eating, and steady sleep cues."
            enableVibration(true)
          }
        manager.createNotificationChannel(channel)
      }
    }
  }

  fun syncReminders(context: Context, reminders: List<DailyReminder>) {
    ensureChannel(context)
    val previousIds = loadCachedReminderIds(context)
    val currentIds = reminders.map { it.id }.toSet()

    // Cancel reminders that were deleted or disabled
    for (oldId in previousIds) {
      if (oldId !in currentIds) {
        cancelReminderAlarm(context, oldId)
      }
    }

    saveRemindersToCache(context, reminders)

    for (reminder in reminders) {
      if (reminder.enabled && reminder.daysOfWeek.isNotEmpty()) {
        scheduleNextOccurrence(
          context = context,
          reminderId = reminder.id,
          title = reminder.title,
          message = reminder.message,
          hour = reminder.hour,
          minute = reminder.minute,
          daysOfWeek = reminder.daysOfWeek.map { it.toInt() },
        )
      } else {
        cancelReminderAlarm(context, reminder.id)
      }
    }
  }

  fun rescheduleAllFromCache(context: Context) {
    ensureChannel(context)
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_REMINDERS_JSON, null) ?: return
    val array = runCatching { JSONArray(raw) }.getOrNull() ?: return
    for (i in 0 until array.length()) {
      val obj = array.optJSONObject(i) ?: continue
      val enabled = obj.optBoolean("enabled", false)
      val id = obj.optString("id", "")
      if (!enabled || id.isBlank()) continue
      val daysArr = obj.optJSONArray("days")
      val days = mutableListOf<Int>()
      if (daysArr != null) {
        for (d in 0 until daysArr.length()) {
          days.add(daysArr.optInt(d))
        }
      }
      if (days.isNotEmpty()) {
        scheduleNextOccurrence(
          context = context,
          reminderId = id,
          title = obj.optString("title", "VitalPrep Reminder"),
          message = obj.optString("message", ""),
          hour = obj.optInt("hour", 6),
          minute = obj.optInt("minute", 0),
          daysOfWeek = days,
        )
      }
    }
  }

  fun scheduleNextOccurrence(
    context: Context,
    reminderId: String,
    title: String,
    message: String,
    hour: Int,
    minute: Int,
    daysOfWeek: List<Int>,
  ) {
    val triggerAtMillis = computeNextTriggerMillis(hour, minute, daysOfWeek) ?: return
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pendingIntent = buildAlarmPendingIntent(context, reminderId, title, message, hour, minute, daysOfWeek)
    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
  }

  fun cancelReminderAlarm(context: Context, reminderId: String) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ReminderReceiver::class.java)
    val pendingIntent =
      PendingIntent.getBroadcast(
        context,
        reminderId.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    alarmManager.cancel(pendingIntent)
  }

  fun sendImmediateNotification(context: Context, title: String, message: String, notificationId: Int = 9001) {
    ensureChannel(context)
    if (
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
          PackageManager.PERMISSION_GRANTED
    ) {
      return
    }

    val openIntent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
    val contentPendingIntent =
      PendingIntent.getActivity(
        context,
        notificationId,
        openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val notification =
      NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher_round)
        .setContentTitle(title)
        .setContentText(message.ifBlank { "Time for your consistent daily VitalPrep habit." })
        .setStyle(
          NotificationCompat.BigTextStyle()
            .bigText(message.ifBlank { "Time for your consistent daily VitalPrep habit." })
        )
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(contentPendingIntent)
        .build()

    NotificationManagerCompat.from(context).notify(notificationId, notification)
  }

  private fun buildAlarmPendingIntent(
    context: Context,
    reminderId: String,
    title: String,
    message: String,
    hour: Int,
    minute: Int,
    daysOfWeek: List<Int>,
  ): PendingIntent {
    val intent =
      Intent(context, ReminderReceiver::class.java).apply {
        putExtra(EXTRA_REMINDER_ID, reminderId)
        putExtra(EXTRA_TITLE, title)
        putExtra(EXTRA_MESSAGE, message)
        putExtra(EXTRA_HOUR, hour)
        putExtra(EXTRA_MINUTE, minute)
        putExtra(EXTRA_DAYS, daysOfWeek.toIntArray())
      }
    return PendingIntent.getBroadcast(
      context,
      reminderId.hashCode(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  internal fun computeNextTriggerMillis(
    hour: Int,
    minute: Int,
    daysOfWeek: List<Int>,
    nowMillis: Long = System.currentTimeMillis(),
  ): Long? {
    if (daysOfWeek.isEmpty()) return null
    val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
    for (offset in 0..7) {
      val candidate =
        (cal.clone() as Calendar).apply {
          add(Calendar.DAY_OF_YEAR, offset)
          set(Calendar.HOUR_OF_DAY, hour)
          set(Calendar.MINUTE, minute)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
        }
      if (candidate.timeInMillis <= nowMillis) continue
      // Map Calendar.DAY_OF_WEEK (Sun=1..Sat=7) -> ISO weekday (Mon=1..Sun=7)
      val isoDay =
        when (candidate.get(Calendar.DAY_OF_WEEK)) {
          Calendar.MONDAY -> 1
          Calendar.TUESDAY -> 2
          Calendar.WEDNESDAY -> 3
          Calendar.THURSDAY -> 4
          Calendar.FRIDAY -> 5
          Calendar.SATURDAY -> 6
          Calendar.SUNDAY -> 7
          else -> 1
        }
      if (isoDay in daysOfWeek) {
        return candidate.timeInMillis
      }
    }
    return null
  }

  private fun saveRemindersToCache(context: Context, reminders: List<DailyReminder>) {
    val array = JSONArray()
    reminders.forEach { reminder ->
      val obj =
        JSONObject().apply {
          put("id", reminder.id)
          put("title", reminder.title)
          put("message", reminder.message)
          put("hour", reminder.hour)
          put("minute", reminder.minute)
          put("enabled", reminder.enabled)
          put("days", JSONArray(reminder.daysOfWeek.map { it.toInt() }))
        }
      array.put(obj)
    }
    context
      .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putString(KEY_REMINDERS_JSON, array.toString())
      .apply()
  }

  private fun loadCachedReminderIds(context: Context): Set<String> {
    val raw =
      context
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_REMINDERS_JSON, null) ?: return emptySet()
    val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptySet()
    val ids = mutableSetOf<String>()
    for (i in 0 until array.length()) {
      array.optJSONObject(i)?.optString("id")?.takeIf { it.isNotBlank() }?.let { ids.add(it) }
    }
    return ids
  }
}

class ReminderReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val reminderId = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_ID) ?: return
    val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: "VitalPrep Reminder"
    val message = intent.getStringExtra(ReminderScheduler.EXTRA_MESSAGE) ?: ""
    val hour = intent.getIntExtra(ReminderScheduler.EXTRA_HOUR, 6)
    val minute = intent.getIntExtra(ReminderScheduler.EXTRA_MINUTE, 0)
    val days = intent.getIntArrayExtra(ReminderScheduler.EXTRA_DAYS)?.toList() ?: emptyList()

    ReminderScheduler.sendImmediateNotification(
      context = context,
      title = title,
      message = message,
      notificationId = reminderId.hashCode(),
    )

    if (days.isNotEmpty()) {
      ReminderScheduler.scheduleNextOccurrence(
        context = context,
        reminderId = reminderId,
        title = title,
        message = message,
        hour = hour,
        minute = minute,
        daysOfWeek = days,
      )
    }
  }
}

class ReminderBootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    ReminderScheduler.rescheduleAllFromCache(context)
  }
}
