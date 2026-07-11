package com.astro.onward.reminders

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import com.astro.onward.data.DayEntry
import com.astro.onward.data.DayStatus
import com.astro.onward.data.OnwardDatabase
import com.astro.onward.widget.OnwardWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Handles the "✓ Hit it" action on the check-off notification: marks today as
 * HIT without opening the app. An explicit "yes" from the user, so it also
 * upgrades an earlier "not really" if they changed their mind.
 */
class CheckInReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                OnwardDatabase.get(context).dayEntryDao()
                    .upsert(DayEntry(LocalDate.now().toEpochDay(), DayStatus.HIT))
                context.getSystemService(NotificationManager::class.java)
                    .cancel(CHECK_OFF_REMINDER_ID)
                OnwardWidget().updateAll(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION = "com.astro.onward.CHECK_IN"
        const val CHECK_OFF_REMINDER_ID = 5
    }
}
