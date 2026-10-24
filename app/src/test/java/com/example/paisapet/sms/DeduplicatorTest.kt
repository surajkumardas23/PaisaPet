package com.example.paisapet.sms

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class DeduplicatorTest {

    private val t0: Instant = Instant.parse("2026-06-26T06:30:00Z")

    private fun debit(
        hash: String,
        ref: String? = null,
        amountPaise: Long = 14900,
        payee: String = "ZEPTO MARKETPLAC",
        occurredAt: Instant = t0,
    ) = ParsedDebit(
        amountPaise = amountPaise,
        payee = payee,
        upiRef = ref,
        bank = "SBIUPI",
        occurredAt = occurredAt,
        monthKey = "2026-06",
        dateFromMessage = true,
        smsHash = hash,
    )

    private fun dup(candidate: ParsedDebit, vararg existing: ParsedDebit) =
        Deduplicator.isDuplicate(candidate, existing.toList())

    @Test
    fun sameHash_isDuplicateEvenIfFieldsDiffer() = assertTrue(
        dup(
            debit("h1", ref = "111111111111", amountPaise = 100, payee = "A"),
            debit("h1", ref = "222222222222", amountPaise = 999, payee = "B", occurredAt = t0.plusSeconds(86_400)),
        ),
    )

    @Test
    fun sameRef_isDuplicate() =
        assertTrue(dup(debit("h1", ref = "296517043820"), debit("h2", ref = "296517043820", payee = "OTHER")))

    @Test
    fun differentRefs_sameAmountPayeeTime_isNotDuplicate() =
        // S02 vs D06
        assertFalse(dup(debit("h1", ref = "296517043999"), debit("h2", ref = "296517043820")))

    @Test
    fun noRef_119SecondsApart_isDuplicate() =
        assertTrue(dup(debit("h1"), debit("h2", occurredAt = t0.plusSeconds(119))))

    @Test
    fun noRef_exactly120SecondsApart_isDuplicate() {
        assertTrue(dup(debit("h1"), debit("h2", occurredAt = t0.plusSeconds(120))))
        assertTrue(dup(debit("h1"), debit("h2", occurredAt = t0.minusSeconds(120))))
    }

    @Test
    fun noRef_121SecondsApart_isNotDuplicate() {
        assertFalse(dup(debit("h1"), debit("h2", occurredAt = t0.plusSeconds(121))))
        assertFalse(dup(debit("h1"), debit("h2", occurredAt = t0.minusSeconds(121))))
    }

    @Test
    fun noRef_payeeCaseAndEndSpaces_isDuplicate() =
        assertTrue(dup(debit("h1", payee = "zepto marketplac"), debit("h2", payee = "  ZEPTO MARKETPLAC ")))

    @Test
    fun noRef_differentPayee_isNotDuplicate() =
        assertFalse(dup(debit("h1", payee = "ZEPTO MARKETPLAC"), debit("h2", payee = "BLINKIT")))

    @Test
    fun noRef_differentAmount_isNotDuplicate() =
        assertFalse(dup(debit("h1", amountPaise = 14900), debit("h2", amountPaise = 15000)))

    @Test
    fun oneRefMissing_usesAmountPayeeTimeRule() {
        assertTrue(dup(debit("h1", ref = "296517043820"), debit("h2", ref = null, occurredAt = t0.plusSeconds(60))))
        assertTrue(dup(debit("h1", ref = null), debit("h2", ref = "296517043820", occurredAt = t0.plusSeconds(60))))
        assertFalse(dup(debit("h1", ref = "296517043820"), debit("h2", ref = null, occurredAt = t0.plusSeconds(121))))
    }

    @Test
    fun emptyExisting_isNeverDuplicate() =
        assertFalse(Deduplicator.isDuplicate(debit("h1"), emptyList()))

    @Test
    fun anyMatchingItemIsEnough() =
        assertTrue(dup(debit("h1", ref = "1"), debit("h2", ref = "2"), debit("h3", ref = "1")))
}
