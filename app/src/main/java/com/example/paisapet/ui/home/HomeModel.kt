package com.example.paisapet.ui.home

import com.example.paisapet.budget.BuddyState
import com.example.paisapet.data.MonthView
import com.example.paisapet.data.db.TransactionEntity
import com.example.paisapet.format.formatRupees
import com.example.paisapet.list.ChipKind
import com.example.paisapet.list.TxFilter
import com.example.paisapet.list.chipOf
import com.example.paisapet.list.debitText
import com.example.paisapet.list.filterCounts
import com.example.paisapet.list.isReadOnly
import com.example.paisapet.list.monthLabel
import com.example.paisapet.list.recentSubtitle
import com.example.paisapet.list.untaggedText
import com.example.paisapet.model.Source
import com.example.paisapet.ui.buddy.buddyHomeLabel
import com.example.paisapet.ui.buddy.buddyName
import java.time.LocalDate
import java.time.ZoneId

// Pure: everything Home (S4, S5) shows, built from the month's rows and BudgetSummary. No Compose here.

const val RECENT_LIMIT = 5

/** One payment row, ready to draw. */
data class RowModel(
    val id: Long,
    val payee: String,
    val subtitle: String,
    val amountText: String,
    val chip: ChipKind,
    val manual: Boolean,
)

sealed interface BuddyCardModel {
    /** The month has no budget at all (summary is null). */
    data object NoBudget : BuddyCardModel

    data class Budgeted(
        val state: BuddyState,
        val homeLabel: String,
        /** "₹1,800" left, or the amount over budget when [overBudget]. */
        val amountText: String,
        /** "left of ₹5,000" or "over budget of ₹5,000". */
        val amountCaption: String,
        val overBudget: Boolean,
        /** "64% used · Worried". */
        val chipText: String,
        /** Bar fill, 0 to 100: the whole % used, full once over budget. */
        val barPercent: Int,
        /** "₹3,200 spent", or "Month ended · ₹3,200 spent" for a past month. */
        val footerStart: String,
        /** The pace line, or "D days left" when over budget; null for a past month. */
        val footerEnd: String?,
    ) : BuddyCardModel
}

/** [tagNow] false for a past month: a plain line, no button. */
data class UntaggedModel(val title: String, val subtitle: String?, val tagNow: Boolean)

data class HomeModel(
    val monthKey: String,
    /** "October 2026". */
    val monthLabel: String,
    val readOnly: Boolean,
    /** "Back to October 2026" for a past month, else null. */
    val backToCurrentLabel: String?,
    val buddy: BuddyCardModel,
    val untagged: UntaggedModel?,
    /** S4: the current month has no payments yet. */
    val showFreshMonthCard: Boolean,
    val recent: List<RowModel>,
    val emptyRecentText: String?,
    val showAddExpense: Boolean,
)

fun buildHomeModel(view: MonthView, monthKey: String, currentKey: String, today: LocalDate, zone: ZoneId): HomeModel {
    val readOnly = isReadOnly(monthKey, currentKey)
    val isEmpty = view.transactions.isEmpty()
    val untaggedCount = view.summary?.untaggedCount ?: filterCounts(view.transactions).getValue(TxFilter.NOT_TAGGED)
    return HomeModel(
        monthKey = monthKey,
        monthLabel = monthLabel(monthKey),
        readOnly = readOnly,
        backToCurrentLabel = if (readOnly) "Back to ${monthLabel(currentKey)}" else null,
        buddy = buddyCard(view, readOnly),
        untagged = when {
            untaggedCount == 0 -> null
            readOnly -> UntaggedModel("$untaggedCount not tagged · counted in Misc", null, tagNow = false)
            else -> UntaggedModel(untaggedText(untaggedCount), "Currently counted in Misc", tagNow = true)
        },
        showFreshMonthCard = isEmpty && !readOnly,
        recent = view.transactions.take(RECENT_LIMIT).map { rowModel(it, zone, today) },
        emptyRecentText = when {
            !isEmpty -> null
            readOnly -> "No transactions this month"
            else -> "No transactions yet this month"
        },
        showAddExpense = !readOnly,
    )
}

fun rowModel(t: TransactionEntity, zone: ZoneId, today: LocalDate): RowModel = RowModel(
    id = t.id,
    payee = t.payee,
    subtitle = recentSubtitle(t, zone, today),
    amountText = debitText(t.amountPaise),
    chip = chipOf(t.category),
    manual = t.source == Source.MANUAL,
)

private fun buddyCard(view: MonthView, readOnly: Boolean): BuddyCardModel {
    val s = view.summary ?: return BuddyCardModel.NoBudget
    val over = s.leftPaise < 0
    val budget = formatRupees(s.budgetPaise)
    val spent = formatRupees(s.miscSpentPaise)
    return BuddyCardModel.Budgeted(
        state = s.buddyState,
        homeLabel = buddyHomeLabel(s.buddyState),
        amountText = formatRupees(if (over) -s.leftPaise else s.leftPaise),
        amountCaption = if (over) "over budget of $budget" else "left of $budget",
        overBudget = over,
        chipText = "${s.pctUsedFloor}% used · ${buddyName(s.buddyState)}",
        barPercent = s.pctUsedFloor.coerceIn(0, 100).toInt(),
        footerStart = if (readOnly) "Month ended · $spent spent" else "$spent spent",
        footerEnd = when {
            readOnly -> null
            over -> daysLeftText(s.daysLeft)
            else -> "${formatRupees(s.pacePaisePerDay)}/day for ${daysLeftText(s.daysLeft)}"
        },
    )
}

/** "1 day left", "7 days left". */
fun daysLeftText(days: Int): String = if (days == 1) "1 day left" else "$days days left"
