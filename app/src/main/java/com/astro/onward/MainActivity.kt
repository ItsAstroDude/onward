package com.astro.onward

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.astro.onward.reminders.Notifications
import com.astro.onward.ui.OnwardRoot
import com.astro.onward.ui.theme.OnwardTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    /** Destination requested by a notification tap ("today", "shopping", ...). */
    private val pendingDestination = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeIntent(intent)
        setContent {
            OnwardTheme {
                OnwardRoot(
                    pendingDestination = pendingDestination,
                    onDestinationConsumed = { pendingDestination.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumeIntent(intent)
    }

    private fun consumeIntent(intent: Intent?) {
        val dest = intent?.getStringExtra(Notifications.EXTRA_DESTINATION) ?: return
        pendingDestination.value = dest
        intent.removeExtra(Notifications.EXTRA_DESTINATION)
    }
}
