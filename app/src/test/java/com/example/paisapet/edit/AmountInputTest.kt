package com.example.paisapet.edit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmountInputTest {

    // parseAmountPaise

    @Test fun wholeAndDecimalRupees() {
        assertEquals(24_000L, parseAmountPaise("240"))
        assertEquals(24_050L, parseAmountPaise("240.5"))
        assertEquals(24_050L, parseAmountPaise("240.50"))
        assertEquals(1L, parseAmountPaise("0.01"))
        assertEquals(50L, parseAmountPaise(".5"))
        assertEquals(700L, parseAmountPaise("007"))
    }

    @Test fun trailingDotIsWholeRupees() {
        assertEquals(100L, parseAmountPaise("1."))
    }

    @Test fun zeroParsesAsZero() {
        assertEquals(0L, parseAmountPaise("0"))
        assertEquals(0L, parseAmountPaise("0.00"))
    }

    @Test fun malformedIsNull() {
        for (text in listOf("", ".", "1.234", "1,000", "-5", "abc", "1.2.3", " 5", "₹5", "10000000")) {
            assertNull(text, parseAmountPaise(text))
        }
    }

    @Test fun sevenRupeeDigitsIsTheMost() {
        assertEquals(999_999_999L, parseAmountPaise("9999999.99"))
        assertNull(parseAmountPaise("12345678"))
    }

    // acceptAmountText

    @Test fun acceptsDigitsAndOneDot() {
        assertEquals("240", acceptAmountText("24", "240"))
        assertEquals("240.", acceptAmountText("240", "240."))
        assertEquals("240.5", acceptAmountText("240.", "240.5"))
    }

    @Test fun rejectsAThirdDecimalSecondDotLettersAndMinus() {
        assertEquals("240.50", acceptAmountText("240.50", "240.505"))
        assertEquals("240.5", acceptAmountText("240.5", "240.5."))
        assertEquals("24", acceptAmountText("24", "24a"))
        assertEquals("", acceptAmountText("", "-"))
    }

    @Test fun rejectsAnEighthRupeeDigit() {
        assertEquals("1234567", acceptAmountText("1234567", "12345678"))
    }

    @Test fun pasteDropsCommasSpacesAndRupeeSign() {
        assertEquals("1234.50", acceptAmountText("", "₹1,234.50"))
        assertEquals("100000", acceptAmountText("", "1 00 000"))
    }

    // amountTextOf

    @Test fun editableTextOfStoredAmounts() {
        assertEquals("240", amountTextOf(24_000))
        assertEquals("240.50", amountTextOf(24_050))
        assertEquals("0.05", amountTextOf(5))
        assertEquals("1000000", amountTextOf(MAX_AMOUNT_PAISE))
    }

    // checkAmount

    @Test fun checkAmountErrors() {
        assertEquals(AmountCheck.Error(AddError.AMOUNT_EMPTY), checkAmount(""))
        assertEquals(AmountCheck.Error(AddError.AMOUNT_EMPTY), checkAmount("  "))
        assertEquals(AmountCheck.Error(AddError.AMOUNT_INVALID), checkAmount("abc"))
        assertEquals(AmountCheck.Error(AddError.AMOUNT_INVALID), checkAmount("-5"))
        assertEquals(AmountCheck.Error(AddError.AMOUNT_ZERO), checkAmount("0"))
        assertEquals(AmountCheck.Error(AddError.AMOUNT_ZERO), checkAmount("0.00"))
    }

    @Test fun capIsTenLakhInclusive() {
        assertEquals(AmountCheck.Valid(MAX_AMOUNT_PAISE), checkAmount("1000000"))
        assertEquals(AmountCheck.Error(AddError.AMOUNT_TOO_LARGE), checkAmount("1000000.01"))
        assertEquals(AmountCheck.Error(AddError.AMOUNT_TOO_LARGE), checkAmount("9999999"))
    }

    @Test fun smallestValidAmount() {
        assertEquals(AmountCheck.Valid(1), checkAmount("0.01"))
    }
}
