package com.example.paisapet.list

import com.example.paisapet.data.db.TransactionEntity
import com.example.paisapet.model.Category

/** S6 filter chips. MISC lists only rows tagged Misc; Not tagged rows have their own chip. */
enum class TxFilter { ALL, MISC, ESSENTIAL, NOT_TAGGED }

fun TxFilter.matches(category: Category): Boolean = when (this) {
    TxFilter.ALL -> true
    TxFilter.MISC -> category == Category.MISC
    TxFilter.ESSENTIAL -> category == Category.ESSENTIAL
    TxFilter.NOT_TAGGED -> category == Category.UNTAGGED
}

/** The number on each chip; every filter has an entry. */
fun filterCounts(rows: List<TransactionEntity>): Map<TxFilter, Int> =
    TxFilter.entries.associateWith { f -> rows.count { f.matches(it.category) } }

fun applyFilter(rows: List<TransactionEntity>, filter: TxFilter): List<TransactionEntity> =
    rows.filter { filter.matches(it.category) }
