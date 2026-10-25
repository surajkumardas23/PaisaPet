package com.example.paisapet.export

import com.example.paisapet.IST
import com.example.paisapet.millisAt
import com.example.paisapet.model.Category
import com.example.paisapet.model.Source
import com.example.paisapet.model.TagSource
import com.example.paisapet.tx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CsvExportTest {

    private fun lines(csv: String) = csv.removeSuffix("\r\n").split("\r\n")

    @Test fun headerOnlyForNoRows() {
        assertEquals(CSV_HEADER + "\r\n", buildCsv(emptyList(), IST))
        assertEquals("date,time,amount_inr,payee,category,source,tag_source,upi_ref,bank,captured_at,tagged_at", CSV_HEADER)
    }

    @Test fun oneFullRowInLocalTime() {
        val t = tx(
            id = 1,
            amountPaise = 24_000,
            payee = "Swiggy",
            occurredAt = millisAt(2026, 10, 25, 13, 15, 7),
            category = Category.MISC,
            source = Source.SMS,
            tagSource = TagSource.NOTIFICATION,
            taggedAt = millisAt(2026, 10, 25, 13, 16, 0),
            upiRef = "123456789012",
            bank = "HDFCBK",
            createdAt = millisAt(2026, 10, 25, 13, 15, 9),
        )
        assertEquals(
            listOf(
                CSV_HEADER,
                "2026-10-25,13:15:07,240.00,Swiggy,Misc,SMS,Notification,123456789012,HDFCBK,2026-10-25 13:15:09,2026-10-25 13:16:00",
            ),
            lines(buildCsv(listOf(t), IST)),
        )
    }

    @Test fun manualUntaggedRowHasEmptyNullFields() {
        val t = tx(amountPaise = 5, payee = "Manual entry", occurredAt = millisAt(2026, 10, 1, 0, 0),
            category = Category.UNTAGGED, source = Source.MANUAL)
        assertEquals("2026-10-01,00:00:00,0.05,Manual entry,Not tagged,Manual,,,,2026-10-01 00:00:00,", lines(buildCsv(listOf(t), IST))[1])
    }

    @Test fun categoryAndSourceSpellings() {
        val rows = listOf(
            tx(id = 1, occurredAt = millisAt(2026, 10, 1), category = Category.ESSENTIAL, tagSource = TagSource.APP),
        )
        assertEquals(listOf("Essential", "SMS", "App"), lines(buildCsv(rows, IST))[1].split(",").subList(4, 7))
    }

    @Test fun rowsOldestFirstAndTiesById() {
        val sameTime = millisAt(2026, 10, 20, 9, 0)
        val rows = listOf(
            tx(id = 5, payee = "Newest", occurredAt = millisAt(2026, 10, 25)),
            tx(id = 3, payee = "TieB", occurredAt = sameTime),
            tx(id = 2, payee = "TieA", occurredAt = sameTime),
            tx(id = 9, payee = "Oldest", occurredAt = millisAt(2026, 9, 2)),
        )
        val payees = lines(buildCsv(rows, IST)).drop(1).map { it.split(",")[3] }
        assertEquals(listOf("Oldest", "TieA", "TieB", "Newest"), payees)
    }

    @Test fun usesCrlfLineEnds() {
        val csv = buildCsv(listOf(tx()), IST)
        assertEquals(2, Regex("\r\n").findAll(csv).count())
        assertEquals(0, Regex("(?<!\r)\n").findAll(csv).count())
    }

    @Test fun fileName() {
        assertEquals("paisa-pet-2026-01-05.csv", csvFileName(LocalDate.of(2026, 1, 5)))
        assertEquals("paisa-pet-2026-10-25.csv", csvFileName(LocalDate.of(2026, 10, 25)))
    }

    // Amounts

    @Test fun rupeesWithTwoDecimals() {
        assertEquals("240.00", rupeesText(24_000))
        assertEquals("0.05", rupeesText(5))
        assertEquals("776.63", rupeesText(77_663))
        assertEquals("100000.00", rupeesText(10_000_000))
        assertEquals("0.00", rupeesText(0))
        assertEquals("-1.50", rupeesText(-150))
    }

    // Quoting

    @Test fun commaIsQuoted() {
        assertEquals("\"Sharma, General Store\"", csvCell("Sharma, General Store"))
    }

    @Test fun quotesAreDoubled() {
        assertEquals("\"He said \"\"hi\"\"\"", csvCell("He said \"hi\""))
    }

    @Test fun lineBreaksAreQuoted() {
        assertEquals("\"a\nb\"", csvCell("a\nb"))
        assertEquals("\"a\r\nb\"", csvCell("a\r\nb"))
    }

    @Test fun plainTextIsLeftAlone() {
        assertEquals("Swiggy", csvCell("Swiggy"))
        assertEquals("", csvCell(""))
    }

    @Test fun payeeWithTabInsideIsKept() {
        assertEquals("Chai\tstall", csvCell("Chai\tstall"))
    }

    @Test fun quotedPayeeStaysOneCell() {
        val row = lines(buildCsv(listOf(tx(payee = "A, \"B\"\nC")), IST))
        // The line break inside the quotes is not a record end; only CRLF ends a record.
        assertEquals(CSV_HEADER, row[0])
        assertEquals("\"A, \"\"B\"\"\nC\",Not tagged,SMS,,,,2026-10-25 12:00:00,", row[1].substringAfter("240.00,"))
    }

    // Formula injection

    @Test fun formulaStartsAreNeutralised() {
        assertEquals("'=SUM(A1)", csvCell("=SUM(A1)"))
        assertEquals("'+919876543210", csvCell("+919876543210"))
        assertEquals("'-5", csvCell("-5"))
        assertEquals("'@x", csvCell("@x"))
        assertEquals("'\tcmd", csvCell("\tcmd"))
    }

    @Test fun leadingCrIsNeutralisedAndQuoted() {
        assertEquals("\"'\rx\"", csvCell("\rx"))
    }

    @Test fun neutralisedCellWithCommaIsAlsoQuoted() {
        assertEquals("\"'=HYPERLINK(\"\"x\"\",\"\"y\"\")\"", csvCell("=HYPERLINK(\"x\",\"y\")"))
    }

    @Test fun formulaCharLaterInTheTextIsHarmless() {
        assertEquals("a=b", neutraliseFormula("a=b"))
        assertEquals("", neutraliseFormula(""))
    }

    @Test fun payeeAndUpiRefAreNeutralisedInTheRow() {
        val t = tx(payee = "=1+1", upiRef = "-12345", bank = "@BANK")
        val cells = lines(buildCsv(listOf(t), IST))[1].split(",")
        assertEquals("'=1+1", cells[3])
        assertEquals("'-12345", cells[7])
        assertEquals("'@BANK", cells[8])
    }

    // Step 10 additions

    @Test fun emptyListIsExactlyTheHeaderLine() {
        val csv = buildCsv(emptyList(), IST)
        assertEquals(listOf(CSV_HEADER), csv.split("\r\n").dropLast(1))
        assertEquals("", csv.split("\r\n").last())
    }

    @Test fun oneRowEndsWithCrlf() {
        val csv = buildCsv(listOf(tx()), IST)
        assertTrue(csv.endsWith("\r\n"))
        assertTrue(!csv.endsWith("\r\n\r\n"))
        assertEquals(2, lines(csv).size)
    }

    @Test fun smsRowWithNullTagFields() {
        val t = tx(category = Category.UNTAGGED, source = Source.SMS, tagSource = null, taggedAt = null,
            upiRef = "123456789012", bank = "HDFCBK", occurredAt = millisAt(2026, 10, 25, 9, 0))
        val cells = lines(buildCsv(listOf(t), IST))[1].split(",")
        assertEquals(11, cells.size)
        assertEquals("Not tagged", cells[4])
        assertEquals("SMS", cells[5])
        assertEquals("", cells[6])
        assertEquals("", cells[10])
    }

    @Test fun payeeWithCommaQuoteNewlineAndEmoji() {
        val payee = "Chai, \"best\"\nstall 😀"
        val row = lines(buildCsv(listOf(tx(payee = payee)), IST))[1]
        assertTrue(row, row.contains("\"Chai, \"\"best\"\"\nstall 😀\""))
    }

    @Test fun everyFormulaStartIsNeutralisedInTheRow() {
        for (start in listOf("=", "+", "-", "@", "\t")) {
            val row = lines(buildCsv(listOf(tx(payee = "${start}cmd")), IST))[1]
            assertTrue("$start → $row", row.contains(",'${start}cmd,"))
        }
    }

    @Test fun upiRefStartingWithMinusIsNeutralised() {
        val cells = lines(buildCsv(listOf(tx(upiRef = "-1")), IST))[1].split(",")
        assertEquals("'-1", cells[7])
    }

    @Test fun amountsInTheRows() {
        val amounts = listOf(5L, 100L, 24_000L, 77_663L).mapIndexed { i, p ->
            tx(id = i + 1L, amountPaise = p, occurredAt = millisAt(2026, 10, 1 + i))
        }
        val cells = lines(buildCsv(amounts, IST)).drop(1).map { it.split(",")[2] }
        assertEquals(listOf("0.05", "1.00", "240.00", "776.63"), cells)
        assertEquals("1.00", rupeesText(100))
    }

    @Test fun timesAreLocalIst() {
        // 20:00 UTC on 24 Oct is 01:30 on 25 Oct in India.
        val utc = java.time.Instant.parse("2026-10-24T20:00:00Z").toEpochMilli()
        val cells = lines(buildCsv(listOf(tx(occurredAt = utc, createdAt = utc + 5_000)), IST))[1].split(",")
        assertEquals("2026-10-25", cells[0])
        assertEquals("01:30:00", cells[1])
        assertEquals("2026-10-25 01:30:05", cells[9])
    }
}
