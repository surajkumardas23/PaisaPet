package com.example.paisapet.ui.edit

import com.example.paisapet.IST
import com.example.paisapet.edit.ActionResult
import com.example.paisapet.edit.AddError
import com.example.paisapet.millisAt
import com.example.paisapet.model.Category
import com.example.paisapet.model.Source
import com.example.paisapet.tx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditModelTest {

    private val sms = tx(id = 1, amountPaise = 240_00, payee = "Swiggy", occurredAt = millisAt(2026, 10, 25, 13, 15), category = Category.UNTAGGED)
    private val manual = tx(id = 2, amountPaise = 20_50, payee = "Chai", occurredAt = millisAt(2026, 10, 24, 8, 5), category = Category.MISC, source = Source.MANUAL)

    @Test fun smsDetails() {
        val d = editDetails(sms, "2026-10", IST)
        assertEquals("₹240", d.amountText)
        assertEquals("Swiggy", d.payee)
        assertEquals("Sun, 25 Oct 2026 · 1:15 PM", d.dateTimeText)
        assertEquals("From SMS", d.sourceText)
        assertFalse(d.manual)
        assertFalse(d.readOnly)
        assertFalse(d.canEditAmountAndPayee)
    }

    @Test fun manualDetailsAreEditable() {
        val d = editDetails(manual, "2026-10", IST)
        assertEquals("₹20.50", d.amountText)
        assertEquals("Sat, 24 Oct 2026 · 8:05 AM", d.dateTimeText)
        assertEquals("Added manually", d.sourceText)
        assertTrue(d.manual)
        assertTrue(d.canEditAmountAndPayee)
    }

    @Test fun pastMonthIsReadOnlyEvenForManual() {
        val d = editDetails(manual, "2026-11", IST)
        assertTrue(d.readOnly)
        assertFalse(d.canEditAmountAndPayee)
    }

    @Test fun outcomes() {
        assertEquals(EditOutcome.Close, editOutcome(ActionResult.Done, deleting = false))
        assertEquals(EditOutcome.Close, editOutcome(ActionResult.Done, deleting = true))
        assertEquals(EditOutcome.Deleted, editOutcome(ActionResult.NotFound, deleting = false))
        assertEquals(EditOutcome.Deleted, editOutcome(ActionResult.NotFound, deleting = true))
        assertEquals(EditOutcome.Error("Couldn't save. Try again."), editOutcome(ActionResult.Failed, deleting = false))
        assertEquals(EditOutcome.Error("Couldn't delete. Try again."), editOutcome(ActionResult.Failed, deleting = true))
        assertEquals(
            EditOutcome.Error("This payment is now in a past month and can't be changed."),
            editOutcome(ActionResult.ReadOnly, deleting = false),
        )
        assertEquals(
            EditOutcome.Error("Check the amount and try again."),
            editOutcome(ActionResult.Invalid(setOf(AddError.AMOUNT_ZERO)), deleting = false),
        )
    }
}
