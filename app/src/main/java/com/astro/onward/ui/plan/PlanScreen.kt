package com.astro.onward.ui.plan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.astro.onward.data.MealSlot
import com.astro.onward.data.PlanDay
import com.astro.onward.data.SeedData
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

private data class SheetTarget(val day: PlanDay, val slot: MealSlot)

@Composable
fun PlanScreen() {
    val vm: PlanViewModel = viewModel { PlanViewModel(this[APPLICATION_KEY] as OnwardApp) }
    val state by vm.state.collectAsStateWithLifecycle()
    var sheetTarget by remember { mutableStateOf<SheetTarget?>(null) }
    val listState = rememberLazyListState()

    // Land on today's card, not Monday's.
    var scrolledToToday by remember { mutableStateOf(false) }
    LaunchedEffect(state.days) {
        if (!scrolledToToday && state.days.isNotEmpty()) {
            scrolledToToday = true
            if (state.todayIndex > 0) listState.animateScrollToItem(state.todayIndex + 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text("The week, on rotation", style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Every day has the same shape: ${SeedData.LUNCH_TEMPLATE} — dinner is the lighter version. " +
                        "Tap any meal to swap it. Nothing is locked.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
            }
        }
        items(state.days, key = { it.dayIndex }) { day ->
            DayCard(
                day = day,
                isToday = day.dayIndex == state.todayIndex,
                onMealClick = { slot -> sheetTarget = SheetTarget(day, slot) },
            )
        }
    }

    sheetTarget?.let { target ->
        SwapSheet(
            target = target,
            options = state.options.filter { it.slot == target.slot }.map { it.example },
            onPick = { example ->
                vm.setMeal(target.day, target.slot, example)
                sheetTarget = null
            },
            onCustom = { example ->
                vm.addCustomOption(target.day, target.slot, example)
                sheetTarget = null
            },
            onDismiss = { sheetTarget = null },
        )
    }
}

@Composable
private fun DayCard(day: PlanDay, isToday: Boolean, onMealClick: (MealSlot) -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isToday) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            },
        ),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    DayOfWeek.of(day.dayIndex + 1).getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                if (isToday) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.tertiary,
                    ) {
                        Text(
                            "today",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            PlanMealRow("🥤", "Breakfast", day.breakfast) { onMealClick(MealSlot.BREAKFAST) }
            Spacer(Modifier.height(10.dp))
            PlanMealRow("🍗", "Lunch — ${day.lunchTemplate}", day.lunchExample) { onMealClick(MealSlot.LUNCH) }
            Spacer(Modifier.height(10.dp))
            PlanMealRow("🥗", "Dinner — ${day.dinnerTemplate}", day.dinnerExample) { onMealClick(MealSlot.DINNER) }
        }
    }
}

@Composable
private fun PlanMealRow(emoji: String, label: String, example: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
    ) {
        Text(emoji, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(example, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwapSheet(
    target: SheetTarget,
    options: List<String>,
    onPick: (String) -> Unit,
    onCustom: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var custom by remember { mutableStateOf("") }
    val current = when (target.slot) {
        MealSlot.BREAKFAST -> target.day.breakfast
        MealSlot.LUNCH -> target.day.lunchExample
        MealSlot.DINNER -> target.day.dinnerExample
    }
    val slotName = target.slot.name.lowercase(Locale.ENGLISH)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .imePadding(),
        ) {
            Text(
                "Swap $slotName — ${DayOfWeek.of(target.day.dayIndex + 1).getDisplayName(TextStyle.FULL, Locale.ENGLISH)}",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(8.dp))

            (options + SeedData.OPTION_LEFTOVERS).distinct().forEach { option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(option) }
                        .padding(vertical = 4.dp),
                ) {
                    RadioButton(selected = option == current, onClick = { onPick(option) })
                    Text(option, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = custom,
                    onValueChange = { custom = it },
                    label = { Text("Or your own…") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = { if (custom.isNotBlank()) onCustom(custom.trim()) },
                    enabled = custom.isNotBlank(),
                ) { Text("Use") }
            }
        }
    }
}
