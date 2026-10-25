package com.example.paisapet.widget

import com.example.paisapet.budget.BuddyState
import com.example.paisapet.budget.Spend
import com.example.paisapet.budget.summarize
import com.example.paisapet.data.MonthView
import com.example.paisapet.data.db.TransactionEntity
import com.example.paisapet.millisAt
import com.example.paisapet.model.Category
import com.example.paisapet.model.Source
import com.example.paisapet.sms.SmsParser
import com.example.paisapet.tx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WidgetModelTest {

    private val today = LocalDate.of(2026, 10, 25)
    private val budget = 5_000_00L

    /** The real summarize, as the repository builds it. */
    private fun view(rows: List<TransactionEntity>, budgetPaise: Long? = budget, monthKey: String = "2026-10") =
        MonthView(rows, budgetPaise?.let { summarize(monthKey, it, rows.map { r -> Spend(r.amountPaise, r.category) }, today) })

    private fun model(v: MonthView, monthKey: String = "2026-10", onboarded: Boolean = true) =
        buildWidgetModel(onboarded, v, monthKey)

    private fun budgeted(v: MonthView, monthKey: String = "2026-10") = model(v, monthKey) as WidgetModel.Budgeted

    private fun misc(paise: Long, id: Long = 1) = tx(id = id, amountPaise = paise, category = Category.MISC)

    // Set up and no budget

    @Test fun setUpBeforeOnboardingEvenWithASummary() {
        assertSame(WidgetModel.SetUp, model(view(listOf(misc(1_000_00))), onboarded = false))
        assertSame(WidgetModel.SetUp, model(view(emptyList(), budgetPaise = null), onboarded = false))
    }

    @Test fun noBudgetWhenSummaryIsNull() {
        assertSame(WidgetModel.NoBudget, model(view(listOf(misc(1_000_00)), budgetPaise = null)))
    }

    @Test fun texts() {
        assertEquals("Set up Paisa Pet", WIDGET_SET_UP_TEXT)
        assertEquals("Set your budget", WIDGET_NO_BUDGET_TEXT)
        assertEquals("No payments yet this month", WIDGET_NO_PAYMENTS_TEXT)
    }

    // Under, at and over budget

    @Test fun underBudget() {
        val m = budgeted(view(listOf(misc(3_200_00))))
        assertEquals("₹1,800", m.amountText)
        assertFalse(m.overBudget)
        assertEquals("left of ₹5,000", m.ofBudgetText)
        assertEquals("64% used", m.pctChip)
        assertEquals(64, m.barPercent)
        assertEquals("Budget ₹5,000 for Oct", m.budgetLine)
        assertEquals(BuddyState.WORRIED, m.state)
    }

    @Test fun exactly100PercentIsNotOver() {
        val m = budgeted(view(listOf(misc(5_000_00))))
        assertEquals(BuddyState.SICK, m.state)
        assertFalse(m.overBudget)
        assertEquals("₹0", m.amountText)
        assertEquals("left of ₹5,000", m.ofBudgetText)
        assertEquals("100% used", m.pctChip)
        assertEquals(100, m.barPercent)
    }

    @Test fun overBudgetAt100Point01Percent() {
        val m = budgeted(view(listOf(misc(5_000_50))))
        assertEquals(BuddyState.FAINTED, m.state)
        assertTrue(m.overBudget)
        assertEquals("₹0.50", m.amountText)
        assertEquals("over budget of ₹5,000", m.ofBudgetText)
        assertEquals("100% used", m.pctChip)
        assertEquals(100, m.barPercent)
    }

    @Test fun barClampsAt100For300Percent() {
        val m = budgeted(view(listOf(misc(15_000_00))))
        assertEquals("300% used", m.pctChip)
        assertEquals(100, m.barPercent)
        assertEquals("₹10,000", m.amountText)
        assertTrue(m.overBudget)
    }

    @Test fun emptyMonthIsThrivingWithNoLastPayment() {
        val m = budgeted(view(emptyList()))
        assertEquals(BuddyState.THRIVING, m.state)
        assertEquals("₹5,000", m.amountText)
        assertEquals("0% used", m.pctChip)
        assertEquals(0, m.barPercent)
        assertNull(m.lastPayment)
    }

    @Test fun everyBuddyStateIsPassedThrough() {
        val cases = mapOf(
            1_000_00L to BuddyState.THRIVING,
            1_250_00L to BuddyState.FINE,
            2_500_00L to BuddyState.WORRIED,
            3_750_00L to BuddyState.SICK,
            5_000_01L to BuddyState.FAINTED,
        )
        for ((spent, state) in cases) {
            assertEquals("spent $spent", state, budgeted(view(listOf(misc(spent)))).state)
        }
    }

    // Money format

    @Test fun indianGroupingForALakhBudget() {
        val m = budgeted(view(listOf(misc(12_345_00)), budgetPaise = 1_00_000_00))
        assertEquals("₹87,655", m.amountText)
        assertEquals("left of ₹1,00,000", m.ofBudgetText)
        assertEquals("Budget ₹1,00,000 for Oct", m.budgetLine)
    }

    @Test fun paiseOnlyWhenNonZero() {
        assertEquals("₹4,759.25", budgeted(view(listOf(misc(240_75)))).amountText)
        assertEquals("₹4,760", budgeted(view(listOf(misc(240_00)))).amountText)
        assertEquals("–₹240.75 · Swiggy · Misc", budgeted(view(listOf(misc(240_75)))).lastPayment)
    }

    // Month label

    @Test fun shortMonthLabels() {
        assertEquals("Jan", shortMonth("2027-01"))
        assertEquals("Oct", shortMonth("2026-10"))
        assertEquals("Dec", shortMonth("2026-12"))
        assertEquals("Budget ₹5,000 for Jan", budgeted(view(emptyList(), monthKey = "2027-01"), "2027-01").budgetLine)
        assertEquals("Budget ₹5,000 for Dec", budgeted(view(emptyList(), monthKey = "2026-12"), "2026-12").budgetLine)
    }

    // Last payment

    @Test fun lastPaymentPerCategory() {
        assertEquals("–₹240 · Swiggy · Misc", budgeted(view(listOf(tx(category = Category.MISC)))).lastPayment)
        assertEquals("–₹240 · Swiggy · Essential", budgeted(view(listOf(tx(category = Category.ESSENTIAL)))).lastPayment)
        assertEquals("–₹240 · Swiggy · Not tagged", budgeted(view(listOf(tx(category = Category.UNTAGGED)))).lastPayment)
    }

    @Test fun lastPaymentForManualAndSmsRows() {
        val manual = tx(payee = "Chai stall", category = Category.MISC, source = Source.MANUAL)
        val sms = tx(payee = "Zomato", category = Category.ESSENTIAL, source = Source.SMS)
        assertEquals("–₹240 · Chai stall · Misc", budgeted(view(listOf(manual))).lastPayment)
        assertEquals("–₹240 · Zomato · Essential", budgeted(view(listOf(sms))).lastPayment)
    }

    @Test fun longPayeeIsShortened() {
        val payee = "ABCDEFGHIJKLMNOPQRSTUVWXYZ abcdefghijkl" // 39 characters
        val payee40 = payee + "m"
        assertEquals(40, payee40.length)
        val shortened = shortPayee(payee40)
        assertEquals("ABCDEFGHIJKLMNOPQ…", shortened)
        assertEquals(WIDGET_PAYEE_MAX, shortened.length)
        assertEquals("–₹240 · ABCDEFGHIJKLMNOPQ… · Misc", budgeted(view(listOf(tx(payee = payee40, category = Category.MISC)))).lastPayment)
    }

    @Test fun payeeAtTheLimitIsKept() {
        assertEquals("ABCDEFGHIJKLMNOPQR", shortPayee("ABCDEFGHIJKLMNOPQR"))
        assertEquals("ABCDEFGHIJKLMNOPQ…", shortPayee("ABCDEFGHIJKLMNOPQRS"))
        // No space before the ellipsis when the cut lands after a space.
        assertEquals("Big Bazaar Store…", shortPayee("Big Bazaar Store Andheri"))
    }

    @Test fun upiPaymentPayeeStays() {
        assertEquals(
            "–₹240 · UPI payment · Not tagged",
            budgeted(view(listOf(tx(payee = SmsParser.NO_PAYEE)))).lastPayment,
        )
    }

    @Test fun newestByOccurredAtNotListOrder() {
        val older = tx(id = 9, payee = "Older", occurredAt = millisAt(2026, 10, 3))
        val newer = tx(id = 2, payee = "Newer", occurredAt = millisAt(2026, 10, 20))
        assertEquals("–₹240 · Newer · Not tagged", budgeted(view(listOf(older, newer))).lastPayment)
        assertEquals("–₹240 · Newer · Not tagged", budgeted(view(listOf(newer, older))).lastPayment)
    }

    @Test fun sameTimeHigherIdWins() {
        val at = millisAt(2026, 10, 20, 9, 30)
        val first = tx(id = 4, payee = "First", occurredAt = at)
        val second = tx(id = 5, payee = "Second", occurredAt = at)
        assertEquals("–₹240 · Second · Not tagged", budgeted(view(listOf(second, first))).lastPayment)
        assertEquals("–₹240 · Second · Not tagged", budgeted(view(listOf(first, second))).lastPayment)
    }
}
