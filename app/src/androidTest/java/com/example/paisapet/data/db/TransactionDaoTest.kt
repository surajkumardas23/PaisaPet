package com.example.paisapet.data.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.paisapet.model.Category
import com.example.paisapet.model.Source
import com.example.paisapet.model.TagSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
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
import java.time.LocalDateTime
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {

    private val ist: ZoneId = ZoneId.of("Asia/Kolkata")
    private val t0: Long = istMillis(2026, 10, 15, 12, 0)

    private lateinit var db: PaisaPetDatabase
    private lateinit var dao: TransactionDao

    @Before fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, PaisaPetDatabase::class.java).build()
        dao = db.transactionDao()
    }

    @After fun tearDown() = db.close()

    private fun istMillis(y: Int, m: Int, d: Int, h: Int, min: Int, s: Int = 0): Long =
        LocalDateTime.of(y, m, d, h, min, s).atZone(ist).toInstant().toEpochMilli()

    private fun row(
        hash: String?,
        ref: String? = null,
        amountPaise: Long = 14900,
        occurredAt: Long = t0,
        monthKey: String = "2026-10",
        source: Source = Source.SMS,
        payee: String = "ZEPTO",
    ) = TransactionEntity(
        amountPaise = amountPaise,
        payee = payee,
        upiRef = ref,
        bank = "SBIUPI",
        occurredAt = occurredAt,
        monthKey = monthKey,
        category = Category.UNTAGGED,
        source = source,
        tagSource = null,
        taggedAt = null,
        smsHash = hash,
        createdAt = 1L,
        updatedAt = 1L,
    )

    private suspend fun <T> ReceiveChannel<T>.awaitMatching(predicate: (T) -> Boolean): T =
        withTimeout(5_000) {
            var item = receive()
            while (!predicate(item)) item = receive()
            item
        }

    // Insert and read back

    @Test fun everyFieldRoundTrips() = runBlocking {
        val original = TransactionEntity(
            amountPaise = 62000,
            payee = "BIRRA BEVERAGES",
            upiRef = "651196203847",
            bank = "ICICIT",
            occurredAt = t0,
            monthKey = "2026-10",
            category = Category.ESSENTIAL,
            source = Source.SMS,
            tagSource = TagSource.NOTIFICATION,
            taggedAt = t0 + 5_000,
            smsHash = "hash-1",
            createdAt = t0 + 1_000,
            updatedAt = t0 + 6_000,
        )
        val id = dao.insert(original)
        assertTrue(id > 0)
        assertEquals(listOf(original.copy(id = id)), dao.observeByMonth("2026-10").first())

        val manual = original.copy(
            category = Category.MISC, source = Source.MANUAL, tagSource = TagSource.APP,
            smsHash = null, upiRef = null, bank = null,
        )
        val manualId = dao.insert(manual)
        val rows = dao.observeByMonth("2026-10").first()
        assertEquals(manual.copy(id = manualId), rows.single { it.id == manualId })
    }

    @Test fun sameSmsHashTwice_secondReturnsMinusOne() = runBlocking {
        assertTrue(dao.insert(row("h1")) > 0)
        assertEquals(-1L, dao.insert(row("h1", ref = "999999999999", amountPaise = 1)))
        assertEquals(1, dao.observeByMonth("2026-10").first().size)
    }

    @Test fun twoManualRowsWithNullHash_bothKept() = runBlocking {
        assertTrue(dao.insert(row(null, source = Source.MANUAL)) > 0)
        assertTrue(dao.insert(row(null, source = Source.MANUAL)) > 0)
        assertEquals(2, dao.observeByMonth("2026-10").first().size)
    }

    // observeByMonth

    @Test fun observeByMonth_onlyThatMonth_newestFirst() = runBlocking {
        dao.insert(row("old", occurredAt = t0))
        dao.insert(row("new", occurredAt = t0 + 60_000))
        dao.insert(row("mid", occurredAt = t0 + 30_000))
        dao.insert(row("nov", occurredAt = istMillis(2026, 11, 1, 0, 0), monthKey = "2026-11"))

        assertEquals(listOf("new", "mid", "old"), dao.observeByMonth("2026-10").first().map { it.smsHash })
        assertEquals(listOf("nov"), dao.observeByMonth("2026-11").first().map { it.smsHash })
    }

    @Test fun observeByMonth_emitsAfterInsertAndCategoryChange() = runBlocking<Unit> {
        val scope = CoroutineScope(Dispatchers.Default)
        val emissions = Channel<List<TransactionEntity>>(Channel.UNLIMITED)
        scope.launch { dao.observeByMonth("2026-10").collect { emissions.send(it) } }
        try {
            emissions.awaitMatching { it.isEmpty() }

            val id = dao.insert(row("h1"))
            emissions.awaitMatching { rows -> rows.singleOrNull()?.id == id }

            dao.updateCategory(id, Category.MISC, TagSource.APP, t0 + 1, t0 + 1)
            emissions.awaitMatching { rows -> rows.singleOrNull()?.category == Category.MISC }
        } finally {
            scope.cancel()
        }
    }

    // findDuplicateCandidates

    @Test fun candidates_byHash() = runBlocking {
        dao.insert(row("h1", ref = "111111111111", amountPaise = 100, occurredAt = t0 - 86_400_000))
        val found = dao.findDuplicateCandidates("h1", null, 999, t0 - 120_000, t0 + 120_000)
        assertEquals(listOf("h1"), found.map { it.smsHash })
    }

    @Test fun candidates_byUpiRef() = runBlocking {
        dao.insert(row("h1", ref = "296517043820", amountPaise = 100, occurredAt = t0 - 86_400_000))
        val found = dao.findDuplicateCandidates("other", "296517043820", 999, t0 - 120_000, t0 + 120_000)
        assertEquals(listOf("h1"), found.map { it.smsHash })
    }

    @Test fun candidates_sameAmountAt120s_foundAt121s_not() = runBlocking {
        dao.insert(row("plus120", occurredAt = t0 + 120_000))
        dao.insert(row("minus120", occurredAt = t0 - 120_000))
        dao.insert(row("plus121", occurredAt = t0 + 121_000))
        dao.insert(row("minus121", occurredAt = t0 - 121_000))

        val found = dao.findDuplicateCandidates("new", null, 14900, t0 - 120_000, t0 + 120_000)
        assertEquals(setOf("plus120", "minus120"), found.map { it.smsHash }.toSet())
    }

    @Test fun candidates_neverDifferentAmountOrManual() = runBlocking {
        dao.insert(row("otherAmount", amountPaise = 15000))
        dao.insert(row(null, ref = "296517043820", source = Source.MANUAL))

        val found = dao.findDuplicateCandidates("new", "296517043820", 14900, t0 - 120_000, t0 + 120_000)
        assertTrue(found.isEmpty())
    }

    // updateCategory

    @Test fun updateCategory_existingIdReturns1_keepsMonthAndTime() = runBlocking {
        val id = dao.insert(row("h1"))
        assertEquals(1, dao.updateCategory(id, Category.ESSENTIAL, TagSource.NOTIFICATION, 500L, 600L))

        val updated = dao.observeByMonth("2026-10").first().single()
        assertEquals(Category.ESSENTIAL, updated.category)
        assertEquals(TagSource.NOTIFICATION, updated.tagSource)
        assertEquals(500L, updated.taggedAt)
        assertEquals(600L, updated.updatedAt)
        assertEquals("2026-10", updated.monthKey)
        assertEquals(t0, updated.occurredAt)
    }

    @Test fun updateCategory_missingIdReturns0() = runBlocking {
        assertEquals(0, dao.updateCategory(12345L, Category.MISC, TagSource.APP, 1L, 1L))
    }

    // Phase 5 queries

    @Test fun observeByMonth_equalTimes_newestIdFirst() = runBlocking {
        val first = dao.insert(row("a", occurredAt = t0))
        val second = dao.insert(row("b", occurredAt = t0))
        val third = dao.insert(row("c", occurredAt = t0))
        assertEquals(listOf(third, second, first), dao.observeByMonth("2026-10").first().map { it.id })
    }

    @Test fun observeById_emitsRowThenNullAfterDelete() = runBlocking<Unit> {
        val id = dao.insert(row("h1"))
        val scope = CoroutineScope(Dispatchers.Default)
        val emissions = Channel<TransactionEntity?>(Channel.UNLIMITED)
        scope.launch { dao.observeById(id).collect { emissions.send(it) } }
        try {
            emissions.awaitMatching { it?.id == id }
            assertEquals(1, dao.deleteById(id))
            emissions.awaitMatching { it == null }
        } finally {
            scope.cancel()
        }
    }

    @Test fun getById_missingIsNull() = runBlocking {
        val id = dao.insert(row("h1"))
        assertEquals("h1", dao.getById(id)?.smsHash)
        assertNull(dao.getById(id + 100))
    }

    @Test fun observeMonthKeys_distinctNewestFirst() = runBlocking {
        dao.insert(row("o1", monthKey = "2026-10"))
        dao.insert(row("s1", monthKey = "2026-09"))
        dao.insert(row("o2", monthKey = "2026-10"))
        dao.insert(row("d1", monthKey = "2025-12"))
        dao.insert(row("n1", monthKey = "2026-11"))
        assertEquals(listOf("2026-11", "2026-10", "2026-09", "2025-12"), dao.observeMonthKeys().first())
    }

    @Test fun observeMonthKeys_emptyTable() = runBlocking {
        assertEquals(emptyList<String>(), dao.observeMonthKeys().first())
    }

    @Test fun getAll_oldestFirstThenId() = runBlocking {
        dao.insert(row("newest", occurredAt = t0 + 60_000))
        val tieA = dao.insert(row("tieA", occurredAt = t0))
        val tieB = dao.insert(row("tieB", occurredAt = t0))
        dao.insert(row("oldest", occurredAt = istMillis(2026, 9, 2, 9, 0), monthKey = "2026-09"))
        val all = dao.getAll()
        assertEquals(listOf("oldest", "tieA", "tieB", "newest"), all.map { it.smsHash })
        assertTrue(tieA < tieB)
    }

    @Test fun deleteById_returns1Then0ForMissing() = runBlocking {
        val id = dao.insert(row("h1"))
        assertEquals(1, dao.deleteById(id))
        assertEquals(0, dao.deleteById(id))
        assertEquals(0, dao.deleteById(98765L))
        assertTrue(dao.observeByMonth("2026-10").first().isEmpty())
    }

    @Test fun updateManual_smsRowTouches0Rows() = runBlocking {
        val id = dao.insert(row("h1"))
        assertEquals(0, dao.updateManual(id, 1L, "Changed", Category.MISC, TagSource.APP, 5L, 5L))
        val unchanged = dao.getById(id)!!
        assertEquals(14900L, unchanged.amountPaise)
        assertEquals("ZEPTO", unchanged.payee)
        assertEquals(Category.UNTAGGED, unchanged.category)
    }

    @Test fun updateManual_manualRowChangesFieldsNotTimeOrMonth() = runBlocking {
        val id = dao.insert(row(null, source = Source.MANUAL))
        assertEquals(1, dao.updateManual(id, 2_500L, "Chai", Category.ESSENTIAL, TagSource.APP, 7L, 8L))
        val updated = dao.getById(id)!!
        assertEquals(2_500L, updated.amountPaise)
        assertEquals("Chai", updated.payee)
        assertEquals(Category.ESSENTIAL, updated.category)
        assertEquals(TagSource.APP, updated.tagSource)
        assertEquals(7L, updated.taggedAt)
        assertEquals(8L, updated.updatedAt)
        assertEquals(t0, updated.occurredAt)
        assertEquals("2026-10", updated.monthKey)
    }

    @Test fun updateManual_missingIdTouches0Rows() = runBlocking {
        assertEquals(0, dao.updateManual(12345L, 1L, "X", Category.MISC, null, null, 1L))
    }
}
