package com.example.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppscSyllabus
import com.example.data.model.DailyLog
import com.example.data.model.FocusSession
import com.example.data.model.SessionCategory
import com.example.data.model.UserSettings
import com.example.ui.theme.EveningIndigo
import com.example.ui.theme.ExerciseCoral
import com.example.ui.theme.MindfulSage
import com.example.ui.theme.MorningGold
import com.example.ui.theme.SleepPlum
import com.example.util.DateUtils
import kotlin.math.roundToInt

@Composable
fun ProgressScreen(
  settings: UserSettings,
  recentLogs: List<DailyLog>,
  recentSessions: List<FocusSession>,
  streakDays: Int,
  onSaveTargets: (UserSettings) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showTargetsDialog by remember { mutableStateOf(false) }

  val last7Keys = remember { DateUtils.lastNDaysKeys(7) }
  val logMap = remember(recentLogs) { recentLogs.associateBy { it.dateKey } }
  val last7Logs =
    remember(last7Keys, logMap) {
      last7Keys.map { key -> logMap[key] ?: DailyLog(dateKey = key) }
    }

  val totalExercise7d = last7Logs.sumOf { it.exerciseMinutes }
  val totalStudy7d = last7Logs.sumOf { it.morningStudyMinutes + it.eveningStudyMinutes }
  val sleepEntries = last7Logs.filter { it.sleepHours > 0.0 }
  val avgSleep =
    if (sleepEntries.isEmpty()) 0.0
    else ((sleepEntries.map { it.sleepHours }.average()) * 10.0).roundToInt() / 10.0
  val totalMeals7d = last7Logs.sumOf { it.totalMealsCount }
  val mindfulMeals7d = last7Logs.sumOf { it.mindfulMealsCount }
  val mindfulRate =
    if (totalMeals7d == 0) 0 else ((mindfulMeals7d.toFloat() / totalMeals7d) * 100f).roundToInt()

  // Paper breakdown over last 14 days
  val studySessions =
    remember(recentSessions) {
      recentSessions.filter { it.category != SessionCategory.EXERCISE.wireValue }
    }
  val paperTotals =
    remember(studySessions) {
      AppscSyllabus.papers.map { paper ->
        val mins = studySessions.filter { it.paperTag == paper }.sumOf { it.durationMinutes }
        paper to mins
      }
    }
  val maxPaperMins = (paperTotals.maxOfOrNull { it.second } ?: 60).coerceAtLeast(60)

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 720.dp),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(text = "Progress & Analytics", style = MaterialTheme.typography.headlineMedium)
            Text(
              text = "7-day consistency & APPSC Group 1 balance",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          OutlinedButton(
            onClick = { showTargetsDialog = true },
            modifier = Modifier.testTag("edit_targets_button"),
          ) {
            Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(4.dp))
            Text("Targets")
          }
        }
      }

      // 2x2 Summary KPIs
      item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            MetricSummaryCard(
              label = "7d Exercise",
              value = "${String.format("%.1f", totalExercise7d / 60.0)} hrs",
              detail = "$totalExercise7d min logged",
              accent = ExerciseCoral,
              modifier = Modifier.weight(1f),
            )
            MetricSummaryCard(
              label = "7d APPSC Study",
              value = "${String.format("%.1f", totalStudy7d / 60.0)} hrs",
              detail = "$streakDays-day active streak",
              accent = MorningGold,
              modifier = Modifier.weight(1f),
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            MetricSummaryCard(
              label = "Avg Nightly Sleep",
              value = if (avgSleep > 0) "$avgSleep hrs" else "—",
              detail = "Goal ${settings.targetSleepHours} hrs",
              accent = SleepPlum,
              modifier = Modifier.weight(1f),
            )
            MetricSummaryCard(
              label = "Mindful Meals",
              value = if (totalMeals7d > 0) "$mindfulRate%" else "—",
              detail = "$mindfulMeals7d / $totalMeals7d meals",
              accent = MindfulSage,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }

      // 7-Day Focus & Exercise Stacked Bar Chart
      item {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = MaterialTheme.shapes.large,
          tonalElevation = 2.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Text(
              text = "7-Day Exercise & APPSC Study (Minutes)",
              style = MaterialTheme.typography.titleMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              LegendChip("Exercise", ExerciseCoral)
              LegendChip("Morning G1", MorningGold)
              LegendChip("Evening G1", EveningIndigo)
            }
            val dailyTargetTotal =
              (settings.targetExerciseMinutes +
                  settings.targetMorningStudyMinutes +
                  settings.targetEveningStudyMinutes)
                .coerceAtLeast(60)
            val maxChartMins =
              maxOf(
                dailyTargetTotal,
                last7Logs.maxOfOrNull {
                  it.exerciseMinutes + it.morningStudyMinutes + it.eveningStudyMinutes
                } ?: 180,
              )

            val trackColor = MaterialTheme.colorScheme.surfaceVariant
            val dashedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)

            Canvas(modifier = Modifier.fillMaxWidth().height(170.dp)) {
              val barCount = last7Logs.size
              val slotWidth = size.width / barCount
              val barWidth = (slotWidth * 0.46f).coerceAtMost(34.dp.toPx())
              val chartHeight = size.height

              // Dashed daily goal reference line
              val goalY = chartHeight * (1f - dailyTargetTotal.toFloat() / maxChartMins.toFloat())
              drawLine(
                color = dashedColor,
                start = Offset(0f, goalY),
                end = Offset(size.width, goalY),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
              )

              last7Logs.forEachIndexed { index, log ->
                val x = index * slotWidth + (slotWidth - barWidth) / 2f
                // Background slot bar
                drawRoundRect(
                  color = trackColor,
                  topLeft = Offset(x, 0f),
                  size = Size(barWidth, chartHeight),
                  cornerRadius = CornerRadius(8.dp.toPx()),
                )

                var currentBottom = chartHeight
                val segments =
                  listOf(
                    log.exerciseMinutes to ExerciseCoral,
                    log.morningStudyMinutes to MorningGold,
                    log.eveningStudyMinutes to EveningIndigo,
                  )
                segments.forEach { (mins, color) ->
                  if (mins > 0) {
                    val segHeight = (mins.toFloat() / maxChartMins.toFloat()) * chartHeight
                    val topY = (currentBottom - segHeight).coerceAtLeast(0f)
                    drawRoundRect(
                      color = color,
                      topLeft = Offset(x, topY),
                      size = Size(barWidth, (currentBottom - topY).coerceAtLeast(2f)),
                      cornerRadius = CornerRadius(4.dp.toPx()),
                    )
                    currentBottom = topY
                  }
                }
              }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
              last7Keys.forEach { key ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                  Text(
                    text = DateUtils.shortDayLabel(key),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }

      // 7-Day Sleep Consistency Chart
      item {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = MaterialTheme.shapes.large,
          tonalElevation = 1.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "7-Day Sleep Consistency",
                style = MaterialTheme.typography.titleMedium,
              )
              Text(
                text = "Target ${settings.targetSleepHours}h",
                style = MaterialTheme.typography.labelSmall,
                color = SleepPlum,
              )
            }
            val sleepTrack = MaterialTheme.colorScheme.surfaceVariant
            Canvas(modifier = Modifier.fillMaxWidth().height(110.dp)) {
              val barCount = last7Logs.size
              val slotWidth = size.width / barCount
              val barWidth = (slotWidth * 0.44f).coerceAtMost(30.dp.toPx())
              val maxHours = 10f
              val targetY = size.height * (1f - (settings.targetSleepHours.toFloat() / maxHours))

              drawLine(
                color = SleepPlum.copy(alpha = 0.5f),
                start = Offset(0f, targetY),
                end = Offset(size.width, targetY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
              )

              last7Logs.forEachIndexed { idx, log ->
                val x = idx * slotWidth + (slotWidth - barWidth) / 2f
                drawRoundRect(
                  color = sleepTrack,
                  topLeft = Offset(x, 0f),
                  size = Size(barWidth, size.height),
                  cornerRadius = CornerRadius(6.dp.toPx()),
                )
                if (log.sleepHours > 0.0) {
                  val h =
                    ((log.sleepHours.toFloat() / maxHours).coerceIn(0f, 1f)) * size.height
                  drawRoundRect(
                    color = SleepPlum,
                    topLeft = Offset(x, size.height - h),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(6.dp.toPx()),
                  )
                }
              }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
              last7Keys.forEach { key ->
                val hrs = logMap[key]?.sleepHours ?: 0.0
                Column(
                  modifier = Modifier.weight(1f),
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  Text(
                    text = if (hrs > 0.0) "${hrs}h" else "—",
                    style = MaterialTheme.typography.labelSmall,
                    color = SleepPlum,
                  )
                  Text(
                    text = DateUtils.shortDayLabel(key),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }

      // APPSC Group 1 Paper-Wise Distribution
      item {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = MaterialTheme.shapes.large,
          tonalElevation = 1.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Text(
              text = "APPSC Group 1 Subject Balance (14 Days)",
              style = MaterialTheme.typography.titleMedium,
            )
            paperTotals.forEach { (paper, mins) ->
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(text = paper, style = MaterialTheme.typography.bodySmall)
                  Text(
                    text = "${mins}m",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
                LinearProgressIndicator(
                  progress = { (mins.toFloat() / maxPaperMins.toFloat()).coerceIn(0f, 1f) },
                  strokeCap = StrokeCap.Round,
                  modifier = Modifier.fillMaxWidth().height(7.dp),
                )
              }
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(12.dp)) }
    }
  }

  if (showTargetsDialog) {
    TargetsDialog(
      settings = settings,
      onDismiss = { showTargetsDialog = false },
      onSave = { updated ->
        onSaveTargets(updated)
        showTargetsDialog = false
      },
    )
  }
}

@Composable
private fun MetricSummaryCard(
  label: String,
  value: String,
  detail: String,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = MaterialTheme.shapes.medium,
    tonalElevation = 1.dp,
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = accent,
      )
      Text(
        text = detail,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun LegendChip(label: String, color: Color) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    Box(
      modifier =
        Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color)
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun TargetsDialog(
  settings: UserSettings,
  onDismiss: () -> Unit,
  onSave: (UserSettings) -> Unit,
) {
  val context = LocalContext.current
  var exerciseMins by remember { mutableIntStateOf(settings.targetExerciseMinutes) }
  var morningMins by remember { mutableIntStateOf(settings.targetMorningStudyMinutes) }
  var eveningMins by remember { mutableIntStateOf(settings.targetEveningStudyMinutes) }
  var sleepHours by remember { mutableDoubleStateOf(settings.targetSleepHours) }
  var bedtimeMins by remember { mutableIntStateOf(settings.targetBedtimeMinutes) }
  var wakeMins by remember { mutableIntStateOf(settings.targetWakeTimeMinutes) }
  var waterGlasses by remember { mutableIntStateOf(settings.targetWaterGlasses) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Customize Daily Targets") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Exercise Goal", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(45, 60, 75, 90).forEach { m ->
            FilterChip(
              selected = exerciseMins == m,
              onClick = { exerciseMins = m },
              label = { Text("${m}m") },
            )
          }
        }
        Text("Morning APPSC Study Goal", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(45, 60, 90, 120).forEach { m ->
            FilterChip(
              selected = morningMins == m,
              onClick = { morningMins = m },
              label = { Text("${m}m") },
            )
          }
        }
        Text("Evening APPSC Study Goal", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(45, 60, 90, 120).forEach { m ->
            FilterChip(
              selected = eveningMins == m,
              onClick = { eveningMins = m },
              label = { Text("${m}m") },
            )
          }
        }
        Text("Steady Sleep Window", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = {
              TimePickerDialog(
                  context,
                  { _, h, m ->
                    bedtimeMins = h * 60 + m
                    sleepHours = DateUtils.computeSleepHours(bedtimeMins, wakeMins)
                  },
                  bedtimeMins / 60,
                  bedtimeMins % 60,
                  false,
                )
                .show()
            },
            modifier = Modifier.weight(1f),
          ) {
            Text("Bed ${DateUtils.formatMinutesOfDay(bedtimeMins)}", style = MaterialTheme.typography.labelSmall)
          }
          OutlinedButton(
            onClick = {
              TimePickerDialog(
                  context,
                  { _, h, m ->
                    wakeMins = h * 60 + m
                    sleepHours = DateUtils.computeSleepHours(bedtimeMins, wakeMins)
                  },
                  wakeMins / 60,
                  wakeMins % 60,
                  false,
                )
                .show()
            },
            modifier = Modifier.weight(1f),
          ) {
            Text("Wake ${DateUtils.formatMinutesOfDay(wakeMins)}", style = MaterialTheme.typography.labelSmall)
          }
        }
        Text("Hydration Goal (${waterGlasses} glasses)", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(6, 8, 10, 12).forEach { g ->
            FilterChip(
              selected = waterGlasses == g,
              onClick = { waterGlasses = g },
              label = { Text("$g") },
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(
            settings.copy(
              targetExerciseMinutes = exerciseMins,
              targetMorningStudyMinutes = morningMins,
              targetEveningStudyMinutes = eveningMins,
              targetSleepHours = sleepHours.coerceIn(4.0, 12.0),
              targetBedtimeMinutes = bedtimeMins,
              targetWakeTimeMinutes = wakeMins,
              targetWaterGlasses = waterGlasses,
            )
          )
        }
      ) {
        Text("Save Targets")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
