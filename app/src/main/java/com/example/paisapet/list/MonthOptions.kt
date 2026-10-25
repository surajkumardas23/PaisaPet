package com.example.paisapet.list

import com.example.paisapet.budget.parseMonthKey
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MONTH_LABEL = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

/** The month dropdown (FR-23): the current month first, then the other months with data, newest first. */
fun monthOptions(keysWithData: List<String>, currentKey: String): List<String> =
    listOf(currentKey) + keysWithData.filter { it != currentKey }.distinct().sortedDescending()

/** "2026-10" → "October 2026". */
fun monthLabel(key: String): String = parseMonthKey(key).format(MONTH_LABEL)

/** The month to show: [selected] while it is still an option, otherwise the current month. Null means current. */
fun effectiveMonth(selected: String?, options: List<String>, currentKey: String): String =
    if (selected != null && selected in options) selected else currentKey

/** Every month except the current one is a read-only view. */
fun isReadOnly(monthKey: String, currentKey: String): Boolean = monthKey != currentKey
