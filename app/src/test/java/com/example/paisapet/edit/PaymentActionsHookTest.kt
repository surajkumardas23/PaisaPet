package com.example.paisapet.edit

import com.example.paisapet.IST
import com.example.paisapet.millisAt
import com.example.paisapet.model.Category
import com.example.paisapet.model.Source
import com.example.paisapet.model.TagSource
import com.example.paisapet.tx
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/** The onDataChanged hook (Phase 7): after every Done, never after ReadOnly, NotFound, Invalid or Failed. */
class PaymentActionsHookTest {

    private class FakeStore : PaymentStore {
        var found = true
        var failWith: Exception? = null

        private fun answer(): Boolean {
            failWith?.let { throw it }
            return found
        }

        override suspend fun setCategory(id: Long, category: Category, tagSource: TagSource) = answer()
        override suspend fun updateManual(id: Long, amountPaise: Long, payee: String, category: Category) = answer()
        override suspend fun delete(id: Long) = answer()
        override suspend fun addManual(expense: ManualExpense): Long {
            answer()
            return 42
        }
    }

    private val store = FakeStore()
    private var hookCalls = 0
    private fun actions(onDataChanged: suspend () -> Unit = { hookCalls++ }) =
        PaymentActions(store, cancelPrompt = {}, currentMonthKey = { "2026-10" }, onDataChanged = onDataChanged)

    private val october = tx(id = 7, occurredAt = millisAt(2026, 10, 20), category = Category.UNTAGGED)
    private val september = tx(id = 8, occurredAt = millisAt(2026, 9, 20), category = Category.UNTAGGED)
    private val manual = tx(id = 9, occurredAt = millisAt(2026, 10, 20), category = Category.MISC, source = Source.MANUAL)

    private val now = LocalDateTime.of(2026, 10, 25, 15, 30)
    private suspend fun add(amount: String = "20", picked: LocalDateTime = now) =
        actions().add(AddInput(amount, "", Category.MISC, picked), now.atZone(IST).toInstant(), IST)

    @Test fun calledOnceAfterEachDone() = runBlocking {
        assertEquals(ActionResult.Done, actions().saveEdit(october, EditChange.Tag(Category.MISC)))
        assertEquals(1, hookCalls)
        assertEquals(ActionResult.Done, actions().saveEdit(manual, EditChange.Manual(2_500, "Chai", Category.ESSENTIAL)))
        assertEquals(2, hookCalls)
        assertEquals(ActionResult.Done, actions().delete(october))
        assertEquals(3, hookCalls)
        assertEquals(ActionResult.Done, add())
        assertEquals(4, hookCalls)
    }

    @Test fun notCalledWhenReadOnly() = runBlocking {
        assertEquals(ActionResult.ReadOnly, actions().saveEdit(september, EditChange.Tag(Category.MISC)))
        assertEquals(ActionResult.ReadOnly, actions().delete(september))
        assertEquals(0, hookCalls)
    }

    @Test fun notCalledWhenNotFound() = runBlocking {
        store.found = false
        assertEquals(ActionResult.NotFound, actions().saveEdit(october, EditChange.Tag(Category.MISC)))
        assertEquals(ActionResult.NotFound, actions().delete(october))
        assertEquals(0, hookCalls)
    }

    @Test fun notCalledWhenInvalid() = runBlocking {
        assertEquals(ActionResult.Invalid(), actions().saveEdit(october, EditChange.None))
        assertEquals(ActionResult.Invalid(), actions().saveEdit(october, EditChange.Tag(Category.UNTAGGED)))
        assertEquals(ActionResult.Invalid(setOf(AddError.AMOUNT_ZERO)), actions().saveEdit(manual, EditChange.Manual(0, "X", Category.MISC)))
        assertTrue(add(amount = "0") is ActionResult.Invalid)
        assertTrue(add(picked = now.plusDays(1)) is ActionResult.Invalid)
        assertEquals(0, hookCalls)
    }

    @Test fun notCalledWhenFailed() = runBlocking {
        store.failWith = IllegalStateException("disk full")
        assertEquals(ActionResult.Failed, actions().saveEdit(october, EditChange.Tag(Category.MISC)))
        assertEquals(ActionResult.Failed, actions().delete(october))
        assertEquals(ActionResult.Failed, add())
        assertEquals(0, hookCalls)
    }

    @Test fun throwingHookStillDone() = runBlocking {
        val throwing = actions(onDataChanged = { throw IllegalStateException("widget") })
        assertEquals(ActionResult.Done, throwing.saveEdit(october, EditChange.Tag(Category.MISC)))
        assertEquals(ActionResult.Done, throwing.delete(october))
    }
}
