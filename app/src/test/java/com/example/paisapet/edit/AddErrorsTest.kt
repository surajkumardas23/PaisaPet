package com.example.paisapet.edit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AddErrorsTest {

    @Test fun errorTextForEveryError() {
        assertEquals(
            listOf(
                "Enter an amount",
                "Enter a valid amount, like 240 or 240.50",
                "Amount must be more than ₹0",
                "Amount can be at most ₹10,00,000",
                "Pick a date in the current month",
                "The time can't be in the future",
            ),
            listOf(
                AddError.AMOUNT_EMPTY, AddError.AMOUNT_INVALID, AddError.AMOUNT_ZERO,
                AddError.AMOUNT_TOO_LARGE, AddError.DATE_BEFORE_MONTH, AddError.DATE_IN_FUTURE,
            ).map(::errorText),
        )
    }

    @Test fun everyErrorHasAText() {
        for (e in AddError.entries) assertTrue(e.name, errorText(e).isNotBlank())
    }

    // Which errors show

    @Test fun nothingShowsBeforeTouchOrSubmit() {
        val e = addFieldErrors(setOf(AddError.AMOUNT_EMPTY, AddError.DATE_IN_FUTURE), false, false, false)
        assertEquals(AddFieldErrors(null, null), e)
    }

    @Test fun submitShowsBoth() {
        val e = addFieldErrors(setOf(AddError.AMOUNT_EMPTY, AddError.DATE_IN_FUTURE), false, false, true)
        assertEquals(AddFieldErrors("Enter an amount", "The time can't be in the future"), e)
    }

    @Test fun touchedFieldShowsOnlyItsOwnError() {
        val errors = setOf(AddError.AMOUNT_ZERO, AddError.DATE_BEFORE_MONTH)
        assertEquals(AddFieldErrors("Amount must be more than ₹0", null), addFieldErrors(errors, true, false, false))
        assertEquals(AddFieldErrors(null, "Pick a date in the current month"), addFieldErrors(errors, false, true, false))
    }

    @Test fun noErrorsShowNothingEvenAfterSubmit() {
        assertEquals(AddFieldErrors(null, null), addFieldErrors(emptySet(), true, true, true))
    }

    // Date picker rule in picker millis

    @Test fun pickerMillisFollowTheDateRule() {
        val today = LocalDate.of(2026, 10, 25)
        assertTrue(isSelectablePickerMillis(dateToPickerUtcMillis(LocalDate.of(2026, 10, 1)), today))
        assertTrue(isSelectablePickerMillis(dateToPickerUtcMillis(today), today))
        assertFalse(isSelectablePickerMillis(dateToPickerUtcMillis(LocalDate.of(2026, 9, 30)), today))
        assertFalse(isSelectablePickerMillis(dateToPickerUtcMillis(LocalDate.of(2026, 10, 26)), today))
    }
}
