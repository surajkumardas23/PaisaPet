package com.example.paisapet.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class BudgetInputTest {

    private fun accept(new: String, old: String = "OLD") = BudgetInput.accept(old, new)

    // accept

    @Test fun plainDigitsStay() = assertEquals("5000", accept("5000"))

    @Test fun emptyStays() = assertEquals("", accept(""))

    @Test fun deletingBackToEmpty() = assertEquals("", BudgetInput.accept("5", ""))

    @Test fun singleZeroBecomesEmpty() = assertEquals("", accept("0"))

    @Test fun allZerosBecomeEmpty() = assertEquals("", accept("000"))

    @Test fun leadingZeroStripped() = assertEquals("500", accept("0500"))

    @Test fun pastedThousandsComma() = assertEquals("5000", accept("5,000"))

    @Test fun pastedLakhCommas() = assertEquals("100000", accept("1,00,000"))

    @Test fun spacesDropped() = assertEquals("5000", accept(" 5 000 "))

    @Test fun rupeeSignDropped() = assertEquals("5000", accept("₹5,000"))

    @Test fun pastedRupeeSignSpacesAndCommas() = assertEquals("100000", accept(" ₹ 1,00,000 "))

    @Test fun decimalPointKeepsOld() = assertEquals("OLD", accept("5000.50"))

    @Test fun letterKeepsOld() = assertEquals("OLD", accept("50a"))

    @Test fun minusKeepsOld() = assertEquals("OLD", accept("-500"))

    @Test fun devanagariDigitsKeepOld() = assertEquals("OLD", accept("५०००"))

    @Test fun fullWidthDigitsKeepOld() = assertEquals("OLD", accept("５０００"))

    @Test fun sevenDigitsAccepted() = assertEquals("1000000", accept("1000000"))

    @Test fun eightDigitsKeepOld() = assertEquals("OLD", accept("12345678"))

    @Test fun thousandDigitPasteKeepsOld() = assertEquals("OLD", accept("9".repeat(1_000)))

    @Test fun typingAnEighthDigitIsIgnored() = assertEquals("1000000", BudgetInput.accept("1000000", "10000005"))

    // check

    @Test fun checkEmpty() = assertEquals(BudgetCheck.Empty, BudgetInput.check(""))

    @Test fun check99TooLow() = assertEquals(BudgetCheck.TooLow, BudgetInput.check("99"))

    @Test fun check100Valid() = assertEquals(BudgetCheck.Valid(10_000), BudgetInput.check("100"))

    @Test fun check5000Valid() = assertEquals(BudgetCheck.Valid(500_000), BudgetInput.check("5000"))

    @Test fun checkTenLakhValid() = assertEquals(BudgetCheck.Valid(100_000_000), BudgetInput.check("1000000"))

    @Test fun checkJustOverTenLakhTooHigh() = assertEquals(BudgetCheck.TooHigh, BudgetInput.check("1000001"))

    @Test fun checkSevenNinesTooHigh() = assertEquals(BudgetCheck.TooHigh, BudgetInput.check("9999999"))

    @Test fun checkHugeTextTooHighWithoutOverflow() =
        assertEquals(BudgetCheck.TooHigh, BudgetInput.check("9".repeat(30)))

    @Test fun checkNonDigitIsEmpty() = assertEquals(BudgetCheck.Empty, BudgetInput.check("5,000"))

    // errorMessage

    @Test fun emptyHasNoMessage() = assertNull(BudgetInput.errorMessage(BudgetCheck.Empty))

    @Test fun tooLowMessage() = assertEquals("Enter at least ₹100", BudgetInput.errorMessage(BudgetCheck.TooLow))

    @Test fun tooHighMessage() = assertEquals("Maximum is ₹10,00,000", BudgetInput.errorMessage(BudgetCheck.TooHigh))

    @Test fun validHasNoMessage() = assertNull(BudgetInput.errorMessage(BudgetCheck.Valid(500_000)))

    // selectedChip

    @Test fun chip5000Selected() = assertEquals(5_000L, BudgetInput.selectedChip("5000"))

    @Test fun chip3000Selected() = assertEquals(3_000L, BudgetInput.selectedChip("3000"))

    @Test fun noChipFor5001() = assertNull(BudgetInput.selectedChip("5001"))

    @Test fun noChipForEmpty() = assertNull(BudgetInput.selectedChip(""))

    // prefillText

    @Test fun prefillFirstRun() = assertEquals("5000", BudgetInput.prefillText(null))

    @Test fun prefillSaved3000() = assertEquals("3000", BudgetInput.prefillText(300_000))

    @Test fun prefillSaved10000() = assertEquals("10000", BudgetInput.prefillText(1_000_000))

    // approxPerDayRupees

    @Test fun fiveThousandInNovember2026() =
        assertEquals(166L, BudgetInput.approxPerDayRupees(5_000, LocalDate.of(2026, 11, 10)))

    @Test fun fiveThousandInOctober2026() =
        assertEquals(161L, BudgetInput.approxPerDayRupees(5_000, LocalDate.of(2026, 10, 25)))

    @Test fun fiveThousandInFebruary2026() =
        assertEquals(178L, BudgetInput.approxPerDayRupees(5_000, LocalDate.of(2026, 2, 1)))

    @Test fun fiveThousandInLeapFebruary2028() =
        assertEquals(172L, BudgetInput.approxPerDayRupees(5_000, LocalDate.of(2028, 2, 29)))

    @Test fun minimumIn31DayMonth() =
        assertEquals(3L, BudgetInput.approxPerDayRupees(100, LocalDate.of(2026, 10, 1)))

    @Test fun maximumIn31DayMonth() =
        assertEquals(32_258L, BudgetInput.approxPerDayRupees(10_00_000, LocalDate.of(2026, 10, 31)))
}
