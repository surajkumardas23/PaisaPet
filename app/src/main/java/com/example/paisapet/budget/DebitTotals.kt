package com.example.paisapet.budget

import com.example.paisapet.model.Category

/** The S6 header. Misc includes UNTAGGED, as on Home (FR-08). */
data class DebitTotals(
    val miscPaise: Long,
    val essentialPaise: Long,
    val totalPaise: Long,
    val count: Int,
    /** Rounded down; 0 when there are no debits. */
    val miscPct: Int,
    /** 100 − [miscPct] so the two always add to 100; 0 when there are no debits. */
    val essentialPct: Int,
)

fun debitTotals(spends: List<Spend>): DebitTotals {
    val misc = miscSpent(spends)
    val essential = spends.filter { it.category == Category.ESSENTIAL }.sumOf { it.amountPaise }
    val total = Math.addExact(misc, essential)
    val miscPct = if (total == 0L) 0 else (Math.multiplyExact(misc, 100L) / total).toInt()
    return DebitTotals(
        miscPaise = misc,
        essentialPaise = essential,
        totalPaise = total,
        count = spends.size,
        miscPct = miscPct,
        essentialPct = if (total == 0L) 0 else 100 - miscPct,
    )
}
