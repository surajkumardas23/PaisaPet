package com.example.paisapet.export

import com.example.paisapet.data.db.TransactionEntity
import com.example.paisapet.model.Category
import com.example.paisapet.model.Source
import com.example.paisapet.model.TagSource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** FR-18 columns, from "Data model". */
const val CSV_HEADER = "date,time,amount_inr,payee,category,source,tag_source,upi_ref,bank,captured_at,tagged_at"

private const val CRLF = "\r\n"
private val DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT)
private val TIME = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ROOT)
private val DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT)

/** All payments, oldest first (ties by id), local times, CRLF line ends. The UTF-8 BOM is added by the file writer. */
fun buildCsv(rows: List<TransactionEntity>, zone: ZoneId): String = buildString {
    append(CSV_HEADER).append(CRLF)
    for (t in rows.sortedWith(compareBy({ it.occurredAt }, { it.id }))) {
        val occurred = Instant.ofEpochMilli(t.occurredAt).atZone(zone)
        val cells = listOf(
            occurred.format(DATE),
            occurred.format(TIME),
            rupeesText(t.amountPaise),
            t.payee,
            categoryText(t.category),
            sourceText(t.source),
            tagSourceText(t.tagSource),
            t.upiRef.orEmpty(),
            t.bank.orEmpty(),
            Instant.ofEpochMilli(t.createdAt).atZone(zone).format(DATE_TIME),
            t.taggedAt?.let { Instant.ofEpochMilli(it).atZone(zone).format(DATE_TIME) }.orEmpty(),
        )
        cells.joinTo(this, ",") { csvCell(it) }
        append(CRLF)
    }
}

/** "paisa-pet-2026-10-25.csv". */
fun csvFileName(today: LocalDate): String = "paisa-pet-${today.format(DATE)}.csv"

/** Paise as rupees with 2 decimals, no grouping: 24000 → "240.00", 5 → "0.05". */
internal fun rupeesText(paise: Long): String {
    val negative = paise < 0
    val rupees = (paise / 100).let { if (negative) -it else it }
    val rest = (paise % 100).let { if (negative) -it else it }
    return (if (negative) "-" else "") + "$rupees." + rest.toString().padStart(2, '0')
}

/** One cell: formula-neutralised, then quoted when it holds a comma, quote or line break. */
internal fun csvCell(text: String): String {
    val safe = neutraliseFormula(text)
    val needsQuotes = safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
    return if (needsQuotes) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
}

/** A cell starting with =, +, -, @, TAB or CR gets a leading ' so spreadsheets show it as text, not a formula. */
internal fun neutraliseFormula(text: String): String =
    if (text.isNotEmpty() && text[0] in FORMULA_STARTS) "'$text" else text

private val FORMULA_STARTS = setOf('=', '+', '-', '@', '\t', '\r')

private fun categoryText(c: Category): String = when (c) {
    Category.MISC -> "Misc"
    Category.ESSENTIAL -> "Essential"
    Category.UNTAGGED -> "Not tagged"
}

private fun sourceText(s: Source): String = when (s) {
    Source.SMS -> "SMS"
    Source.MANUAL -> "Manual"
}

private fun tagSourceText(s: TagSource?): String = when (s) {
    TagSource.NOTIFICATION -> "Notification"
    TagSource.APP -> "App"
    null -> ""
}
