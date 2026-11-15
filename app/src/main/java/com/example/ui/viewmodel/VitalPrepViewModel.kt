package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppscSyllabus
import com.example.data.model.DailyLog
import com.example.data.model.DailyReminder
import com.example.data.model.FocusSession
import com.example.data.model.MealEntry
import com.example.data.model.SessionCategory
import com.example.data.model.UserSettings
import com.example.data.repository.VitalPrepRepository
import com.example.util.DateUtils
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class TimerState(
  val category: SessionCategory = SessionCategory.MORNING_APPSC,
  val targetMinutes: Int = 60,
  val remainingSeconds: Int = 60 * 60,
  val isRunning: Boolean = false,
  val selectedTag: String = AppscSyllabus.papers.first(),
  val topic: String = "",
) {
  val elapsedMinutes: Int
    get() = (((targetMinutes * 60) - remainingSeconds) / 60).coerceAtLeast(0)

  val progressFraction: Float
    get() {
      val total = (targetMinutes * 60).coerceAtLeast(1)
      return ((total - remainingSeconds).toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }
}

data class DailyDashboardScore(
  val totalPercent: Int = 0,
  val exerciseProgress: Float = 0f,
  val morningStudyProgress: Float = 0f,
  val eveningStudyProgress: Float = 0f,
  val sleepProgress: Float = 0f,
  val nutritionProgress: Float = 0f,
  val currentStreakDays: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class VitalPrepViewModel(
  private val repository: VitalPrepRepository,
  private val currentUserId: String,
  private val initialDisplayName: String,
) : ViewModel() {

  private val _selectedDateKey = MutableStateFlow(DateUtils.todayKey())
  val selectedDateKey: StateFlow<String> = _selectedDateKey.asStateFlow()

  private val _errorMessage = MutableStateFlow<String?>(null)
  val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

  private val _statusMessage = MutableStateFlow<String?>(null)
  val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

  private val _timerState = MutableStateFlow(TimerState())
  val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

  private var timerJob: Job? = null

  private fun unwrapError(error: Throwable, contextLabel: String) {
    val firestoreError =
      generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val rootError = firestoreError ?: error
    Log.w(TAG, "Error $contextLabel", rootError)
    _errorMessage.value = rootError.localizedMessage ?: "Unable to sync $contextLabel"
  }

  val userSettings: StateFlow<UserSettings> =
    repository
      .observeUserSettings(currentUserId)
      .catch { unwrapError(it, "user settings") }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UserSettings(userId = currentUserId, displayName = initialDisplayName),
      )

  val selectedDailyLog: StateFlow<DailyLog> =
    _selectedDateKey
      .flatMapLatest { dateKey -> repository.observeDailyLog(currentUserId, dateKey) }
      .catch { unwrapError(it, "daily summary") }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = DailyLog(userId = currentUserId, dateKey = DateUtils.todayKey()),
      )

  val sessionsForSelectedDate: StateFlow<List<FocusSession>> =
    _selectedDateKey
      .flatMapLatest { dateKey -> repository.observeSessionsForDate(currentUserId, dateKey) }
      .catch { unwrapError(it, "focus sessions") }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList(),
      )

  val mealsForSelectedDate: StateFlow<List<MealEntry>> =
    _selectedDateKey
      .flatMapLatest { dateKey -> repository.observeMealsForDate(currentUserId, dateKey) }
      .catch { unwrapError(it, "mindful meals") }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList(),
      )

  private val analyticsStartDateKey = DateUtils.offsetDateKey(-13)

  val recentDailyLogs: StateFlow<List<DailyLog>> =
    repository
      .observeRecentDailyLogs(currentUserId, analyticsStartDateKey)
      .catch { unwrapError(it, "recent history") }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList(),
      )

  val recentSessions: StateFlow<List<FocusSession>> =
    repository
      .observeRecentSessions(currentUserId, analyticsStartDateKey)
      .catch { unwrapError(it, "recent sessions") }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList(),
      )

  val reminders: StateFlow<List<DailyReminder>> =
    repository
      .observeReminders(currentUserId)
      .catch { unwrapError(it, "reminders") }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList(),
      )

  val dashboardScore: StateFlow<DailyDashboardScore> =
    combine(selectedDailyLog, userSettings, recentDailyLogs) { log, settings, history ->
      val exRatio =
        (log.exerciseMinutes.toFloat() / settings.targetExerciseMinutes.coerceAtLeast(1)).coerceIn(
          0f,
          1f,
        )
      val mornRatio =
        (log.morningStudyMinutes.toFloat() / settings.targetMorningStudyMinutes.coerceAtLeast(1))
          .coerceIn(0f, 1f)
      val eveRatio =
        (log.eveningStudyMinutes.toFloat() / settings.targetEveningStudyMinutes.coerceAtLeast(1))
          .coerceIn(0f, 1f)
      val sleepRatio =
        if (log.sleepHours <= 0.0) 0f
        else
          ((log.sleepHours / settings.targetSleepHours.coerceAtLeast(1.0)).toFloat()).coerceIn(
            0f,
            1f,
          )
      val mealRatio =
        if (log.totalMealsCount == 0) 0f
        else (log.mindfulMealsCount.toFloat() / 3f).coerceIn(0f, 1f)
      val waterRatio =
        (log.waterGlasses.toFloat() / settings.targetWaterGlasses.coerceAtLeast(1)).coerceIn(0f, 1f)
      val nutritionRatio = (mealRatio * 0.65f + waterRatio * 0.35f).coerceIn(0f, 1f)

      val overall =
        ((exRatio * 0.22f +
          mornRatio * 0.22f +
          eveRatio * 0.22f +
          sleepRatio * 0.17f +
          nutritionRatio * 0.17f) * 100f)
          .roundToInt()
          .coerceIn(0, 100)

      val historyMap = history.associateBy { it.dateKey }
      var streak = 0
      for (offset in 0 downTo -13) {
        val key = DateUtils.offsetDateKey(offset)
        val dayLog = historyMap[key]
        val activeDay =
          dayLog != null &&
            (dayLog.exerciseMinutes >= 30 ||
              dayLog.morningStudyMinutes >= 30 ||
              dayLog.eveningStudyMinutes >= 30)
        if (activeDay) {
          streak++
        } else if (offset < 0) {
          break
        }
      }

      DailyDashboardScore(
        totalPercent = overall,
        exerciseProgress = exRatio,
        morningStudyProgress = mornRatio,
        eveningStudyProgress = eveRatio,
        sleepProgress = sleepRatio,
        nutritionProgress = nutritionRatio,
        currentStreakDays = streak,
      )
    }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = DailyDashboardScore(),
      )

  init {
    viewModelScope.launch {
      try {
        repository.ensureUserInitialized(initialDisplayName.ifBlank { "Aspirant" })
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        Log.w(TAG, "Initialization warning", e)
      }
    }
  }

  fun selectDate(dateKey: String) {
    _selectedDateKey.value = dateKey
  }

  fun shiftSelectedDate(daysOffset: Int) {
    _selectedDateKey.value = DateUtils.shiftDateKey(_selectedDateKey.value, daysOffset)
  }

  fun clearMessages() {
    _errorMessage.value = null
    _statusMessage.value = null
  }

  // --- Timer Controls ---

  fun selectTimerCategory(category: SessionCategory) {
    timerJob?.cancel()
    val defaultTag =
      if (category == SessionCategory.EXERCISE) AppscSyllabus.exerciseTypes.first()
      else AppscSyllabus.papers.first()
    _timerState.value =
      TimerState(
        category = category,
        targetMinutes = category.defaultMinutes,
        remainingSeconds = category.defaultMinutes * 60,
        isRunning = false,
        selectedTag = defaultTag,
        topic = "",
      )
  }

  fun setTimerDurationMinutes(minutes: Int) {
    val clamped = minutes.coerceIn(5, 180)
    timerJob?.cancel()
    _timerState.update {
      it.copy(targetMinutes = clamped, remainingSeconds = clamped * 60, isRunning = false)
    }
  }

  fun updateTimerTag(tag: String) {
    _timerState.update { it.copy(selectedTag = tag) }
  }

  fun updateTimerTopic(topic: String) {
    _timerState.update { it.copy(topic = topic) }
  }

  fun toggleTimer() {
    val current = _timerState.value
    if (current.isRunning) {
      timerJob?.cancel()
      _timerState.update { it.copy(isRunning = false) }
    } else {
      _timerState.update { it.copy(isRunning = true) }
      timerJob?.cancel()
      timerJob =
        viewModelScope.launch {
          while (_timerState.value.isRunning && _timerState.value.remainingSeconds > 0) {
            delay(1000L)
            _timerState.update { state ->
              val next = (state.remainingSeconds - 1).coerceAtLeast(0)
              state.copy(remainingSeconds = next, isRunning = next > 0)
            }
          }
          if (_timerState.value.remainingSeconds == 0) {
            completeTimerAndSave(notes = "Completed full focus block")
          }
        }
    }
  }

  fun resetTimer() {
    timerJob?.cancel()
    _timerState.update { it.copy(remainingSeconds = it.targetMinutes * 60, isRunning = false) }
  }

  fun completeTimerAndSave(notes: String = "") {
    timerJob?.cancel()
    val current = _timerState.value
    val elapsed = current.elapsedMinutes.coerceAtLeast(1)
    val topicToSave = current.topic.ifBlank { current.selectedTag }
    viewModelScope.launch {
      try {
        repository.addFocusSession(
          dateKey = _selectedDateKey.value,
          category = current.category,
          topic = topicToSave,
          paperTag = current.selectedTag,
          durationMinutes = elapsed,
          notes = notes,
        )
        _timerState.update {
          it.copy(remainingSeconds = it.targetMinutes * 60, isRunning = false, topic = "")
        }
        _statusMessage.value = "Saved $elapsed min ${current.category.shortLabel} session"
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        _errorMessage.value = e.localizedMessage ?: "Failed to save focus session"
      }
    }
  }

  // --- Data Mutations ---

  fun logManualSession(
    category: SessionCategory,
    topic: String,
    paperTag: String,
    durationMinutes: Int,
    notes: String,
  ) {
    viewModelScope.launch {
      try {
        repository.addFocusSession(
          dateKey = _selectedDateKey.value,
          category = category,
          topic = topic,
          paperTag = paperTag,
          durationMinutes = durationMinutes,
          notes = notes,
        )
        _statusMessage.value = "Logged ${durationMinutes}m ${category.shortLabel}"
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        _errorMessage.value = e.localizedMessage ?: "Failed to log session"
      }
    }
  }

  fun deleteSession(sessionId: String) {
    viewModelScope.launch {
      try {
        repository.deleteFocusSession(sessionId, _selectedDateKey.value)
        _statusMessage.value = "Session removed"
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        _errorMessage.value = e.localizedMessage ?: "Failed to delete session"
      }
    }
  }

  fun saveSleep(
    bedtimeMinutes: Int,
    wakeTimeMinutes: Int,
    quality: Int,
    screenFreeWindDown: Boolean,
  ) {
    val computedHours = DateUtils.computeSleepHours(bedtimeMinutes, wakeTimeMinutes)
    viewModelScope.launch {
      try {
        repository.updateSleepLog(
          dateKey = _selectedDateKey.value,
          sleepHours = computedHours,
          bedtimeMinutes = bedtimeMinutes,
          wakeTimeMinutes = wakeTimeMinutes,
          sleepQuality = quality,
          screenFreeWindDown = screenFreeWindDown,
        )
        _statusMessage.value = "Sleep logged: $computedHours hrs"
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        _errorMessage.value = e.localizedMessage ?: "Failed to save sleep log"
      }
    }
  }

  fun adjustWater(delta: Int) {
    val next = (selectedDailyLog.value.waterGlasses + delta).coerceIn(0, 30)
    viewModelScope.launch {
      try {
        repository.updateWaterGlasses(_selectedDateKey.value, next)
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        _errorMessage.value = e.localizedMessage ?: "Failed to update hydration"
      }
    }
  }

  fun addMeal(
    mealType: String,
    description: String,
    distractionFree: Boolean,
    slowPace: Boolean,
    satisfiedStop: Boolean,
    nutritionQuality: String,
  ) {
    viewModelScope.launch {
      try {
        repository.addMealEntry(
          dateKey = _selectedDateKey.value,
          mealType = mealType,
          description = description,
          distractionFree = distractionFree,
          slowPace = slowPace,
          satisfiedStop = satisfiedStop,
          nutritionQuality = nutritionQuality,
        )
        _statusMessage.value = "Mindful meal logged"
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        _errorMessage.value = e.localizedMessage ?: "Failed to log meal"
      }
    }
  }

  fun deleteMeal(mealId: String) {
    viewModelScope.launch {
      try {
        repository.deleteMealEntry(mealId, _selectedDateKey.value)
        _statusMessage.value = "Meal entry removed"
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        _errorMessage.value = e.localizedMessage ?: "Failed to remove meal"
      }
    }
  }

  fun saveReminder(reminder: DailyReminder) {
    viewModelScope.launch {
      try {
        repository.saveReminder(reminder)
        _statusMessage.value = "Reminder scheduled for ${DateUtils.formatTime(reminder.hour, reminder.minute)}"
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        _errorMessage.value = e.localizedMessage ?: "Failed to save reminder"
      }
    }
  }

  fun toggleReminder(reminder: DailyReminder, enabled: Boolean) {
    viewModelScope.launch {
      try {
        repository.toggleReminder(reminder, enabled)
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        _errorMessage.value = e.localizedMessage ?: "Failed to toggle reminder"
      }
    }
  }

  fun deleteReminder(reminderId: String) {
    viewModelScope.launch {
      try {
        repository.deleteReminder(reminderId)
        _statusMessage.value = "Reminder deleted"
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        _errorMessage.value = e.localizedMessage ?: "Failed to delete reminder"
      }
    }
  }

  fun saveTargets(settings: UserSettings) {
    viewModelScope.launch {
      try {
        repository.saveUserSettings(settings)
        _statusMessage.value = "Daily targets updated"
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        _errorMessage.value = e.localizedMessage ?: "Failed to update targets"
      }
    }
  }

  private companion object {
    const val TAG = "VitalPrepVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
