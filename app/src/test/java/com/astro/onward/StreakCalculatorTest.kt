package com.astro.onward

import com.astro.onward.data.DayStatus
import com.astro.onward.data.StreakCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StreakCalculatorTest {

    private val today: LocalDate = LocalDate.of(2026, 7, 10)
    private val t = today.toEpochDay()

    private fun days(vararg pairs: Pair<Long, DayStatus>) = mapOf(*pairs)

    @Test
    fun `empty history means zero run`() {
        val r = StreakCalculator.calculate(emptyMap(), today)
        assertEquals(0, r.currentRun)
        assertEquals(0, r.longestRun)
        assertFalse(r.freezeUsedThisWeek)
    }

    @Test
    fun `simple run of hits`() {
        val r = StreakCalculator.calculate(
            days(t - 2 to DayStatus.HIT, t - 1 to DayStatus.HIT, t to DayStatus.HIT), today,
        )
        assertEquals(3, r.currentRun)
        assertEquals(3, r.longestRun)
    }

    @Test
    fun `today unanswered is pending, not a miss`() {
        val r = StreakCalculator.calculate(
            days(t - 2 to DayStatus.HIT, t - 1 to DayStatus.HIT), today,
        )
        assertEquals(2, r.currentRun)
        assertFalse(r.freezeUsedThisWeek)
    }

    @Test
    fun `one miss is absorbed by a freeze`() {
        val r = StreakCalculator.calculate(
            days(
                t - 3 to DayStatus.HIT, t - 2 to DayStatus.HIT,
                t - 1 to DayStatus.MISS, t to DayStatus.HIT,
            ),
            today,
        )
        assertEquals(3, r.currentRun) // frozen day preserved the run
        assertTrue(r.freezeUsedThisWeek)
        assertEquals(listOf(t - 1), r.freezeDays)
    }

    @Test
    fun `unanswered past day counts as a miss and is absorbed`() {
        val r = StreakCalculator.calculate(
            days(t - 3 to DayStatus.HIT, t - 2 to DayStatus.HIT, t to DayStatus.HIT), today,
        )
        assertEquals(3, r.currentRun)
        assertTrue(r.freezeUsedThisWeek)
    }

    @Test
    fun `two misses in a rolling week end the run`() {
        val r = StreakCalculator.calculate(
            days(
                t - 4 to DayStatus.HIT, t - 3 to DayStatus.MISS,
                t - 2 to DayStatus.HIT, t - 1 to DayStatus.MISS,
                t to DayStatus.HIT,
            ),
            today,
        )
        assertEquals(1, r.currentRun) // new run started today
        assertEquals(2, r.longestRun) // hit, frozen, hit = 2 before the break
    }

    @Test
    fun `a second freeze is available once the week rolls past`() {
        val entries = buildMap {
            put(t - 10, DayStatus.HIT)
            put(t - 9, DayStatus.MISS) // freeze #1
            for (d in (t - 8)..(t - 2)) put(d, DayStatus.HIT)
            put(t - 1, DayStatus.MISS) // 8 days later: freeze #2, run survives
            put(t, DayStatus.HIT)
        }
        val r = StreakCalculator.calculate(entries, today)
        assertEquals(9, r.currentRun)
        assertEquals(2, r.freezeDays.size)
    }

    @Test
    fun `no freeze is consumed when there is no run to protect`() {
        val r = StreakCalculator.calculate(
            days(t - 2 to DayStatus.MISS, t - 1 to DayStatus.MISS, t to DayStatus.HIT), today,
        )
        assertEquals(1, r.currentRun)
        assertTrue(r.freezeDays.isEmpty())
    }

    @Test
    fun `longest run survives a broken streak`() {
        val entries = buildMap {
            for (d in (t - 20)..(t - 11)) put(d, DayStatus.HIT) // run of 10
            put(t - 10, DayStatus.MISS)
            put(t - 9, DayStatus.MISS) // break
            for (d in (t - 8)..t) put(d, DayStatus.HIT) // run of 9
        }
        val r = StreakCalculator.calculate(entries, today)
        assertEquals(9, r.currentRun)
        assertEquals(10, r.longestRun) // the old 10-day run is remembered
    }

    @Test
    fun `month stats use the 80 percent framing`() {
        // July 1..10, hit 8 of 10 days.
        val entries = buildMap {
            for (d in 1..10) {
                val date = LocalDate.of(2026, 7, d).toEpochDay()
                put(date, if (d == 3 || d == 7) DayStatus.MISS else DayStatus.HIT)
            }
        }
        val r = StreakCalculator.calculate(entries, today)
        assertEquals(8, r.hitsThisMonth)
        assertEquals(10, r.daysElapsedThisMonth)
        assertEquals(80, r.monthPercent)
    }

    @Test
    fun `stored FREEZE_USED status is treated as an absorbable miss`() {
        val r = StreakCalculator.calculate(
            days(t - 2 to DayStatus.HIT, t - 1 to DayStatus.FREEZE_USED, t to DayStatus.HIT), today,
        )
        assertEquals(2, r.currentRun)
        assertTrue(r.freezeUsedThisWeek)
    }
}
