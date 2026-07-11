package com.astro.onward.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.astro.onward.MainActivity
import com.astro.onward.data.DayEntry
import com.astro.onward.data.DayStatus
import com.astro.onward.data.OnwardDatabase
import com.astro.onward.data.PlanDay
import com.astro.onward.data.StreakCalculator
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class OnwardWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OnwardWidget()
}

/**
 * The streak, at a glance — pine-and-citrus like the app, resizable in BOTH
 * dimensions. Breakpoints: tiny (number only) → wide (number + status) →
 * tall (adds the check-off button) → full (adds today's meals).
 */
class OnwardWidget : GlanceAppWidget() {

    companion object {
        private val TINY = DpSize(57.dp, 57.dp)
        private val WIDE = DpSize(180.dp, 57.dp)
        private val TALL = DpSize(180.dp, 160.dp)
        private val FULL = DpSize(260.dp, 240.dp)

        val Pine = Color(0xFF1E4635)
        val Citrus = Color(0xFFE8813A)
        val OffWhite = Color(0xFFF5F6F1)
        val Sage = Color(0xFFA9C3AF)
    }

    override val sizeMode = SizeMode.Responsive(setOf(TINY, WIDE, TALL, FULL))

    data class WidgetState(
        val run: Int,
        val todayStatus: DayStatus,
        val freezeUsedThisWeek: Boolean,
        val plan: PlanDay?,
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = OnwardDatabase.get(context)
        val today = LocalDate.now()
        val byDay = db.dayEntryDao().observeAll().first().associate { it.epochDay to it.status }
        val streak = StreakCalculator.calculate(byDay, today)
        val state = WidgetState(
            run = streak.currentRun,
            todayStatus = byDay[today.toEpochDay()] ?: DayStatus.NONE,
            freezeUsedThisWeek = streak.freezeUsedThisWeek,
            plan = db.planDao().observeDays().first()
                .firstOrNull { it.dayIndex == today.dayOfWeek.value - 1 },
        )
        provideContent { WidgetContent(state) }
    }
}

private val white = ColorProvider(OnwardWidget.OffWhite)
private val citrus = ColorProvider(OnwardWidget.Citrus)
private val sage = ColorProvider(OnwardWidget.Sage)

@Composable
private fun WidgetContent(state: OnwardWidget.WidgetState) {
    val size = LocalSize.current
    val hit = state.todayStatus == DayStatus.HIT

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(OnwardWidget.Pine)
            .cornerRadius(24.dp)
            .clickable(actionStartActivity<MainActivity>())
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            size.height >= 160.dp && size.width >= 260.dp -> FullLayout(state, hit)
            size.height >= 160.dp -> TallLayout(state, hit)
            size.width >= 180.dp -> WideLayout(state, hit)
            else -> TinyLayout(state, hit)
        }
    }
}

@Composable
private fun StreakNumber(run: Int, hit: Boolean, fontSize: Int) {
    Text(
        text = run.toString(),
        style = TextStyle(
            color = if (hit) citrus else white,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
        ),
    )
}

@Composable
private fun StatusLine(state: OnwardWidget.WidgetState, hit: Boolean) {
    Text(
        text = when {
            hit -> "✓ locked in"
            state.todayStatus == DayStatus.MISS -> "onward — tomorrow"
            else -> "not checked yet"
        },
        style = TextStyle(color = if (hit) citrus else sage, fontSize = 12.sp),
    )
}

@Composable
private fun TinyLayout(state: OnwardWidget.WidgetState, hit: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        StreakNumber(state.run, hit, 32)
        Text("day run", style = TextStyle(color = sage, fontSize = 10.sp))
    }
}

@Composable
private fun WideLayout(state: OnwardWidget.WidgetState, hit: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StreakNumber(state.run, hit, 36)
        Spacer(GlanceModifier.width(12.dp))
        Column {
            Text("day run ››", style = TextStyle(color = sage, fontSize = 12.sp))
            StatusLine(state, hit)
        }
    }
}

@Composable
private fun CheckOffButton() {
    Box(
        modifier = GlanceModifier
            .background(OnwardWidget.Citrus)
            .cornerRadius(20.dp)
            .padding(horizontal = 18.dp, vertical = 8.dp)
            .clickable(actionRunCallback<WidgetCheckInAction>()),
    ) {
        Text(
            "Yes, hit it",
            style = TextStyle(
                color = white,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

@Composable
private fun TallLayout(state: OnwardWidget.WidgetState, hit: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        StreakNumber(state.run, hit, 48)
        Text("day run ››", style = TextStyle(color = sage, fontSize = 12.sp))
        Spacer(GlanceModifier.height(10.dp))
        if (state.todayStatus == DayStatus.NONE) CheckOffButton() else StatusLine(state, hit)
        if (state.freezeUsedThisWeek) {
            Spacer(GlanceModifier.height(6.dp))
            Text("1 free miss used — covered", style = TextStyle(color = sage, fontSize = 10.sp))
        }
    }
}

@Composable
private fun MealLine(emoji: String, text: String) {
    Text(
        "$emoji  $text",
        style = TextStyle(color = white, fontSize = 12.sp),
        maxLines = 1,
    )
}

@Composable
private fun FullLayout(state: OnwardWidget.WidgetState, hit: Boolean) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier.fillMaxWidth(),
        ) {
            StreakNumber(state.run, hit, 44)
            Spacer(GlanceModifier.width(12.dp))
            Column {
                Text("day run ››", style = TextStyle(color = sage, fontSize = 12.sp))
                StatusLine(state, hit)
            }
        }
        Spacer(GlanceModifier.height(10.dp))
        state.plan?.let { plan ->
            MealLine("🥤", plan.breakfast)
            Spacer(GlanceModifier.height(3.dp))
            MealLine("🍗", plan.lunchExample)
            Spacer(GlanceModifier.height(3.dp))
            MealLine("🥗", plan.dinnerExample)
        }
        if (state.todayStatus == DayStatus.NONE) {
            Spacer(GlanceModifier.height(10.dp))
            CheckOffButton()
        }
    }
}

/** One-tap check-off straight from the widget. */
class WidgetCheckInAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val db = OnwardDatabase.get(context)
        val today = LocalDate.now().toEpochDay()
        val existing = db.dayEntryDao().observeAll().first().firstOrNull { it.epochDay == today }
        if (existing == null) {
            db.dayEntryDao().upsert(DayEntry(today, DayStatus.HIT))
        }
        OnwardWidget().updateAll(context)
    }
}
