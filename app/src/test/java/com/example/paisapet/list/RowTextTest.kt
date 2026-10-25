package com.example.paisapet.list

import com.example.paisapet.IST
import com.example.paisapet.millisAt
import com.example.paisapet.model.Category
import com.example.paisapet.model.Source
import com.example.paisapet.tx
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RowTextTest {

    private val today = LocalDate.of(2026, 10, 25)

    @Test fun times() {
        assertEquals("1:15 PM", timeText(millisAt(2026, 10, 25, 13, 15), IST))
        assertEquals("12:00 AM", timeText(millisAt(2026, 10, 25, 0, 0), IST))
        assertEquals("12:00 PM", timeText(millisAt(2026, 10, 25, 12, 0), IST))
        assertEquals("9:05 AM", timeText(millisAt(2026, 10, 25, 9, 5), IST))
    }

    @Test fun transactionsSubtitleAddsManualOnlyForManualRows() {
        val at = millisAt(2026, 10, 25, 13, 15)
        assertEquals("1:15 PM", transactionsSubtitle(tx(occurredAt = at, source = Source.SMS), IST))
        assertEquals("1:15 PM · Manual", transactionsSubtitle(tx(occurredAt = at, source = Source.MANUAL), IST))
    }

    @Test fun recentSubtitleToday() {
        assertEquals("1:15 PM", recentSubtitle(tx(occurredAt = millisAt(2026, 10, 25, 13, 15)), IST, today))
    }

    @Test fun recentSubtitleYesterdayAndOlder() {
        assertEquals("Yesterday", recentSubtitle(tx(occurredAt = millisAt(2026, 10, 24, 19, 45)), IST, today))
        assertEquals("21 Oct", recentSubtitle(tx(occurredAt = millisAt(2026, 10, 21, 16, 10)), IST, today))
    }

    @Test fun recentSubtitleManual() {
        assertEquals("Yesterday · Manual",
            recentSubtitle(tx(occurredAt = millisAt(2026, 10, 24), source = Source.MANUAL), IST, today))
        assertEquals("1:15 PM · Manual",
            recentSubtitle(tx(occurredAt = millisAt(2026, 10, 25, 13, 15), source = Source.MANUAL), IST, today))
    }

    @Test fun debitTextUsesEnDash() {
        assertEquals("–₹240", debitText(24_000))
        assertEquals("–₹776.63", debitText(77_663))
        assertEquals("–₹1,00,000", debitText(10_000_000))
    }

    @Test fun untaggedWording() {
        assertEquals("1 payment not tagged", untaggedText(1))
        assertEquals("2 payments not tagged", untaggedText(2))
        assertEquals("12 payments not tagged", untaggedText(12))
    }

    @Test fun chips() {
        assertEquals(ChipKind.MISC, chipOf(Category.MISC))
        assertEquals(ChipKind.ESSENTIAL, chipOf(Category.ESSENTIAL))
        assertEquals(ChipKind.NOT_TAGGED, chipOf(Category.UNTAGGED))
    }
}
