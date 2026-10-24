package com.example.paisapet.data.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MonthBudgetDaoTest {

    private lateinit var db: PaisaPetDatabase
    private lateinit var dao: MonthBudgetDao

    @Before fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, PaisaPetDatabase::class.java).build()
        dao = db.monthBudgetDao()
    }

    @After fun tearDown() = db.close()

    @Test fun observe_nullThenRow() = runBlocking {
        assertNull(dao.observe("2026-10").first())
        val row = MonthBudgetEntity("2026-10", 5_000_00L)
        dao.insertIfMissing(row)
        assertEquals(row, dao.observe("2026-10").first())
    }

    @Test fun insertIfMissing_insertsWhenAbsent() = runBlocking {
        assertTrue(dao.insertIfMissing(MonthBudgetEntity("2026-10", 5_000_00L)) != -1L)
        assertEquals(5_000_00L, dao.observe("2026-10").first()?.budgetPaise)
    }

    @Test fun insertIfMissing_presentReturnsMinusOne_doesNotOverwrite() = runBlocking {
        val first = MonthBudgetEntity("2026-10", 5_000_00L, alerted75 = true)
        dao.insertIfMissing(first)
        assertEquals(-1L, dao.insertIfMissing(MonthBudgetEntity("2026-10", 9_000_00L)))
        assertEquals(first, dao.observe("2026-10").first())
    }
}
