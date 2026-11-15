package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firestore.OperationType
import com.example.data.firestore.handleFirestoreError
import com.example.data.model.DailyLog
import com.example.data.model.DailyReminder
import com.example.data.model.FocusSession
import com.example.data.model.MealEntry
import com.example.data.model.ReminderCategory
import com.example.data.model.SessionCategory
import com.example.data.model.UserSettings
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class VitalPrepRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(
    context: Context
  ) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    Firebase.auth,
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before writing data.")
  }

  private fun <T> Flow<T>.logFirestoreErrors(
    userId: String,
    operationType: OperationType,
    path: String,
  ): Flow<T> = catch { error ->
    val firestoreError =
      generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val exceptionToLog = firestoreError ?: (error as? Exception)
    if (exceptionToLog != null) {
      handleFirestoreError(exceptionToLog, operationType, path)
    }
    throw error
  }

  fun observeUserSettings(userId: String): Flow<UserSettings> = flow {
    val docRef = db.collection("users").document(userId)
    emitAll(
      docRef
        .snapshots()
        .map { snapshot ->
          snapshot.toObject(
            UserSettings::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          ) ?: UserSettings(userId = userId)
        }
        .logFirestoreErrors(userId, OperationType.GET, docRef.path)
    )
  }

  fun observeDailyLog(userId: String, dateKey: String): Flow<DailyLog> = flow {
    val docRef = db.collection("users").document(userId).collection("dailyLogs").document(dateKey)
    emitAll(
      docRef
        .snapshots()
        .map { snapshot ->
          snapshot.toObject(
            DailyLog::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          ) ?: DailyLog(userId = userId, dateKey = dateKey)
        }
        .logFirestoreErrors(userId, OperationType.GET, docRef.path)
    )
  }

  fun observeRecentDailyLogs(userId: String, startDateKey: String): Flow<List<DailyLog>> = flow {
    val colRef = db.collection("users").document(userId).collection("dailyLogs")
    emitAll(
      colRef
        .whereGreaterThanOrEqualTo("dateKey", startDateKey)
        .orderBy("dateKey", Query.Direction.ASCENDING)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            DailyLog::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .logFirestoreErrors(userId, OperationType.LIST, colRef.path)
    )
  }

  fun observeSessionsForDate(userId: String, dateKey: String): Flow<List<FocusSession>> = flow {
    val colRef = db.collection("users").document(userId).collection("sessions")
    emitAll(
      colRef
        .whereEqualTo("dateKey", dateKey)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              FocusSession::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .logFirestoreErrors(userId, OperationType.LIST, colRef.path)
    )
  }

  fun observeRecentSessions(userId: String, startDateKey: String): Flow<List<FocusSession>> = flow {
    val colRef = db.collection("users").document(userId).collection("sessions")
    emitAll(
      colRef
        .whereGreaterThanOrEqualTo("dateKey", startDateKey)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            FocusSession::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .logFirestoreErrors(userId, OperationType.LIST, colRef.path)
    )
  }

  fun observeMealsForDate(userId: String, dateKey: String): Flow<List<MealEntry>> = flow {
    val colRef = db.collection("users").document(userId).collection("meals")
    emitAll(
      colRef
        .whereEqualTo("dateKey", dateKey)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              MealEntry::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .logFirestoreErrors(userId, OperationType.LIST, colRef.path)
    )
  }

  fun observeReminders(userId: String): Flow<List<DailyReminder>> = flow {
    val colRef = db.collection("users").document(userId).collection("reminders")
    emitAll(
      colRef
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              DailyReminder::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .sortedWith(compareBy({ it.hour }, { it.minute }))
        }
        .logFirestoreErrors(userId, OperationType.LIST, colRef.path)
    )
  }

  suspend fun ensureUserInitialized(displayName: String) {
    val uid = requireUserId()
    val userDocRef = db.collection("users").document(uid)
    try {
      val snapshot = userDocRef.get().await()
      if (!snapshot.exists()) {
        val payload =
          mapOf(
            "userId" to uid,
            "displayName" to displayName.take(80),
            "targetExerciseMinutes" to 60,
            "targetMorningStudyMinutes" to 60,
            "targetEveningStudyMinutes" to 60,
            "targetSleepHours" to 7.5,
            "targetBedtimeMinutes" to 22 * 60 + 30,
            "targetWakeTimeMinutes" to 5 * 60 + 30,
            "targetWaterGlasses" to 8,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        userDocRef.set(payload).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, userDocRef.path)
      throw e
    }

    seedDefaultRemindersIfEmpty(uid)
  }

  suspend fun seedDefaultRemindersIfEmpty(userId: String? = null) {
    val uid = userId ?: requireUserId()
    val colRef = db.collection("users").document(uid).collection("reminders")
    try {
      val existing = colRef.limit(1).get().await()
      if (!existing.isEmpty) return

      val defaults =
        listOf(
          DailyReminder(
            id = "morning_exercise_default",
            userId = uid,
            title = "Morning Exercise · 60 Min",
            message = "Start strong: 1 hour of movement to energize your APPSC day.",
            category = ReminderCategory.EXERCISE.wireValue,
            hour = 6,
            minute = 0,
            enabled = true,
            daysOfWeek = listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L),
          ),
          DailyReminder(
            id = "morning_appsc_default",
            userId = uid,
            title = "Morning APPSC Group 1 Focus",
            message = "60-minute distraction-free study block. Stay consistent!",
            category = ReminderCategory.MORNING_APPSC.wireValue,
            hour = 7,
            minute = 30,
            enabled = true,
            daysOfWeek = listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L),
          ),
          DailyReminder(
            id = "evening_appsc_default",
            userId = uid,
            title = "Evening APPSC Group 1 Revision",
            message = "Lock in your second 60-minute study session for today.",
            category = ReminderCategory.EVENING_APPSC.wireValue,
            hour = 18,
            minute = 30,
            enabled = true,
            daysOfWeek = listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L),
          ),
          DailyReminder(
            id = "steady_sleep_default",
            userId = uid,
            title = "Steady Sleep Wind-Down",
            message = "Dim screens and prepare for steady restorative sleep.",
            category = ReminderCategory.SLEEP.wireValue,
            hour = 22,
            minute = 15,
            enabled = true,
            daysOfWeek = listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L),
          ),
        )

      for (reminder in defaults) {
        val payload =
          mapOf(
            "userId" to uid,
            "title" to reminder.title,
            "message" to reminder.message,
            "category" to reminder.category,
            "hour" to reminder.hour,
            "minute" to reminder.minute,
            "enabled" to reminder.enabled,
            "daysOfWeek" to reminder.daysOfWeek,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        colRef.document(reminder.id).set(payload).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, colRef.path)
      throw e
    }
  }

  suspend fun saveUserSettings(settings: UserSettings) {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    try {
      val snapshot = docRef.get().await()
      if (!snapshot.exists()) {
        val payload =
          mapOf(
            "userId" to uid,
            "displayName" to settings.displayName.take(80),
            "targetExerciseMinutes" to settings.targetExerciseMinutes.coerceIn(10, 240),
            "targetMorningStudyMinutes" to settings.targetMorningStudyMinutes.coerceIn(10, 240),
            "targetEveningStudyMinutes" to settings.targetEveningStudyMinutes.coerceIn(10, 240),
            "targetSleepHours" to settings.targetSleepHours.coerceIn(4.0, 12.0),
            "targetBedtimeMinutes" to settings.targetBedtimeMinutes.coerceIn(0, 1439),
            "targetWakeTimeMinutes" to settings.targetWakeTimeMinutes.coerceIn(0, 1439),
            "targetWaterGlasses" to settings.targetWaterGlasses.coerceIn(1, 20),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.set(payload).await()
      } else {
        val updateMap =
          mapOf(
            "displayName" to settings.displayName.take(80),
            "targetExerciseMinutes" to settings.targetExerciseMinutes.coerceIn(10, 240),
            "targetMorningStudyMinutes" to settings.targetMorningStudyMinutes.coerceIn(10, 240),
            "targetEveningStudyMinutes" to settings.targetEveningStudyMinutes.coerceIn(10, 240),
            "targetSleepHours" to settings.targetSleepHours.coerceIn(4.0, 12.0),
            "targetBedtimeMinutes" to settings.targetBedtimeMinutes.coerceIn(0, 1439),
            "targetWakeTimeMinutes" to settings.targetWakeTimeMinutes.coerceIn(0, 1439),
            "targetWaterGlasses" to settings.targetWaterGlasses.coerceIn(1, 20),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.update(updateMap).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  private suspend fun ensureDailyLogDoc(uid: String, dateKey: String): Boolean {
    val docRef = db.collection("users").document(uid).collection("dailyLogs").document(dateKey)
    val snapshot = docRef.get().await()
    if (!snapshot.exists()) {
      val createPayload =
        mapOf(
          "userId" to uid,
          "dateKey" to dateKey,
          "exerciseMinutes" to 0,
          "morningStudyMinutes" to 0,
          "eveningStudyMinutes" to 0,
          "sleepHours" to 0.0,
          "bedtimeMinutes" to 22 * 60 + 30,
          "wakeTimeMinutes" to 6 * 60,
          "sleepQuality" to 0,
          "screenFreeWindDown" to false,
          "waterGlasses" to 0,
          "mindfulMealsCount" to 0,
          "totalMealsCount" to 0,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      docRef.set(createPayload).await()
      return true
    }
    return false
  }

  suspend fun updateSleepLog(
    dateKey: String,
    sleepHours: Double,
    bedtimeMinutes: Int,
    wakeTimeMinutes: Int,
    sleepQuality: Int,
    screenFreeWindDown: Boolean,
  ) {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("dailyLogs").document(dateKey)
    try {
      ensureDailyLogDoc(uid, dateKey)
      val updateMap =
        mapOf(
          "sleepHours" to sleepHours.coerceIn(0.0, 24.0),
          "bedtimeMinutes" to bedtimeMinutes.coerceIn(0, 1439),
          "wakeTimeMinutes" to wakeTimeMinutes.coerceIn(0, 1439),
          "sleepQuality" to sleepQuality.coerceIn(0, 5),
          "screenFreeWindDown" to screenFreeWindDown,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      docRef.update(updateMap).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun updateWaterGlasses(dateKey: String, waterGlasses: Int) {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("dailyLogs").document(dateKey)
    try {
      ensureDailyLogDoc(uid, dateKey)
      docRef
        .update(
          mapOf(
            "waterGlasses" to waterGlasses.coerceIn(0, 30),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  private suspend fun recalculateDailySessionTotals(uid: String, dateKey: String) {
    val sessionsSnapshot =
      db.collection("users")
        .document(uid)
        .collection("sessions")
        .whereEqualTo("dateKey", dateKey)
        .get()
        .await()
    val sessions =
      sessionsSnapshot.toObjects(
        FocusSession::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
      )
    val exerciseMins =
      sessions
        .filter { it.category == SessionCategory.EXERCISE.wireValue }
        .sumOf { it.durationMinutes }
        .coerceIn(0, 600)
    val morningMins =
      sessions
        .filter { it.category == SessionCategory.MORNING_APPSC.wireValue }
        .sumOf { it.durationMinutes }
        .coerceIn(0, 600)
    val eveningMins =
      sessions
        .filter { it.category == SessionCategory.EVENING_APPSC.wireValue }
        .sumOf { it.durationMinutes }
        .coerceIn(0, 600)

    ensureDailyLogDoc(uid, dateKey)
    db.collection("users")
      .document(uid)
      .collection("dailyLogs")
      .document(dateKey)
      .update(
        mapOf(
          "exerciseMinutes" to exerciseMins,
          "morningStudyMinutes" to morningMins,
          "eveningStudyMinutes" to eveningMins,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      )
      .await()
  }

  private suspend fun recalculateDailyMealTotals(uid: String, dateKey: String) {
    val mealsSnapshot =
      db.collection("users")
        .document(uid)
        .collection("meals")
        .whereEqualTo("dateKey", dateKey)
        .get()
        .await()
    val meals =
      mealsSnapshot.toObjects(
        MealEntry::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
      )
    val mindfulCount = meals.count { it.isMindful }.coerceIn(0, 20)
    val totalCount = meals.size.coerceIn(0, 20)

    ensureDailyLogDoc(uid, dateKey)
    db.collection("users")
      .document(uid)
      .collection("dailyLogs")
      .document(dateKey)
      .update(
        mapOf(
          "mindfulMealsCount" to mindfulCount,
          "totalMealsCount" to totalCount,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      )
      .await()
  }

  suspend fun addFocusSession(
    dateKey: String,
    category: SessionCategory,
    topic: String,
    paperTag: String,
    durationMinutes: Int,
    notes: String,
  ): String {
    val uid = requireUserId()
    val colRef = db.collection("users").document(uid).collection("sessions")
    val docRef = colRef.document()
    val payload =
      mapOf(
        "userId" to uid,
        "dateKey" to dateKey,
        "category" to category.wireValue,
        "topic" to topic.trim().ifEmpty { category.displayLabel }.take(120),
        "paperTag" to paperTag.trim().take(60),
        "durationMinutes" to durationMinutes.coerceIn(1, 360),
        "notes" to notes.trim().take(500),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    try {
      docRef.set(payload).await()
      recalculateDailySessionTotals(uid, dateKey)
      return docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteFocusSession(sessionId: String, dateKey: String) {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("sessions").document(sessionId)
    try {
      docRef.delete().await()
      recalculateDailySessionTotals(uid, dateKey)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  suspend fun addMealEntry(
    dateKey: String,
    mealType: String,
    description: String,
    distractionFree: Boolean,
    slowPace: Boolean,
    satisfiedStop: Boolean,
    nutritionQuality: String,
  ): String {
    val uid = requireUserId()
    val colRef = db.collection("users").document(uid).collection("meals")
    val docRef = colRef.document()
    val payload =
      mapOf(
        "userId" to uid,
        "dateKey" to dateKey,
        "mealType" to mealType,
        "description" to description.trim().ifEmpty { "Mindful meal" }.take(160),
        "distractionFree" to distractionFree,
        "slowPace" to slowPace,
        "satisfiedStop" to satisfiedStop,
        "nutritionQuality" to nutritionQuality,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    try {
      docRef.set(payload).await()
      recalculateDailyMealTotals(uid, dateKey)
      return docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteMealEntry(mealId: String, dateKey: String) {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("meals").document(mealId)
    try {
      docRef.delete().await()
      recalculateDailyMealTotals(uid, dateKey)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  suspend fun saveReminder(reminder: DailyReminder): String {
    val uid = requireUserId()
    val colRef = db.collection("users").document(uid).collection("reminders")
    val isNew = reminder.id.isBlank()
    val docRef = if (isNew) colRef.document() else colRef.document(reminder.id)
    try {
      val snapshot = if (isNew) null else docRef.get().await()
      if (snapshot == null || !snapshot.exists()) {
        val createPayload =
          mapOf(
            "userId" to uid,
            "title" to reminder.title.trim().ifEmpty { "Daily Reminder" }.take(80),
            "message" to reminder.message.trim().take(200),
            "category" to reminder.category,
            "hour" to reminder.hour.coerceIn(0, 23),
            "minute" to reminder.minute.coerceIn(0, 59),
            "enabled" to reminder.enabled,
            "daysOfWeek" to reminder.daysOfWeek.take(7),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.set(createPayload).await()
      } else {
        val updateMap =
          mapOf(
            "title" to reminder.title.trim().ifEmpty { "Daily Reminder" }.take(80),
            "message" to reminder.message.trim().take(200),
            "category" to reminder.category,
            "hour" to reminder.hour.coerceIn(0, 23),
            "minute" to reminder.minute.coerceIn(0, 59),
            "enabled" to reminder.enabled,
            "daysOfWeek" to reminder.daysOfWeek.take(7),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.update(updateMap).await()
      }
      return docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  suspend fun toggleReminder(reminder: DailyReminder, enabled: Boolean) {
    val uid = requireUserId()
    require(reminder.id.isNotBlank()) { "Reminder ID must not be blank" }
    val docRef = db.collection("users").document(uid).collection("reminders").document(reminder.id)
    try {
      docRef
        .update(
          mapOf(
            "enabled" to enabled,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteReminder(reminderId: String) {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("reminders").document(reminderId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }
}
