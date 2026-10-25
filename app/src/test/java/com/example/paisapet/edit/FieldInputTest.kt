package com.example.paisapet.edit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The payee field filter and the amount messages (shared by Edit and Add). */
class FieldInputTest {

    @Test fun payeeUpTo60IsKeptAsTyped() {
        assertEquals("", acceptPayeeText(""))
        assertEquals("  Chai ", acceptPayeeText("  Chai ")) // trimmed only on save
        val sixty = "a".repeat(60)
        assertEquals(sixty, acceptPayeeText(sixty))
    }

    @Test fun payeeOver60IsCut() {
        assertEquals("a".repeat(60), acceptPayeeText("a".repeat(61)))
        assertEquals("a".repeat(60), acceptPayeeText("a".repeat(200)))
    }

    @Test fun payeeCutNeverSplitsAnEmoji() {
        assertEquals("a".repeat(59), acceptPayeeText("a".repeat(59) + "😀b"))
    }

    @Test fun amountMessages() {
        assertEquals("Enter an amount", amountErrorMessage(AddError.AMOUNT_EMPTY))
        assertEquals("Enter a valid amount, like 240 or 240.50", amountErrorMessage(AddError.AMOUNT_INVALID))
        assertEquals("Amount must be more than ₹0", amountErrorMessage(AddError.AMOUNT_ZERO))
        assertEquals("Amount can be at most ₹10,00,000", amountErrorMessage(AddError.AMOUNT_TOO_LARGE))
        assertNull(amountErrorMessage(AddError.DATE_IN_FUTURE))
        assertNull(amountErrorMessage(AddError.DATE_BEFORE_MONTH))
    }
}
