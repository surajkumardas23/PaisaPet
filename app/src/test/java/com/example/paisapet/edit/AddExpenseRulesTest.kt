package com.example.paisapet.edit

import com.example.paisapet.IST
import com.example.paisapet.model.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class AddExpenseRulesTest {

    private val nowLocal = LocalDateTime.of(2026, 10, 25, 15, 30)
    private val now = nowLocal.atZone(IST).toInstant()

    private fun input(
        amount: String = "20",
        payee: String = "",
        category: Category = Category.MISC,
        at: LocalDateTime = nowLocal,
    ) = AddInput(amount, payee, category, at)

    private fun errors(check: AddCheck) = (check as AddCheck.Invalid).errors
    private fun expense(check: AddCheck) = (check as AddCheck.Valid).expense

    // Dates

    @Test fun nowIsAccepted() {
        val e = expense(checkAdd(input(), now, IST))
        assertEquals(now, e.occurredAt)
    }

    @Test fun oneMinuteInTheFutureIsRejected() {
        assertEquals(setOf(AddError.DATE_IN_FUTURE), errors(checkAdd(input(at = nowLocal.plusMinutes(1)), now, IST)))
    }

    @Test fun firstOfMonthAtMidnightIsAccepted() {
        expense(checkAdd(input(at = LocalDateTime.of(2026, 10, 1, 0, 0)), now, IST))
    }

    @Test fun lastMinuteOfPreviousMonthIsRejected() {
        assertEquals(
            setOf(AddError.DATE_BEFORE_MONTH),
            errors(checkAdd(input(at = LocalDateTime.of(2026, 9, 30, 23, 59)), now, IST)),
        )
    }

    // Amount and payee

    @Test fun blankPayeeBecomesManualEntry() {
        assertEquals(MANUAL_PAYEE, expense(checkAdd(input(payee = ""), now, IST)).payee)
        assertEquals(MANUAL_PAYEE, expense(checkAdd(input(payee = "   "), now, IST)).payee)
    }

    @Test fun payeeIsTrimmed() {
        assertEquals("Chai stall", expense(checkAdd(input(payee = "  Chai stall "), now, IST)).payee)
    }

    @Test fun payeeOf60CharsIsKeptAnd61IsCut() {
        val sixty = "a".repeat(60)
        assertEquals(sixty, expense(checkAdd(input(payee = sixty), now, IST)).payee)
        assertEquals(sixty, expense(checkAdd(input(payee = sixty + "b"), now, IST)).payee)
    }

    @Test fun payeeCutNeverSplitsAnEmoji() {
        val text = "a".repeat(59) + "😀" // the emoji is two chars: 59 and 60
        assertEquals("a".repeat(59), normalisePayee(text))
    }

    @Test fun payeeLineBreaksAndTabsBecomeSpaces() {
        assertEquals("Chai  stall x", normalisePayee("Chai\n\tstall\rx"))
    }

    @Test fun validExpenseKeepsAmountAndCategory() {
        val e = expense(checkAdd(input(amount = "240.50", payee = "Auto", category = Category.ESSENTIAL), now, IST))
        assertEquals(ManualExpense(24_050, "Auto", Category.ESSENTIAL, now), e)
    }

    @Test fun errorsAreCollectedTogether() {
        assertEquals(
            setOf(AddError.AMOUNT_ZERO, AddError.DATE_IN_FUTURE),
            errors(checkAdd(input(amount = "0", at = nowLocal.plusDays(1)), now, IST)),
        )
        assertEquals(setOf(AddError.AMOUNT_EMPTY), errors(checkAdd(input(amount = ""), now, IST)))
        assertEquals(setOf(AddError.AMOUNT_TOO_LARGE), errors(checkAdd(input(amount = "1000001"), now, IST)))
    }

    // Date picker

    @Test fun selectableDatesAreThisMonthUpToToday() {
        val today = LocalDate.of(2026, 10, 25)
        assertTrue(isSelectableDate(LocalDate.of(2026, 10, 1), today))
        assertTrue(isSelectableDate(today, today))
        assertFalse(isSelectableDate(LocalDate.of(2026, 9, 30), today))
        assertFalse(isSelectableDate(LocalDate.of(2026, 10, 26), today))
    }

    @Test fun onTheFirstOnlyTheFirstIsSelectable() {
        val first = LocalDate.of(2026, 11, 1)
        assertTrue(isSelectableDate(first, first))
        assertFalse(isSelectableDate(LocalDate.of(2026, 10, 31), first))
    }

    @Test fun pickerMillisRoundTrip() {
        for (date in listOf(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), LocalDate.of(2024, 2, 29))) {
            assertEquals(date, pickerUtcMillisToDate(dateToPickerUtcMillis(date)))
        }
    }

    @Test fun pickerMillisDoNotShiftInIndiaOrANegativeOffsetZone() {
        val millis = dateToPickerUtcMillis(LocalDate.of(2026, 10, 1))
        // Read as a local date, UTC midnight is still 1 Oct in India but 30 Sep in New York.
        assertEquals(LocalDate.of(2026, 10, 1), java.time.Instant.ofEpochMilli(millis).atZone(IST).toLocalDate())
        assertEquals(LocalDate.of(2026, 9, 30),
            java.time.Instant.ofEpochMilli(millis).atZone(ZoneId.of("America/New_York")).toLocalDate())
        // The helper ignores the phone's zone, so it is right in both.
        assertEquals(LocalDate.of(2026, 10, 1), pickerUtcMillisToDate(millis))
    }
}
