package com.example.paisapet.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.paisapet.budget.BudgetSummary
import com.example.paisapet.budget.monthKeyOf
import com.example.paisapet.data.db.MonthBudgetEntity
import com.example.paisapet.data.db.PaisaPetDatabase
import com.example.paisapet.data.db.TransactionEntity
import com.example.paisapet.data.settings.SettingsStore
import com.example.paisapet.edit.ManualExpense
import com.example.paisapet.edit.PaymentStore
import com.example.paisapet.model.Category
import com.example.paisapet.model.Source
import com.example.paisapet.model.TagSource
import com.example.paisapet.sms.ParsedDebit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class TransactionRepositoryTest {

    /** A clock a test can move forward. */
    private class MutableClock(var now: Instant, private val zone: ZoneId) : Clock() {
        override fun getZone(): ZoneId = zone
        override fun withZone(zone: ZoneId): Clock = MutableClock(now, zone)
        override fun instant(): Instant = now
    }

    private val ist: ZoneId = ZoneId.of("Asia/Kolkata")
    private val t0: Instant = istAt(2026, 10, 15, 12, 0)
    private val today: LocalDate = LocalDate.of(2026, 10, 15)

    private lateinit var db: PaisaPetDatabase
    private lateinit var settings: SettingsStore
    private lateinit var clock: MutableClock
    private lateinit var repo: TransactionRepository
    private lateinit var dataStoreJob: Job
    private lateinit var dataStoreFile: File

    @Before fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, PaisaPetDatabase::class.java).build()
        dataStoreJob = Job()
        dataStoreFile = File(context.cacheDir, "test-${UUID.randomUUID()}.preferences_pb")
        settings = SettingsStore(
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(Dispatchers.IO + dataStoreJob),
                produceFile = { dataStoreFile },
            ),
        )
        clock = MutableClock(istAt(2026, 10, 20, 9, 0), ist)
        repo = TransactionRepository(db, settings, clock)
    }

    @After fun tearDown() {
        runBlocking { dataStoreJob.cancelAndJoin() }
        dataStoreFile.delete()
        db.close()
    }

    private fun istAt(y: Int, m: Int, d: Int, h: Int, min: Int, s: Int = 0): Instant =
        LocalDateTime.of(y, m, d, h, min, s).atZone(ist).toInstant()

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
        monthKey = monthKeyOf(occurredAt, ist),
        dateFromMessage = true,
        smsHash = hash,
    )

    private suspend fun rows(monthKey: String = "2026-10"): List<TransactionEntity> =
        db.transactionDao().observeByMonth(monthKey).first()

    private suspend fun monthRow(monthKey: String = "2026-10"): MonthBudgetEntity? =
        db.monthBudgetDao().observe(monthKey).first()

    private suspend fun saveOk(debit: ParsedDebit): Long {
        val result = repo.saveParsedDebit(debit)
        assertTrue("expected Saved but got $result", result is SaveResult.Saved)
        return (result as SaveResult.Saved).id
    }

    // Saving

    @Test fun savedDebit_isUntaggedSms_withClockTimes() = runBlocking {
        val id = saveOk(debit("h1", ref = "296517043820"))
        val row = rows().single()
        assertEquals(id, row.id)
        assertEquals(Category.UNTAGGED, row.category)
        assertEquals(Source.SMS, row.source)
        assertNull(row.tagSource)
        assertNull(row.taggedAt)
        assertEquals(clock.millis(), row.createdAt)
        assertEquals(clock.millis(), row.updatedAt)
        assertEquals(t0.toEpochMilli(), row.occurredAt)
        assertEquals("h1", row.smsHash)
    }

    // Duplicates

    @Test fun sameSmsHash_isDuplicate_countStays1() = runBlocking {
        saveOk(debit("h1", ref = "296517043820"))
        assertEquals(SaveResult.Duplicate, repo.saveParsedDebit(debit("h1", ref = "296517043820")))
        assertEquals(1, rows().size)
    }

    @Test fun sameUpiRef_differentHash_isDuplicate() = runBlocking {
        saveOk(debit("h1", ref = "296517043820"))
        assertEquals(
            SaveResult.Duplicate,
            repo.saveParsedDebit(debit("h2", ref = "296517043820", occurredAt = t0.plusSeconds(3_600))),
        )
        assertEquals(1, rows().size)
    }

    @Test fun differentUpiRefs_sameAmountSameMinute_bothSaved() = runBlocking {
        saveOk(debit("h1", ref = "296517043820"))
        saveOk(debit("h2", ref = "296517043999", occurredAt = t0.plusSeconds(30)))
        assertEquals(2, rows().size)
    }

    @Test fun noUpiRef_sameAmountAndPayeeWithin2Min_isDuplicate() = runBlocking {
        saveOk(debit("h1"))
        assertEquals(SaveResult.Duplicate, repo.saveParsedDebit(debit("h2", occurredAt = t0.plusSeconds(119))))
        assertEquals(1, rows().size)
    }

    @Test fun twoCoroutinesSavingSameDebit_oneSavedOneDuplicate() = runBlocking {
        val d = debit("h1", ref = "296517043820")
        val a = async(Dispatchers.IO) { repo.saveParsedDebit(d) }
        val b = async(Dispatchers.IO) { repo.saveParsedDebit(d) }
        val results = listOf(a.await(), b.await())
        assertEquals(1, results.count { it is SaveResult.Saved })
        assertEquals(1, results.count { it == SaveResult.Duplicate })
        assertEquals(1, rows().size)
    }

    // Month budget row

    @Test fun save_createsMonthRowFromDefault() = runBlocking {
        settings.setDefaultBudgetPaise(5_000_00L)
        saveOk(debit("h1"))
        assertEquals(MonthBudgetEntity("2026-10", 5_000_00L), monthRow())
    }

    @Test fun save_doesNotOverwriteExistingMonthRow() = runBlocking {
        db.monthBudgetDao().insertIfMissing(MonthBudgetEntity("2026-10", 3_000_00L))
        settings.setDefaultBudgetPaise(5_000_00L)
        saveOk(debit("h1"))
        assertEquals(3_000_00L, monthRow()?.budgetPaise)
    }

    @Test fun save_noDefault_createsNoMonthRow() = runBlocking {
        saveOk(debit("h1"))
        assertNull(monthRow())
    }

    // setBudget (clock is 20 Oct 2026)

    @Test fun setBudget_noRow_createsRowAndDefault() = runBlocking {
        repo.setBudget(4_000_00L)
        assertEquals(MonthBudgetEntity("2026-10", 4_000_00L), monthRow())
        assertEquals(4_000_00L, settings.defaultBudgetPaise.first())
    }

    @Test fun setBudget_existingRow_replacesBudgetKeepsAlertFlags() = runBlocking {
        db.monthBudgetDao().insertIfMissing(
            MonthBudgetEntity("2026-10", 3_000_00L, alerted75 = true, alerted100 = true),
        )
        repo.setBudget(8_000_00L)
        assertEquals(MonthBudgetEntity("2026-10", 8_000_00L, alerted75 = true, alerted100 = true), monthRow())
        assertEquals(8_000_00L, settings.defaultBudgetPaise.first())
    }

    @Test fun setBudget_leavesOtherMonthUntouched() = runBlocking {
        val september = MonthBudgetEntity("2026-09", 2_000_00L, alerted75 = true, alerted100 = false)
        db.monthBudgetDao().insertIfMissing(september)
        repo.setBudget(6_000_00L)
        assertEquals(september, monthRow("2026-09"))
        assertEquals(6_000_00L, monthRow()?.budgetPaise)
    }

    // setCategory

    @Test fun setCategory_setsCategoryTagSourceAndTaggedAt() = runBlocking {
        val id = saveOk(debit("h1"))
        clock.now = clock.now.plusSeconds(60)
        assertTrue(repo.setCategory(id, Category.ESSENTIAL, TagSource.NOTIFICATION))
        val row = rows().single()
        assertEquals(Category.ESSENTIAL, row.category)
        assertEquals(TagSource.NOTIFICATION, row.tagSource)
        assertEquals(clock.millis(), row.taggedAt)
        assertEquals(clock.millis(), row.updatedAt)
    }

    @Test fun setCategory_unknownId_returnsFalse() = runBlocking {
        assertEquals(false, repo.setCategory(12345L, Category.MISC, TagSource.APP))
    }

    @Test fun setCategory_untagged_clearsTagSourceAndTaggedAt() = runBlocking {
        val id = saveOk(debit("h1"))
        repo.setCategory(id, Category.MISC, TagSource.APP)
        assertTrue(repo.setCategory(id, Category.UNTAGGED, TagSource.APP))
        val row = rows().single()
        assertEquals(Category.UNTAGGED, row.category)
        assertNull(row.tagSource)
        assertNull(row.taggedAt)
    }

    @Test fun taggedAfterMidnight_staysInPaymentMonth() = runBlocking {
        val occurred = istAt(2026, 10, 31, 23, 59)
        clock.now = occurred.plusSeconds(30)
        val id = saveOk(debit("h1", occurredAt = occurred))

        clock.now = istAt(2026, 11, 1, 0, 5)
        assertTrue(repo.setCategory(id, Category.MISC, TagSource.NOTIFICATION))

        val row = rows("2026-10").single()
        assertEquals(Category.MISC, row.category)
        assertEquals(clock.millis(), row.taggedAt)
        assertEquals(occurred.toEpochMilli(), row.occurredAt)
        assertTrue(rows("2026-11").isEmpty())
    }

    @Test fun paymentsEitherSideOfMidnight_differentMonths() = runBlocking {
        saveOk(debit("h1", ref = "111111111111", occurredAt = istAt(2026, 10, 31, 23, 59)))
        saveOk(debit("h2", ref = "222222222222", occurredAt = istAt(2026, 11, 1, 0, 0)))
        assertEquals(listOf("h1"), rows("2026-10").map { it.smsHash })
        assertEquals(listOf("h2"), rows("2026-11").map { it.smsHash })
    }

    // observeMonth

    @Test fun observeMonth_untaggedAndMiscCount_essentialDoesNot() = runBlocking {
        settings.setDefaultBudgetPaise(10_000_00L)
        saveOk(debit("untagged", ref = "111111111111", amountPaise = 100_00))
        val misc = saveOk(debit("misc", ref = "222222222222", amountPaise = 200_00))
        val essential = saveOk(debit("essential", ref = "333333333333", amountPaise = 400_00))
        repo.setCategory(misc, Category.MISC, TagSource.APP)
        repo.setCategory(essential, Category.ESSENTIAL, TagSource.APP)

        val view = repo.observeMonth("2026-10", today).first()
        assertEquals(3, view.transactions.size)
        assertEquals(300_00L, view.summary!!.miscSpentPaise)
        assertEquals(400_00L, view.summary!!.essentialSpentPaise)
        assertEquals(9_700_00L, view.summary!!.leftPaise)
    }

    @Test fun observeMonth_budgetFromRow_elseDefault() = runBlocking {
        settings.setDefaultBudgetPaise(1_000_00L)
        db.monthBudgetDao().insertIfMissing(MonthBudgetEntity("2026-10", 5_000_00L))
        assertEquals(5_000_00L, repo.observeMonth("2026-10", today).first().summary?.budgetPaise)
        assertEquals(
            1_000_00L,
            repo.observeMonth("2026-09", LocalDate.of(2026, 9, 15)).first().summary?.budgetPaise,
        )
    }

    @Test fun observeMonth_noBudgetThenDefaultChanges_updatesMonthWithoutRow() = runBlocking {
        saveOk(debit("h1"))
        assertNull(monthRow())

        val scope = CoroutineScope(Dispatchers.Default)
        val summaries = Channel<BudgetSummary?>(Channel.UNLIMITED)
        scope.launch { repo.observeMonth("2026-10", today).collect { summaries.send(it.summary) } }
        suspend fun awaitBudget(expected: Long?) = withTimeout(5_000) {
            var s = summaries.receive()
            while (s?.budgetPaise != expected) s = summaries.receive()
        }
        try {
            awaitBudget(null)
            settings.setDefaultBudgetPaise(1_000_00L)
            awaitBudget(1_000_00L)
            settings.setDefaultBudgetPaise(2_000_00L)
            awaitBudget(2_000_00L)
        } finally {
            scope.cancel()
        }
        assertNull(monthRow())
    }

    @Test fun observeMonth_noBudgetAtAll_summaryNull() = runBlocking {
        saveOk(debit("h1"))
        val view = repo.observeMonth("2026-10", today).first()
        assertEquals(1, view.transactions.size)
        assertNull(view.summary)
    }

    // addManual (clock is 20 Oct 2026, IST)

    private fun expense(
        amountPaise: Long = 20_00,
        payee: String = "Chai stall",
        category: Category = Category.MISC,
        occurredAt: Instant = istAt(2026, 10, 20, 8, 30),
    ) = ManualExpense(amountPaise, payee, category, occurredAt)

    @Test fun addManual_savesManualFields() = runBlocking {
        val id = repo.addManual(expense(category = Category.ESSENTIAL))
        val row = rows().single()
        assertEquals(id, row.id)
        assertEquals(20_00L, row.amountPaise)
        assertEquals("Chai stall", row.payee)
        assertEquals(Category.ESSENTIAL, row.category)
        assertEquals(Source.MANUAL, row.source)
        assertEquals(TagSource.APP, row.tagSource)
        assertEquals(clock.millis(), row.taggedAt)
        assertEquals(clock.millis(), row.createdAt)
        assertEquals(clock.millis(), row.updatedAt)
        assertEquals(istAt(2026, 10, 20, 8, 30).toEpochMilli(), row.occurredAt)
        assertNull(row.smsHash)
        assertNull(row.upiRef)
        assertNull(row.bank)
    }

    @Test fun addManual_at2359On31Oct_isOctober() = runBlocking {
        clock.now = istAt(2026, 10, 31, 23, 59, 30)
        repo.addManual(expense(occurredAt = istAt(2026, 10, 31, 23, 59)))
        assertEquals("2026-10", rows("2026-10").single().monthKey)
        assertTrue(rows("2026-11").isEmpty())
    }

    @Test fun addManual_createsMonthRowFromDefault() = runBlocking {
        settings.setDefaultBudgetPaise(5_000_00L)
        repo.addManual(expense())
        assertEquals(MonthBudgetEntity("2026-10", 5_000_00L), monthRow())
    }

    @Test fun addManual_doesNotOverwriteExistingMonthRow() = runBlocking {
        db.monthBudgetDao().insertIfMissing(MonthBudgetEntity("2026-10", 3_000_00L, alerted75 = true))
        settings.setDefaultBudgetPaise(5_000_00L)
        repo.addManual(expense())
        assertEquals(MonthBudgetEntity("2026-10", 3_000_00L, alerted75 = true), monthRow())
    }

    @Test fun addManual_noDefault_createsNoMonthRow() = runBlocking {
        repo.addManual(expense())
        assertNull(monthRow())
    }

    @Test fun addManual_twoIdenticalEntries_bothKept() = runBlocking {
        repo.addManual(expense())
        repo.addManual(expense())
        assertEquals(2, rows().size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun addManual_zeroAmountThrows() = runBlocking<Unit> {
        repo.addManual(expense(amountPaise = 0))
    }

    // updateManual

    @Test fun updateManual_sameCategory_keepsTagFieldsAndTime() = runBlocking {
        val id = repo.addManual(expense())
        val before = rows().single()
        clock.now = clock.now.plusSeconds(600)
        assertTrue(repo.updateManual(id, 25_00L, "Tea", Category.MISC))
        val after = rows().single()
        assertEquals(25_00L, after.amountPaise)
        assertEquals("Tea", after.payee)
        assertEquals(TagSource.APP, after.tagSource)
        assertEquals(before.taggedAt, after.taggedAt)
        assertEquals(clock.millis(), after.updatedAt)
        assertEquals(before.occurredAt, after.occurredAt)
        assertEquals(before.monthKey, after.monthKey)
    }

    @Test fun updateManual_newCategory_setsAppAndTaggedAtNow() = runBlocking {
        val id = repo.addManual(expense())
        clock.now = clock.now.plusSeconds(600)
        assertTrue(repo.updateManual(id, 20_00L, "Chai stall", Category.ESSENTIAL))
        val after = rows().single()
        assertEquals(Category.ESSENTIAL, after.category)
        assertEquals(TagSource.APP, after.tagSource)
        assertEquals(clock.millis(), after.taggedAt)
        assertEquals(istAt(2026, 10, 20, 8, 30).toEpochMilli(), after.occurredAt)
        assertEquals("2026-10", after.monthKey)
    }

    @Test fun updateManual_afterMonthChange_staysInItsMonth() = runBlocking {
        val id = repo.addManual(expense(occurredAt = istAt(2026, 10, 31, 23, 0)))
        clock.now = istAt(2026, 11, 1, 0, 10)
        assertTrue(repo.updateManual(id, 30_00L, "Late chai", Category.MISC))
        assertEquals("2026-10", rows("2026-10").single().monthKey)
        assertTrue(rows("2026-11").isEmpty())
    }

    @Test fun updateManual_smsRow_returnsFalseAndChangesNothing() = runBlocking {
        val id = saveOk(debit("h1"))
        val before = rows().single()
        assertEquals(false, repo.updateManual(id, 1_00L, "Changed", Category.MISC))
        assertEquals(before, rows().single())
    }

    @Test fun updateManual_missingId_returnsFalse() = runBlocking {
        assertEquals(false, repo.updateManual(12345L, 1_00L, "X", Category.MISC))
    }

    @Test(expected = IllegalArgumentException::class)
    fun updateManual_zeroAmountThrows() = runBlocking<Unit> {
        val id = repo.addManual(expense())
        repo.updateManual(id, 0, "X", Category.MISC)
    }

    // delete and observers

    @Test fun delete_removesRowAndReturnsFalseSecondTime() = runBlocking {
        val id = saveOk(debit("h1"))
        assertTrue(repo.delete(id))
        assertTrue(rows().isEmpty())
        assertEquals(false, repo.delete(id))
    }

    @Test fun delete_recalculatesMonthSummary() = runBlocking {
        settings.setDefaultBudgetPaise(1_000_00L)
        val id = saveOk(debit("h1", amountPaise = 400_00))
        assertEquals(400_00L, repo.observeMonth("2026-10", today).first().summary!!.miscSpentPaise)
        repo.delete(id)
        assertEquals(0L, repo.observeMonth("2026-10", today).first().summary!!.miscSpentPaise)
    }

    @Test fun observeTransaction_nullAfterDelete() = runBlocking {
        val id = repo.addManual(expense())
        assertEquals(id, repo.observeTransaction(id).first()?.id)
        repo.delete(id)
        assertNull(repo.observeTransaction(id).first())
    }

    @Test fun observeMonthsWithData_andAllForExport() = runBlocking {
        saveOk(debit("sep", ref = "111111111111", occurredAt = istAt(2026, 9, 10, 10, 0)))
        repo.addManual(expense())
        assertEquals(listOf("2026-10", "2026-09"), repo.observeMonthsWithData().first())
        assertEquals(listOf("sep", null), repo.allForExport().map { it.smsHash })
    }

    @Test fun setBudgetFromSettings_keepsBothAlertFlags() = runBlocking {
        db.monthBudgetDao().insertIfMissing(
            MonthBudgetEntity("2026-10", 5_000_00L, alerted75 = true, alerted100 = true),
        )
        repo.addManual(expense(amountPaise = 6_000_00L))
        repo.setBudget(10_000_00L)
        assertEquals(MonthBudgetEntity("2026-10", 10_000_00L, alerted75 = true, alerted100 = true), monthRow())
    }

    @Test fun repositoryIsThePaymentStore() = runBlocking {
        val store: PaymentStore = repo
        val id = store.addManual(expense())
        assertTrue(store.setCategory(id, Category.ESSENTIAL, TagSource.APP))
        assertTrue(store.delete(id))
    }
}
