package com.astro.onward.ui.today

import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astro.onward.OnwardApp
import com.astro.onward.data.CalorieEntry
import com.astro.onward.data.DayEntry
import com.astro.onward.data.DayStatus
import com.astro.onward.data.PlanDay
import com.astro.onward.data.StreakCalculator
import com.astro.onward.widget.OnwardWidget
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TodayUiState(
    val loading: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val todayStatus: DayStatus = DayStatus.NONE,
    /** NONE + hasHistory → the "And yesterday?" grace chip shows. */
    val yesterdayStatus: DayStatus = DayStatus.NONE,
    val hasHistory: Boolean = false,
    val streak: StreakCalculator.Result = StreakCalculator.calculate(emptyMap(), LocalDate.now()),
    val freezeAbsorbedToday: Boolean = false,
    val plan: PlanDay? = null,
    val calorieEnabled: Boolean = false,
    val todayCalories: List<CalorieEntry> = emptyList(),
    /** Last 7 days (oldest first): epochDay to total kcal. */
    val weekTotals: List<Pair<Long, Int>> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(private val app: OnwardApp) : ViewModel() {

    private val db = app.database
    private val refreshTick = MutableStateFlow(0)

    /** Re-anchor "today" (the date can roll over while the app is backgrounded). */
    fun refresh() {
        refreshTick.value++
    }

    val state = combine(
        refreshTick,
        db.dayEntryDao().observeAll(),
        db.settingsDao().observe(),
        db.planDao().observeDays(),
        refreshTick.flatMapLatest {
            db.calorieDao().observeFrom(LocalDate.now().toEpochDay() - 6)
        },
    ) { _, entries, settings, planDays, calories ->
        val today = LocalDate.now()
        val todayEpoch = today.toEpochDay()
        val byDay = entries.associate { it.epochDay to it.status }
        val streak = StreakCalculator.calculate(byDay, today)
        TodayUiState(
            loading = false,
            today = today,
            todayStatus = byDay[todayEpoch] ?: DayStatus.NONE,
            yesterdayStatus = byDay[todayEpoch - 1] ?: DayStatus.NONE,
            hasHistory = entries.any { it.epochDay < todayEpoch - 1 },
            streak = streak,
            freezeAbsorbedToday = todayEpoch in streak.freezeDays,
            plan = planDays.firstOrNull { it.dayIndex == today.dayOfWeek.value - 1 },
            calorieEnabled = settings?.calorieTrackerEnabled == true,
            todayCalories = calories.filter { it.epochDay == todayEpoch },
            weekTotals = ((todayEpoch - 6)..todayEpoch).map { day ->
                day to calories.filter { it.epochDay == day }.sumOf { it.kcal }
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    fun checkIn(hit: Boolean) = setDay(LocalDate.now().toEpochDay(), hit)

    /** The one-day grace: answer yesterday before it silently counts as a miss. */
    fun checkInYesterday(hit: Boolean) = setDay(LocalDate.now().toEpochDay() - 1, hit)

    private fun setDay(epochDay: Long, hit: Boolean) {
        viewModelScope.launch {
            val status = if (hit) DayStatus.HIT else DayStatus.MISS
            db.dayEntryDao().upsert(DayEntry(epochDay, status))
            OnwardWidget().updateAll(app)
        }
    }

    fun clearToday() {
        viewModelScope.launch {
            db.dayEntryDao().delete(LocalDate.now().toEpochDay())
            OnwardWidget().updateAll(app)
        }
    }

    fun addCalories(label: String, kcal: Int) {
        viewModelScope.launch {
            db.calorieDao().insert(
                CalorieEntry(
                    epochDay = LocalDate.now().toEpochDay(),
                    label = label,
                    kcal = kcal,
                    timestamp = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun removeCalories(id: Long) {
        viewModelScope.launch { db.calorieDao().delete(id) }
    }
}
