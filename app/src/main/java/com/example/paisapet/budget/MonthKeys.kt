package com.example.paisapet.budget

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeParseException

/** "YYYY-MM" of [instant] in [zone]. Same expression as SmsParser, so stored keys agree. */
fun monthKeyOf(instant: Instant, zone: ZoneId): String =
    YearMonth.from(instant.atZone(zone)).toString()

/** The 1st of the month at 00:00 local time. */
fun monthStart(monthKey: String, zone: ZoneId): Instant =
    parseMonthKey(monthKey).atDay(1).atStartOfDay(zone).toInstant()

/** The next month's 1st at 00:00 local time. */
fun monthEndExclusive(monthKey: String, zone: ZoneId): Instant =
    parseMonthKey(monthKey).plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()

/** Days from [today] to month-end, counting today. 0 for a past month, the whole month for a future one. */
fun daysLeft(today: LocalDate, monthKey: String): Int {
    val month = parseMonthKey(monthKey)
    val current = YearMonth.from(today)
    return when {
        month < current -> 0
        month > current -> month.lengthOfMonth()
        else -> month.lengthOfMonth() - today.dayOfMonth + 1
    }
}

/** @throws IllegalArgumentException if [monthKey] is not a valid "YYYY-MM". */
fun parseMonthKey(monthKey: String): YearMonth =
    try {
        YearMonth.parse(monthKey)
    } catch (e: DateTimeParseException) {
        throw IllegalArgumentException("Bad month key \"$monthKey\"", e)
    }
