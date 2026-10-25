package com.example.paisapet.sms

import com.example.paisapet.data.SaveResult
import kotlinx.coroutines.CancellationException
import java.time.Instant

/** What happened to one incoming SMS. Never holds the body or the sender. */
sealed interface PipelineResult {
    /** [prompted] is false when the tag prompt could not be shown (for example, notifications off).
     *  [viaFallback] is true when the generic rule, not a bank template, found the debit. */
    data class Saved(val id: Long, val prompted: Boolean, val viaFallback: Boolean = false) : PipelineResult
    data object Duplicate : PipelineResult
    data class Ignored(val reason: IgnoreReason) : PipelineResult
    /** No exception message is kept: it could echo the body. */
    data class Failed(val stage: Stage) : PipelineResult

    enum class Stage { PARSE, SAVE }
}

/**
 * Parse → save → prompt for one SMS (PRD "SMS capture and parsing rules", Pipeline).
 * The SMS receiver and debug Simulate SMS both call [process].
 */
class SmsPipeline(
    private val parse: (sender: String, body: String, receivedAt: Instant) -> ParseResult,
    private val save: suspend (ParsedDebit) -> SaveResult,
    /** Posts the tag prompt; returns false when it could not be shown. */
    private val prompt: (id: Long, debit: ParsedDebit) -> Boolean,
    /** Called after a save; the widget refresh goes here later. */
    private val onDataChanged: () -> Unit = {},
    /** Called exactly once per [process] with its result. */
    private val onResult: (PipelineResult) -> Unit = {},
) {

    /** Never throws, except [CancellationException]. */
    suspend fun process(sms: IncomingSms): PipelineResult {
        val result = run(sms)
        try {
            onResult(result)
        } catch (e: Exception) {
            // A broken hook must not change the result.
        }
        return result
    }

    private suspend fun run(sms: IncomingSms): PipelineResult {
        val parsed = try {
            parse(sms.sender, sms.body, sms.receivedAt)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return PipelineResult.Failed(PipelineResult.Stage.PARSE)
        }
        val debit = when (parsed) {
            is ParseResult.Ignored -> return PipelineResult.Ignored(parsed.reason)
            is ParseResult.Debit -> parsed.debit
        }

        val saved = try {
            save(debit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return PipelineResult.Failed(PipelineResult.Stage.SAVE)
        }
        val id = when (saved) {
            SaveResult.Duplicate -> return PipelineResult.Duplicate
            is SaveResult.Saved -> saved.id
        }

        // The transaction is saved; nothing after this point may lose it.
        val prompted = try {
            prompt(id, debit)
        } catch (e: Exception) {
            false
        }
        try {
            onDataChanged()
        } catch (e: Exception) {
            // Ignored: the data is saved and the UI observes Room.
        }
        return PipelineResult.Saved(id, prompted, debit.viaFallback)
    }
}
