package com.example.paisapet.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.paisapet.model.Category
import com.example.paisapet.model.Source
import com.example.paisapet.model.TagSource

/** One UPI debit. The raw SMS body is never stored, only [smsHash]. */
@Entity(
    tableName = "transactions",
    indices = [Index(value = ["smsHash"], unique = true), Index(value = ["monthKey"])],
)
data class TransactionEntity(
    /** Also the notification ID. */
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Always > 0. */
    val amountPaise: Long,
    val payee: String,
    val upiRef: String?,
    val bank: String?,
    /** Epoch ms. */
    val occurredAt: Long,
    /** YYYY-MM of [occurredAt] in local time, fixed when the row is saved. */
    val monthKey: String,
    val category: Category,
    val source: Source,
    val tagSource: TagSource?,
    val taggedAt: Long?,
    /** Null for manual entries; SQLite allows several NULLs in a unique index. */
    val smsHash: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
