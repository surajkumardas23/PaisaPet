package com.example.paisapet.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** One test per block of docs/sample-sms.txt, named after the block ID. */
@RunWith(Parameterized::class)
class SmsFixtureTest(private val id: String, private val block: FixtureBlock) {

    companion object {
        private val ZONE: ZoneId = ZoneId.of("Asia/Kolkata")
        private val NOON: LocalTime = LocalTime.NOON
        private val NO_DATE_DEFAULT: LocalDate = LocalDate.of(2026, 1, 1)

        private val ALL_BLOCKS: List<FixtureBlock> by lazy { SmsFixtures.load() }

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun blocks(): List<Array<Any>> = ALL_BLOCKS.map { arrayOf(it.id, it) }

        private fun receivedAtFor(block: FixtureBlock): Instant =
            block.receivedAt ?: (block.date ?: NO_DATE_DEFAULT).atTime(NOON).atZone(ZONE).toInstant()

        private fun parseBlock(block: FixtureBlock): ParseResult =
            SmsParser.parse(block.sender, block.body, receivedAtFor(block), ZONE)
    }

    private val receivedAt: Instant = receivedAtFor(block)

    @Test
    fun parse() {
        val result = parseBlock(block)
        when (block.expect) {
            Expect.DEBIT -> {
                checkDebit(result)
                checkNotDuplicateOfOtherDebits(result)
            }
            Expect.IGNORE -> checkIgnore(result)
            Expect.DUPLICATE -> checkDuplicate(result)
        }
    }

    /** No two DEBIT blocks are the same payment (covers D06 vs S02 and D02 vs S05). */
    private fun checkNotDuplicateOfOtherDebits(result: ParseResult) {
        val debit = (result as ParseResult.Debit).debit
        for (other in ALL_BLOCKS.filter { it.expect == Expect.DEBIT && it.id != id }) {
            val otherDebit = parseBlock(other)
            assertTrue("${other.id}: expected DEBIT", otherDebit is ParseResult.Debit)
            assertFalse(
                "$id must not be a duplicate of ${other.id}",
                Deduplicator.isDuplicate(debit, listOf((otherDebit as ParseResult.Debit).debit)),
            )
        }
    }

    private fun checkDuplicate(result: ParseResult) {
        val original = ALL_BLOCKS.single { it.id == block.duplicateOf }
        val originalResult = parseBlock(original)
        assertTrue("$id: expected Debit but got $result", result is ParseResult.Debit)
        assertTrue("${original.id}: expected Debit but got $originalResult", originalResult is ParseResult.Debit)
        val duplicate = (result as ParseResult.Debit).debit
        val first = (originalResult as ParseResult.Debit).debit
        assertEquals("$id smsHash", first.smsHash, duplicate.smsHash)
        assertTrue("$id must be a duplicate of ${original.id}", Deduplicator.isDuplicate(duplicate, listOf(first)))
    }

    private fun checkDebit(result: ParseResult) {
        assertTrue("$id: expected DEBIT but got $result", result is ParseResult.Debit)
        val debit = (result as ParseResult.Debit).debit
        val occurredLocal = debit.occurredAt.atZone(ZONE)

        assertEquals("$id amountPaise", block.amountPaise, debit.amountPaise)
        assertEquals("$id merchant", block.merchant, debit.payee)
        assertEquals("$id date", block.date, occurredLocal.toLocalDate())
        assertEquals("$id month", block.month.toString(), debit.monthKey)
        assertEquals("$id ref", block.ref, debit.upiRef)
        assertEquals("$id bank", block.sender.split("-")[1], debit.bank)
        assertTrue("$id dateFromMessage", debit.dateFromMessage)
        assertEquals("$id time of day", receivedAt.atZone(ZONE).toLocalTime(), occurredLocal.toLocalTime())
    }

    private fun checkIgnore(result: ParseResult) {
        assertTrue("$id: expected IGNORE (${block.reason}) but got $result", result is ParseResult.Ignored)
        assertEquals("$id reason", block.reason, (result as ParseResult.Ignored).reason.code)
    }
}
