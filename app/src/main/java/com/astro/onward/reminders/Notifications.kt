package com.astro.onward.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.astro.onward.MainActivity
import com.astro.onward.R

object Notifications {
    const val CHANNEL_ID = "reminders"
    const val EXTRA_DESTINATION = "destination"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Meal & check-off reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Gentle nudges for the eating pattern"
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun show(context: Context, id: Int, message: String, destination: String?) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (destination != null) putExtra(EXTRA_DESTINATION, destination)
        }
        val pending = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_onward)
            .setContentTitle("Onward")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pending)
            .setAutoCancel(true)

        // The check-off nudge gets a one-tap action — no need to open the app.
        if (destination == "today") {
            val checkIn = Intent(context, CheckInReceiver::class.java)
                .setAction(CheckInReceiver.ACTION)
            builder.addAction(
                0, "✓ Hit it",
                PendingIntent.getBroadcast(
                    context, 100 + id, checkIn,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
        }

        context.getSystemService(NotificationManager::class.java).notify(id, builder.build())
    }
}
