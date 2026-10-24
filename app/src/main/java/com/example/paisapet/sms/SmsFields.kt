package com.example.paisapet.sms

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.util.Locale

/** Small pure helpers used by [SmsParser]. No Android classes. */
object SmsFields {

    private val AMOUNT = Regex("""^(\d*)(?:\.(\d{1,2}))?$""")
    private val SENDER = Regex("""^[A-Z]{2,3}-([A-Z0-9]+)(?:-[A-Z])?$""", RegexOption.IGNORE_CASE)
    private val BARE_SENDER = Regex("""^[A-Z0-9]+$""", RegexOption.IGNORE_CASE)

    // Day first, as Indian banks print it. "uu" is a two-digit year meaning 2000 + yy.
    private val DATE_FORMATS: List<DateTimeFormatter> =
        listOf("d/M/uu", "dMMMuu", "d-MMM-uu").map { pattern ->
            DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern(pattern)
                .toFormatter(Locale.ENGLISH)
                .withResolverStyle(ResolverStyle.STRICT)
        }

    /**
     * "1,250.00" -> 125000, "49" -> 4900, ".5" -> 50. Returns null if the text is not an
     * amount or does not fit in a Long. Integer maths only.
     */
    fun amountToPaise(text: String): Long? {
        val match = AMOUNT.matchEntire(text.trim().replace(",", "")) ?: return null
        val rupeesText = match.groupValues[1]
        val paiseText = match.groupValues[2]
        if (rupeesText.isEmpty() && paiseText.isEmpty()) return null
        val rupees = if (rupeesText.isEmpty()) 0L else rupeesText.toLongOrNull() ?: return null
        val paise = if (paiseText.isEmpty()) 0L else paiseText.padEnd(2, '0').toLong()
        return try {
            Math.addExact(Math.multiplyExact(rupees, 100L), paise)
        } catch (e: ArithmeticException) {
            null
        }
    }

    /** Reads 03/10/26, 28Sep26 (any case) or 28-Jun-26. Returns null for anything else or an impossible date. */
    fun readMessageDate(text: String): LocalDate? {
        val trimmed = text.trim()
        for (format in DATE_FORMATS) {
            try {
                return LocalDate.parse(trimmed, format)
            } catch (e: DateTimeParseException) {
                // try the next shape
            }
        }
        return null
    }

    /** AD-HDFCBK-S -> HDFCBK, VM-HDFCBK -> HDFCBK, HDFCBK -> HDFCBK, anything else -> null. */
    fun bankFromSender(sender: String): String? {
        val trimmed = sender.trim()
        SENDER.matchEntire(trimmed)?.let { return it.groupValues[1].uppercase(Locale.ROOT) }
        if (BARE_SENDER.matches(trimmed) && trimmed.any { it.isLetter() }) return trimmed.uppercase(Locale.ROOT)
        return null
    }

    /** True for personal numbers such as +919876500123 (10 or more digits after removing + and spaces). */
    fun isPhoneNumberSender(sender: String): Boolean {
        val digits = sender.trim().removePrefix("+").replace(" ", "")
        return digits.length >= 10 && digits.all { it in '0'..'9' }
    }
}
