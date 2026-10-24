package com.example.paisapet.data

import com.example.paisapet.model.Category
import com.example.paisapet.model.Source
import com.example.paisapet.sms.Deduplicator
import com.example.paisapet.sms.FixtureBlock
import com.example.paisapet.sms.ParseResult
import com.example.paisapet.sms.ParsedDebit
import com.example.paisapet.sms.SmsFixtures
import com.example.paisapet.sms.SmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class TransactionMappersTest {

    private val ist: ZoneId = ZoneId.of("Asia/Kolkata")
    private val now = 1_782_000_000_000L

    private val blocks: List<FixtureBlock> by lazy { SmsFixtures.load() }

    /** Parsed the same way as SmsFixtureTest: received at noon IST on the block's date (1 Jan 2026 if none). */
    private fun parsed(id: String): ParsedDebit {
        val block = blocks.single { it.id == id }
        val receivedAt = block.receivedAt
            ?: (block.date ?: LocalDate.of(2026, 1, 1)).atTime(LocalTime.NOON).atZone(ist).toInstant()
        val result = SmsParser.parse(block.sender, block.body, receivedAt, ist)
        return (result as ParseResult.Debit).debit
    }

    private fun roundTripped(id: String): ParsedDebit = parsed(id).toEntity(now).toParsedDebit()

    @Test fun toEntity_D04() {
        val debit = parsed("D04")
        val entity = debit.toEntity(now)

        assertEquals(Category.UNTAGGED, entity.category)
        assertEquals(Source.SMS, entity.source)
        assertNull(entity.tagSource)
        assertNull(entity.taggedAt)
        assertEquals(now, entity.createdAt)
        assertEquals(now, entity.updatedAt)
        assertEquals(62000L, entity.amountPaise)
        assertEquals("BIRRA BEVERAGES", entity.payee)
        assertEquals("651196203847", entity.upiRef)
        assertEquals(debit.bank, entity.bank)
        assertEquals(debit.smsHash, entity.smsHash)
        assertEquals("2026-06", entity.monthKey)
        assertEquals(debit.occurredAt.toEpochMilli(), entity.occurredAt)
        assertEquals(0L, entity.id)
    }

    @Test fun roundTripKeepsDedupFields() {
        val debit = parsed("D04")
        val back = debit.toEntity(now).toParsedDebit()

        assertEquals(debit.amountPaise, back.amountPaise)
        assertEquals(debit.payee, back.payee)
        assertEquals(debit.upiRef, back.upiRef)
        assertEquals(debit.occurredAt, back.occurredAt)
        assertEquals(debit.monthKey, back.monthKey)
        assertEquals(debit.smsHash, back.smsHash)
        assertTrue(back.dateFromMessage)
    }

    @Test(expected = IllegalStateException::class)
    fun toParsedDebit_nullHashThrows() {
        parsed("D04").toEntity(now).copy(smsHash = null, source = Source.MANUAL).toParsedDebit()
    }

    @Test fun candidateWindow_boundaries() {
        val t = Instant.parse("2026-06-26T06:30:00Z").toEpochMilli()
        val window = candidateWindow(t)

        assertEquals(t - 120_000, window.fromMs)
        assertEquals(t + 120_000, window.toMs)
        assertTrue(t + 119_000 in window)
        assertTrue(t - 119_000 in window)
        assertTrue(t + 120_000 in window)
        assertTrue(t - 120_000 in window)
        assertFalse(t + 121_000 in window)
        assertFalse(t - 121_000 in window)
    }

    @Test fun roundTripped_D06_S02_differentRefs_notDuplicate() =
        assertFalse(Deduplicator.isDuplicate(roundTripped("S02"), listOf(roundTripped("D06"))))

    @Test fun roundTripped_S01_D04_sameSms_isDuplicate() =
        assertTrue(Deduplicator.isDuplicate(roundTripped("S01"), listOf(roundTripped("D04"))))
}
