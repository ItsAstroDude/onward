package com.astro.onward.ui.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.astro.onward.OnwardApp
import com.astro.onward.data.ReminderSetting
import com.astro.onward.data.SeedData
import java.util.Locale

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel { SettingsViewModel(this[APPLICATION_KEY] as OnwardApp) }
    val state by vm.state.collectAsStateWithLifecycle()
    var timeTarget by remember { mutableStateOf<ReminderSetting?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { vm.setStarted(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
            }
            Text("Settings", style = MaterialTheme.typography.displaySmall)
        }
        Spacer(Modifier.height(12.dp))

        SettingsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Reminders", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Turn on when you're ready — no nagging until then.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.settings.started,
                    onCheckedChange = { on ->
                        if (on) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            vm.setStarted(false)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = MaterialTheme.colorScheme.tertiary,
                    ),
                )
            }

            Spacer(Modifier.height(8.dp))
            state.reminders.forEach { reminder ->
                ReminderRow(
                    reminder = reminder,
                    masterOn = state.settings.started,
                    onToggle = { vm.setReminderEnabled(reminder, it) },
                    onEditTime = { timeTarget = reminder },
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        SettingsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Show me a rough calorie picture", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Optional, off by default. Quick-add estimates and a weekly trend — " +
                            "no targets, no failing, never touches the streak.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.settings.calorieTrackerEnabled,
                    onCheckedChange = vm::setCalorieTracker,
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        SettingsCard {
            Text("The fine print", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                "Onward follows a cardiologist's general recommendations, but it isn't medical advice. " +
                    "Any specific numbers from your doctor — salt limits, cholesterol targets, calorie " +
                    "deficit — override anything in this app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    timeTarget?.let { reminder ->
        TimeDialog(
            reminder = reminder,
            onDismiss = { timeTarget = null },
            onConfirm = { hour, minute ->
                vm.setReminderTime(reminder, hour, minute)
                timeTarget = null
            },
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
    ) {
        Column(Modifier.padding(20.dp)) { content() }
    }
}

@Composable
private fun ReminderRow(
    reminder: ReminderSetting,
    masterOn: Boolean,
    onToggle: (Boolean) -> Unit,
    onEditTime: () -> Unit,
) {
    val timeText = String.format(Locale.ENGLISH, "%02d:%02d", reminder.hour, reminder.minute)
    val repeatText = if (reminder.daysMask == SeedData.DAILY) "daily" else "Sundays"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(reminder.label, style = MaterialTheme.typography.titleSmall)
            Text(
                reminder.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.clickable(onClick = onEditTime),
        ) {
            Text(
                "$timeText · $repeatText",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = reminder.enabled,
            onCheckedChange = onToggle,
            enabled = masterOn,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(
    reminder: ReminderSetting,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    val timeState = rememberTimePickerState(
        initialHour = reminder.hour,
        initialMinute = reminder.minute,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(reminder.label, style = MaterialTheme.typography.headlineSmall) },
        text = { TimePicker(state = timeState) },
        confirmButton = {
            TextButton(onClick = { onConfirm(timeState.hour, timeState.minute) }) { Text("Set") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
