package com.astro.onward.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.astro.onward.data.OnwardDatabase

class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getInt(KEY_ID, -1)
        if (id == -1) return Result.failure()

        val db = OnwardDatabase.get(applicationContext)
        val reminder = db.reminderDao().get(id) ?: return Result.success()
        val started = db.settingsDao().get()?.started == true

        if (started && reminder.enabled) {
            Notifications.show(applicationContext, reminder.id, reminder.message, reminder.destination)
            ReminderScheduler(applicationContext).schedule(reminder)
        }
        return Result.success()
    }

    companion object {
        const val KEY_ID = "reminder_id"
    }
}
