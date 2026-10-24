package com.example.paisapet.sms

import java.time.Duration
import java.util.Locale

/** FR-06 duplicate rules as a pure function. Phase 2 feeds it the candidate rows from Room. */
object Deduplicator {

    private val WINDOW: Duration = Duration.ofSeconds(120)

    fun isDuplicate(candidate: ParsedDebit, existing: List<ParsedDebit>): Boolean =
        existing.any { isSamePayment(candidate, it) }

    private fun isSamePayment(a: ParsedDebit, b: ParsedDebit): Boolean {
        // 1. The same SMS delivered again.
        if (a.smsHash == b.smsHash) return true
        // 2. Both have a reference: only the reference decides.
        if (a.upiRef != null && b.upiRef != null) return a.upiRef == b.upiRef
        // 3. A reference is missing: same amount and payee within 2 minutes.
        return a.amountPaise == b.amountPaise &&
            normalisePayee(a.payee) == normalisePayee(b.payee) &&
            Duration.between(a.occurredAt, b.occurredAt).abs() <= WINDOW
    }

    private fun normalisePayee(payee: String) = payee.trim().lowercase(Locale.ROOT)
}
