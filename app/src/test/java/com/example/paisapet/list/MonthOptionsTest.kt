package com.example.paisapet.list

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthOptionsTest {

    @Test fun currentMonthAddedWhenItHasNoData() {
        assertEquals(listOf("2026-11", "2026-10", "2026-09"), monthOptions(listOf("2026-10", "2026-09"), "2026-11"))
    }

    @Test fun noDuplicatesAndNewestFirst() {
        assertEquals(
            listOf("2026-10", "2026-09", "2026-08", "2025-12"),
            monthOptions(listOf("2026-08", "2026-10", "2025-12", "2026-09", "2026-08"), "2026-10"),
        )
    }

    @Test fun currentMonthStaysFirstEvenWithFutureData() {
        // Only possible if the phone's clock was changed.
        assertEquals(listOf("2026-10", "2026-12"), monthOptions(listOf("2026-12"), "2026-10"))
    }

    @Test fun noDataGivesOnlyCurrent() {
        assertEquals(listOf("2026-10"), monthOptions(emptyList(), "2026-10"))
    }

    @Test fun labels() {
        assertEquals("October 2026", monthLabel("2026-10"))
        assertEquals("January 2027", monthLabel("2027-01"))
    }

    @Test fun effectiveMonthKeepsAValidSelection() {
        assertEquals("2026-09", effectiveMonth("2026-09", listOf("2026-10", "2026-09"), "2026-10"))
    }

    @Test fun effectiveMonthFallsBackWhenMissingOrNull() {
        assertEquals("2026-10", effectiveMonth("2026-07", listOf("2026-10", "2026-09"), "2026-10"))
        assertEquals("2026-10", effectiveMonth(null, listOf("2026-10", "2026-09"), "2026-10"))
    }

    @Test fun readOnlyForPastAndFutureMonths() {
        assertTrue(isReadOnly("2026-09", "2026-10"))
        assertFalse(isReadOnly("2026-10", "2026-10"))
        assertTrue(isReadOnly("2026-11", "2026-10"))
    }
}
