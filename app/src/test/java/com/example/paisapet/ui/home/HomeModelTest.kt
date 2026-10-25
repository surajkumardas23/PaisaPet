package com.example.paisapet.ui.home

import com.example.paisapet.IST
import com.example.paisapet.budget.BuddyState
import com.example.paisapet.budget.Spend
import com.example.paisapet.budget.summarize
import com.example.paisapet.data.MonthView
import com.example.paisapet.data.db.TransactionEntity
import com.example.paisapet.list.ChipKind
import com.example.paisapet.millisAt
import com.example.paisapet.model.Category
import com.example.paisapet.model.Source
import com.example.paisapet.tx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HomeModelTest {

    private val today = LocalDate.of(2026, 10, 25) // 7 days left in October, counting today
    private val budget = 5_000_00L

    /** Rows newest first, as the DAO returns them, with the real summarize. */
    private fun view(rows: List<TransactionEntity>, monthKey: String = "2026-10", budgetPaise: Long? = budget) =
        MonthView(
            rows,
            budgetPaise?.let { summarize(monthKey, it, rows.map { r -> Spend(r.amountPaise, r.category) }, today) },
        )

    private fun home(v: MonthView, monthKey: String = "2026-10", currentKey: String = "2026-10") =
        buildHomeModel(v, monthKey, currentKey, today, IST)

    private fun budgeted(m: HomeModel) = m.buddy as BuddyCardModel.Budgeted

    @Test fun emptyMonth() {
        val m = home(view(emptyList()))
        val b = budgeted(m)
        assertEquals(BuddyState.THRIVING, b.state)
        assertEquals("₹5,000", b.amountText)
        assertEquals("left of ₹5,000", b.amountCaption)
        assertEquals("0% used · Thriving", b.chipText)
        assertEquals("Buddy is thriving", b.homeLabel)
        assertEquals(0, b.barPercent)
        assertEquals("₹0 spent", b.footerStart)
        assertEquals("₹714.28/day for 7 days left", b.footerEnd)
        assertTrue(m.showFreshMonthCard)
        assertEquals("No transactions yet this month", m.emptyRecentText)
        assertTrue(m.recent.isEmpty())
        assertNull(m.untagged)
        assertTrue(m.showAddExpense)
        assertFalse(m.readOnly)
        assertNull(m.backToCurrentLabel)
        assertEquals("October 2026", m.monthLabel)
    }

    @Test fun normalMonth() {
        val rows = listOf(
            tx(id = 2, amountPaise = 1_200_00, occurredAt = millisAt(2026, 10, 25, 13, 15), category = Category.MISC),
            tx(id = 1, amountPaise = 2_000_00, occurredAt = millisAt(2026, 10, 24, 9, 0), category = Category.MISC),
        )
        val m = home(view(rows))
        val b = budgeted(m)
        assertEquals(BuddyState.WORRIED, b.state)
        assertEquals("₹1,800", b.amountText)
        assertEquals("left of ₹5,000", b.amountCaption)
        assertFalse(b.overBudget)
        assertEquals("64% used · Worried", b.chipText)
        assertEquals("Buddy is getting nervous", b.homeLabel)
        assertEquals(64, b.barPercent)
        assertEquals("₹3,200 spent", b.footerStart)
        assertEquals("₹257.14/day for 7 days left", b.footerEnd)
        assertFalse(m.showFreshMonthCard)
        assertNull(m.emptyRecentText)
        assertEquals(listOf("1:15 PM", "Yesterday"), m.recent.map { it.subtitle })
        assertEquals("–₹1,200", m.recent[0].amountText)
    }

    @Test fun overBudgetAt100Point01Percent() {
        val m = home(view(listOf(tx(amountPaise = 5_000_50, category = Category.MISC))))
        val b = budgeted(m)
        assertEquals(BuddyState.FAINTED, b.state)
        assertTrue(b.overBudget)
        assertEquals("₹0.50", b.amountText)
        assertEquals("over budget of ₹5,000", b.amountCaption)
        assertEquals("100% used · Fainted", b.chipText)
        assertEquals("Buddy fainted", b.homeLabel)
        assertEquals(100, b.barPercent)
        assertEquals("7 days left", b.footerEnd)
    }

    @Test fun wayOverBudgetBarStaysFull() {
        val b = budgeted(home(view(listOf(tx(amountPaise = 15_000_00, category = Category.MISC)))))
        assertEquals(100, b.barPercent)
        assertEquals("300% used · Fainted", b.chipText)
        assertEquals("₹10,000", b.amountText)
    }

    @Test fun exactly100PercentIsSickWithZeroLeft() {
        val b = budgeted(home(view(listOf(tx(amountPaise = budget, category = Category.UNTAGGED)))))
        assertEquals(BuddyState.SICK, b.state)
        assertFalse(b.overBudget)
        assertEquals("₹0", b.amountText)
        assertEquals("left of ₹5,000", b.amountCaption)
        assertEquals("100% used · Sick", b.chipText)
        assertEquals("₹0/day for 7 days left", b.footerEnd)
    }

    @Test fun essentialDoesNotMoveBuddy() {
        val b = budgeted(home(view(listOf(tx(amountPaise = 50_000_00, category = Category.ESSENTIAL)))))
        assertEquals(BuddyState.THRIVING, b.state)
        assertEquals("₹5,000", b.amountText)
    }

    @Test fun nullSummaryDoesNotCrash() {
        val m = home(view(listOf(tx(category = Category.UNTAGGED)), budgetPaise = null))
        assertEquals(BuddyCardModel.NoBudget, m.buddy)
        assertEquals("1 payment not tagged", m.untagged?.title)
        assertEquals(1, m.recent.size)
    }

    @Test fun nullSummaryEmptyMonth() {
        val m = home(view(emptyList(), budgetPaise = null))
        assertEquals(BuddyCardModel.NoBudget, m.buddy)
        assertTrue(m.showFreshMonthCard)
        assertNull(m.untagged)
    }

    @Test fun pastMonth() {
        val rows = listOf(
            tx(id = 1, amountPaise = 1_000_00, occurredAt = millisAt(2026, 9, 20), category = Category.UNTAGGED),
            tx(id = 2, amountPaise = 500_00, occurredAt = millisAt(2026, 9, 10), category = Category.MISC),
        )
        val m = home(view(rows, monthKey = "2026-09", budgetPaise = 3_000_00), monthKey = "2026-09")
        val b = budgeted(m)
        assertTrue(m.readOnly)
        assertEquals("September 2026", m.monthLabel)
        assertEquals("Back to October 2026", m.backToCurrentLabel)
        assertEquals("left of ₹3,000", b.amountCaption)
        assertEquals("Month ended · ₹1,500 spent", b.footerStart)
        assertNull(b.footerEnd)
        assertEquals(UntaggedModel("1 not tagged · counted in Misc", null, tagNow = false), m.untagged)
        assertFalse(m.showAddExpense)
        assertFalse(m.showFreshMonthCard)
        assertEquals(listOf("20 Sep", "10 Sep"), m.recent.map { it.subtitle })
    }

    @Test fun pastMonthOverBudgetHasNoFooterEnd() {
        val rows = listOf(tx(amountPaise = 4_000_00, occurredAt = millisAt(2026, 9, 20), category = Category.MISC))
        val b = budgeted(home(view(rows, monthKey = "2026-09", budgetPaise = 3_000_00), monthKey = "2026-09"))
        assertTrue(b.overBudget)
        assertNull(b.footerEnd)
        assertEquals("Month ended · ₹4,000 spent", b.footerStart)
    }

    @Test fun recentLimitedTo5NewestInOrder() {
        val rows = (7L downTo 1L).map { tx(id = it, occurredAt = millisAt(2026, 10, 20, it.toInt(), 0)) }
        val m = home(view(rows))
        assertEquals(RECENT_LIMIT, m.recent.size)
        assertEquals(listOf(7L, 6L, 5L, 4L, 3L), m.recent.map { it.id })
    }

    @Test fun untaggedCardHiddenAtZero() {
        val m = home(view(listOf(tx(category = Category.MISC), tx(id = 2, category = Category.ESSENTIAL))))
        assertNull(m.untagged)
    }

    @Test fun untaggedCardWordingSingularAndPlural() {
        val one = home(view(listOf(tx(category = Category.UNTAGGED))))
        assertEquals(UntaggedModel("1 payment not tagged", "Currently counted in Misc", tagNow = true), one.untagged)
        val two = home(view(listOf(tx(id = 1, category = Category.UNTAGGED), tx(id = 2, category = Category.UNTAGGED))))
        assertEquals("2 payments not tagged", two.untagged?.title)
    }

    @Test fun rowModelChipAndManualFlag() {
        val r = rowModel(
            tx(id = 9, amountPaise = 2_000, source = Source.MANUAL, category = Category.MISC,
                occurredAt = millisAt(2026, 10, 25, 9, 5)),
            IST, today,
        )
        assertEquals(RowModel(9, "Swiggy", "9:05 AM · Manual", "–₹20", ChipKind.MISC, manual = true), r)
    }

    @Test fun daysLeftWording() {
        assertEquals("1 day left", daysLeftText(1))
        assertEquals("31 days left", daysLeftText(31))
    }
}
