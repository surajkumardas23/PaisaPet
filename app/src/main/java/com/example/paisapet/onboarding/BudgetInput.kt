package com.example.paisapet.onboarding

import java.time.LocalDate
import java.time.YearMonth

/** Result of checking the S2 budget text. */
sealed interface BudgetCheck {
    data object Empty : BudgetCheck

    /** Below ₹100. */
    data object TooLow : BudgetCheck

    /** Above ₹10,00,000. */
    data object TooHigh : BudgetCheck

    data class Valid(val paise: Long) : BudgetCheck
}

/** The S2 budget field (FR-02): whole rupees only, ₹100 to ₹10,00,000. */
object BudgetInput {
    const val MIN_RUPEES = 100L
    const val MAX_RUPEES = 10_00_000L
    const val MAX_DIGITS = 7
    val CHIPS_RUPEES = listOf(3_000L, 5_000L, 8_000L)

    /** Pre-filled on first run, when no budget has been saved yet. */
    const val FIRST_RUN_RUPEES = 5_000L

    /**
     * What the field should hold after the user types or pastes [new]. Commas, whitespace and "₹" are dropped
     * and leading zeros stripped. Any other character (letters, '.', '-', non-ASCII digits), or more than
     * [MAX_DIGITS] digits left over, keeps [old].
     */
    fun accept(old: String, new: String): String {
        val digits = StringBuilder()
        for (c in new) {
            when {
                // Not Char.isDigit(): that also accepts Devanagari and full-width digits.
                c in '0'..'9' -> digits.append(c)
                c == ',' || c == '₹' || c.isWhitespace() -> Unit
                else -> return old
            }
        }
        val cleaned = digits.trimStart('0')
        return if (cleaned.length > MAX_DIGITS) old else cleaned.toString()
    }

    /** Expects text that [accept] produced; anything with a non-digit counts as Empty. */
    fun check(text: String): BudgetCheck {
        if (text.isEmpty() || text.any { it !in '0'..'9' }) return BudgetCheck.Empty
        if (text.trimStart('0').length > MAX_DIGITS) return BudgetCheck.TooHigh
        val rupees = text.toLong()
        return when {
            rupees < MIN_RUPEES -> BudgetCheck.TooLow
            rupees > MAX_RUPEES -> BudgetCheck.TooHigh
            else -> BudgetCheck.Valid(rupees * 100)
        }
    }

    /** The inline message under the field, or null when there is nothing to say. */
    fun errorMessage(check: BudgetCheck): String? = when (check) {
        BudgetCheck.TooLow -> "Enter at least ₹100"
        BudgetCheck.TooHigh -> "Maximum is ₹10,00,000"
        BudgetCheck.Empty, is BudgetCheck.Valid -> null
    }

    /** The chip whose amount equals [text], or null. */
    fun selectedChip(text: String): Long? {
        val check = check(text) as? BudgetCheck.Valid ?: return null
        return CHIPS_RUPEES.firstOrNull { it * 100 == check.paise }
    }

    /** The saved budget in whole rupees, or [FIRST_RUN_RUPEES] when nothing is saved. */
    fun prefillText(savedPaise: Long?): String =
        if (savedPaise == null || savedPaise <= 0) FIRST_RUN_RUPEES.toString()
        else (savedPaise / 100).toString().trimStart('0')

    /** "Approx. ₹X / day": the budget over the days in [today]'s month, rounded down. */
    fun approxPerDayRupees(budgetRupees: Long, today: LocalDate): Long =
        budgetRupees / YearMonth.from(today).lengthOfMonth()
}
