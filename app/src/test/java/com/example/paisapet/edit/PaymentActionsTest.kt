package com.example.paisapet.edit

import com.example.paisapet.IST
import com.example.paisapet.millisAt
import com.example.paisapet.model.Category
import com.example.paisapet.model.Source
import com.example.paisapet.model.TagSource
import com.example.paisapet.tx
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class PaymentActionsTest {

    private class FakeStore : PaymentStore {
        val calls = mutableListOf<String>()
        var found = true
        var failWith: Exception? = null

        private fun record(call: String): Boolean {
            failWith?.let { throw it }
            calls += call
            return found
        }

        override suspend fun setCategory(id: Long, category: Category, tagSource: TagSource) =
            record("setCategory($id, $category, $tagSource)")

        override suspend fun updateManual(id: Long, amountPaise: Long, payee: String, category: Category) =
            record("updateManual($id, $amountPaise, $payee, $category)")

        override suspend fun delete(id: Long) = record("delete($id)")

        override suspend fun addManual(expense: ManualExpense): Long {
            record("addManual($expense)")
            return 42
        }
    }

    private val store = FakeStore()
    private val cancelled = mutableListOf<Long>()
    private var currentMonth = "2026-10"
    private val actions = PaymentActions(store, cancelPrompt = { cancelled += it }, currentMonthKey = { currentMonth })

    private val october = tx(id = 7, occurredAt = millisAt(2026, 10, 20), category = Category.UNTAGGED)
    private val september = tx(id = 8, occurredAt = millisAt(2026, 9, 20), category = Category.UNTAGGED)
    private val manual = tx(id = 9, occurredAt = millisAt(2026, 10, 20), category = Category.MISC, source = Source.MANUAL)

    // Edit

    @Test fun tagWritesAppTagThenCancelsPrompt() = runBlocking {
        assertEquals(ActionResult.Done, actions.saveEdit(october, EditChange.Tag(Category.MISC)))
        assertEquals(listOf("setCategory(7, MISC, APP)"), store.calls)
        assertEquals(listOf(7L), cancelled)
    }

    @Test fun manualEditWritesThenCancels() = runBlocking {
        assertEquals(ActionResult.Done, actions.saveEdit(manual, EditChange.Manual(2_500, "Chai", Category.ESSENTIAL)))
        assertEquals(listOf("updateManual(9, 2500, Chai, ESSENTIAL)"), store.calls)
        assertEquals(listOf(9L), cancelled)
    }

    @Test fun manualChangeOnSmsRowIsInvalid() = runBlocking {
        assertEquals(ActionResult.Invalid(), actions.saveEdit(october, EditChange.Manual(2_500, "X", Category.MISC)))
        assertTrue(store.calls.isEmpty())
        assertTrue(cancelled.isEmpty())
    }

    @Test fun badAmountsAndUntaggedAreInvalid() = runBlocking {
        assertEquals(ActionResult.Invalid(setOf(AddError.AMOUNT_ZERO)),
            actions.saveEdit(manual, EditChange.Manual(0, "X", Category.MISC)))
        assertEquals(ActionResult.Invalid(setOf(AddError.AMOUNT_TOO_LARGE)),
            actions.saveEdit(manual, EditChange.Manual(MAX_AMOUNT_PAISE + 1, "X", Category.MISC)))
        assertEquals(ActionResult.Invalid(), actions.saveEdit(october, EditChange.Tag(Category.UNTAGGED)))
        assertEquals(ActionResult.Invalid(), actions.saveEdit(october, EditChange.None))
        assertTrue(store.calls.isEmpty())
        assertTrue(cancelled.isEmpty())
    }

    @Test fun pastMonthIsReadOnlyWithNoWriteAndNoCancel() = runBlocking {
        assertEquals(ActionResult.ReadOnly, actions.saveEdit(september, EditChange.Tag(Category.MISC)))
        assertEquals(ActionResult.ReadOnly, actions.delete(september))
        assertTrue(store.calls.isEmpty())
        assertTrue(cancelled.isEmpty())
    }

    @Test fun afterMonthChangeLastMonthBecomesReadOnly() = runBlocking {
        currentMonth = "2026-11"
        assertEquals(ActionResult.ReadOnly, actions.saveEdit(october, EditChange.Tag(Category.MISC)))
        assertTrue(store.calls.isEmpty())
    }

    @Test fun notFoundIsPassedThroughWithoutCancel() = runBlocking {
        store.found = false
        assertEquals(ActionResult.NotFound, actions.saveEdit(october, EditChange.Tag(Category.ESSENTIAL)))
        assertEquals(ActionResult.NotFound, actions.delete(october))
        assertTrue(cancelled.isEmpty())
    }

    @Test fun failureDoesNotCancel() = runBlocking {
        store.failWith = IllegalStateException("disk full")
        assertEquals(ActionResult.Failed, actions.saveEdit(october, EditChange.Tag(Category.MISC)))
        assertEquals(ActionResult.Failed, actions.delete(october))
        assertTrue(cancelled.isEmpty())
    }

    @Test(expected = CancellationException::class)
    fun cancellationIsNotSwallowed() = runBlocking<Unit> {
        store.failWith = CancellationException("left the screen")
        actions.saveEdit(october, EditChange.Tag(Category.MISC))
    }

    @Test fun cancelPromptThrowingStillReportsDone() = runBlocking {
        val throwing = PaymentActions(store, cancelPrompt = { throw SecurityException("no") }, currentMonthKey = { "2026-10" })
        assertEquals(ActionResult.Done, throwing.saveEdit(october, EditChange.Tag(Category.MISC)))
    }

    // Delete

    @Test fun deleteThenCancel() = runBlocking {
        assertEquals(ActionResult.Done, actions.delete(october))
        assertEquals(listOf("delete(7)"), store.calls)
        assertEquals(listOf(7L), cancelled)
    }

    // Add

    @Test fun addSavesWithoutAnyPrompt() = runBlocking {
        val nowLocal = LocalDateTime.of(2026, 10, 25, 15, 30)
        val input = AddInput("20", "", Category.MISC, nowLocal)
        assertEquals(ActionResult.Done, actions.add(input, nowLocal.atZone(IST).toInstant(), IST))
        assertEquals(1, store.calls.size)
        assertTrue(store.calls.single().startsWith("addManual(ManualExpense(amountPaise=2000, payee=Manual entry, category=MISC"))
        assertTrue(cancelled.isEmpty())
    }

    @Test fun addRevalidatesWithAFreshClock() = runBlocking {
        // The sheet was opened on 31 Oct; Save is tapped after midnight.
        val picked = LocalDateTime.of(2026, 10, 31, 23, 50)
        val afterMidnight = LocalDateTime.of(2026, 11, 1, 0, 5).atZone(IST).toInstant()
        val result = actions.add(AddInput("20", "", Category.MISC, picked), afterMidnight, IST)
        assertEquals(ActionResult.Invalid(setOf(AddError.DATE_BEFORE_MONTH)), result)
        assertTrue(store.calls.isEmpty())
    }

    @Test fun addFailureIsReported() = runBlocking {
        store.failWith = IllegalStateException("disk full")
        val nowLocal = LocalDateTime.of(2026, 10, 25, 15, 30)
        assertEquals(ActionResult.Failed,
            actions.add(AddInput("20", "", Category.MISC, nowLocal), nowLocal.atZone(IST).toInstant(), IST))
    }
}
