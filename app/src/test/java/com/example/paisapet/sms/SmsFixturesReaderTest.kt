package com.example.paisapet.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

class SmsFixturesReaderTest {

    private val blocks = SmsFixtures.load()
    private fun block(id: String) = blocks.single { it.id == id }

    @Test
    fun counts() {
        assertEquals(33, blocks.size)
        assertEquals(13, blocks.count { it.expect == Expect.DEBIT })
        assertEquals(19, blocks.count { it.expect == Expect.IGNORE })
        assertEquals(1, blocks.count { it.expect == Expect.DUPLICATE })
    }

    @Test
    fun idsAreUnique() = assertEquals(blocks.size, blocks.map { it.id }.toSet().size)

    @Test
    fun d05_merchantKeepsDoubleSpace() = assertEquals("MOHAN  SINGH", block("D05").merchant)

    @Test
    fun d01_bodyKeepsLineBreaks() {
        val lines = block("D01").body.split("\n")
        assertEquals(7, lines.size)
        assertEquals("UPI Mandate:", lines.first())
    }

    @Test
    fun x07_bodyKeepsBlankLine() = assertTrue(block("X07").body.split("\n").contains(""))

    @Test
    fun s09_receivedAt() =
        assertEquals(Instant.parse("2026-06-30T18:34:00Z"), block("S09").receivedAt)

    @Test
    fun s01_duplicateOf() = assertEquals("D04", block("S01").duplicateOf)

    @Test
    fun d02_typedFields() {
        val d02 = block("D02")
        assertEquals(4900L, d02.amountPaise)
        assertEquals(LocalDate.of(2026, 9, 28), d02.date)
        assertEquals(YearMonth.of(2026, 9), d02.month)
    }

    @Test
    fun everyDebitHasExpectedFields() {
        for (b in blocks.filter { it.expect == Expect.DEBIT }) {
            assertNotNull("${b.id} amountPaise", b.amountPaise)
            assertNotNull("${b.id} merchant", b.merchant)
            assertNotNull("${b.id} date", b.date)
            assertNotNull("${b.id} month", b.month)
            assertNotNull("${b.id} ref", b.ref)
        }
    }

    @Test
    fun everyIgnoreHasReason() {
        for (b in blocks.filter { it.expect == Expect.IGNORE }) {
            assertNotNull("${b.id} reason", b.reason)
        }
    }

    @Test
    fun unknownKeyThrowsWithBlockIdAndKey() {
        val text = """
            ### T01
            sender: AD-HDFCBK-S
            expect: IGNORE
            reasn: typo
            body:
            hello
            ---
        """.trimIndent()
        val e = assertThrows(IllegalArgumentException::class.java) { SmsFixtures.parse(text) }
        assertTrue(e.message!!, e.message!!.contains("T01") && e.message!!.contains("reasn"))
    }

    @Test
    fun smallStringKeepsBodyExactly() {
        val text = "### T02\nsender: X\nexpect: IGNORE\nbody:\n  a  b\n\n# not a comment\n---\n"
        assertEquals("  a  b\n\n# not a comment", SmsFixtures.parse(text).single().body)
    }

    @Test
    fun crlfLineEndingsAreHandled() {
        val text = "### T03\r\nsender: X\r\nexpect: IGNORE\r\nbody:\r\nline1\r\nline2\r\n---\r\n"
        assertEquals("line1\nline2", SmsFixtures.parse(text).single().body)
    }
}
