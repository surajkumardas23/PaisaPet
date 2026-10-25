package com.example.paisapet.export

import com.example.paisapet.data.db.TransactionEntity
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId

/** Byte order mark U+FEFF, so spreadsheet apps read the file as UTF-8 (non-English payees). Built from its code point. */
val UTF8_BOM: String = Char(0xFEFF).toString()

/**
 * Writes the export into [dir] as "paisa-pet-YYYY-MM-DD.csv" (UTF-8 with a BOM) and returns the file.
 * Every older file in [dir] is deleted first, so at most one export with payment data sits on the phone.
 * Blocking I/O: call it off the main thread.
 */
fun writeExportFile(dir: File, rows: List<TransactionEntity>, zone: ZoneId, today: LocalDate): File {
    if (!dir.isDirectory && !dir.mkdirs()) throw IOException("Can't create ${dir.name}")
    dir.listFiles()?.forEach { it.delete() }
    val file = File(dir, csvFileName(today))
    file.writeText(UTF8_BOM + buildCsv(rows, zone), Charsets.UTF_8)
    return file
}
