package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class SessionCategory(
  val wireValue: String,
  val displayLabel: String,
  val shortLabel: String,
  val defaultMinutes: Int,
) {
  EXERCISE("EXERCISE", "Daily Exercise", "Exercise", 60),
  MORNING_APPSC("MORNING_APPSC", "Morning APPSC Group 1", "Morning G1", 60),
  EVENING_APPSC("EVENING_APPSC", "Evening APPSC Group 1", "Evening G1", 60);

  companion object {
    fun fromWire(value: String): SessionCategory =
      entries.firstOrNull { it.wireValue == value } ?: MORNING_APPSC
  }
}

enum class MealType(val wireValue: String, val displayLabel: String) {
  BREAKFAST("BREAKFAST", "Breakfast"),
  LUNCH("LUNCH", "Lunch"),
  DINNER("DINNER", "Dinner"),
  SNACK("SNACK", "Healthy Snack");

  companion object {
    fun fromWire(value: String): MealType =
      entries.firstOrNull { it.wireValue == value } ?: BREAKFAST
  }
}

enum class NutritionQuality(val wireValue: String, val displayLabel: String) {
  BALANCED("BALANCED", "Whole & Balanced"),
  LIGHT("LIGHT", "Light & Fresh"),
  INDULGENT("INDULGENT", "Rich / Heavy");

  companion object {
    fun fromWire(value: String): NutritionQuality =
      entries.firstOrNull { it.wireValue == value } ?: BALANCED
  }
}

enum class ReminderCategory(val wireValue: String, val displayLabel: String) {
  EXERCISE("EXERCISE", "Morning Exercise"),
  MORNING_APPSC("MORNING_APPSC", "Morning APPSC Focus"),
  EVENING_APPSC("EVENING_APPSC", "Evening APPSC Focus"),
  SLEEP("SLEEP", "Steady Sleep Wind-Down"),
  MINDFUL_EATING("MINDFUL_EATING", "Mindful Meal & Hydration");

  companion object {
    fun fromWire(value: String): ReminderCategory =
      entries.firstOrNull { it.wireValue == value } ?: MORNING_APPSC
  }
}

object AppscSyllabus {
  val papers =
    listOf(
      "Paper I · General Essay",
      "Paper II · History & AP Culture",
      "Paper III · Polity & Governance",
      "Paper IV · Economy & AP Dev",
      "Paper V · Science & Environment",
      "Prelims · GS & Mental Ability",
    )

  val exerciseTypes =
    listOf(
      "Brisk Walk / Run",
      "Yoga & Mobility",
      "Strength Training",
      "Cycling / Cardio",
      "Pranayama & Core",
    )
}

@IgnoreExtraProperties
data class UserSettings(
  val userId: String = "",
  val displayName: String = "",
  val targetExerciseMinutes: Int = 60,
  val targetMorningStudyMinutes: Int = 60,
  val targetEveningStudyMinutes: Int = 60,
  val targetSleepHours: Double = 7.5,
  val targetBedtimeMinutes: Int = 22 * 60 + 30,
  val targetWakeTimeMinutes: Int = 5 * 60 + 30,
  val targetWaterGlasses: Int = 8,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class DailyLog(
  val userId: String = "",
  val dateKey: String = "",
  val exerciseMinutes: Int = 0,
  val morningStudyMinutes: Int = 0,
  val eveningStudyMinutes: Int = 0,
  val sleepHours: Double = 0.0,
  val bedtimeMinutes: Int = 22 * 60 + 30,
  val wakeTimeMinutes: Int = 6 * 60,
  val sleepQuality: Int = 0,
  val screenFreeWindDown: Boolean = false,
  val waterGlasses: Int = 0,
  val mindfulMealsCount: Int = 0,
  val totalMealsCount: Int = 0,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class FocusSession(
  @DocumentId val id: String = "",
  val userId: String = "",
  val dateKey: String = "",
  val category: String = SessionCategory.MORNING_APPSC.wireValue,
  val topic: String = "",
  val paperTag: String = "",
  val durationMinutes: Int = 60,
  val notes: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class MealEntry(
  @DocumentId val id: String = "",
  val userId: String = "",
  val dateKey: String = "",
  val mealType: String = MealType.BREAKFAST.wireValue,
  val description: String = "",
  val distractionFree: Boolean = true,
  val slowPace: Boolean = true,
  val satisfiedStop: Boolean = true,
  val nutritionQuality: String = NutritionQuality.BALANCED.wireValue,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val mindfulScore: Int
    get() =
      listOf(distractionFree, slowPace, satisfiedStop).count { it } +
        if (nutritionQuality != NutritionQuality.INDULGENT.wireValue) 1 else 0

  val isMindful: Boolean
    get() = listOf(distractionFree, slowPace, satisfiedStop).count { it } >= 2
}

@IgnoreExtraProperties
data class DailyReminder(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val message: String = "",
  val category: String = ReminderCategory.MORNING_APPSC.wireValue,
  val hour: Int = 6,
  val minute: Int = 0,
  val enabled: Boolean = true,
  val daysOfWeek: List<Long> = listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)
