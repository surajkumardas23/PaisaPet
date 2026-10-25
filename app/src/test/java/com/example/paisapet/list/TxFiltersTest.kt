package com.example.paisapet.list

import com.example.paisapet.data.db.TransactionEntity
import com.example.paisapet.model.Category.ESSENTIAL
import com.example.paisapet.model.Category.MISC
import com.example.paisapet.model.Category.UNTAGGED
import com.example.paisapet.tx
import org.junit.Assert.assertEquals
import org.junit.Test

class TxFiltersTest {

    private val misc1 = tx(id = 1, category = MISC)
    private val misc2 = tx(id = 2, category = MISC)
    private val essential = tx(id = 3, category = ESSENTIAL)
    private val untagged = tx(id = 4, category = UNTAGGED)
    private val rows = listOf(misc1, essential, untagged, misc2)

    @Test fun countsForMixedCategories() {
        assertEquals(
            mapOf(TxFilter.ALL to 4, TxFilter.MISC to 2, TxFilter.ESSENTIAL to 1, TxFilter.NOT_TAGGED to 1),
            filterCounts(rows),
        )
    }

    @Test fun allKeepsEverythingInOrder() {
        assertEquals(rows, applyFilter(rows, TxFilter.ALL))
    }

    @Test fun miscExcludesNotTagged() {
        assertEquals(listOf(misc1, misc2), applyFilter(rows, TxFilter.MISC))
    }

    @Test fun essentialOnly() {
        assertEquals(listOf(essential), applyFilter(rows, TxFilter.ESSENTIAL))
    }

    @Test fun notTaggedOnly() {
        assertEquals(listOf(untagged), applyFilter(rows, TxFilter.NOT_TAGGED))
    }

    @Test fun emptyList() {
        val none = emptyList<TransactionEntity>()
        assertEquals(TxFilter.entries.associateWith { 0 }, filterCounts(none))
        for (f in TxFilter.entries) assertEquals(none, applyFilter(none, f))
    }
}
