package com.example.paisapet.format

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {

    @Test fun zero() = assertEquals("₹0", formatRupees(0))

    @Test fun paiseOnly() = assertEquals("₹0.05", formatRupees(5))

    @Test fun oneRupee() = assertEquals("₹1", formatRupees(100))

    @Test fun wholeRupeesHidePaise() = assertEquals("₹240", formatRupees(24000))

    @Test fun nonZeroPaiseShown() = assertEquals("₹776.63", formatRupees(77663))

    @Test fun trailingZeroPaiseKeepTwoDigits() = assertEquals("₹776.60", formatRupees(77660))

    @Test fun justUnderAThousand() = assertEquals("₹999.99", formatRupees(99999))

    @Test fun thousand() = assertEquals("₹1,000", formatRupees(100000))

    @Test fun lakh() = assertEquals("₹1,00,000", formatRupees(10000000))

    @Test fun crore() = assertEquals("₹1,23,45,678", formatRupees(1234567800))

    @Test fun lakhsWithPaise() = assertEquals("₹12,34,567.89", formatRupees(123456789))

    @Test fun negative() = assertEquals("-₹240", formatRupees(-24000))

    @Test fun longMaxDoesNotCrash() =
        assertEquals("₹92,23,37,20,36,85,47,758.07", formatRupees(Long.MAX_VALUE))

    @Test fun longMinDoesNotCrash() =
        assertEquals("-₹92,23,37,20,36,85,47,758.08", formatRupees(Long.MIN_VALUE))
}
