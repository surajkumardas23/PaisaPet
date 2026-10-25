package com.example.paisapet.list

import com.example.paisapet.data.db.TransactionEntity
import com.example.paisapet.format.formatRupees
import com.example.paisapet.model.Category
import com.example.paisapet.model.Source
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

/** "1:15 PM" in local time. */
fun timeText(occurredAt: Long, zone: ZoneId): String = Instant.ofEpochMilli(occurredAt).atZone(zone).format(TIME)

/** S6 row: the time, plus " · Manual" for entries added by hand. */
fun transactionsSubtitle(t: TransactionEntity, zone: ZoneId): String = withManual(timeText(t.occurredAt, zone), t)

/** Home Recent row: the time for today, otherwise the day label; plus " · Manual". */
fun recentSubtitle(t: TransactionEntity, zone: ZoneId, today: LocalDate): String {
    val date = localDateOf(t.occurredAt, zone)
    val text = if (date == today) timeText(t.occurredAt, zone) else dayLabel(date, today)
    return withManual(text, t)
}

private fun withManual(text: String, t: TransactionEntity): String =
    if (t.source == Source.MANUAL) "$text · Manual" else text

/** A debit as shown in lists: "–₹240" (en dash). */
fun debitText(paise: Long): String = "–" + formatRupees(paise)

/** The Home untagged card: "1 payment not tagged", "2 payments not tagged". */
fun untaggedText(n: Int): String = if (n == 1) "1 payment not tagged" else "$n payments not tagged"

enum class ChipKind { MISC, ESSENTIAL, NOT_TAGGED }

fun chipOf(category: Category): ChipKind = when (category) {
    Category.MISC -> ChipKind.MISC
    Category.ESSENTIAL -> ChipKind.ESSENTIAL
    Category.UNTAGGED -> ChipKind.NOT_TAGGED
}
