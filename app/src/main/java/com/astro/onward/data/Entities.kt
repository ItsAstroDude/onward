package com.astro.onward.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** How a single day went. There is deliberately no "failed" state. */
enum class DayStatus { HIT, MISS, FREEZE_USED, NONE }

/** One row per calendar day the user has answered. Key = LocalDate.toEpochDay(). */
@Entity(tableName = "day_entries")
data class DayEntry(
    @PrimaryKey val epochDay: Long,
    val status: DayStatus,
    val note: String? = null,
)

enum class MealSlot { BREAKFAST, LUNCH, DINNER }

/**
 * One day of the rotating 7-day plan. dayIndex 0 = Monday .. 6 = Sunday.
 * Each meal is a template (the rule) + a concrete example (what to actually make).
 */
@Entity(tableName = "plan_days")
data class PlanDay(
    @PrimaryKey val dayIndex: Int,
    val breakfast: String,
    val lunchTemplate: String,
    val lunchExample: String,
    val dinnerTemplate: String,
    val dinnerExample: String,
)

/** Pool of alternative examples the user can swap into a plan slot. */
@Entity(tableName = "meal_options")
data class MealOption(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val slot: MealSlot,
    val example: String,
    val custom: Boolean = false,
)

@Entity(tableName = "shopping_items")
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val checked: Boolean = false,
    val custom: Boolean = false,
    val position: Int = 0,
)

/**
 * One scheduled nudge. daysMask: bit 0 = Monday .. bit 6 = Sunday (127 = daily).
 * All reminders stay OFF until AppSettings.started is true AND enabled is true.
 */
@Entity(tableName = "reminder_settings")
data class ReminderSetting(
    @PrimaryKey val id: Int,
    val label: String,
    val message: String,
    val enabled: Boolean,
    val hour: Int,
    val minute: Int,
    val daysMask: Int,
    val destination: String? = null,
)

/** Single-row settings table (id is always 0). No calorie target field — there is no goal to store. */
@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 0,
    val onboarded: Boolean = false,
    val started: Boolean = false,
    val calorieTrackerEnabled: Boolean = false,
    val seedVersion: Int = 0,
)

/** Rough quick-add calorie entry. Only exists if the loose tracker is enabled. */
@Entity(tableName = "calorie_entries")
data class CalorieEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val label: String,
    val kcal: Int,
    val timestamp: Long,
)
