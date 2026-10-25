package com.example.paisapet.budget

import com.example.paisapet.model.Category.ESSENTIAL
import com.example.paisapet.model.Category.MISC
import com.example.paisapet.model.Category.UNTAGGED
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DebitTotalsTest {

    @Test fun miscIncludesUntagged() {
        val t = debitTotals(listOf(Spend(100, MISC), Spend(200, UNTAGGED), Spend(700, ESSENTIAL)))
        assertEquals(300L, t.miscPaise)
        assertEquals(700L, t.essentialPaise)
        assertEquals(1_000L, t.totalPaise)
        assertEquals(3, t.count)
    }

    @Test fun mockupSplit43And57() {
        val t = debitTotals(listOf(Spend(320_000, MISC), Spend(415_000, ESSENTIAL)))
        assertEquals(735_000L, t.totalPaise)
        assertEquals(43, t.miscPct)
        assertEquals(57, t.essentialPct)
    }

    @Test fun noDebitsGivesZeroZero() {
        val t = debitTotals(emptyList())
        assertEquals(DebitTotals(0, 0, 0, 0, 0, 0), t)
    }

    @Test fun allEssential() {
        val t = debitTotals(listOf(Spend(500, ESSENTIAL)))
        assertEquals(0, t.miscPct)
        assertEquals(100, t.essentialPct)
    }

    @Test fun allMisc() {
        val t = debitTotals(listOf(Spend(500, MISC), Spend(1, UNTAGGED)))
        assertEquals(100, t.miscPct)
        assertEquals(0, t.essentialPct)
    }

    @Test fun tinyMiscRoundsDownToZero() {
        // ₹1 misc of ₹1,000 total: 0.1% shows as 0%, Essential 100%. Documented in the plan.
        val t = debitTotals(listOf(Spend(100, MISC), Spend(99_900, ESSENTIAL)))
        assertEquals(0, t.miscPct)
        assertEquals(100, t.essentialPct)
    }

    @Test fun percentagesAlwaysAddTo100() {
        val t = debitTotals(listOf(Spend(1, MISC), Spend(2, ESSENTIAL)))
        assertEquals(33, t.miscPct)
        assertEquals(67, t.essentialPct)
    }

    @Test fun largeAmountsDoNotOverflow() {
        val big = Long.MAX_VALUE / 200
        val t = debitTotals(listOf(Spend(big, MISC), Spend(big, ESSENTIAL)))
        assertEquals(50, t.miscPct)
    }

    @Test(expected = ArithmeticException::class)
    fun overflowThrowsInsteadOfWrapping() {
        debitTotals(listOf(Spend(Long.MAX_VALUE / 2, MISC), Spend(Long.MAX_VALUE / 2 + 10, ESSENTIAL)))
    }

    @Test fun summaryUsesTheSameTotals() {
        val spends = listOf(Spend(320_000, MISC), Spend(5_000, UNTAGGED), Spend(415_000, ESSENTIAL))
        val s = summarize("2026-10", 500_000, spends, LocalDate.of(2026, 10, 25))
        val t = debitTotals(spends)
        assertEquals(t.miscPaise, s.miscSpentPaise)
        assertEquals(t.essentialPaise, s.essentialSpentPaise)
        assertEquals(t.totalPaise, s.totalSpentPaise)
    }
}
