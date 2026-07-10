package com.astro.onward.data

import java.time.LocalDate

/**
 * The forgiving streak engine. Pure function over day history — nothing is
 * mutated, freezes are derived on read.
 *
 * Rules:
 *  - A HIT extends the current run.
 *  - A miss (answered "not really", or a past day left unanswered) is absorbed
 *    by a streak freeze if no freeze was used in the trailing 7 days AND there
 *    is a run to protect. A frozen day keeps the run alive but doesn't grow it.
 *  - A second miss inside the same rolling week ends the run. No shame — the
 *    next HIT simply starts a new run.
 *  - Today, while unanswered, is pending: it never counts against the run.
 */
object StreakCalculator {

    data class Result(
        val currentRun: Int,
        val longestRun: Int,
        val freezeDays: List<Long>,
        val freezeUsedThisWeek: Boolean,
        val hitsThisMonth: Int,
        val daysElapsedThisMonth: Int,
    ) {
        val monthPercent: Int
            get() = if (daysElapsedThisMonth == 0) 0
            else hitsThisMonth * 100 / daysElapsedThisMonth
    }

    fun calculate(entries: Map<Long, DayStatus>, today: LocalDate): Result {
        val todayEpoch = today.toEpochDay()
        val known = entries.filterKeys { it <= todayEpoch }
        val firstDay = known.keys.minOrNull() ?: todayEpoch

        var run = 0
        var longest = 0
        val freezeDays = mutableListOf<Long>()

        for (day in firstDay..todayEpoch) {
            val status = known[day] ?: DayStatus.NONE
            when {
                status == DayStatus.HIT -> {
                    run++
                    if (run > longest) longest = run
                }
                day == todayEpoch && status == DayStatus.NONE -> {
                    // Today isn't over — pending, not a miss.
                }
                else -> {
                    // MISS, FREEZE_USED, or a past unanswered day.
                    val freezeFreeThisWeek = freezeDays.none { it >= day - 6 }
                    if (run > 0 && freezeFreeThisWeek) {
                        freezeDays += day // absorbed: run survives, doesn't grow
                    } else {
                        run = 0
                    }
                }
            }
        }

        var hits = 0
        val monthStart = today.withDayOfMonth(1).toEpochDay()
        for (day in monthStart..todayEpoch) {
            if (known[day] == DayStatus.HIT) hits++
        }

        return Result(
            currentRun = run,
            longestRun = longest,
            freezeDays = freezeDays,
            freezeUsedThisWeek = freezeDays.any { it >= todayEpoch - 6 },
            hitsThisMonth = hits,
            daysElapsedThisMonth = today.dayOfMonth,
        )
    }
}
