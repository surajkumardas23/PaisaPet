package com.example.paisapet.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test

class IndianGroupingTest {

    // group

    @Test fun empty() = assertEquals("", IndianGrouping.group(""))

    @Test fun threeDigitsNoComma() = assertEquals("999", IndianGrouping.group("999"))

    @Test fun thousand() = assertEquals("5,000", IndianGrouping.group("5000"))

    @Test fun lakh() = assertEquals("1,00,000", IndianGrouping.group("100000"))

    @Test fun tenLakh() = assertEquals("10,00,000", IndianGrouping.group("1000000"))

    // Cursor mapping, "100000" ↔ "1,00,000"

    private val lakh = "100000"

    @Test fun lakhOriginalToTransformed() =
        assertEquals(listOf(0, 1, 3, 4, 6, 7, 8), (0..6).map { IndianGrouping.originalToTransformed(lakh, it) })

    @Test fun lakhTransformedToOriginal() =
        assertEquals(listOf(0, 1, 1, 2, 3, 3, 4, 5, 6), (0..8).map { IndianGrouping.transformedToOriginal(lakh, it) })

    @Test fun roundTripEveryOriginalOffset() {
        for (digits in listOf("", "9", "999", "5000", "100000", "1000000")) {
            for (o in 0..digits.length) {
                val t = IndianGrouping.originalToTransformed(digits, o)
                assertEquals("$digits at $o", o, IndianGrouping.transformedToOriginal(digits, t))
            }
        }
    }

    @Test fun endMapsToEnd() {
        for (digits in listOf("", "999", "5000", "100000", "1000000")) {
            assertEquals(IndianGrouping.group(digits).length, IndianGrouping.originalToTransformed(digits, digits.length))
            assertEquals(digits.length, IndianGrouping.transformedToOriginal(digits, IndianGrouping.group(digits).length))
        }
    }

    @Test fun startMapsToStart() {
        assertEquals(0, IndianGrouping.originalToTransformed("1000000", 0))
        assertEquals(0, IndianGrouping.transformedToOriginal("1000000", 0))
    }

    @Test fun cursorBesideCommaStaysLeftOfIt() {
        // "5,000": cursor after "5" sits before the comma.
        assertEquals(1, IndianGrouping.originalToTransformed("5000", 1))
        // Cursor just after the comma maps back to after "5".
        assertEquals(1, IndianGrouping.transformedToOriginal("5000", 2))
    }

    @Test fun outOfRangeOffsetsAreClamped() {
        assertEquals(5, IndianGrouping.originalToTransformed("5000", 99))
        assertEquals(4, IndianGrouping.transformedToOriginal("5000", 99))
        assertEquals(0, IndianGrouping.originalToTransformed("5000", -1))
    }
}
