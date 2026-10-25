package com.example.paisapet.sms

import com.example.paisapet.data.SaveResult
import com.example.paisapet.sms.PipelineResult.Stage
import com.example.paisapet.sms.SmsPipelineFixtureTest.Companion.smsOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class SmsPipelineTest {

    private val zone: ZoneId = ZoneId.of("Asia/Kolkata")
    private val blocks = SmsFixtures.load().associateBy { it.id }
    private fun sms(id: String) = smsOf(blocks.getValue(id))

    // Recorded calls.
    private val saves = mutableListOf<ParsedDebit>()
    private val prompts = mutableListOf<Pair<Long, ParsedDebit>>()
    private var dataChanges = 0
    private val results = mutableListOf<PipelineResult>()

    private fun pipeline(
        parse: (String, String, java.time.Instant) -> ParseResult = { s, b, t -> SmsParser.parse(s, b, t, zone) },
        save: suspend (ParsedDebit) -> SaveResult = { saves += it; SaveResult.Saved(42) },
        prompt: (Long, ParsedDebit) -> Boolean = { id, debit -> prompts += id to debit; true },
        onDataChanged: suspend (Long, Boolean) -> Unit = { _, _ -> dataChanges++ },
        onResult: (PipelineResult) -> Unit = { results += it },
    ) = SmsPipeline(parse, save, prompt, onDataChanged, onResult)

    private fun assertSavedAndPrompted(blockId: String) = runBlocking {
        assertEquals(PipelineResult.Saved(42, prompted = true), pipeline().process(sms(blockId)))
        val debit = saves.single()
        assertEquals(42L, prompts.single().first)
        assertSame(debit, prompts.single().second)
        assertEquals(1, dataChanges)
        assertEquals(1, results.size)
    }

    @Test fun hdfcDebitIsSavedAndPrompted() = assertSavedAndPrompted("D01")

    @Test fun sbiDebitIsSavedAndPrompted() = assertSavedAndPrompted("D02")

    @Test fun iciciDebitIsSavedAndPrompted() = assertSavedAndPrompted("D03")

    @Test fun phoneNumberSenderIsIgnored() = runBlocking {
        assertEquals(PipelineResult.Ignored(IgnoreReason.SENDER_IS_PHONE_NUMBER), pipeline().process(sms("S04")))
        // A real bank body from a phone number is still dropped.
        val fromPhone = sms("D01").copy(sender = "+919876543210")
        assertEquals(PipelineResult.Ignored(IgnoreReason.SENDER_IS_PHONE_NUMBER), pipeline().process(fromPhone))
        assertTrue(saves.isEmpty())
        assertTrue(prompts.isEmpty())
    }

    @Test fun creditIsIgnored() = runBlocking {
        assertEquals(PipelineResult.Ignored(IgnoreReason.CREDIT_UPI), pipeline().process(sms("C01")))
        assertTrue(saves.isEmpty())
        assertTrue(prompts.isEmpty())
        assertEquals(0, dataChanges)
    }

    @Test fun duplicateIsNotPrompted() = runBlocking {
        val result = pipeline(save = { SaveResult.Duplicate }).process(sms("D01"))
        assertEquals(PipelineResult.Duplicate, result)
        assertTrue(prompts.isEmpty())
        assertEquals(0, dataChanges)
    }

    @Test fun parseThrowsGivesFailedParse() = runBlocking {
        val result = pipeline(parse = { _, _, _ -> throw IllegalStateException("boom") }).process(sms("D01"))
        assertEquals(PipelineResult.Failed(Stage.PARSE), result)
        assertTrue(saves.isEmpty())
    }

    @Test fun saveThrowsGivesFailedSave() = runBlocking {
        val result = pipeline(save = { throw IllegalStateException("disk full") }).process(sms("D01"))
        assertEquals(PipelineResult.Failed(Stage.SAVE), result)
        assertTrue(prompts.isEmpty())
        assertEquals(0, dataChanges)
    }

    @Test fun promptThrowsStillSaved() = runBlocking {
        val result = pipeline(prompt = { _, _ -> throw SecurityException("no permission") }).process(sms("D01"))
        assertEquals(PipelineResult.Saved(42, prompted = false), result)
        assertEquals(1, saves.size)
    }

    @Test fun promptNotShownStillSaved() = runBlocking {
        val result = pipeline(prompt = { _, _ -> false }).process(sms("D01"))
        assertEquals(PipelineResult.Saved(42, prompted = false), result)
    }

    @Test fun dataChangedThrowsStillSaved() = runBlocking {
        val result = pipeline(onDataChanged = { _, _ -> throw IllegalStateException() }).process(sms("D01"))
        assertEquals(PipelineResult.Saved(42, prompted = true), result)
    }

    @Test fun cancellationIsRethrown() {
        assertThrows(CancellationException::class.java) {
            runBlocking { pipeline(save = { throw CancellationException("cancelled") }).process(sms("D01")) }
        }
    }

    @Test fun lateSmsIsFiledUnderTheMonthInTheMessage() = runBlocking {
        assertEquals(PipelineResult.Saved(42, prompted = true), pipeline().process(sms("S09")))
        assertEquals("2026-06", saves.single().monthKey)
    }

    @Test fun onResultCalledOnceForEachOutcome() = runBlocking {
        val cases = listOf(
            pipeline() to PipelineResult.Saved(42, prompted = true),
            pipeline(save = { SaveResult.Duplicate }) to PipelineResult.Duplicate,
            pipeline(parse = { _, _, _ -> ParseResult.Ignored(IgnoreReason.OTP) }) to PipelineResult.Ignored(IgnoreReason.OTP),
            pipeline(parse = { _, _, _ -> throw IllegalStateException() }) to PipelineResult.Failed(Stage.PARSE),
            pipeline(save = { throw IllegalStateException() }) to PipelineResult.Failed(Stage.SAVE),
        )
        for ((pipeline, expected) in cases) {
            results.clear()
            assertEquals(expected, pipeline.process(sms("D01")))
            assertEquals(listOf(expected), results)
        }
    }

    @Test fun throwingOnResultDoesNotChangeTheResult() = runBlocking {
        val throwing: (PipelineResult) -> Unit = { throw IllegalStateException("hook") }
        assertEquals(PipelineResult.Saved(42, prompted = true), pipeline(onResult = throwing).process(sms("D01")))
        assertEquals(PipelineResult.Duplicate, pipeline(save = { SaveResult.Duplicate }, onResult = throwing).process(sms("D01")))
        assertEquals(PipelineResult.Ignored(IgnoreReason.CREDIT_UPI), pipeline(onResult = throwing).process(sms("C01")))
        assertEquals(
            PipelineResult.Failed(Stage.SAVE),
            pipeline(save = { throw IllegalStateException() }, onResult = throwing).process(sms("D01")),
        )
    }

    @Test fun savedCarriesViaFallback() = runBlocking {
        val axis = IncomingSms(
            "AX-AXISBK-S",
            "AXISBK: INR 450.00 debited from A/c XX1111 on 25-10-2026 via UPI Ref 123456789012. Not you? Call 1800",
            sms("D04").receivedAt,
        )
        assertEquals(PipelineResult.Saved(42, prompted = true, viaFallback = true), pipeline().process(axis))
        assertEquals(PipelineResult.Saved(42, prompted = true, viaFallback = false), pipeline().process(sms("D04")))
    }
}
