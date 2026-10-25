package com.example.paisapet.ui.transactions

import com.example.paisapet.budget.Spend
import com.example.paisapet.budget.debitTotals
import com.example.paisapet.data.MonthView
import com.example.paisapet.data.db.TransactionEntity
import com.example.paisapet.format.formatRupees
import com.example.paisapet.list.TxFilter
import com.example.paisapet.list.applyFilter
import com.example.paisapet.list.chipOf
import com.example.paisapet.list.debitText
import com.example.paisapet.list.filterCounts
import com.example.paisapet.list.groupByDay
import com.example.paisapet.list.isReadOnly
import com.example.paisapet.list.monthLabel
import com.example.paisapet.list.transactionsSubtitle
import com.example.paisapet.model.Source
import com.example.paisapet.ui.home.RowModel
import java.time.LocalDate
import java.time.ZoneId

// Pure: everything Transactions (S6) shows. No Compose here.

/** The month header: totals over all the month's debits, whatever the filter. Misc includes Not tagged. */
data class TxHeaderModel(
    /** "₹7,350". */
    val totalText: String,
    /** "28 spends". */
    val countText: String,
    val miscText: String,
    /** "43% of total". */
    val miscPctText: String,
    val essentialText: String,
    val essentialPctText: String,
    /** Width of the Misc part of the split bar, 0 to 100. */
    val miscPercent: Int,
)

data class FilterChipModel(val filter: TxFilter, val label: String, val selected: Boolean)

data class DayGroupModel(
    /** "Today", "Yesterday", "21 Oct". */
    val label: String,
    /** "₹425 spent": the rows shown under this day for the current filter. */
    val totalText: String,
    val rows: List<RowModel>,
)

data class TransactionsModel(
    val monthKey: String,
    val monthLabel: String,
    val readOnly: Boolean,
    /** "Back to October 2026" for a past month, else null. */
    val backToCurrentLabel: String?,
    val header: TxHeaderModel,
    val chips: List<FilterChipModel>,
    val groups: List<DayGroupModel>,
    /** Shown instead of the list when it is empty. */
    val emptyText: String?,
    /** "Tag me?" in the current month; past months are read-only, so the chip just says "Not tagged". */
    val untaggedChipText: String,
)

fun buildTransactionsModel(
    view: MonthView,
    filter: TxFilter,
    monthKey: String,
    currentKey: String,
    today: LocalDate,
    zone: ZoneId,
): TransactionsModel {
    val readOnly = isReadOnly(monthKey, currentKey)
    val rows = view.transactions
    val shown = applyFilter(rows, filter)
    return TransactionsModel(
        monthKey = monthKey,
        monthLabel = monthLabel(monthKey),
        readOnly = readOnly,
        backToCurrentLabel = if (readOnly) "Back to ${monthLabel(currentKey)}" else null,
        header = header(rows),
        chips = chips(rows, filter),
        groups = groupByDay(shown, zone, today).map { g ->
            DayGroupModel(g.label, "${formatRupees(g.totalPaise)} spent", g.rows.map { txRowModel(it, zone) })
        },
        emptyText = when {
            rows.isEmpty() && readOnly -> "No transactions this month"
            rows.isEmpty() -> "No transactions yet this month"
            shown.isEmpty() -> "No payments match this filter"
            else -> null
        },
        untaggedChipText = if (readOnly) "Not tagged" else "Tag me?",
    )
}

/** S6 row: the subtitle is the time, plus " · Manual". */
fun txRowModel(t: TransactionEntity, zone: ZoneId): RowModel = RowModel(
    id = t.id,
    payee = t.payee,
    subtitle = transactionsSubtitle(t, zone),
    amountText = debitText(t.amountPaise),
    chip = chipOf(t.category),
    manual = t.source == Source.MANUAL,
)

private fun header(rows: List<TransactionEntity>): TxHeaderModel {
    // From the rows, so it works without a budget; debitTotals is the same function summarize uses.
    val t = debitTotals(rows.map { Spend(it.amountPaise, it.category) })
    return TxHeaderModel(
        totalText = formatRupees(t.totalPaise),
        countText = if (t.count == 1) "1 spend" else "${t.count} spends",
        miscText = formatRupees(t.miscPaise),
        miscPctText = "${t.miscPct}% of total",
        essentialText = formatRupees(t.essentialPaise),
        essentialPctText = "${t.essentialPct}% of total",
        miscPercent = t.miscPct,
    )
}

private fun chips(rows: List<TransactionEntity>, selected: TxFilter): List<FilterChipModel> {
    val counts = filterCounts(rows)
    return TxFilter.entries.map { f ->
        val label = when (f) {
            TxFilter.ALL -> "All"
            TxFilter.MISC -> "Misc (${counts.getValue(f)})"
            TxFilter.ESSENTIAL -> "Essential (${counts.getValue(f)})"
            TxFilter.NOT_TAGGED -> "Not tagged (${counts.getValue(f)})"
        }
        FilterChipModel(f, label, f == selected)
    }
}
