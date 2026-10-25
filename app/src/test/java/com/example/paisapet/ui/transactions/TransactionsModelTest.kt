package com.example.paisapet.ui.transactions

import com.example.paisapet.IST
import com.example.paisapet.budget.Spend
import com.example.paisapet.budget.monthKeyOf
import com.example.paisapet.budget.summarize
import com.example.paisapet.data.MonthView
import com.example.paisapet.data.db.TransactionEntity
import com.example.paisapet.format.formatRupees
import com.example.paisapet.list.ChipKind
import com.example.paisapet.list.TxFilter
import com.example.paisapet.millisAt
import com.example.paisapet.model.Category
import com.example.paisapet.model.Source
import com.example.paisapet.tx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class TransactionsModelTest {

    private val today = LocalDate.of(2026, 10, 25)

    private fun view(rows: List<TransactionEntity>, monthKey: String = "2026-10", budgetPaise: Long? = 5_000_00) =
        MonthView(
            rows,
            budgetPaise?.let { summarize(monthKey, it, rows.map { r -> Spend(r.amountPaise, r.category) }, today) },
        )

    private fun model(
        v: MonthView,
        filter: TxFilter = TxFilter.ALL,
        monthKey: String = "2026-10",
        currentKey: String = "2026-10",
        day: LocalDate = today,
    ) = buildTransactionsModel(v, filter, monthKey, currentKey, day, IST)

    // The mockup's month, newest first as the DAO returns it.
    private val swiggy = tx(id = 6, amountPaise = 240_00, payee = "Swiggy", occurredAt = millisAt(2026, 10, 25, 13, 15), category = Category.MISC)
    private val blinkit = tx(id = 5, amountPaise = 185_00, payee = "Blinkit", occurredAt = millisAt(2026, 10, 25, 11, 30), category = Category.MISC)
    private val dmart = tx(id = 4, amountPaise = 1_850_00, payee = "DMart", occurredAt = millisAt(2026, 10, 24, 17, 20), category = Category.ESSENTIAL)
    private val auto = tx(id = 3, amountPaise = 85_00, payee = "Rapido Auto", occurredAt = millisAt(2026, 10, 24, 9, 15), category = Category.MISC, source = Source.MANUAL)
    private val ramesh = tx(id = 2, amountPaise = 30_00, payee = "ramesh.k@ybl", occurredAt = millisAt(2026, 10, 21, 16, 10), category = Category.UNTAGGED)
    private val airtel = tx(id = 1, amountPaise = 1_179_00, payee = "Airtel Broadband", occurredAt = millisAt(2026, 10, 21, 10, 0), category = Category.ESSENTIAL)
    private val month = listOf(swiggy, blinkit, dmart, auto, ramesh, airtel)

    @Test fun emptyMonth() {
        val m = model(view(emptyList()))
        assertEquals("₹0", m.header.totalText)
        assertEquals("0 spends", m.header.countText)
        assertEquals("0% of total", m.header.miscPctText)
        assertEquals("0% of total", m.header.essentialPctText)
        assertEquals(0, m.header.miscPercent)
        assertEquals(listOf("All", "Misc (0)", "Essential (0)", "Not tagged (0)"), m.chips.map { it.label })
        assertTrue(m.groups.isEmpty())
        assertEquals("No transactions yet this month", m.emptyText)
        assertFalse(m.readOnly)
        assertEquals("Tag me?", m.untaggedChipText)
    }

    @Test fun mixedCategoriesHeader() {
        val h = model(view(month)).header
        // Misc 240 + 185 + 85 + 30 (Not tagged) = 540; Essential 1,850 + 1,179 = 3,029; total 3,569.
        assertEquals("₹3,569", h.totalText)
        assertEquals("6 spends", h.countText)
        assertEquals("₹540", h.miscText)
        assertEquals("₹3,029", h.essentialText)
        assertEquals("15% of total", h.miscPctText)
        assertEquals("85% of total", h.essentialPctText)
        assertEquals(15, h.miscPercent)
    }

    @Test fun oneSpendIsSingular() {
        assertEquals("1 spend", model(view(listOf(swiggy))).header.countText)
    }

    @Test fun headerMiscEqualsSummaryMisc() {
        val v = view(month)
        val h = model(v).header
        assertEquals(formatRupees(v.summary!!.miscSpentPaise), h.miscText)
        assertEquals(formatRupees(v.summary!!.essentialSpentPaise), h.essentialText)
        assertEquals(formatRupees(v.summary!!.totalSpentPaise), h.totalText)
    }

    @Test fun filterCountsOnChips() {
        val m = model(view(month))
        assertEquals(listOf("All", "Misc (3)", "Essential (2)", "Not tagged (1)"), m.chips.map { it.label })
        assertEquals(listOf(true, false, false, false), m.chips.map { it.selected })
    }

    @Test fun selectedChipFollowsFilter() {
        val m = model(view(month), TxFilter.NOT_TAGGED)
        assertEquals(TxFilter.NOT_TAGGED, m.chips.single { it.selected }.filter)
    }

    @Test fun allGroupsByDayWithTotalsAndRows() {
        val m = model(view(month))
        assertEquals(listOf("Today", "Yesterday", "21 Oct"), m.groups.map { it.label })
        assertEquals(listOf("₹425 spent", "₹1,935 spent", "₹1,209 spent"), m.groups.map { it.totalText })
        assertEquals(listOf(6L, 5L), m.groups[0].rows.map { it.id })
        assertEquals("1:15 PM", m.groups[0].rows[0].subtitle)
        assertEquals("9:15 AM · Manual", m.groups[1].rows[1].subtitle)
        assertEquals("–₹240", m.groups[0].rows[0].amountText)
        assertNull(m.emptyText)
    }

    @Test fun miscFilterExcludesNotTaggedAndTotalsOnlyShownRows() {
        val m = model(view(month), TxFilter.MISC)
        assertEquals(listOf("Today", "Yesterday"), m.groups.map { it.label })
        assertEquals(listOf("₹425 spent", "₹85 spent"), m.groups.map { it.totalText })
        assertTrue(m.groups.flatMap { it.rows }.all { it.chip == ChipKind.MISC })
    }

    @Test fun essentialFilter() {
        val m = model(view(month), TxFilter.ESSENTIAL)
        assertEquals(listOf(4L, 1L), m.groups.flatMap { it.rows }.map { it.id })
        assertEquals(listOf("₹1,850 spent", "₹1,179 spent"), m.groups.map { it.totalText })
    }

    @Test fun notTaggedFilter() {
        val m = model(view(month), TxFilter.NOT_TAGGED)
        assertEquals(listOf(2L), m.groups.flatMap { it.rows }.map { it.id })
        assertEquals(ChipKind.NOT_TAGGED, m.groups.single().rows.single().chip)
    }

    @Test fun headerIgnoresTheFilter() {
        assertEquals(model(view(month)).header, model(view(month), TxFilter.ESSENTIAL).header)
    }

    @Test fun emptyFilterText() {
        val m = model(view(listOf(swiggy)), TxFilter.NOT_TAGGED)
        assertTrue(m.groups.isEmpty())
        assertEquals("No payments match this filter", m.emptyText)
        assertEquals("Not tagged (0)", m.chips.last().label)
    }

    @Test fun oct31At2359AndNov1At0000InDifferentGroupsAndMonths() {
        val late = tx(id = 1, occurredAt = millisAt(2026, 10, 31, 23, 59), category = Category.MISC)
        val early = tx(id = 2, occurredAt = millisAt(2026, 11, 1, 0, 0), category = Category.MISC)
        assertNotEquals(late.monthKey, early.monthKey)
        assertEquals("2026-10", monthKeyOf(Instant.ofEpochMilli(late.occurredAt), IST))
        assertEquals("2026-11", monthKeyOf(Instant.ofEpochMilli(early.occurredAt), IST))

        // Each month's view holds only its own row; seen together they are two days.
        val nov1 = LocalDate.of(2026, 11, 1)
        val october = buildTransactionsModel(MonthView(listOf(late), null), TxFilter.ALL, "2026-10", "2026-11", nov1, IST)
        val november = buildTransactionsModel(MonthView(listOf(early), null), TxFilter.ALL, "2026-11", "2026-11", nov1, IST)
        assertEquals(listOf("Yesterday"), october.groups.map { it.label })
        assertEquals(listOf("Today"), november.groups.map { it.label })
        val both = buildTransactionsModel(MonthView(listOf(early, late), null), TxFilter.ALL, "2026-11", "2026-11", nov1, IST)
        assertEquals(listOf("Today", "Yesterday"), both.groups.map { it.label })
    }

    @Test fun readOnlyPastMonth() {
        val sep = listOf(tx(id = 1, occurredAt = millisAt(2026, 9, 20), category = Category.UNTAGGED))
        val m = model(view(sep, monthKey = "2026-09"), monthKey = "2026-09")
        assertTrue(m.readOnly)
        assertEquals("September 2026", m.monthLabel)
        assertEquals("Back to October 2026", m.backToCurrentLabel)
        assertEquals("Not tagged", m.untaggedChipText)
        assertEquals(listOf("20 Sep"), m.groups.map { it.label })
    }

    @Test fun readOnlyEmptyMonthText() {
        val m = model(view(emptyList(), monthKey = "2026-09"), monthKey = "2026-09")
        assertEquals("No transactions this month", m.emptyText)
    }

    @Test fun currentMonthIsNotReadOnly() {
        val m = model(view(month))
        assertFalse(m.readOnly)
        assertNull(m.backToCurrentLabel)
    }

    @Test fun nullSummaryHeaderStillWorks() {
        val m = model(view(month, budgetPaise = null))
        assertEquals("₹3,569", m.header.totalText)
        assertEquals("₹540", m.header.miscText)
        assertEquals(3, m.groups.size)
    }

    @Test fun onlyEssentialIs0And100() {
        val m = model(view(listOf(dmart, airtel)))
        assertEquals("₹0", m.header.miscText)
        assertEquals("0% of total", m.header.miscPctText)
        assertEquals("100% of total", m.header.essentialPctText)
        assertEquals(0, m.header.miscPercent)
    }

    @Test fun txRowModelHasTimeSubtitleEvenForOlderDays() {
        val r = txRowModel(airtel, IST)
        assertEquals("10:00 AM", r.subtitle)
        assertEquals(ChipKind.ESSENTIAL, r.chip)
        assertFalse(r.manual)
    }
}
