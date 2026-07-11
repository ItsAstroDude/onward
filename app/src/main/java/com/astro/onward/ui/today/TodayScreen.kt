package com.astro.onward.ui.today

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import com.astro.onward.updates.Updates
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.astro.onward.OnwardApp
import com.astro.onward.data.DayStatus
import com.astro.onward.ui.theme.rememberReducedMotion
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

@Composable
fun TodayScreen(
    onOpenPlan: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val vm: TodayViewModel = viewModel { TodayViewModel(this[APPLICATION_KEY] as OnwardApp) }
    val state by vm.state.collectAsStateWithLifecycle()
    val update by Updates.available.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "ONWARD",
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 3.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "››",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                Text(
                    state.today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            IconButton(onClick = onOpenHistory) {
                Icon(
                    Icons.Outlined.History,
                    contentDescription = "History",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        update?.let { release ->
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { Updates.download(context, release) },
            ) {
                Text(
                    "v${release.version} is ready — tap to update ››",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        StreakHero(
            run = state.streak.currentRun,
            todayHit = state.todayStatus == DayStatus.HIT,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(Modifier.height(20.dp))
        CheckInCard(state, onAnswer = vm::checkIn, onChange = vm::clearToday)

        if (state.yesterdayStatus == DayStatus.NONE && state.hasHistory) {
            Spacer(Modifier.height(12.dp))
            YesterdayGraceCard(onAnswer = vm::checkInYesterday)
        }

        if (state.streak.freezeUsedThisWeek) {
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.AcUnit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "1 free miss used this week — covered",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        StatsRow(state)

        Spacer(Modifier.height(16.dp))
        TodayPlanCard(state, onOpenPlan)

        if (state.calorieEnabled) {
            Spacer(Modifier.height(16.dp))
            CalorieCard(state, vm::addCalories, vm::removeCalories)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StreakHero(run: Int, todayHit: Boolean, modifier: Modifier = Modifier) {
    val reducedMotion = rememberReducedMotion()
    val sweep by animateFloatAsState(
        targetValue = if (todayHit) 360f else 0f,
        animationSpec = if (reducedMotion) snap() else tween(900, easing = FastOutSlowInEasing),
        label = "sweep",
    )
    val scale by animateFloatAsState(
        targetValue = if (todayHit) 1f else 0.96f,
        animationSpec = if (reducedMotion) {
            snap()
        } else {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        },
        label = "scale",
    )

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val citrus = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = modifier
            .size(230.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2
            val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke.width, size.height - stroke.width)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(trackColor, -90f, 360f, false, topLeft = topLeft, size = arcSize, style = stroke)
            if (sweep > 0f) {
                drawArc(citrus, -90f, sweep, false, topLeft = topLeft, size = arcSize, style = stroke)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                run.toString(),
                style = MaterialTheme.typography.displayLarge,
                color = if (todayHit) citrus else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                when {
                    run == 0 && !todayHit -> "starts with today"
                    run == 1 -> "day — the run begins"
                    else -> "day run"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CheckInCard(
    state: TodayUiState,
    onAnswer: (Boolean) -> Unit,
    onChange: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        ),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            when (state.todayStatus) {
                DayStatus.NONE -> {
                    Text("Hit the pattern today?", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Mostly followed the shape of the day? That's a yes — 80% counts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row {
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onAnswer(true)
                            },
                            modifier = Modifier.weight(1.4f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) { Text("Yes, hit it") }
                        Spacer(Modifier.width(10.dp))
                        OutlinedButton(
                            onClick = { onAnswer(false) },
                            modifier = Modifier.weight(1f),
                        ) { Text("Not really") }
                    }
                }
                DayStatus.HIT -> {
                    val milestone = when (state.streak.currentRun) {
                        7 -> "A full week"
                        14 -> "Two weeks strong"
                        30 -> "A whole month"
                        50 -> "Fifty days"
                        100 -> "A hundred days"
                        365 -> "A whole year"
                        else -> null
                    }
                    Text(
                        if (milestone != null) {
                            "$milestone. Day ${state.streak.currentRun}. ✓"
                        } else {
                            "Locked in. Day ${state.streak.currentRun}. ✓"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (milestone != null) {
                            "That's not luck — that's a pattern. Onward."
                        } else {
                            "Beautiful. See you at tomorrow's shake."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ChangeAnswer(onChange)
                }
                else -> {
                    if (state.freezeAbsorbedToday) {
                        Text("Covered by your free miss.", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "The run is safe. Onward — tomorrow's a fresh plate.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text("Logged. New run starts today.", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "No stress — one good day gets it going again. Onward.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    ChangeAnswer(onChange)
                }
            }
        }
    }
}

@Composable
private fun YesterdayGraceCard(onAnswer: (Boolean) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("And yesterday?", style = MaterialTheme.typography.titleSmall)
                Text(
                    "No stress either way.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = { onAnswer(true) }) { Text("Hit it") }
            TextButton(onClick = { onAnswer(false) }) {
                Text("Not really", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ChangeAnswer(onChange: () -> Unit) {
    TextButton(onClick = onChange) {
        Text(
            "Change answer",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun StatsRow(state: TodayUiState) {
    val monthWin = state.streak.monthPercent >= 80
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard("Longest run", "${state.streak.longestRun}", Modifier.weight(1f))
        StatCard(
            if (monthWin) "This month — a win" else "This month",
            "${state.streak.monthPercent}%",
            Modifier.weight(1f),
            emphasize = monthWin,
        )
        StatCard("Days hit", "${state.streak.hitsThisMonth}", Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier, emphasize: Boolean = false) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (emphasize) {
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            },
        ),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 14.dp)) {
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                color = if (emphasize) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TodayPlanCard(state: TodayUiState, onOpenPlan: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Today's shape", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            val plan = state.plan
            if (plan == null) {
                Text(
                    "Plan is loading…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                MealRow("🥤", "Breakfast", plan.breakfast)
                Spacer(Modifier.height(10.dp))
                MealRow("🍗", "Lunch", plan.lunchExample)
                Spacer(Modifier.height(10.dp))
                MealRow("🥗", "Dinner", plan.dinnerExample)
            }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onOpenPlan, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Text("Full week plan ››", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun MealRow(emoji: String, label: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(emoji, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private data class Preset(val label: String, val kcal: Int)

private val presets = listOf(
    Preset("Shake", 350),
    Preset("Lunch", 500),
    Preset("Dinner", 400),
    Preset("Snack", 150),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalorieCard(
    state: TodayUiState,
    onAdd: (String, Int) -> Unit,
    onRemove: (Long) -> Unit,
) {
    var showCustom by remember { mutableStateOf(false) }
    val todayTotal = state.todayCalories.sumOf { it.kcal }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Rough picture", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text(
                    "≈ $todayTotal kcal",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { preset ->
                    AssistChip(
                        onClick = { onAdd(preset.label, preset.kcal) },
                        label = { Text("${preset.label} ~${preset.kcal}") },
                    )
                }
                AssistChip(onClick = { showCustom = true }, label = { Text("+ custom") })
            }
            if (showCustom) {
                CustomCalorieDialog(onDismiss = { showCustom = false }, onAdd = onAdd)
            }

            if (state.todayCalories.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                state.todayCalories.forEach { entry ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            entry.label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${entry.kcal}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        IconButton(onClick = { onRemove(entry.id) }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "Remove ${entry.label}",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            WeekBars(state.weekTotals)
            Spacer(Modifier.height(10.dp))
            Text(
                "A real target (like a deficit size) should come from your cardiologist or a dietitian — this is just a rough picture. It never touches the streak.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WeekBars(totals: List<Pair<Long, Int>>) {
    val max = (totals.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
    val nonZeroDays = totals.count { it.second > 0 }
    val avg = if (nonZeroDays == 0) 0 else totals.sumOf { it.second } / nonZeroDays
    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            totals.forEach { (day, total) ->
                val fraction = total.toFloat() / max
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Surface(
                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                        color = if (total > 0) {
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((40 * fraction).coerceAtLeast(3f).dp),
                    ) {}
                    Spacer(Modifier.height(4.dp))
                    Text(
                        java.time.LocalDate.ofEpochDay(day).dayOfWeek
                            .getDisplayName(JavaTextStyle.NARROW, Locale.ENGLISH),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (avg > 0) {
            Spacer(Modifier.height(4.dp))
            Text(
                "7-day average ≈ $avg kcal (on logged days)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CustomCalorieDialog(onDismiss: () -> Unit, onAdd: (String, Int) -> Unit) {
    var label by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick add", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("What (optional)") },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = kcal,
                    onValueChange = { new -> kcal = new.filter { it.isDigit() }.take(5) },
                    label = { Text("Rough kcal") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = kcal.toIntOrNull() ?: return@TextButton
                    onAdd(label.ifBlank { "Custom" }, value)
                    onDismiss()
                },
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
