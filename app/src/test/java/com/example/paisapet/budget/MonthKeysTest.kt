package com.example.paisapet.budget

import com.example.paisapet.sms.ParseResult
import com.example.paisapet.sms.SmsFixtures
import com.example.paisapet.sms.SmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class MonthKeysTest {

    private val ist: ZoneId = ZoneId.of("Asia/Kolkata")

    private fun istAt(y: Int, m: Int, d: Int, h: Int, min: Int, s: Int = 0, nanos: Int = 0): Instant =
        LocalDateTime.of(y, m, d, h, min, s, nanos).atZone(ist).toInstant()

    // monthKeyOf

    @Test fun lastMinuteOfOctober() = assertEquals("2026-10", monthKeyOf(istAt(2026, 10, 31, 23, 59), ist))

    @Test fun firstMinuteOfNovember() = assertEquals("2026-11", monthKeyOf(istAt(2026, 11, 1, 0, 0), ist))

    @Test fun lastMillisecondOfOctober() =
        assertEquals("2026-10", monthKeyOf(istAt(2026, 10, 31, 23, 59, 59, 999_000_000), ist))

    @Test fun lastMinuteOfYear() = assertEquals("2026-12", monthKeyOf(istAt(2026, 12, 31, 23, 59), ist))

    @Test fun firstMinuteOfYear() = assertEquals("2027-01", monthKeyOf(istAt(2027, 1, 1, 0, 0), ist))

    @Test fun sameInstantDiffersByZone() {
        val instant = Instant.parse("2026-10-31T20:00:00Z")
        assertEquals("2026-10", monthKeyOf(instant, ZoneOffset.UTC))
        assertEquals("2026-11", monthKeyOf(instant, ist))
    }

    // monthStart / monthEndExclusive

    @Test fun octoberRange() {
        assertEquals(istAt(2026, 10, 1, 0, 0), monthStart("2026-10", ist))
        assertEquals(istAt(2026, 11, 1, 0, 0), monthEndExclusive("2026-10", ist))
    }

    @Test fun decemberEndsOnFirstOfJanuary() {
        assertEquals(istAt(2026, 12, 1, 0, 0), monthStart("2026-12", ist))
        assertEquals(istAt(2027, 1, 1, 0, 0), monthEndExclusive("2026-12", ist))
    }

    // daysLeft

    @Test fun firstDayCountsWholeMonth() = assertEquals(31, daysLeft(LocalDate.of(2026, 10, 1), "2026-10"))

    @Test fun lastDayCountsToday() = assertEquals(1, daysLeft(LocalDate.of(2026, 10, 31), "2026-10"))

    @Test fun lastDayOfFebruary() = assertEquals(1, daysLeft(LocalDate.of(2027, 2, 28), "2027-02"))

    @Test fun leapDay() = assertEquals(1, daysLeft(LocalDate.of(2028, 2, 29), "2028-02"))

    @Test fun leapFebruaryFromFirst() = assertEquals(29, daysLeft(LocalDate.of(2028, 2, 1), "2028-02"))

    @Test fun pastMonthIsZero() = assertEquals(0, daysLeft(LocalDate.of(2026, 11, 1), "2026-10"))

    @Test fun futureMonthIsWholeMonth() = assertEquals(30, daysLeft(LocalDate.of(2026, 10, 15), "2026-11"))

    // Malformed keys

    @Test(expected = IllegalArgumentException::class)
    fun monthThirteenThrows() {
        monthStart("2026-13", ist)
    }

    @Test(expected = IllegalArgumentException::class)
    fun wordThrows() {
        daysLeft(LocalDate.of(2026, 10, 1), "October")
    }

    @Test(expected = IllegalArgumentException::class)
    fun malformedEndThrows() {
        monthEndExclusive("2026-13", ist)
    }

    // Agreement with SmsParser, same zone and receivedAt rules as SmsFixtureTest

    @Test fun agreesWithParserForD01() = checkAgreesWithParser("D01", "2026-10")

    @Test fun agreesWithParserForS09() = checkAgreesWithParser("S09", "2026-06")

    private fun checkAgreesWithParser(id: String, expectedMonth: String) {
        val block = SmsFixtures.load().single { it.id == id }
        val receivedAt = block.receivedAt
            ?: (block.date ?: LocalDate.of(2026, 1, 1)).atTime(LocalTime.NOON).atZone(ist).toInstant()
        val result = SmsParser.parse(block.sender, block.body, receivedAt, ist)
        assertTrue("$id should parse as a debit", result is ParseResult.Debit)
        val parsed = (result as ParseResult.Debit).debit
        assertEquals(expectedMonth, parsed.monthKey)
        assertEquals(parsed.monthKey, monthKeyOf(parsed.occurredAt, ist))
    }
}
