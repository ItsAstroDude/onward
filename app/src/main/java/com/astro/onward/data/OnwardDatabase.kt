package com.astro.onward.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter fun dayStatusToString(v: DayStatus): String = v.name
    @TypeConverter fun stringToDayStatus(v: String): DayStatus = DayStatus.valueOf(v)
    @TypeConverter fun mealSlotToString(v: MealSlot): String = v.name
    @TypeConverter fun stringToMealSlot(v: String): MealSlot = MealSlot.valueOf(v)
}

@Database(
    entities = [
        DayEntry::class, PlanDay::class, MealOption::class,
        ShoppingItem::class, ReminderSetting::class, AppSettings::class,
        CalorieEntry::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class OnwardDatabase : RoomDatabase() {
    abstract fun dayEntryDao(): DayEntryDao
    abstract fun planDao(): PlanDao
    abstract fun shoppingDao(): ShoppingDao
    abstract fun reminderDao(): ReminderDao
    abstract fun settingsDao(): SettingsDao
    abstract fun calorieDao(): CalorieDao

    companion object {
        @Volatile private var instance: OnwardDatabase? = null

        fun get(context: Context): OnwardDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    OnwardDatabase::class.java,
                    "onward.db",
                ).build().also { instance = it }
            }
    }

    /**
     * Idempotent first-run seeding. Runs on every app start; only inserts when
     * the stored seedVersion is behind. Keeps user edits intact.
     */
    suspend fun ensureSeeded() {
        val settings = settingsDao().get() ?: AppSettings().also { settingsDao().upsert(it) }
        if (settings.seedVersion >= SeedData.SEED_VERSION) return

        planDao().insertDays(SeedData.planDays)
        planDao().insertOptions(SeedData.mealOptions)
        shoppingDao().insertAll(SeedData.shoppingStaples)
        reminderDao().insertAll(SeedData.reminders)
        settingsDao().upsert(settings.copy(seedVersion = SeedData.SEED_VERSION))
    }
}
