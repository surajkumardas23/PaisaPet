package com.example.paisapet

import com.example.paisapet.budget.monthKeyOf
import com.example.paisapet.data.db.TransactionEntity
import com.example.paisapet.model.Category
import com.example.paisapet.model.Source
import com.example.paisapet.model.TagSource
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** Shared by the pure-logic tests of Phase 5. */
val IST: ZoneId = ZoneId.of("Asia/Kolkata")

/** Epoch ms of a local time in [zone]. */
fun millisAt(y: Int, mo: Int, d: Int, h: Int = 12, mi: Int = 0, s: Int = 0, zone: ZoneId = IST): Long =
    LocalDateTime.of(y, mo, d, h, mi, s).atZone(zone).toInstant().toEpochMilli()

/** A transaction whose monthKey follows [occurredAt] in [zone], as the app stores it. */
fun tx(
    id: Long = 1,
    amountPaise: Long = 24_000,
    payee: String = "Swiggy",
    occurredAt: Long = millisAt(2026, 10, 25),
    category: Category = Category.UNTAGGED,
    source: Source = Source.SMS,
    tagSource: TagSource? = null,
    taggedAt: Long? = null,
    upiRef: String? = null,
    bank: String? = null,
    createdAt: Long = occurredAt,
    zone: ZoneId = IST,
) = TransactionEntity(
    id = id,
    amountPaise = amountPaise,
    payee = payee,
    upiRef = upiRef,
    bank = bank,
    occurredAt = occurredAt,
    monthKey = monthKeyOf(Instant.ofEpochMilli(occurredAt), zone),
    category = category,
    source = source,
    tagSource = tagSource,
    taggedAt = taggedAt,
    smsHash = if (source == Source.SMS) "hash$id" else null,
    createdAt = createdAt,
    updatedAt = createdAt,
)
