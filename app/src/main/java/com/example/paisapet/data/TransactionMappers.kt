package com.example.paisapet.data

import com.example.paisapet.data.db.TransactionEntity
import com.example.paisapet.model.Category
import com.example.paisapet.model.Source
import com.example.paisapet.sms.Deduplicator
import com.example.paisapet.sms.ParsedDebit
import java.time.Instant

/** A new SMS row: UNTAGGED, never tagged, created and updated at [nowMillis]. */
fun ParsedDebit.toEntity(nowMillis: Long): TransactionEntity = TransactionEntity(
    amountPaise = amountPaise,
    payee = payee,
    upiRef = upiRef,
    bank = bank,
    occurredAt = occurredAt.toEpochMilli(),
    monthKey = monthKey,
    category = Category.UNTAGGED,
    source = Source.SMS,
    tagSource = null,
    taggedAt = null,
    smsHash = smsHash,
    createdAt = nowMillis,
    updatedAt = nowMillis,
)

/**
 * For the duplicate check only. [ParsedDebit.dateFromMessage] is set to true because the
 * check ignores it. Throws for a row with no smsHash (manual rows are never candidates).
 */
fun TransactionEntity.toParsedDebit(): ParsedDebit = ParsedDebit(
    amountPaise = amountPaise,
    payee = payee,
    upiRef = upiRef,
    bank = bank,
    occurredAt = Instant.ofEpochMilli(occurredAt),
    monthKey = monthKey,
    dateFromMessage = true,
    smsHash = checkNotNull(smsHash) { "Transaction $id has no smsHash, so it cannot be a duplicate candidate" },
)

/** Epoch-ms bounds, both inclusive, for the same-amount part of the candidates query. */
data class CandidateWindow(val fromMs: Long, val toMs: Long) {
    operator fun contains(epochMs: Long): Boolean = epochMs in fromMs..toMs
}

fun candidateWindow(occurredAtMillis: Long): CandidateWindow {
    val windowMs = Deduplicator.WINDOW.toMillis()
    return CandidateWindow(occurredAtMillis - windowMs, occurredAtMillis + windowMs)
}
