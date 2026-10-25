package com.example.paisapet.data

import androidx.room.withTransaction
import com.example.paisapet.budget.BudgetSummary
import com.example.paisapet.budget.Spend
import com.example.paisapet.budget.effectiveBudget
import com.example.paisapet.budget.monthKeyOf
import com.example.paisapet.budget.summarize
import com.example.paisapet.data.db.MonthBudgetEntity
import com.example.paisapet.data.db.PaisaPetDatabase
import com.example.paisapet.data.db.TransactionEntity
import com.example.paisapet.data.settings.SettingsStore
import com.example.paisapet.model.Category
import com.example.paisapet.model.TagSource
import com.example.paisapet.sms.Deduplicator
import com.example.paisapet.sms.ParsedDebit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate

sealed interface SaveResult {
    data class Saved(val id: Long) : SaveResult
    data object Duplicate : SaveResult
}

data class MonthView(val transactions: List<TransactionEntity>, val summary: BudgetSummary?)

class TransactionRepository(
    private val db: PaisaPetDatabase,
    private val settings: SettingsStore,
    private val clock: Clock,
) {
    private val transactionDao = db.transactionDao()
    private val monthBudgetDao = db.monthBudgetDao()

    /** Saves a new SMS debit unless it is a duplicate (FR-06), and creates the month's budget row from the default. */
    suspend fun saveParsedDebit(debit: ParsedDebit): SaveResult {
        // Read before the transaction so the write lock is not held while DataStore does I/O.
        val defaultBudget = settings.defaultBudgetPaise.first()
        return db.withTransaction {
            val now = clock.millis()
            val window = candidateWindow(debit.occurredAt.toEpochMilli())
            val candidates = transactionDao.findDuplicateCandidates(
                debit.smsHash, debit.upiRef, debit.amountPaise, window.fromMs, window.toMs,
            ).map { it.toParsedDebit() }
            if (Deduplicator.isDuplicate(debit, candidates)) return@withTransaction SaveResult.Duplicate

            val id = transactionDao.insert(debit.toEntity(now))
            if (id == -1L) return@withTransaction SaveResult.Duplicate

            // insertIfMissing never overwrites an existing month row.
            if (defaultBudget != null) monthBudgetDao.insertIfMissing(MonthBudgetEntity(debit.monthKey, defaultBudget))
            SaveResult.Saved(id)
        }
    }

    /** Saves [budgetPaise] as the default for future months and as this month's budget, keeping its alert flags. */
    suspend fun setBudget(budgetPaise: Long) {
        // DataStore first, outside the Room transaction. If killed in between, the default still applies.
        settings.setDefaultBudgetPaise(budgetPaise)
        val monthKey = monthKeyOf(clock.instant(), clock.zone)
        db.withTransaction {
            if (monthBudgetDao.insertIfMissing(MonthBudgetEntity(monthKey, budgetPaise)) == -1L) {
                monthBudgetDao.updateBudget(monthKey, budgetPaise)
            }
        }
    }

    /**
     * MISC and ESSENTIAL record [tagSource] and the tag time; UNTAGGED (Undo) clears both.
     * Never moves the row to another month. Returns false if [id] no longer exists.
     */
    suspend fun setCategory(id: Long, category: Category, tagSource: TagSource): Boolean {
        val now = clock.millis()
        val rows = when (category) {
            Category.MISC, Category.ESSENTIAL -> transactionDao.updateCategory(id, category, tagSource, now, now)
            Category.UNTAGGED -> transactionDao.updateCategory(id, Category.UNTAGGED, null, null, now)
        }
        return rows > 0
    }

    /** The month's rows (newest first) and its summary; the summary is null when there is no budget at all. */
    fun observeMonth(monthKey: String, today: LocalDate): Flow<MonthView> = combine(
        transactionDao.observeByMonth(monthKey),
        monthBudgetDao.observe(monthKey),
        settings.defaultBudgetPaise,
    ) { rows, monthRow, defaultBudget ->
        val summary = effectiveBudget(monthRow?.budgetPaise, defaultBudget)?.let { budget ->
            summarize(monthKey, budget, rows.map { Spend(it.amountPaise, it.category) }, today)
        }
        MonthView(rows, summary)
    }
}
