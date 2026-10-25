package com.example.paisapet.list

import com.example.paisapet.IST
import com.example.paisapet.budget.monthKeyOf
import com.example.paisapet.millisAt
import com.example.paisapet.tx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class DayGroupsTest {

    private val today = LocalDate.of(2026, 10, 25)

    // Labels

    @Test fun todayYesterdayAndOlder() {
        assertEquals("Today", dayLabel(today, today))
        assertEquals("Yesterday", dayLabel(today.minusDays(1), today))
        assertEquals("23 Oct", dayLabel(today.minusDays(2), today))
        assertEquals("21 Oct", dayLabel(LocalDate.of(2026, 10, 21), today))
    }

    @Test fun yesterdayAcrossMonthBoundary() {
        assertEquals("Yesterday", dayLabel(LocalDate.of(2026, 10, 31), LocalDate.of(2026, 11, 1)))
        assertEquals("30 Oct", dayLabel(LocalDate.of(2026, 10, 30), LocalDate.of(2026, 11, 1)))
    }

    @Test fun yesterdayAcrossYearBoundary() {
        assertEquals("Yesterday", dayLabel(LocalDate.of(2026, 12, 31), LocalDate.of(2027, 1, 1)))
    }

    @Test fun otherYearShowsTheYear() {
        assertEquals("30 Dec 2026", dayLabel(LocalDate.of(2026, 12, 30), LocalDate.of(2027, 1, 1)))
    }

    // Grouping

    @Test fun lateNightAndJustAfterMidnightAreDifferentDays() {
        val late = tx(id = 1, occurredAt = millisAt(2026, 10, 24, 23, 59))
        val early = tx(id = 2, occurredAt = millisAt(2026, 10, 25, 0, 1))
        val groups = groupByDay(listOf(early, late), IST, today)
        assertEquals(listOf(today, today.minusDays(1)), groups.map { it.date })
        assertEquals(listOf(early), groups[0].rows)
        assertEquals(listOf(late), groups[1].rows)
    }

    @Test fun oct31At2359AndNov1At0000AreDifferentDaysAndMonths() {
        val oct31 = tx(id = 1, occurredAt = millisAt(2026, 10, 31, 23, 59))
        val nov1 = tx(id = 2, occurredAt = millisAt(2026, 11, 1, 0, 0))
        val groups = groupByDay(listOf(nov1, oct31), IST, LocalDate.of(2026, 11, 1))
        assertEquals(2, groups.size)
        assertEquals(LocalDate.of(2026, 11, 1), groups[0].date)
        assertEquals(LocalDate.of(2026, 10, 31), groups[1].date)
        assertEquals("Today", groups[0].label)
        assertEquals("Yesterday", groups[1].label)
        assertEquals("2026-10", oct31.monthKey)
        assertEquals("2026-11", nov1.monthKey)
        assertNotEquals(
            monthKeyOf(Instant.ofEpochMilli(oct31.occurredAt), IST),
            monthKeyOf(Instant.ofEpochMilli(nov1.occurredAt), IST),
        )
    }

    @Test fun groupsByLocalDateNotUtc() {
        // 20:00 UTC on 24 Oct is 01:30 on 25 Oct in India.
        val ms = millisAt(2026, 10, 24, 20, 0, zone = ZoneOffset.UTC)
        val groups = groupByDay(listOf(tx(occurredAt = ms)), IST, today)
        assertEquals(today, groups.single().date)
        assertEquals("Today", groups.single().label)
    }

    @Test fun dayTotalsAddEveryRowAndKeepRowOrder() {
        val a = tx(id = 1, amountPaise = 24_000, occurredAt = millisAt(2026, 10, 25, 13, 15))
        val b = tx(id = 2, amountPaise = 18_500, occurredAt = millisAt(2026, 10, 25, 11, 30))
        val c = tx(id = 3, amountPaise = 6_000, occurredAt = millisAt(2026, 10, 24, 19, 45))
        val groups = groupByDay(listOf(a, b, c), IST, today)
        assertEquals(42_500L, groups[0].totalPaise)
        assertEquals(listOf(a, b), groups[0].rows)
        assertEquals(6_000L, groups[1].totalPaise)
    }

    @Test fun groupsAreNewestDayFirstWhateverTheInputOrder() {
        val older = tx(id = 1, occurredAt = millisAt(2026, 10, 21))
        val newer = tx(id = 2, occurredAt = millisAt(2026, 10, 25))
        val middle = tx(id = 3, occurredAt = millisAt(2026, 10, 24))
        val groups = groupByDay(listOf(older, newer, middle), IST, today)
        assertEquals(listOf("Today", "Yesterday", "21 Oct"), groups.map { it.label })
    }

    @Test fun emptyListGivesNoGroups() {
        assertEquals(emptyList<DayGroup>(), groupByDay(emptyList(), IST, today))
    }
}
