package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DailyLog
import com.example.data.model.DailyReminder
import com.example.data.model.FocusSession
import com.example.data.model.SessionCategory
import com.example.data.model.UserSettings
import com.example.ui.theme.EveningIndigo
import com.example.ui.theme.ExerciseCoral
import com.example.ui.theme.MindfulSage
import com.example.ui.theme.MorningGold
import com.example.ui.theme.SleepPlum
import com.example.ui.viewmodel.DailyDashboardScore
import com.example.util.DateUtils

@Composable
fun DashboardScreen(
  selectedDateKey: String,
  settings: UserSettings,
  dailyLog: DailyLog,
  score: DailyDashboardScore,
  sessions: List<FocusSession>,
  reminders: List<DailyReminder>,
  onShiftDate: (Int) -> Unit,
  onStartFocusCategory: (SessionCategory) -> Unit,
  onOpenWellness: () -> Unit,
  onOpenReminders: () -> Unit,
  onDeleteSession: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val animatedRing by
    animateFloatAsState(
      targetValue = score.totalPercent / 100f,
      animationSpec = tween(durationMillis = 650),
      label = "vitalityRing",
    )

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 720.dp),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Date Selector Header
      item {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = MaterialTheme.shapes.medium,
          tonalElevation = 1.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            IconButton(
              onClick = { onShiftDate(-1) },
              modifier = Modifier.testTag("prev_day_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous day",
              )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = DateUtils.formatDisplayDate(selectedDateKey),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "APPSC Group 1 · Daily Wellness",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            IconButton(
              onClick = { onShiftDate(1) },
              enabled = selectedDateKey < DateUtils.todayKey(),
              modifier = Modifier.testTag("next_day_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next day",
              )
            }
          }
        }
      }

      // Hero Readiness Card
      item {
        Card(
          shape = MaterialTheme.shapes.large,
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Box(modifier = Modifier.fillMaxWidth()) {
            Image(
              painter = painterResource(id = R.drawable.img_wellness_hero),
              contentDescription = "Morning wellness and APPSC study sanctuary",
              modifier = Modifier.matchParentSize(),
              contentScale = ContentScale.Crop,
            )
            Box(
              modifier =
                Modifier.matchParentSize()
                  .background(
                    Brush.horizontalGradient(
                      colors =
                        listOf(
                          Color(0xE60B2B2C),
                          Color(0xCC0F3E3F),
                          Color(0x99164A4B),
                        )
                    )
                  )
            )
            Row(
              modifier = Modifier.fillMaxWidth().padding(20.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(
                modifier = Modifier.weight(1f).padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Surface(
                  color = Color.White.copy(alpha = 0.16f),
                  shape = RoundedCornerShape(999.dp),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.LocalFireDepartment,
                      contentDescription = null,
                      tint = Color(0xFFFFC86B),
                      modifier = Modifier.size(15.dp),
                    )
                    Text(
                      text = "${score.currentStreakDays}-Day Streak",
                      style = MaterialTheme.typography.labelMedium,
                      color = Color.White,
                    )
                  }
                }
                Text(
                  text = "Hello, ${settings.displayName.ifBlank { "Aspirant" }}",
                  style = MaterialTheme.typography.headlineMedium,
                  color = Color.White,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                val totalFocusMins =
                  dailyLog.exerciseMinutes +
                    dailyLog.morningStudyMinutes +
                    dailyLog.eveningStudyMinutes
                val targetTotalMins =
                  settings.targetExerciseMinutes +
                    settings.targetMorningStudyMinutes +
                    settings.targetEveningStudyMinutes
                Text(
                  text = "$totalFocusMins / $targetTotalMins focus & fitness mins today",
                  style = MaterialTheme.typography.bodyMedium,
                  color = Color.White.copy(alpha = 0.88f),
                )
              }

              Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                  val stroke = 10.dp.toPx()
                  drawArc(
                    color = Color.White.copy(alpha = 0.2f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                  )
                  drawArc(
                    color = Color(0xFFF4B860),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedRing,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                  )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                    text = "${score.totalPercent}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                  )
                  Text(
                    text = "RHYTHM",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f),
                  )
                }
              }
            }
          }
        }
      }

      // 4 Daily Pillars Section Header
      item {
        Text(
          text = "Today's Core Pillars",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onBackground,
        )
      }

      // Row 1: 60m Exercise + 60m Morning APPSC
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          PillarCard(
            title = "Daily Exercise",
            valueText = "${dailyLog.exerciseMinutes} / ${settings.targetExerciseMinutes}m",
            subtitle = "60-min movement goal",
            progress = score.exerciseProgress,
            accentColor = ExerciseCoral,
            icon = Icons.Filled.FitnessCenter,
            actionLabel = "Start 60m",
            testTag = "pillar_exercise_card",
            onAction = { onStartFocusCategory(SessionCategory.EXERCISE) },
            modifier = Modifier.weight(1f),
          )
          PillarCard(
            title = "Morning APPSC",
            valueText = "${dailyLog.morningStudyMinutes} / ${settings.targetMorningStudyMinutes}m",
            subtitle = "Group 1 morning block",
            progress = score.morningStudyProgress,
            accentColor = MorningGold,
            icon = Icons.Filled.WbSunny,
            actionLabel = "Study 60m",
            testTag = "pillar_morning_study_card",
            onAction = { onStartFocusCategory(SessionCategory.MORNING_APPSC) },
            modifier = Modifier.weight(1f),
          )
        }
      }

      // Row 2: 60m Evening APPSC + Sleep & Mindful Eating
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          PillarCard(
            title = "Evening APPSC",
            valueText = "${dailyLog.eveningStudyMinutes} / ${settings.targetEveningStudyMinutes}m",
            subtitle = "Group 1 evening block",
            progress = score.eveningStudyProgress,
            accentColor = EveningIndigo,
            icon = Icons.Filled.NightsStay,
            actionLabel = "Study 60m",
            testTag = "pillar_evening_study_card",
            onAction = { onStartFocusCategory(SessionCategory.EVENING_APPSC) },
            modifier = Modifier.weight(1f),
          )
          PillarCard(
            title = "Sleep & Mindful",
            valueText =
              if (dailyLog.sleepHours > 0) "${dailyLog.sleepHours}h · ${dailyLog.mindfulMealsCount} meals"
              else "${dailyLog.mindfulMealsCount} meals · ${dailyLog.waterGlasses}gl",
            subtitle = "Steady sleep & nutrition",
            progress = (score.sleepProgress * 0.5f + score.nutritionProgress * 0.5f).coerceIn(0f, 1f),
            accentColor = SleepPlum,
            icon = Icons.Filled.Bedtime,
            actionLabel = "Check In",
            testTag = "pillar_wellness_card",
            onAction = onOpenWellness,
            modifier = Modifier.weight(1f),
          )
        }
      }

      // Active Daily Reminders Summary
      item {
        val activeReminders = reminders.filter { it.enabled }
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = MaterialTheme.shapes.medium,
          tonalElevation = 1.dp,
          modifier =
            Modifier.fillMaxWidth()
              .clip(MaterialTheme.shapes.medium)
              .clickable(onClick = onOpenReminders)
              .testTag("dashboard_reminders_card"),
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
                  imageVector = Icons.Filled.NotificationsActive,
                  contentDescription = null,
                  tint = MindfulSage,
                )
                Text(
                  text = "Daily Productivity Schedule",
                  style = MaterialTheme.typography.titleMedium,
                )
              }
              Text(
                text = "Customize",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            if (activeReminders.isEmpty()) {
              Text(
                text = "No active reminders. Tap Customize to schedule your morning cues.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            } else {
              activeReminders.take(4).forEach { rem ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = rem.title,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                  ) {
                    Text(
                      text = DateUtils.formatTime(rem.hour, rem.minute),
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onPrimaryContainer,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                  }
                }
              }
            }
          }
        }
      }

      // Today's Session Feed
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Logged Sessions",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Text(
            text = "${sessions.size} entries",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      if (sessions.isEmpty()) {
        item {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(20.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
              )
              Text(
                text = "No exercise or APPSC Group 1 sessions logged yet.",
                style = MaterialTheme.typography.titleSmall,
              )
              Text(
                text = "Tap any pillar card above to launch a 60-minute focus timer.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      } else {
        items(sessions, key = { it.id }) { session ->
          val category = SessionCategory.fromWire(session.category)
          val accent =
            when (category) {
              SessionCategory.EXERCISE -> ExerciseCoral
              SessionCategory.MORNING_APPSC -> MorningGold
              SessionCategory.EVENING_APPSC -> EveningIndigo
            }
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
                  Modifier.size(40.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector =
                    if (category == SessionCategory.EXERCISE) Icons.Filled.FitnessCenter
                    else Icons.AutoMirrored.Filled.MenuBook,
                  contentDescription = null,
                  tint = accent,
                  modifier = Modifier.size(20.dp),
                )
              }
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                  text = session.topic,
                  style = MaterialTheme.typography.titleSmall,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = "${category.shortLabel} · ${session.paperTag} · ${session.durationMinutes} min",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              IconButton(onClick = { onDeleteSession(session.id) }) {
                Icon(
                  imageVector = Icons.Filled.DeleteOutline,
                  contentDescription = "Delete session",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(12.dp)) }
    }
  }
}

@Composable
private fun PillarCard(
  title: String,
  valueText: String,
  subtitle: String,
  progress: Float,
  accentColor: Color,
  icon: ImageVector,
  actionLabel: String,
  testTag: String,
  onAction: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = MaterialTheme.shapes.medium,
    tonalElevation = 2.dp,
    modifier = modifier.clip(MaterialTheme.shapes.medium).clickable(onClick = onAction).testTag(testTag),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier =
            Modifier.size(34.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(accentColor.copy(alpha = 0.14f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(18.dp),
          )
        }
        if (progress >= 1f) {
          Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = "Goal met",
            tint = MindfulSage,
            modifier = Modifier.size(20.dp),
          )
        }
      }

      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = valueText,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = accentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        color = accentColor,
        trackColor = accentColor.copy(alpha = 0.16f),
        strokeCap = StrokeCap.Round,
        modifier = Modifier.fillMaxWidth().height(6.dp),
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      FilledTonalButton(
        onClick = onAction,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        modifier = Modifier.fillMaxWidth().height(36.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.PlayArrow,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.size(4.dp))
        Text(text = actionLabel, style = MaterialTheme.typography.labelMedium)
      }
    }
  }
}
