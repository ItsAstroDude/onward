package com.astro.onward

import android.app.Application
import com.astro.onward.data.OnwardDatabase
import com.astro.onward.reminders.Notifications
import com.astro.onward.reminders.ReminderScheduler
import com.astro.onward.widget.WidgetRefreshWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class OnwardApp : Application() {

    val database by lazy { OnwardDatabase.get(this) }
    val scheduler by lazy { ReminderScheduler(this) }
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannel(this)
        appScope.launch {
            database.ensureSeeded()
            scheduler.syncAll()
        }
        WidgetRefreshWorker.schedule(this)
    }
}
