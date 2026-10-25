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
import com.example.paisapet.edit.ManualExpense
import com.example.paisapet.edit.PaymentStore
import com.example.paisapet.model.Category
import com.example.paisapet.model.Source
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
) : PaymentStore {
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
    override suspend fun setCategory(id: Long, category: Category, tagSource: TagSource): Boolean {
        val now = clock.millis()
        val rows = when (category) {
            Category.MISC, Category.ESSENTIAL -> transactionDao.updateCategory(id, category, tagSource, now, now)
            Category.UNTAGGED -> transactionDao.updateCategory(id, Category.UNTAGGED, null, null, now)
        }
        return rows > 0
    }

    /**
     * Saves a payment added by hand (FR-14): no smsHash, so it is never part of the duplicate check.
     * Creates the month's budget row from the default like [saveParsedDebit]. Posts no notification.
     */
    override suspend fun addManual(expense: ManualExpense): Long {
        require(expense.amountPaise > 0) { "Amount must be > 0 paise, was ${expense.amountPaise}" }
        // Read before the transaction so the write lock is not held while DataStore does I/O.
        val defaultBudget = settings.defaultBudgetPaise.first()
        return db.withTransaction {
            val now = clock.millis()
            val monthKey = monthKeyOf(expense.occurredAt, clock.zone)
            val id = transactionDao.insert(
                TransactionEntity(
                    amountPaise = expense.amountPaise,
                    payee = expense.payee,
                    upiRef = null,
                    bank = null,
                    occurredAt = expense.occurredAt.toEpochMilli(),
                    monthKey = monthKey,
                    category = expense.category,
                    source = Source.MANUAL,
                    tagSource = TagSource.APP,
                    taggedAt = now,
                    smsHash = null,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            if (defaultBudget != null) monthBudgetDao.insertIfMissing(MonthBudgetEntity(monthKey, defaultBudget))
            id
        }
    }

    /**
     * Manual rows only. occurredAt and monthKey never change. A new category records APP and the time;
     * an unchanged one keeps the old tagSource and taggedAt. Returns false if [id] is missing or not manual.
     */
    override suspend fun updateManual(id: Long, amountPaise: Long, payee: String, category: Category): Boolean {
        require(amountPaise > 0) { "Amount must be > 0 paise, was $amountPaise" }
        return db.withTransaction {
            val row = transactionDao.getById(id)
            if (row == null || row.source != Source.MANUAL) return@withTransaction false
            val now = clock.millis()
            val changed = row.category != category
            transactionDao.updateManual(
                id = id,
                amountPaise = amountPaise,
                payee = payee,
                category = category,
                tagSource = if (changed) TagSource.APP else row.tagSource,
                taggedAt = if (changed) now else row.taggedAt,
                updatedAt = now,
            ) > 0
        }
    }

    /** Returns false if [id] no longer exists. */
    override suspend fun delete(id: Long): Boolean = transactionDao.deleteById(id) > 0

    /** Emits null once the payment is deleted. */
    fun observeTransaction(id: Long): Flow<TransactionEntity?> = transactionDao.observeById(id)

    /** Months with at least one payment, newest first (FR-23). */
    fun observeMonthsWithData(): Flow<List<String>> = transactionDao.observeMonthKeys()

    /** Every payment, oldest first, for the CSV export (FR-18). */
    suspend fun allForExport(): List<TransactionEntity> = transactionDao.getAll()

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
