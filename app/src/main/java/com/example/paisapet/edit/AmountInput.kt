package com.example.paisapet.edit

/** ₹10,00,000: the largest amount Add and Edit accept, the same cap as the budget. */
const val MAX_AMOUNT_PAISE = 1_000_000_00L

private const val MAX_RUPEE_DIGITS = 7
private val AMOUNT_TEXT = Regex("""\d{0,$MAX_RUPEE_DIGITS}(\.\d{0,2})?""")

/**
 * "240" → 24000, "240.5" → 24050, "1." → 100, "0" → 0. Null for empty or malformed text:
 * "", ".", "1.234", "1,000", "-5", "abc", more than 7 rupee digits. No floating point.
 */
fun parseAmountPaise(text: String): Long? {
    if (!AMOUNT_TEXT.matches(text)) return null
    val rupees = text.substringBefore('.')
    val fraction = text.substringAfter('.', "")
    if (rupees.isEmpty() && fraction.isEmpty()) return null
    val rupeePart = if (rupees.isEmpty()) 0L else rupees.toLong()
    val paisePart = fraction.padEnd(2, '0').toLong()
    return rupeePart * 100 + paisePart
}

/**
 * What the amount field keeps after a keystroke or paste: digits and one dot, at most 7 rupee digits and
 * 2 decimals. Commas, spaces and "₹" are dropped; anything else keeps [old].
 */
fun acceptAmountText(old: String, new: String): String {
    val cleaned = new.filterNot { it == ',' || it == ' ' || it == '₹' }
    return if (AMOUNT_TEXT.matches(cleaned)) cleaned else old
}

/** The editable text for a stored amount: 24000 → "240", 24050 → "240.50". */
fun amountTextOf(paise: Long): String {
    val rupees = paise / 100
    val rest = paise % 100
    return if (rest == 0L) rupees.toString() else "$rupees.${rest.toString().padStart(2, '0')}"
}

sealed interface AmountCheck {
    data class Valid(val paise: Long) : AmountCheck
    data class Error(val error: AddError) : AmountCheck
}

/** Amount > 0 and at most [MAX_AMOUNT_PAISE] (FR-14). */
fun checkAmount(text: String): AmountCheck {
    if (text.isBlank()) return AmountCheck.Error(AddError.AMOUNT_EMPTY)
    val paise = parseAmountPaise(text) ?: return AmountCheck.Error(AddError.AMOUNT_INVALID)
    return when {
        paise == 0L -> AmountCheck.Error(AddError.AMOUNT_ZERO)
        paise > MAX_AMOUNT_PAISE -> AmountCheck.Error(AddError.AMOUNT_TOO_LARGE)
        else -> AmountCheck.Valid(paise)
    }
}
