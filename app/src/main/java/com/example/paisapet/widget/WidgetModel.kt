package com.example.paisapet.widget

import com.example.paisapet.budget.BuddyState
import com.example.paisapet.budget.parseMonthKey
import com.example.paisapet.data.MonthView
import com.example.paisapet.data.db.TransactionEntity
import com.example.paisapet.format.formatRupees
import com.example.paisapet.list.debitText
import com.example.paisapet.model.Category
import java.time.format.DateTimeFormatter
import java.util.Locale

// Pure: everything the small and wide widgets show (S11), built from BudgetSummary. No Glance or Android here.

const val WIDGET_SET_UP_TEXT = "Set up Paisa Pet"
const val WIDGET_NO_BUDGET_TEXT = "Set your budget"
const val WIDGET_NO_PAYMENTS_TEXT = "No payments yet this month"

/** Shown when the widget could not read the data. */
const val WIDGET_ERROR_TEXT = "Open Paisa Pet"

/** Longer payees are cut to this many characters, the last one an ellipsis. */
const val WIDGET_PAYEE_MAX = 18

private val SHORT_MONTH = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)

sealed interface WidgetModel {
    /** Onboarding is not complete. */
    data object SetUp : WidgetModel

    /** The month has no budget at all (summary is null). */
    data object NoBudget : WidgetModel

    data class Budgeted(
        val state: BuddyState,
        /** "₹1,800" left, or the amount over budget when [overBudget]. */
        val amountText: String,
        val overBudget: Boolean,
        /** "left of ₹5,000" or "over budget of ₹5,000". */
        val ofBudgetText: String,
        /** "Budget ₹5,000 for Oct". */
        val budgetLine: String,
        /** "64% used". */
        val pctChip: String,
        /** Bar fill, 0 to 100: the whole % used, full once over budget. */
        val barPercent: Int,
        /** "–₹240 · Swiggy · Misc" for the newest payment; null for an empty month. */
        val lastPayment: String?,
    ) : WidgetModel
}

/** [monthKey] is the current month: widgets never show a past one. */
fun buildWidgetModel(onboardingComplete: Boolean, view: MonthView, monthKey: String): WidgetModel {
    if (!onboardingComplete) return WidgetModel.SetUp
    val s = view.summary ?: return WidgetModel.NoBudget
    val over = s.leftPaise < 0
    val budget = formatRupees(s.budgetPaise)
    return WidgetModel.Budgeted(
        state = s.buddyState,
        amountText = formatRupees(if (over) -s.leftPaise else s.leftPaise),
        overBudget = over,
        ofBudgetText = if (over) "over budget of $budget" else "left of $budget",
        budgetLine = "Budget $budget for ${shortMonth(monthKey)}",
        pctChip = "${s.pctUsedFloor}% used",
        barPercent = s.pctUsedFloor.coerceIn(0, 100).toInt(),
        lastPayment = newest(view.transactions)?.let(::lastPaymentText),
    )
}

/** Width of the coloured part of a [trackDp]-wide bar; Glance has no fractional width, so the widget passes dp. */
fun barFillDp(trackDp: Float, barPercent: Int): Float =
    if (trackDp <= 0f) 0f else trackDp * barPercent.coerceIn(0, 100) / 100f

/** "Oct" for "2026-10". */
fun shortMonth(monthKey: String): String = parseMonthKey(monthKey).format(SHORT_MONTH)

/** Latest occurredAt; the higher id breaks a tie. Does not rely on the DAO's order. */
private fun newest(rows: List<TransactionEntity>): TransactionEntity? =
    rows.maxWithOrNull(compareBy<TransactionEntity> { it.occurredAt }.thenBy { it.id })

private fun lastPaymentText(t: TransactionEntity): String =
    "${debitText(t.amountPaise)} · ${shortPayee(t.payee)} · ${categoryWord(t.category)}"

/** Payee as stored, cut to [WIDGET_PAYEE_MAX] characters including the "…". */
fun shortPayee(payee: String): String =
    if (payee.length <= WIDGET_PAYEE_MAX) payee else payee.take(WIDGET_PAYEE_MAX - 1).trimEnd() + "…"

private fun categoryWord(c: Category): String = when (c) {
    Category.MISC -> "Misc"
    Category.ESSENTIAL -> "Essential"
    Category.UNTAGGED -> "Not tagged"
}
