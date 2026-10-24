package com.example.paisapet.format

import java.util.Locale

/**
 * Paise as rupees with Indian grouping: 24000 → "₹240", 77663 → "₹776.63", 10000000 → "₹1,00,000".
 * Paise are shown only when non-zero. Negative amounts get a leading "-".
 */
fun formatRupees(paise: Long): String {
    val negative = paise < 0
    // Divide before negating so Long.MIN_VALUE cannot overflow.
    val rupees = (paise / 100).let { if (negative) -it else it }
    val rest = (paise % 100).let { if (negative) -it else it }
    val sign = if (negative) "-" else ""
    val fraction = if (rest == 0L) "" else String.format(Locale.ROOT, ".%02d", rest)
    return "$sign₹${groupIndian(rupees.toString())}$fraction"
}

/** "1234567" → "12,34,567": last three digits, then groups of two. */
private fun groupIndian(digits: String): String {
    if (digits.length <= 3) return digits
    val head = digits.dropLast(3)
    val groups = head.reversed().chunked(2).joinToString(",").reversed()
    return "$groups,${digits.takeLast(3)}"
}
