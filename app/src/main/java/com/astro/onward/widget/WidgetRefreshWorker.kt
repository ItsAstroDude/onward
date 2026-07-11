package com.astro.onward.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Re-renders the widget just after midnight so "not checked yet" and the day's
 * plan roll over even if the app isn't opened. Reschedules itself daily.
 */
class WidgetRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        OnwardWidget().updateAll(applicationContext)
        schedule(applicationContext)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "widget_midnight_refresh"

        fun schedule(context: Context) {
            val now = LocalDateTime.now()
            val next = now.toLocalDate().plusDays(1).atTime(LocalTime.of(0, 5))
            val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
                .setInitialDelay(Duration.between(now, next))
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
