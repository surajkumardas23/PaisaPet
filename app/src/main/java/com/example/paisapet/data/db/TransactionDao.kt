package com.example.paisapet.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.paisapet.model.Category
import com.example.paisapet.model.TagSource
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    /** Returns the new row ID, or -1 if the smsHash already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Query("SELECT * FROM transactions WHERE monthKey = :monthKey ORDER BY occurredAt DESC")
    fun observeByMonth(monthKey: String): Flow<List<TransactionEntity>>

    /** A superset of possible duplicates of one SMS; Deduplicator makes the final decision. */
    @Query(
        """
        SELECT * FROM transactions
        WHERE source = 'SMS' AND (
              smsHash = :smsHash
           OR (:upiRef IS NOT NULL AND upiRef = :upiRef)
           OR (amountPaise = :amountPaise AND occurredAt BETWEEN :fromMs AND :toMs))
        """
    )
    suspend fun findDuplicateCandidates(
        smsHash: String,
        upiRef: String?,
        amountPaise: Long,
        fromMs: Long,
        toMs: Long,
    ): List<TransactionEntity>

    /** Returns the number of rows changed: 0 if [id] no longer exists. */
    @Query(
        """
        UPDATE transactions
        SET category = :category, tagSource = :tagSource, taggedAt = :taggedAt, updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun updateCategory(
        id: Long,
        category: Category,
        tagSource: TagSource?,
        taggedAt: Long?,
        updatedAt: Long,
    ): Int
}
