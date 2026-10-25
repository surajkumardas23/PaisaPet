package com.example.paisapet.sms

import com.example.paisapet.data.SaveResult
import com.example.paisapet.sms.PipelineResult.Stage
import com.example.paisapet.sms.SmsPipelineFixtureTest.Companion.smsOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

/** The onDataChanged hook (Phase 7): called once after Saved, with the id and whether the prompt was shown. */
class SmsPipelineHookTest {

    private val zone: ZoneId = ZoneId.of("Asia/Kolkata")
    private val blocks = SmsFixtures.load().associateBy { it.id }
    private fun sms(id: String) = smsOf(blocks.getValue(id))

    private val hookCalls = mutableListOf<Pair<Long, Boolean>>()

    private fun pipeline(
        parse: (String, String, java.time.Instant) -> ParseResult = { s, b, t -> SmsParser.parse(s, b, t, zone) },
        save: suspend (ParsedDebit) -> SaveResult = { SaveResult.Saved(42) },
        prompt: (Long, ParsedDebit) -> Boolean = { _, _ -> true },
        onDataChanged: suspend (Long, Boolean) -> Unit = { id, prompted -> hookCalls += id to prompted },
    ) = SmsPipeline(parse, save, prompt, onDataChanged)

    @Test fun calledOnceAfterSavedWithIdAndPrompted() = runBlocking {
        assertEquals(PipelineResult.Saved(42, prompted = true), pipeline().process(sms("D01")))
        assertEquals(listOf(42L to true), hookCalls)
    }

    @Test fun promptNotShownIsPassedOn() = runBlocking {
        pipeline(prompt = { _, _ -> false }).process(sms("D01"))
        pipeline(prompt = { _, _ -> throw SecurityException() }).process(sms("D02"))
        assertEquals(listOf(42L to false, 42L to false), hookCalls)
    }

    @Test fun notCalledAfterDuplicate() = runBlocking {
        assertEquals(PipelineResult.Duplicate, pipeline(save = { SaveResult.Duplicate }).process(sms("D01")))
        assertTrue(hookCalls.isEmpty())
    }

    @Test fun notCalledAfterIgnored() = runBlocking {
        assertTrue(pipeline().process(sms("C01")) is PipelineResult.Ignored)
        assertTrue(pipeline().process(sms("S04")) is PipelineResult.Ignored)
        assertTrue(hookCalls.isEmpty())
    }

    @Test fun notCalledAfterFailed() = runBlocking {
        assertEquals(
            PipelineResult.Failed(Stage.PARSE),
            pipeline(parse = { _, _, _ -> throw IllegalStateException() }).process(sms("D01")),
        )
        assertEquals(
            PipelineResult.Failed(Stage.SAVE),
            pipeline(save = { throw IllegalStateException() }).process(sms("D01")),
        )
        assertTrue(hookCalls.isEmpty())
    }

    @Test fun throwingHookLeavesSaved() = runBlocking {
        val result = pipeline(onDataChanged = { _, _ -> throw IllegalStateException("widget") }).process(sms("D01"))
        assertEquals(PipelineResult.Saved(42, prompted = true), result)
    }

    @Test fun cancellationInTheHookIsRethrown() {
        assertThrows(CancellationException::class.java) {
            runBlocking { pipeline(onDataChanged = { _, _ -> throw CancellationException() }).process(sms("D01")) }
        }
    }
}
