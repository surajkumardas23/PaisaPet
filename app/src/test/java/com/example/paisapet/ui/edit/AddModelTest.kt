package com.example.paisapet.ui.edit

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class AddModelTest {

    private val today = LocalDate.of(2026, 10, 25)

    @Test fun dateTimeChip() {
        assertEquals("Today, 3:30 PM", addDateTimeText(LocalDateTime.of(2026, 10, 25, 15, 30), today))
        assertEquals("Yesterday, 9:05 AM", addDateTimeText(LocalDateTime.of(2026, 10, 24, 9, 5), today))
        assertEquals("21 Oct, 12:00 AM", addDateTimeText(LocalDateTime.of(2026, 10, 21, 0, 0), today))
    }
}
