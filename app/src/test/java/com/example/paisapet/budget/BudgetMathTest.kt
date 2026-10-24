package com.example.paisapet.budget

import com.example.paisapet.model.Category.ESSENTIAL
import com.example.paisapet.model.Category.MISC
import com.example.paisapet.model.Category.UNTAGGED
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class BudgetMathTest {

    private val budget = 1_000_000L
    private val oct1 = LocalDate.of(2026, 10, 1)

    // Uneven budgets: integer comparison, no rounding

    @Test fun unevenBudgetAt25() {
        assertEquals(BuddyState.THRIVING, buddyStateOf(83, 333))
        assertEquals(BuddyState.FINE, buddyStateOf(84, 333))
    }

    @Test fun unevenBudgetAt75() {
        assertEquals(BuddyState.WORRIED, buddyStateOf(750, 1001))
        assertEquals(BuddyState.SICK, buddyStateOf(751, 1001))
    }

    @Test fun unevenBudgetAt100() {
        assertEquals(BuddyState.SICK, buddyStateOf(1001, 1001))
        assertEquals(BuddyState.FAINTED, buddyStateOf(1002, 1001))
    }

    // Budget must be > 0

    @Test(expected = IllegalArgumentException::class)
    fun zeroBudgetThrowsForState() {
        buddyStateOf(0, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeBudgetThrowsForState() {
        buddyStateOf(100, -1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroBudgetThrowsForPct() {
        pctUsedFloor(0, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeBudgetThrowsForSummary() {
        summarize("2026-10", -1, emptyList(), oct1)
    }

    // miscSpent

    @Test fun miscSpentCountsMiscAndUntaggedOnly() {
        val spends = listOf(Spend(100, MISC), Spend(200, UNTAGGED), Spend(5_000, ESSENTIAL))
        assertEquals(300, miscSpent(spends))
    }

    @Test fun allEssentialIsZeroAndThriving() {
        val s = summarize("2026-10", budget, listOf(Spend(2_000_000, ESSENTIAL)), oct1)
        assertEquals(0, s.miscSpentPaise)
        assertEquals(BuddyState.THRIVING, s.buddyState)
    }

    @Test fun emptyListIsZero() = assertEquals(0, miscSpent(emptyList()))

    @Test fun untaggedMovesBuddy() {
        val s = summarize("2026-10", budget, listOf(Spend(500_000, UNTAGGED)), oct1)
        assertEquals(BuddyState.WORRIED, s.buddyState)
        assertEquals(500_000, s.miscSpentPaise)
    }

    // summarize

    @Test fun summaryTotals() {
        val spends = listOf(
            Spend(100_000, MISC),
            Spend(50_000, UNTAGGED),
            Spend(25_000, UNTAGGED),
            Spend(400_000, ESSENTIAL),
        )
        val s = summarize("2026-10", budget, spends, oct1)
        assertEquals("2026-10", s.monthKey)
        assertEquals(budget, s.budgetPaise)
        assertEquals(175_000, s.miscSpentPaise)
        assertEquals(400_000, s.essentialSpentPaise)
        assertEquals(575_000, s.totalSpentPaise)
        assertEquals(2, s.untaggedCount)
        assertEquals(825_000, s.leftPaise)
        assertEquals(17, s.pctUsedFloor)
    }

    @Test fun leftIsZeroAtExactly100() {
        val s = summarize("2026-10", budget, listOf(Spend(1_000_000, MISC)), oct1)
        assertEquals(0, s.leftPaise)
        assertEquals(BuddyState.SICK, s.buddyState)
    }

    @Test fun leftIsNegativeOverBudget() {
        val s = summarize("2026-10", budget, listOf(Spend(1_000_100, MISC)), oct1)
        assertEquals(-100, s.leftPaise)
        assertEquals(100, s.pctUsedFloor)
        assertEquals(BuddyState.FAINTED, s.buddyState)
        assertEquals(0, s.pacePaisePerDay)
    }

    @Test fun firstDayOfMonth() {
        val s = summarize("2026-10", budget, listOf(Spend(70_000, MISC)), oct1)
        assertEquals(31, s.daysLeft)
        assertEquals(930_000L / 31, s.pacePaisePerDay)
    }

    @Test fun lastDayPaceIsLeft() {
        val s = summarize("2026-10", budget, listOf(Spend(950_000, MISC)), LocalDate.of(2026, 10, 31))
        assertEquals(1, s.daysLeft)
        assertEquals(50_000, s.pacePaisePerDay)
        assertEquals(s.leftPaise, s.pacePaisePerDay)
    }

    @Test fun pastMonthHasNoDaysAndNoPace() {
        val s = summarize("2026-10", budget, listOf(Spend(100_000, MISC)), LocalDate.of(2026, 11, 1))
        assertEquals(0, s.daysLeft)
        assertEquals(0, s.pacePaisePerDay)
    }

    // pacePerDay

    @Test fun paceRoundsDown() = assertEquals(33, pacePerDay(100, 3))

    @Test fun paceWithNothingLeft() = assertEquals(0, pacePerDay(0, 10))

    @Test fun paceWhenOverBudget() = assertEquals(0, pacePerDay(-500, 10))

    @Test fun paceWithNoDaysLeft() = assertEquals(0, pacePerDay(1_000, 0))

    // effectiveBudget

    @Test fun rowWinsOverDefault() = assertEquals(2_000_000L, effectiveBudget(2_000_000, 1_000_000))

    @Test fun noRowUsesDefault() = assertEquals(1_000_000L, effectiveBudget(null, 1_000_000))

    @Test fun neitherIsNull() = assertNull(effectiveBudget(null, null))

    // Large amounts

    @Test fun largeSpendDoesNotOverflow() {
        val big = 10_000_000_000_000L // 10^13 paise
        val s = summarize("2026-10", big, listOf(Spend(9_000_000_000_000L, MISC)), oct1)
        assertEquals(90, s.pctUsedFloor)
        assertEquals(BuddyState.SICK, s.buddyState)
        assertEquals(1_000_000_000_000L, s.leftPaise)
    }
}
