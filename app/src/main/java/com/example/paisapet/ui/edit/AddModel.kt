package com.example.paisapet.ui.edit

import com.example.paisapet.list.dayLabel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// Pure: texts for the Add sheet (S8). No Compose here.

private val TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

/** The date and time chip: "Today, 3:30 PM", "Yesterday, 9:05 AM", "21 Oct, 4:10 PM". */
fun addDateTimeText(at: LocalDateTime, today: LocalDate): String = "${dayLabel(at.toLocalDate(), today)}, ${at.format(TIME)}"
