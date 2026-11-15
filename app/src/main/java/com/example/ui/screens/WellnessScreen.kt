package com.example.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DailyLog
import com.example.data.model.MealEntry
import com.example.data.model.MealType
import com.example.data.model.NutritionQuality
import com.example.data.model.UserSettings
import com.example.ui.theme.MindfulSage
import com.example.ui.theme.MorningGold
import com.example.ui.theme.SleepPlum
import com.example.util.DateUtils
import kotlin.math.abs

@Composable
fun WellnessScreen(
  selectedDateKey: String,
  settings: UserSettings,
  dailyLog: DailyLog,
  meals: List<MealEntry>,
  onSaveSleep: (Int, Int, Int, Boolean) -> Unit,
  onAdjustWater: (Int) -> Unit,
  onAddMeal: (String, String, Boolean, Boolean, Boolean, String) -> Unit,
  onDeleteMeal: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var bedtimeMinutes by remember(selectedDateKey) { mutableIntStateOf(dailyLog.bedtimeMinutes) }
  var wakeTimeMinutes by remember(selectedDateKey) { mutableIntStateOf(dailyLog.wakeTimeMinutes) }
  var sleepQuality by remember(selectedDateKey) {
    mutableIntStateOf(if (dailyLog.sleepQuality > 0) dailyLog.sleepQuality else 4)
  }
  var windDown by remember(selectedDateKey) { mutableStateOf(dailyLog.screenFreeWindDown) }
  var showMealDialog by remember { mutableStateOf(false) }

  LaunchedEffect(dailyLog.dateKey, dailyLog.sleepHours, dailyLog.sleepQuality) {
    if (dailyLog.sleepHours > 0.0 || dailyLog.sleepQuality > 0) {
      bedtimeMinutes = dailyLog.bedtimeMinutes
      wakeTimeMinutes = dailyLog.wakeTimeMinutes
      sleepQuality = if (dailyLog.sleepQuality > 0) dailyLog.sleepQuality else 4
      windDown = dailyLog.screenFreeWindDown
    }
  }

  val computedHours = DateUtils.computeSleepHours(bedtimeMinutes, wakeTimeMinutes)
  val bedtimeDrift = abs(bedtimeMinutes - settings.targetBedtimeMinutes).coerceAtMost(360)
  val isSteadySleep = bedtimeDrift <= 30 && computedHours >= (settings.targetSleepHours - 0.5)

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 720.dp),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        Column {
          Text(
            text = "Sleep & Mindful Eating",
            style = MaterialTheme.typography.headlineMedium,
          )
          Text(
            text = DateUtils.formatDisplayDate(selectedDateKey),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Steady Sleep Card
      item {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = MaterialTheme.shapes.large,
          tonalElevation = 2.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Box(
                  modifier =
                    Modifier.size(38.dp)
                      .clip(RoundedCornerShape(10.dp))
                      .background(SleepPlum.copy(alpha = 0.14f)),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = Icons.Filled.Bedtime,
                    contentDescription = null,
                    tint = SleepPlum,
                  )
                }
                Column {
                  Text(text = "Steady Sleep Rhythm", style = MaterialTheme.typography.titleMedium)
                  Text(
                    text =
                      "Target: ${DateUtils.formatMinutesOfDay(settings.targetBedtimeMinutes)} – ${DateUtils.formatMinutesOfDay(settings.targetWakeTimeMinutes)} (${settings.targetSleepHours}h)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }

              Surface(
                color =
                  if (isSteadySleep) MindfulSage.copy(alpha = 0.15f)
                  else MorningGold.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp),
              ) {
                Text(
                  text = if (isSteadySleep) "Steady" else "${computedHours}h",
                  style = MaterialTheme.typography.labelMedium,
                  color = if (isSteadySleep) MindfulSage else MorningGold,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              OutlinedButton(
                onClick = {
                  TimePickerDialog(
                      context,
                      { _, hour, minute -> bedtimeMinutes = hour * 60 + minute },
                      bedtimeMinutes / 60,
                      bedtimeMinutes % 60,
                      false,
                    )
                    .show()
                },
                modifier = Modifier.weight(1f).testTag("bedtime_picker_button"),
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text("Bedtime", style = MaterialTheme.typography.labelSmall)
                  Text(
                    DateUtils.formatMinutesOfDay(bedtimeMinutes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                  )
                }
              }

              OutlinedButton(
                onClick = {
                  TimePickerDialog(
                      context,
                      { _, hour, minute -> wakeTimeMinutes = hour * 60 + minute },
                      wakeTimeMinutes / 60,
                      wakeTimeMinutes % 60,
                      false,
                    )
                    .show()
                },
                modifier = Modifier.weight(1f).testTag("waketime_picker_button"),
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text("Wake Up", style = MaterialTheme.typography.labelSmall)
                  Text(
                    DateUtils.formatMinutesOfDay(wakeTimeMinutes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Restorative Quality",
                style = MaterialTheme.typography.bodyMedium,
              )
              Row {
                (1..5).forEach { star ->
                  IconButton(
                    onClick = { sleepQuality = star },
                    modifier = Modifier.size(36.dp),
                  ) {
                    Icon(
                      imageVector =
                        if (star <= sleepQuality) Icons.Filled.Star else Icons.Outlined.StarOutline,
                      contentDescription = "Rate $star stars",
                      tint = if (star <= sleepQuality) MorningGold else MaterialTheme.colorScheme.outline,
                    )
                  }
                }
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Screen-free 30m wind-down",
                  style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                  text = "No phone/study screens before bed",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Switch(checked = windDown, onCheckedChange = { windDown = it })
            }

            Button(
              onClick = { onSaveSleep(bedtimeMinutes, wakeTimeMinutes, sleepQuality, windDown) },
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_sleep_button"),
            ) {
              Text("Save Sleep Log ($computedHours hrs)")
            }
          }
        }
      }

      // Mindful Hydration Card
      item {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = MaterialTheme.shapes.medium,
          tonalElevation = 1.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.LocalDrink,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
                Column {
                  Text("Mindful Hydration", style = MaterialTheme.typography.titleMedium)
                  Text(
                    "${dailyLog.waterGlasses} / ${settings.targetWaterGlasses} glasses (${dailyLog.waterGlasses * 250} ml)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                  onClick = { onAdjustWater(-1) },
                  enabled = dailyLog.waterGlasses > 0,
                  contentPadding = PaddingValues(horizontal = 12.dp),
                  modifier = Modifier.height(40.dp),
                ) {
                  Icon(Icons.Filled.Remove, contentDescription = "Decrease water")
                }
                Button(
                  onClick = { onAdjustWater(1) },
                  contentPadding = PaddingValues(horizontal = 14.dp),
                  modifier = Modifier.height(40.dp).testTag("add_water_button"),
                ) {
                  Icon(Icons.Filled.Add, contentDescription = "Add 1 glass")
                  Spacer(modifier = Modifier.size(4.dp))
                  Text("1 Glass")
                }
              }
            }
            LinearProgressIndicator(
              progress = {
                (dailyLog.waterGlasses.toFloat() / settings.targetWaterGlasses.coerceAtLeast(1))
                  .coerceIn(0f, 1f)
              },
              strokeCap = StrokeCap.Round,
              modifier = Modifier.fillMaxWidth().height(8.dp),
            )
          }
        }
      }

      // Mindful Eating Header & Action
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text("Mindful Eating Habits", style = MaterialTheme.typography.titleLarge)
            Text(
              "${dailyLog.mindfulMealsCount} mindful of ${dailyLog.totalMealsCount} logged meals",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Button(
            onClick = { showMealDialog = true },
            modifier = Modifier.testTag("log_meal_button"),
          ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(4.dp))
            Text("Log Meal")
          }
        }
      }

      if (meals.isEmpty()) {
        item {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(18.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Restaurant,
                contentDescription = null,
                tint = MindfulSage,
              )
              Text(
                text = "Log your meals with mindful awareness",
                style = MaterialTheme.typography.titleSmall,
              )
              Text(
                text = "Track screen-free eating, unhurried pace, and comfortable satiety.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      } else {
        items(meals, key = { it.id }) { meal ->
          val mealType = MealType.fromWire(meal.mealType)
          val quality = NutritionQuality.fromWire(meal.nutritionQuality)
          Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Box(
                modifier =
                  Modifier.size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MindfulSage.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Filled.Restaurant,
                  contentDescription = null,
                  tint = MindfulSage,
                )
              }
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(text = mealType.displayLabel, style = MaterialTheme.typography.titleSmall)
                  Surface(
                    color =
                      if (meal.isMindful) MindfulSage.copy(alpha = 0.14f)
                      else MorningGold.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(6.dp),
                  ) {
                    Text(
                      text = "${meal.mindfulScore}/4 Mindful",
                      style = MaterialTheme.typography.labelSmall,
                      color = if (meal.isMindful) MindfulSage else MorningGold,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                  }
                }
                Text(
                  text = meal.description,
                  style = MaterialTheme.typography.bodyMedium,
                )
                val habits =
                  buildList {
                      if (meal.distractionFree) add("Screen-free")
                      if (meal.slowPace) add("Unhurried")
                      if (meal.satisfiedStop) add("80% satisfied")
                      add(quality.displayLabel)
                    }
                    .joinToString(" · ")
                Text(
                  text = habits,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              IconButton(onClick = { onDeleteMeal(meal.id) }) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove meal")
              }
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(12.dp)) }
    }
  }

  if (showMealDialog) {
    MindfulMealDialog(
      onDismiss = { showMealDialog = false },
      onSave = { mealType, desc, screenFree, slow, satisfied, quality ->
        onAddMeal(mealType, desc, screenFree, slow, satisfied, quality)
        showMealDialog = false
      },
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MindfulMealDialog(
  onDismiss: () -> Unit,
  onSave: (String, String, Boolean, Boolean, Boolean, String) -> Unit,
) {
  var mealType by remember { mutableStateOf(MealType.BREAKFAST) }
  var description by remember { mutableStateOf("") }
  var distractionFree by remember { mutableStateOf(true) }
  var slowPace by remember { mutableStateOf(true) }
  var satisfiedStop by remember { mutableStateOf(true) }
  var quality by remember { mutableStateOf(NutritionQuality.BALANCED) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Log Mindful Meal") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          MealType.entries.forEach { type ->
            FilterChip(
              selected = mealType == type,
              onClick = { mealType = type },
              label = { Text(type.displayLabel) },
            )
          }
        }
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("What did you eat?") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("meal_description_input"),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          NutritionQuality.entries.forEach { q ->
            FilterChip(
              selected = quality == q,
              onClick = { quality = q },
              label = { Text(q.displayLabel, style = MaterialTheme.typography.labelSmall) },
            )
          }
        }
        MindfulCheckRow("Ate without phone or study screens", distractionFree) {
          distractionFree = it
        }
        MindfulCheckRow("Ate slowly & chewed mindfully", slowPace) { slowPace = it }
        MindfulCheckRow("Stopped at comfortable 80% fullness", satisfiedStop) {
          satisfiedStop = it
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(
            mealType.wireValue,
            description.ifBlank { "${mealType.displayLabel} meal" },
            distractionFree,
            slowPace,
            satisfiedStop,
            quality.wireValue,
          )
        },
        modifier = Modifier.testTag("save_meal_button"),
      ) {
        Text("Save Meal")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
private fun MindfulCheckRow(
  label: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    Text(text = label, style = MaterialTheme.typography.bodySmall)
  }
}
