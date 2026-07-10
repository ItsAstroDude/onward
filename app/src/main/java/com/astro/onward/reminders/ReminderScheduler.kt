package com.astro.onward.reminders

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.astro.onward.data.OnwardDatabase
import com.astro.onward.data.ReminderSetting
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Each reminder is a uniquely-named OneTimeWorkRequest delayed until its next
 * occurrence; the worker fires the notification and schedules the next one.
 * WorkManager persists across reboots, so no boot receiver is needed.
 * Timing is "around then" rather than to-the-second — fine for meal nudges.
 */
class ReminderScheduler(private val context: Context) {

    private fun workName(id: Int) = "reminder_$id"

    /** Align WorkManager with the database: schedule what's on, cancel what's off. */
    suspend fun syncAll() {
        val db = OnwardDatabase.get(context)
        val started = db.settingsDao().get()?.started == true
        for (reminder in db.reminderDao().getAll()) {
            if (started && reminder.enabled) schedule(reminder) else cancel(reminder.id)
        }
    }

    fun schedule(reminder: ReminderSetting, now: LocalDateTime = LocalDateTime.now()) {
        val delay = delayUntilNext(reminder, now) ?: return
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay)
            .setInputData(workDataOf(ReminderWorker.KEY_ID to reminder.id))
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(workName(reminder.id), ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(id: Int) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(id))
    }

    /** Next occurrence of the reminder's time on an enabled weekday. Null if no day is enabled. */
    fun delayUntilNext(reminder: ReminderSetting, now: LocalDateTime): Duration? {
        if (reminder.daysMask == 0) return null
        val time = LocalTime.of(reminder.hour, reminder.minute)
        for (offset in 0..7) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            val candidate = LocalDateTime.of(date, time)
            if (candidate <= now) continue
            val dayBit = 1 shl (date.dayOfWeek.value - 1) // Monday = bit 0
            if (reminder.daysMask and dayBit != 0) return Duration.between(now, candidate)
        }
        return null
    }
}
