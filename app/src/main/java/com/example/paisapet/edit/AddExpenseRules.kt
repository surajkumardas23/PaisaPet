package com.example.paisapet.edit

import com.example.paisapet.budget.monthKeyOf
import com.example.paisapet.budget.monthStart
import com.example.paisapet.model.Category
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/** The payee saved when "Paid to" is left empty. */
const val MANUAL_PAYEE = "Manual entry"

const val MAX_PAYEE_LENGTH = 60

/** A payment added by hand (FR-14), ready to save. */
data class ManualExpense(val amountPaise: Long, val payee: String, val category: Category, val occurredAt: Instant)

/** What the Add sheet (S8) holds. [occurredAt] is the picked local date and time. */
data class AddInput(val amountText: String, val payeeText: String, val category: Category, val occurredAt: LocalDateTime)

enum class AddError { AMOUNT_EMPTY, AMOUNT_INVALID, AMOUNT_ZERO, AMOUNT_TOO_LARGE, DATE_BEFORE_MONTH, DATE_IN_FUTURE }

sealed interface AddCheck {
    data class Valid(val expense: ManualExpense) : AddCheck
    data class Invalid(val errors: Set<AddError>) : AddCheck
}

/** Amount > 0, date and time within the current month and not in the future. All errors are reported together. */
fun checkAdd(input: AddInput, now: Instant, zone: ZoneId): AddCheck {
    val errors = mutableSetOf<AddError>()
    val amount = checkAmount(input.amountText)
    if (amount is AmountCheck.Error) errors += amount.error

    val occurredAt = input.occurredAt.atZone(zone).toInstant()
    when {
        occurredAt.isAfter(now) -> errors += AddError.DATE_IN_FUTURE
        occurredAt.isBefore(monthStart(monthKeyOf(now, zone), zone)) -> errors += AddError.DATE_BEFORE_MONTH
    }

    if (errors.isNotEmpty() || amount !is AmountCheck.Valid) return AddCheck.Invalid(errors)
    return AddCheck.Valid(ManualExpense(amount.paise, normalisePayee(input.payeeText), input.category, occurredAt))
}

/** Trimmed, line breaks and tabs turned into spaces, at most 60 characters; blank becomes "Manual entry". */
fun normalisePayee(text: String): String {
    val oneLine = text.map { if (it.isISOControl()) ' ' else it }.joinToString("").trim()
    if (oneLine.isEmpty()) return MANUAL_PAYEE
    if (oneLine.length <= MAX_PAYEE_LENGTH) return oneLine
    // Do not cut an emoji (a surrogate pair) in half.
    val end = if (oneLine[MAX_PAYEE_LENGTH - 1].isHighSurrogate()) MAX_PAYEE_LENGTH - 1 else MAX_PAYEE_LENGTH
    return oneLine.substring(0, end).trimEnd()
}

/** What the payee field keeps while typing or pasting: at most 60 characters, never half an emoji. Trimmed on save. */
fun acceptPayeeText(new: String): String {
    if (new.length <= MAX_PAYEE_LENGTH) return new
    val end = if (new[MAX_PAYEE_LENGTH - 1].isHighSurrogate()) MAX_PAYEE_LENGTH - 1 else MAX_PAYEE_LENGTH
    return new.substring(0, end)
}

/** The inline message under the amount field (Add and Edit). */
fun amountErrorMessage(error: AddError): String? = when (error) {
    AddError.AMOUNT_EMPTY -> "Enter an amount"
    AddError.AMOUNT_INVALID -> "Enter a valid amount, like 240 or 240.50"
    AddError.AMOUNT_ZERO -> "Amount must be more than ₹0"
    AddError.AMOUNT_TOO_LARGE -> "Amount can be at most ₹10,00,000"
    AddError.DATE_BEFORE_MONTH, AddError.DATE_IN_FUTURE -> null
}

/** The date picker offers the 1st of this month up to today. */
fun isSelectableDate(date: LocalDate, today: LocalDate): Boolean =
    !date.isBefore(today.withDayOfMonth(1)) && !date.isAfter(today)

/** The Material 3 DatePicker works in UTC midnight millis, whatever the phone's zone. */
fun pickerUtcMillisToDate(utcMillis: Long): LocalDate = Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()

fun dateToPickerUtcMillis(date: LocalDate): Long = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
