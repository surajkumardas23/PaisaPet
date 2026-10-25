package com.example.paisapet.list

import com.example.paisapet.data.db.TransactionEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DAY_MONTH = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
private val DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

/** One day on S6: its label, the total of the rows shown under it, and the rows in their given order. */
data class DayGroup(val date: LocalDate, val label: String, val totalPaise: Long, val rows: List<TransactionEntity>)

/** "Today", "Yesterday", "21 Oct"; "21 Oct 2025" when the year differs from [today]'s. */
fun dayLabel(date: LocalDate, today: LocalDate): String = when {
    date == today -> "Today"
    date == today.minusDays(1) -> "Yesterday"
    date.year == today.year -> date.format(DAY_MONTH)
    else -> date.format(DAY_MONTH_YEAR)
}

fun localDateOf(occurredAt: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(occurredAt).atZone(zone).toLocalDate()

/** Groups by local date of occurredAt, newest day first; rows keep their order inside a day. */
fun groupByDay(rows: List<TransactionEntity>, zone: ZoneId, today: LocalDate): List<DayGroup> =
    rows.groupBy { localDateOf(it.occurredAt, zone) }
        .entries
        .sortedByDescending { it.key }
        .map { (date, dayRows) ->
            DayGroup(date, dayLabel(date, today), dayRows.fold(0L) { sum, t -> Math.addExact(sum, t.amountPaise) }, dayRows)
        }
