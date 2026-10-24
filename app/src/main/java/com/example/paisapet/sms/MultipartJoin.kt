package com.example.paisapet.sms

import java.time.Instant

/** One PDU part as Android delivers it. Sender or body may be missing. */
data class SmsPart(val sender: String?, val body: String?, val timestampMillis: Long)

/** A whole message, ready for the pipeline. Never stored or logged. */
data class IncomingSms(val sender: String, val body: String, val receivedAt: Instant)

/**
 * Joins the parts of long messages: parts are grouped by sender (in first-seen order) and their
 * bodies concatenated in arrival order with no separator. receivedAt is the earliest part's timestamp.
 * A missing sender or body counts as "".
 */
fun joinParts(parts: List<SmsPart>): List<IncomingSms> =
    parts.groupBy { it.sender.orEmpty() }.map { (sender, group) ->
        IncomingSms(
            sender = sender,
            body = group.joinToString("") { it.body.orEmpty() },
            receivedAt = Instant.ofEpochMilli(group.minOf { it.timestampMillis }),
        )
    }
