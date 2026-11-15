package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.DailyReminder
import com.example.data.model.MealType
import com.example.data.model.NutritionQuality
import com.example.data.model.ReminderCategory
import com.example.data.model.SessionCategory
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VitalPrepRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun ensureUserInitialized_andAddFocusSession_updatesDailyLogTotals() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = VitalPrepRepository(firestore, auth)

    withTimeout(DEFAULT_TIMEOUT_MS) { repo.ensureUserInitialized("Pragathi") }

    val dateKey = "2026-11-15"
    val initialLog = withTimeout(FLOW_TIMEOUT_MS) { repo.observeDailyLog(uid, dateKey).first() }
    val sessionId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.addFocusSession(
          dateKey = dateKey,
          category = SessionCategory.MORNING_APPSC,
          topic = "Paper III Polity - Fundamental Rights",
          paperTag = "Paper III · Polity & Governance",
          durationMinutes = 60,
          notes = "Completed Articles 12-35",
        )
      }
    assertTrue(sessionId.isNotBlank())

    val expectedMinutes = initialLog.morningStudyMinutes + 60
    val dailyLog =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeDailyLog(uid, dateKey).first { it.morningStudyMinutes == expectedMinutes }
      }
    assertEquals(expectedMinutes, dailyLog.morningStudyMinutes)

    val sessions =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeSessionsForDate(uid, dateKey).first { list -> list.any { it.id == sessionId } }
      }
    assertTrue(sessions.any { it.id == sessionId && it.topic == "Paper III Polity - Fundamental Rights" })
  }

  @Test
  fun addMealAndSleepLog_authenticatedOwner_persistsMindfulData() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = VitalPrepRepository(firestore, auth)
    val dateKey = "2026-11-16"

    val mealId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.addMealEntry(
          dateKey = dateKey,
          mealType = MealType.BREAKFAST.wireValue,
          description = "Millet dosa and sambar",
          distractionFree = true,
          slowPace = true,
          satisfiedStop = true,
          nutritionQuality = NutritionQuality.BALANCED.wireValue,
        )
      }
    assertTrue(mealId.isNotBlank())

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.updateSleepLog(
        dateKey = dateKey,
        sleepHours = 7.5,
        bedtimeMinutes = 22 * 60 + 30,
        wakeTimeMinutes = 6 * 60,
        sleepQuality = 5,
        screenFreeWindDown = true,
      )
    }

    val dailyLog =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeDailyLog(uid, dateKey).first {
          it.mindfulMealsCount >= 1 && it.sleepQuality == 5
        }
      }
    assertEquals(7.5, dailyLog.sleepHours, 0.01)
    assertTrue(dailyLog.mindfulMealsCount >= 1)
  }

  @Test
  fun saveAndToggleReminder_authenticatedOwner_persistsCustomSchedule() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = VitalPrepRepository(firestore, auth)

    val remId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.saveReminder(
          DailyReminder(
            title = "Morning Exercise Cue",
            message = "60 min brisk walk & mobility",
            category = ReminderCategory.EXERCISE.wireValue,
            hour = 5,
            minute = 45,
            enabled = true,
          )
        )
      }
    assertTrue(remId.isNotBlank())

    val reminders =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeReminders(uid).first { list -> list.any { it.id == remId } }
      }
    val created = reminders.first { it.id == remId }
    withTimeout(DEFAULT_TIMEOUT_MS) { repo.toggleReminder(created, false) }

    val updated =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeReminders(uid).first { list -> list.any { it.id == remId && !it.enabled } }
      }
    assertTrue(updated.any { it.id == remId && !it.enabled })
  }

  @Test
  fun crossUserRead_bobCannotReadAliceSessions() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = VitalPrepRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.addFocusSession(
        dateKey = "2026-11-15",
        category = SessionCategory.EXERCISE,
        topic = "Morning Yoga & Jog",
        paperTag = "Yoga & Mobility",
        durationMinutes = 60,
        notes = "",
      )
    }

    signInTestUser(BOB_EMAIL)
    var caught: FirebaseFirestoreException? = null
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("users").document(aliceUid).collection("sessions").get().await()
      }
    } catch (e: FirebaseFirestoreException) {
      caught = e
    }
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, caught?.code)
  }

  @Test
  fun observeSessions_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    auth.signOut()
    val repo = VitalPrepRepository(firestore, auth)

    var caught: Exception? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) { repo.observeSessionsForDate(aliceUid, "2026-11-15").first() }
    } catch (e: Exception) {
      caught = e
    }
    val firestoreException =
      checkNotNull(caught?.unwrapFirestoreException()) {
        "Expected nested FirebaseFirestoreException, got $caught"
      }
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
  }

  private companion object {
    const val ALICE_EMAIL = "alice_vp@test.com"
    const val BOB_EMAIL = "bob_vp@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
