package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppscSyllabus
import com.example.data.model.FocusSession
import com.example.data.model.SessionCategory
import com.example.ui.theme.EveningIndigo
import com.example.ui.theme.ExerciseCoral
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MorningGold
import com.example.ui.viewmodel.TimerState
import com.example.util.DateUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusScreen(
  selectedDateKey: String,
  timerState: TimerState,
  sessions: List<FocusSession>,
  onSelectCategory: (SessionCategory) -> Unit,
  onSetDuration: (Int) -> Unit,
  onSelectTag: (String) -> Unit,
  onTopicChange: (String) -> Unit,
  onToggleTimer: () -> Unit,
  onResetTimer: () -> Unit,
  onCompleteAndSaveTimer: (String) -> Unit,
  onLogManualSession: (SessionCategory, String, String, Int, String) -> Unit,
  onDeleteSession: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showManualDialog by remember { mutableStateOf(false) }

  val accentColor =
    when (timerState.category) {
      SessionCategory.EXERCISE -> ExerciseCoral
      SessionCategory.MORNING_APPSC -> MorningGold
      SessionCategory.EVENING_APPSC -> EveningIndigo
    }

  val availableTags =
    if (timerState.category == SessionCategory.EXERCISE) AppscSyllabus.exerciseTypes
    else AppscSyllabus.papers

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
            Text(
              text = "Focus & Exercise Studio",
              style = MaterialTheme.typography.headlineMedium,
            )
            Text(
              text = DateUtils.formatDisplayDate(selectedDateKey),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          OutlinedButton(
            onClick = { showManualDialog = true },
            modifier = Modifier.testTag("open_manual_session_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Add,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text("Quick Log")
          }
        }
      }

      // Segmented Selector for Exercise / Morning APPSC / Evening APPSC
      item {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          SessionCategory.entries.forEachIndexed { index, category ->
            SegmentedButton(
              selected = timerState.category == category,
              onClick = { onSelectCategory(category) },
              shape =
                SegmentedButtonDefaults.itemShape(
                  index = index,
                  count = SessionCategory.entries.size,
                ),
              modifier = Modifier.testTag("focus_tab_${category.wireValue.lowercase()}"),
            ) {
              Text(
                text = category.shortLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }

      // Live Timer Card
      item {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = MaterialTheme.shapes.large,
          tonalElevation = 2.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            Text(
              text = timerState.category.displayLabel,
              style = MaterialTheme.typography.titleMedium,
              color = accentColor,
            )

            Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
              Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 14.dp.toPx()
                drawArc(
                  color = accentColor.copy(alpha = 0.14f),
                  startAngle = -90f,
                  sweepAngle = 360f,
                  useCenter = false,
                  style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                drawArc(
                  color = accentColor,
                  startAngle = -90f,
                  sweepAngle = 360f * timerState.progressFraction,
                  useCenter = false,
                  style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
              }
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                  text = DateUtils.formatCountdown(timerState.remainingSeconds),
                  style =
                    MaterialTheme.typography.displayLarge.copy(
                      fontFamily = JetBrainsMonoFontFamily
                    ),
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = "${timerState.elapsedMinutes}m elapsed · ${timerState.targetMinutes}m goal",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            // Duration Presets
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf(25, 45, 60, 90).forEach { mins ->
                FilterChip(
                  selected = timerState.targetMinutes == mins,
                  onClick = { onSetDuration(mins) },
                  label = { Text("${mins}m") },
                )
              }
            }

            // Timer Controls
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              OutlinedButton(
                onClick = onResetTimer,
                modifier = Modifier.height(48.dp).testTag("timer_reset_button"),
              ) {
                Icon(imageVector = Icons.Filled.Refresh, contentDescription = "Reset timer")
              }
              Button(
                onClick = onToggleTimer,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                modifier = Modifier.weight(1f).height(48.dp).testTag("timer_toggle_button"),
              ) {
                Icon(
                  imageVector =
                    if (timerState.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                  contentDescription = null,
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(if (timerState.isRunning) "Pause" else "Start Session")
              }
              Button(
                onClick = { onCompleteAndSaveTimer("Completed via live focus timer") },
                enabled = timerState.elapsedMinutes >= 1 || timerState.remainingSeconds < timerState.targetMinutes * 60,
                modifier = Modifier.height(48.dp).testTag("timer_save_button"),
              ) {
                Icon(imageVector = Icons.Filled.Check, contentDescription = "Save session")
                Spacer(modifier = Modifier.size(4.dp))
                Text("Save")
              }
            }

            // Subject / Modality Tags
            Column(
              modifier = Modifier.fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                text =
                  if (timerState.category == SessionCategory.EXERCISE) "Workout Modality"
                  else "APPSC Group 1 Paper / Syllabus",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                availableTags.forEach { tag ->
                  FilterChip(
                    selected = timerState.selectedTag == tag,
                    onClick = { onSelectTag(tag) },
                    label = { Text(tag, style = MaterialTheme.typography.labelMedium) },
                  )
                }
              }
            }

            OutlinedTextField(
              value = timerState.topic,
              onValueChange = onTopicChange,
              label = {
                Text(
                  if (timerState.category == SessionCategory.EXERCISE)
                    "Specific workout note (optional)"
                  else "Chapter / topic focus (e.g., AP Reorganisation Act)"
                )
              },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("timer_topic_input"),
            )
          }
        }
      }

      // Logged Sessions List
      item {
        Text(
          text = "Completed on ${DateUtils.formatDisplayDate(selectedDateKey)}",
          style = MaterialTheme.typography.titleLarge,
        )
      }

      if (sessions.isEmpty()) {
        item {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = "Start a live 60-minute timer or tap Quick Log to record your workout or APPSC Group 1 study session.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(16.dp),
            )
          }
        }
      } else {
        items(sessions, key = { it.id }) { session ->
          val cat = SessionCategory.fromWire(session.category)
          val itemColor =
            when (cat) {
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
                    .background(itemColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector =
                    if (cat == SessionCategory.EXERCISE) Icons.Filled.FitnessCenter
                    else Icons.AutoMirrored.Filled.MenuBook,
                  contentDescription = null,
                  tint = itemColor,
                )
              }
              Column(modifier = Modifier.weight(1f)) {
                Text(text = session.topic, style = MaterialTheme.typography.titleSmall)
                Text(
                  text = "${cat.displayLabel} · ${session.paperTag}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (session.notes.isNotBlank()) {
                  Text(
                    text = session.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
              Text(
                text = "${session.durationMinutes}m",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = itemColor,
              )
              IconButton(onClick = { onDeleteSession(session.id) }) {
                Icon(
                  imageVector = Icons.Filled.DeleteOutline,
                  contentDescription = "Delete session",
                )
              }
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(12.dp)) }
    }
  }

  if (showManualDialog) {
    ManualSessionDialog(
      initialCategory = timerState.category,
      onDismiss = { showManualDialog = false },
      onSave = { cat, topic, tag, mins, notes ->
        onLogManualSession(cat, topic, tag, mins, notes)
        showManualDialog = false
      },
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ManualSessionDialog(
  initialCategory: SessionCategory,
  onDismiss: () -> Unit,
  onSave: (SessionCategory, String, String, Int, String) -> Unit,
) {
  var category by remember { mutableStateOf(initialCategory) }
  var selectedTag by remember(category) {
    mutableStateOf(
      if (category == SessionCategory.EXERCISE) AppscSyllabus.exerciseTypes.first()
      else AppscSyllabus.papers.first()
    )
  }
  var topic by remember { mutableStateOf("") }
  var durationMins by remember { mutableIntStateOf(60) }
  var notes by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Quick Log Session") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          SessionCategory.entries.forEach { cat ->
            FilterChip(
              selected = category == cat,
              onClick = { category = cat },
              label = { Text(cat.shortLabel) },
            )
          }
        }
        val tags =
          if (category == SessionCategory.EXERCISE) AppscSyllabus.exerciseTypes
          else AppscSyllabus.papers
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          tags.forEach { tag ->
            FilterChip(
              selected = selectedTag == tag,
              onClick = { selectedTag = tag },
              label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
            )
          }
        }
        OutlinedTextField(
          value = topic,
          onValueChange = { topic = it },
          label = { Text("Topic / Workout focus") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("manual_topic_input"),
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text("Duration:", style = MaterialTheme.typography.labelLarge)
          listOf(30, 45, 60, 90).forEach { m ->
            FilterChip(
              selected = durationMins == m,
              onClick = { durationMins = m },
              label = { Text("${m}m") },
            )
          }
        }
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Key takeaways (optional)") },
          maxLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(
            category,
            topic.ifBlank { selectedTag },
            selectedTag,
            durationMins,
            notes,
          )
        },
        modifier = Modifier.testTag("save_manual_session_button"),
      ) {
        Text("Save Session")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}
