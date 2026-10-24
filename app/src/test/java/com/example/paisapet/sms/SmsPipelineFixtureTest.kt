package com.example.paisapet.sms

import com.example.paisapet.data.SaveResult
import kotlinx.coroutines.runBlocking
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

/** Every block of docs/sample-sms.txt through the whole pipeline, with an in-memory store. */
@RunWith(Parameterized::class)
class SmsPipelineFixtureTest(private val id: String, private val block: FixtureBlock) {

    companion object {
        private val ZONE: ZoneId = ZoneId.of("Asia/Kolkata")
        private val ALL_BLOCKS: List<FixtureBlock> by lazy { SmsFixtures.load() }

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun blocks(): List<Array<Any>> = ALL_BLOCKS.map { arrayOf(it.id, it) }

        /** Same rule as SmsFixtureTest: the block's receivedAt, else noon on its date (or 1 Jan 2026). */
        fun receivedAtFor(block: FixtureBlock): Instant =
            block.receivedAt
                ?: (block.date ?: LocalDate.of(2026, 1, 1)).atTime(LocalTime.NOON).atZone(ZONE).toInstant()

        fun smsOf(block: FixtureBlock) = IncomingSms(block.sender, block.body, receivedAtFor(block))
    }

    /** Hands out ids 1, 2, 3…; an smsHash seen before is a Duplicate. */
    private class FakeStore {
        val saved = mutableListOf<ParsedDebit>()
        suspend fun save(debit: ParsedDebit): SaveResult =
            if (saved.any { it.smsHash == debit.smsHash }) {
                SaveResult.Duplicate
            } else {
                saved += debit
                SaveResult.Saved(saved.size.toLong())
            }
    }

    private val store = FakeStore()
    private val prompts = mutableListOf<Long>()
    private val results = mutableListOf<PipelineResult>()
    private val pipeline = SmsPipeline(
        parse = { sender, body, receivedAt -> SmsParser.parse(sender, body, receivedAt, ZONE) },
        save = store::save,
        prompt = { txId, _ -> prompts += txId; true },
        onResult = { results += it },
    )

    @Test
    fun process() = runBlocking {
        when (block.expect) {
            Expect.DEBIT -> {
                assertEquals(PipelineResult.Saved(1, prompted = true), pipeline.process(smsOf(block)))
                assertEquals(listOf(1L), prompts)
                assertEquals(block.month.toString(), store.saved.single().monthKey)
            }
            Expect.IGNORE -> {
                val result = pipeline.process(smsOf(block))
                assertTrue("$id: expected Ignored but got $result", result is PipelineResult.Ignored)
                assertEquals(block.reason, (result as PipelineResult.Ignored).reason.code)
                assertTrue(store.saved.isEmpty())
                assertTrue(prompts.isEmpty())
            }
            Expect.DUPLICATE -> {
                val original = ALL_BLOCKS.single { it.id == block.duplicateOf }
                assertEquals(PipelineResult.Saved(1, prompted = true), pipeline.process(smsOf(original)))
                assertEquals(PipelineResult.Duplicate, pipeline.process(smsOf(block)))
                assertEquals("only the first one prompts", listOf(1L), prompts)
            }
        }
        for (result in results) {
            val text = result.toString()
            assertFalse("$id: result must not hold the sender", text.contains(block.sender))
            assertFalse("$id: result must not hold the body", text.contains(block.body.take(20)))
        }
    }
}
