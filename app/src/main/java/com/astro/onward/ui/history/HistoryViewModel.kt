package com.astro.onward.ui.history

import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astro.onward.OnwardApp
import com.astro.onward.data.DayEntry
import com.astro.onward.data.DayStatus
import com.astro.onward.data.StreakCalculator
import com.astro.onward.widget.OnwardWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class HistoryUiState(
    val month: YearMonth = YearMonth.now(),
    val today: LocalDate = LocalDate.now(),
    val statuses: Map<Long, DayStatus> = emptyMap(),
    val notes: Map<Long, String> = emptyMap(),
    val freezeDays: Set<Long> = emptySet(),
) {
    val canGoForward: Boolean get() = month < YearMonth.from(today)
}

class HistoryViewModel(private val app: OnwardApp) : ViewModel() {

    private val db = app.database
    private val month = MutableStateFlow(YearMonth.now())

    val state = combine(month, db.dayEntryDao().observeAll()) { month, entries ->
        val today = LocalDate.now()
        val statuses = entries.associate { it.epochDay to it.status }
        HistoryUiState(
            month = month,
            today = today,
            statuses = statuses,
            notes = entries.mapNotNull { e -> e.note?.let { e.epochDay to it } }.toMap(),
            freezeDays = StreakCalculator.calculate(statuses, today).freezeDays.toSet(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun previousMonth() {
        month.value = month.value.minusMonths(1)
    }

    fun nextMonth() {
        if (state.value.canGoForward) month.value = month.value.plusMonths(1)
    }

    /** Rewriting the past is allowed — it's his data, and forgiveness is the brand. */
    fun setDay(date: LocalDate, status: DayStatus, note: String) {
        viewModelScope.launch {
            val epochDay = date.toEpochDay()
            if (status == DayStatus.NONE && note.isBlank()) {
                db.dayEntryDao().delete(epochDay)
            } else {
                db.dayEntryDao().upsert(DayEntry(epochDay, status, note.ifBlank { null }))
            }
            OnwardWidget().updateAll(app)
        }
    }
}
