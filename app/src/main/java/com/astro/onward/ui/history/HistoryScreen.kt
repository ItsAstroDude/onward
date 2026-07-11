package com.astro.onward.ui.history

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.astro.onward.OnwardApp
import com.astro.onward.data.DayStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val vm: HistoryViewModel = viewModel { HistoryViewModel(this[APPLICATION_KEY] as OnwardApp) }
    val state by vm.state.collectAsStateWithLifecycle()
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }

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
            Text("History", style = MaterialTheme.typography.displaySmall)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Every day you showed up. Tap a day to look back or add a note.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = vm::previousMonth) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Previous month")
            }
            Text(
                "${state.month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${state.month.year}",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            IconButton(onClick = vm::nextMonth, enabled = state.canGoForward) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Next month")
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { d ->
                Text(
                    d,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        val firstDay = state.month.atDay(1)
        val leadingBlanks = firstDay.dayOfWeek.value - 1
        val cells: List<LocalDate?> =
            List(leadingBlanks) { null } + (1..state.month.lengthOfMonth()).map { state.month.atDay(it) }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(3.dp)) {
                        if (date != null) {
                            DayCell(
                                date = date,
                                status = state.statuses[date.toEpochDay()],
                                isFreeze = date.toEpochDay() in state.freezeDays,
                                isToday = date == state.today,
                                isFuture = date.isAfter(state.today),
                                onClick = { selectedDay = date },
                            )
                        }
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f).aspectRatio(1f)) }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LegendDot(MaterialTheme.colorScheme.tertiary, "hit")
            Spacer(Modifier.width(14.dp))
            Icon(
                Icons.Outlined.AcUnit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text("free miss", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(14.dp))
            LegendDot(MaterialTheme.colorScheme.surfaceVariant, "off day")
        }
        Spacer(Modifier.height(24.dp))
    }

    selectedDay?.let { date ->
        DaySheet(
            date = date,
            status = state.statuses[date.toEpochDay()] ?: DayStatus.NONE,
            isFreeze = date.toEpochDay() in state.freezeDays,
            note = state.notes[date.toEpochDay()] ?: "",
            onSave = { status, note ->
                vm.setDay(date, status, note)
                selectedDay = null
            },
            onDismiss = { selectedDay = null },
        )
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Surface(shape = CircleShape, color = color, modifier = Modifier.size(10.dp)) {}
    Spacer(Modifier.width(4.dp))
    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun DayCell(
    date: LocalDate,
    status: DayStatus?,
    isFreeze: Boolean,
    isToday: Boolean,
    isFuture: Boolean,
    onClick: () -> Unit,
) {
    val bg = when {
        status == DayStatus.HIT -> MaterialTheme.colorScheme.tertiary
        isFreeze -> MaterialTheme.colorScheme.secondaryContainer
        status == DayStatus.MISS || status == DayStatus.FREEZE_USED ->
            MaterialTheme.colorScheme.surfaceVariant
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    val fg = when {
        status == DayStatus.HIT -> MaterialTheme.colorScheme.onTertiary
        isFreeze -> MaterialTheme.colorScheme.onSecondaryContainer
        isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.onSurface
    }
    Surface(
        shape = CircleShape,
        color = bg,
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isToday) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                } else {
                    Modifier
                },
            )
            .clickable(enabled = !isFuture, onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = fg,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DaySheet(
    date: LocalDate,
    status: DayStatus,
    isFreeze: Boolean,
    note: String,
    onSave: (DayStatus, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var chosen by remember { mutableStateOf(status) }
    var noteText by remember { mutableStateOf(note) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(
                date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)),
                style = MaterialTheme.typography.headlineSmall,
            )
            if (isFreeze) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "Covered by a free miss — the run stayed safe.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = chosen == DayStatus.HIT,
                    onClick = { chosen = DayStatus.HIT },
                    label = { Text("Hit it") },
                )
                FilterChip(
                    selected = chosen == DayStatus.MISS || chosen == DayStatus.FREEZE_USED,
                    onClick = { chosen = DayStatus.MISS },
                    label = { Text("Not really") },
                )
                FilterChip(
                    selected = chosen == DayStatus.NONE,
                    onClick = { chosen = DayStatus.NONE },
                    label = { Text("No answer") },
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Row {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Cancel") }
                TextButton(onClick = { onSave(chosen, noteText.trim()) }) { Text("Save") }
            }
        }
    }
}
