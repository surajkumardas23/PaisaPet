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

    /** Newest first; equal times by id, newest row first. */
    @Query("SELECT * FROM transactions WHERE monthKey = :monthKey ORDER BY occurredAt DESC, id DESC")
    fun observeByMonth(monthKey: String): Flow<List<TransactionEntity>>

    /** Emits null once the row is deleted. */
    @Query("SELECT * FROM transactions WHERE id = :id")
    fun observeById(id: Long): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    /** Months that have at least one payment, newest first. */
    @Query("SELECT DISTINCT monthKey FROM transactions ORDER BY monthKey DESC")
    fun observeMonthKeys(): Flow<List<String>>

    /** Every payment for the CSV export, oldest first. */
    @Query("SELECT * FROM transactions ORDER BY occurredAt ASC, id ASC")
    suspend fun getAll(): List<TransactionEntity>

    /** Returns the number of rows deleted: 0 if [id] no longer exists. */
    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    /** Manual rows only; never changes occurredAt or monthKey. Returns 0 for a missing or SMS row. */
    @Query(
        """
        UPDATE transactions
        SET amountPaise = :amountPaise, payee = :payee, category = :category,
            tagSource = :tagSource, taggedAt = :taggedAt, updatedAt = :updatedAt
        WHERE id = :id AND source = 'MANUAL'
        """
    )
    suspend fun updateManual(
        id: Long,
        amountPaise: Long,
        payee: String,
        category: Category,
        tagSource: TagSource?,
        taggedAt: Long?,
        updatedAt: Long,
    ): Int

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
