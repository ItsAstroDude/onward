package com.astro.onward.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astro.onward.OnwardApp
import com.astro.onward.data.AppSettings
import com.astro.onward.data.ReminderSetting
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val reminders: List<ReminderSetting> = emptyList(),
)

class SettingsViewModel(private val app: OnwardApp) : ViewModel() {

    private val db = app.database

    val state = combine(
        db.settingsDao().observe(),
        db.reminderDao().observeAll(),
    ) { settings, reminders ->
        SettingsUiState(settings ?: AppSettings(), reminders)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    private fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            val current = db.settingsDao().get() ?: AppSettings()
            db.settingsDao().upsert(transform(current))
            app.scheduler.syncAll()
        }
    }

    fun setStarted(started: Boolean) = updateSettings { it.copy(started = started) }

    fun setCalorieTracker(enabled: Boolean) = updateSettings { it.copy(calorieTrackerEnabled = enabled) }

    fun setReminderEnabled(reminder: ReminderSetting, enabled: Boolean) {
        viewModelScope.launch {
            db.reminderDao().update(reminder.copy(enabled = enabled))
            app.scheduler.syncAll()
        }
    }

    fun setReminderTime(reminder: ReminderSetting, hour: Int, minute: Int) {
        viewModelScope.launch {
            db.reminderDao().update(reminder.copy(hour = hour, minute = minute))
            app.scheduler.syncAll()
        }
    }
}
