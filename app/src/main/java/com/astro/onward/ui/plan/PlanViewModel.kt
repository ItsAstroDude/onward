package com.astro.onward.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astro.onward.OnwardApp
import com.astro.onward.data.MealOption
import com.astro.onward.data.MealSlot
import com.astro.onward.data.PlanDay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PlanUiState(
    val days: List<PlanDay> = emptyList(),
    val options: List<MealOption> = emptyList(),
    val todayIndex: Int = LocalDate.now().dayOfWeek.value - 1,
)

class PlanViewModel(app: OnwardApp) : ViewModel() {

    private val db = app.database

    val state = combine(
        db.planDao().observeDays(),
        db.planDao().observeOptions(),
    ) { days, options ->
        PlanUiState(days, options, LocalDate.now().dayOfWeek.value - 1)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanUiState())

    fun setMeal(day: PlanDay, slot: MealSlot, example: String) {
        viewModelScope.launch {
            db.planDao().updateDay(
                when (slot) {
                    MealSlot.BREAKFAST -> day.copy(breakfast = example)
                    MealSlot.LUNCH -> day.copy(lunchExample = example)
                    MealSlot.DINNER -> day.copy(dinnerExample = example)
                },
            )
        }
    }

    fun addCustomOption(day: PlanDay, slot: MealSlot, example: String) {
        viewModelScope.launch {
            db.planDao().insertOption(MealOption(slot = slot, example = example, custom = true))
            setMeal(day, slot, example)
        }
    }
}
