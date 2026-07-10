package com.astro.onward.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DayEntryDao {
    @Query("SELECT * FROM day_entries ORDER BY epochDay")
    fun observeAll(): Flow<List<DayEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: DayEntry)

    @Query("DELETE FROM day_entries WHERE epochDay = :epochDay")
    suspend fun delete(epochDay: Long)
}

@Dao
interface PlanDao {
    @Query("SELECT * FROM plan_days ORDER BY dayIndex")
    fun observeDays(): Flow<List<PlanDay>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDays(days: List<PlanDay>)

    @Update
    suspend fun updateDay(day: PlanDay)

    @Query("SELECT * FROM meal_options ORDER BY id")
    fun observeOptions(): Flow<List<MealOption>>

    @Insert
    suspend fun insertOptions(options: List<MealOption>)

    @Insert
    suspend fun insertOption(option: MealOption): Long
}

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping_items ORDER BY position, id")
    fun observeAll(): Flow<List<ShoppingItem>>

    @Insert
    suspend fun insertAll(items: List<ShoppingItem>)

    @Insert
    suspend fun insert(item: ShoppingItem): Long

    @Update
    suspend fun update(item: ShoppingItem)

    @Delete
    suspend fun delete(item: ShoppingItem)

    @Query("SELECT MAX(position) FROM shopping_items")
    suspend fun maxPosition(): Int?

    @Query("UPDATE shopping_items SET checked = 0")
    suspend fun uncheckAll()
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminder_settings ORDER BY id")
    fun observeAll(): Flow<List<ReminderSetting>>

    @Query("SELECT * FROM reminder_settings ORDER BY id")
    suspend fun getAll(): List<ReminderSetting>

    @Query("SELECT * FROM reminder_settings WHERE id = :id")
    suspend fun get(id: Int): ReminderSetting?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(reminders: List<ReminderSetting>)

    @Update
    suspend fun update(reminder: ReminderSetting)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 0")
    fun observe(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 0")
    suspend fun get(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: AppSettings)
}

@Dao
interface CalorieDao {
    @Query("SELECT * FROM calorie_entries WHERE epochDay >= :fromEpochDay ORDER BY timestamp")
    fun observeFrom(fromEpochDay: Long): Flow<List<CalorieEntry>>

    @Insert
    suspend fun insert(entry: CalorieEntry)

    @Query("DELETE FROM calorie_entries WHERE id = :id")
    suspend fun delete(id: Long)
}
