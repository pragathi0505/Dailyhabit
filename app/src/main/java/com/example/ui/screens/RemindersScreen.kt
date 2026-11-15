package com.example.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.DailyReminder
import com.example.data.model.ReminderCategory
import com.example.notifications.ReminderScheduler
import com.example.ui.theme.EveningIndigo
import com.example.ui.theme.ExerciseCoral
import com.example.ui.theme.MindfulSage
import com.example.ui.theme.MorningGold
import com.example.ui.theme.SleepPlum
import com.example.util.DateUtils

@Composable
fun RemindersScreen(
  userDisplayName: String,
  userEmail: String?,
  reminders: List<DailyReminder>,
  onSaveReminder: (DailyReminder) -> Unit,
  onToggleReminder: (DailyReminder, Boolean) -> Unit,
  onDeleteReminder: (String) -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var hasNotificationPermission by remember {
    mutableStateOf(
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
      } else {
        true
      }
    )
  }

  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      hasNotificationPermission = granted
      if (granted) {
        ReminderScheduler.sendImmediateNotification(
          context = context,
          title = "VitalPrep Notifications Active",
          message = "Your morning exercise and APPSC Group 1 reminders are ready.",
        )
      }
    }

  var editingReminder by remember { mutableStateOf<DailyReminder?>(null) }
  var showEditor by remember { mutableStateOf(false) }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 720.dp),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(text = "Daily Reminders", style = MaterialTheme.typography.headlineMedium)
            Text(
              text = "Morning & daily cues for consistent productivity",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Button(
            onClick = {
              editingReminder = null
              showEditor = true
            },
            modifier = Modifier.testTag("add_reminder_button"),
          ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(4.dp))
            Text("Add Cue")
          }
        }
      }

      // Notification Permission & Instant Test Banner
      item {
        Surface(
          color =
            if (hasNotificationPermission) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            else MaterialTheme.colorScheme.secondaryContainer,
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.NotificationsActive,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text =
                  if (hasNotificationPermission) "Daily Notifications Enabled"
                  else "Allow Morning Reminder Alerts",
                style = MaterialTheme.typography.titleSmall,
              )
              Text(
                text =
                  if (hasNotificationPermission)
                    "Alarms stay synced across reboots for morning & evening blocks."
                  else "Grant notification permission so VitalPrep can alert you on schedule.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              Button(
                onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                modifier = Modifier.testTag("enable_notifications_button"),
              ) {
                Text("Allow")
              }
            } else {
              OutlinedButton(
                onClick = {
                  ReminderScheduler.sendImmediateNotification(
                    context = context,
                    title = "Morning Productivity Check-In",
                    message = "60m Exercise + 60m APPSC Group 1 Focus ready whenever you are!",
                  )
                },
                modifier = Modifier.testTag("test_notification_button"),
              ) {
                Text("Test")
              }
            }
          }
        }
      }

      if (reminders.isEmpty()) {
        item {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = "Tap Add Cue to create a customizable morning exercise or APPSC Group 1 reminder.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(16.dp),
            )
          }
        }
      } else {
        items(reminders, key = { it.id }) { reminder ->
          val cat = ReminderCategory.fromWire(reminder.category)
          val accent =
            when (cat) {
              ReminderCategory.EXERCISE -> ExerciseCoral
              ReminderCategory.MORNING_APPSC -> MorningGold
              ReminderCategory.EVENING_APPSC -> EveningIndigo
              ReminderCategory.SLEEP -> SleepPlum
              ReminderCategory.MINDFUL_EATING -> MindfulSage
            }
          val isMorning = reminder.hour in 4..11
          Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 1.dp,
            modifier =
              Modifier.fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable {
                  editingReminder = reminder
                  showEditor = true
                },
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Box(
                modifier =
                  Modifier.size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Filled.Schedule,
                  contentDescription = null,
                  tint = accent,
                )
              }
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = DateUtils.formatTime(reminder.hour, reminder.minute),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                  )
                  if (isMorning) {
                    Surface(
                      color = MorningGold.copy(alpha = 0.16f),
                      shape = RoundedCornerShape(6.dp),
                    ) {
                      Text(
                        text = "Morning",
                        style = MaterialTheme.typography.labelSmall,
                        color = MorningGold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                      )
                    }
                  }
                }
                Text(text = reminder.title, style = MaterialTheme.typography.titleSmall)
                if (reminder.message.isNotBlank()) {
                  Text(
                    text = reminder.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Text(
                  text = formatDaysSummary(reminder.daysOfWeek.map { it.toInt() }),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Switch(
                checked = reminder.enabled,
                onCheckedChange = { checked ->
                  if (checked && !hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                  }
                  onToggleReminder(reminder, checked)
                },
              )
              IconButton(onClick = { onDeleteReminder(reminder.id) }) {
                Icon(
                  imageVector = Icons.Filled.DeleteOutline,
                  contentDescription = "Delete reminder",
                )
              }
            }
          }
        }
      }

      // Account Card
      item {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = MaterialTheme.shapes.medium,
          tonalElevation = 1.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = userDisplayName.ifBlank { "VitalPrep Account" },
                style = MaterialTheme.typography.titleMedium,
              )
              if (!userEmail.isNullOrBlank()) {
                Text(
                  text = userEmail,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            OutlinedButton(
              onClick = onSignOut,
              modifier = Modifier.testTag("sign_out_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.size(6.dp))
              Text("Sign Out")
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(12.dp)) }
    }
  }

  if (showEditor) {
    ReminderEditorDialog(
      initial = editingReminder,
      onDismiss = { showEditor = false },
      onSave = { reminder ->
        if (reminder.enabled && !hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        onSaveReminder(reminder)
        showEditor = false
      },
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderEditorDialog(
  initial: DailyReminder?,
  onDismiss: () -> Unit,
  onSave: (DailyReminder) -> Unit,
) {
  val context = LocalContext.current
  var title by remember { mutableStateOf(initial?.title ?: "Morning Productivity Cue") }
  var message by remember {
    mutableStateOf(
      initial?.message ?: "Time for 60 minutes of focused APPSC Group 1 or exercise!"
    )
  }
  var category by remember {
    mutableStateOf(
      ReminderCategory.fromWire(initial?.category ?: ReminderCategory.MORNING_APPSC.wireValue)
    )
  }
  var hour by remember { mutableIntStateOf(initial?.hour ?: 6) }
  var minute by remember { mutableIntStateOf(initial?.minute ?: 30) }
  var selectedDays by remember {
    mutableStateOf(
      initial?.daysOfWeek?.map { it.toInt() }?.toSet() ?: setOf(1, 2, 3, 4, 5, 6, 7)
    )
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initial == null) "New Daily Reminder" else "Edit Reminder") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Reminder Title") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("reminder_title_input"),
        )
        OutlinedTextField(
          value = message,
          onValueChange = { message = it },
          label = { Text("Custom Motivation / Note") },
          maxLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          ReminderCategory.entries.forEach { cat ->
            FilterChip(
              selected = category == cat,
              onClick = { category = cat },
              label = { Text(cat.displayLabel, style = MaterialTheme.typography.labelSmall) },
            )
          }
        }
        OutlinedButton(
          onClick = {
            TimePickerDialog(
                context,
                { _, pickedH, pickedM ->
                  hour = pickedH
                  minute = pickedM
                },
                hour,
                minute,
                false,
              )
              .show()
          },
          modifier = Modifier.fillMaxWidth().testTag("reminder_time_picker_button"),
        ) {
          Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.size(8.dp))
          Text("Time: ${DateUtils.formatTime(hour, minute)}")
        }
        Text("Repeat Days", style = MaterialTheme.typography.labelMedium)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          val labels = listOf(1 to "M", 2 to "T", 3 to "W", 4 to "T", 5 to "F", 6 to "S", 7 to "S")
          labels.forEach { (dayNum, label) ->
            val selected = dayNum in selectedDays
            Surface(
              color =
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(8.dp),
              modifier =
                Modifier.weight(1f)
                  .height(36.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .clickable {
                    selectedDays =
                      if (selected && selectedDays.size > 1) selectedDays - dayNum
                      else selectedDays + dayNum
                  },
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelMedium,
                  color =
                    if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(
            (initial ?: DailyReminder()).copy(
              title = title.ifBlank { category.displayLabel },
              message = message,
              category = category.wireValue,
              hour = hour,
              minute = minute,
              enabled = true,
              daysOfWeek = selectedDays.sorted().map { it.toLong() },
            )
          )
        },
        modifier = Modifier.testTag("save_reminder_button"),
      ) {
        Text("Save Cue")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

private fun formatDaysSummary(days: List<Int>): String {
  if (days.size == 7) return "Every day"
  if (days.sorted() == listOf(1, 2, 3, 4, 5)) return "Mon – Fri"
  if (days.sorted() == listOf(6, 7)) return "Weekends"
  val names = mapOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")
  return days.sorted().mapNotNull { names[it] }.joinToString(", ")
}
