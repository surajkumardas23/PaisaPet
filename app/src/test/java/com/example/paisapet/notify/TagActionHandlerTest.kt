package com.example.paisapet.notify

import com.example.paisapet.model.Category
import com.example.paisapet.model.TagSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TagActionHandlerTest {

    private val writes = mutableListOf<Triple<Long, Category, TagSource>>()
    private val cancelled = mutableListOf<Long>()
    private var dataChanges = 0

    private fun handler(setCategory: suspend (Long, Category, TagSource) -> Boolean = { id, c, s -> writes += Triple(id, c, s); true }) =
        TagActionHandler(setCategory, cancelPrompt = { cancelled += it }, onDataChanged = { dataChanges++ })

    @Test fun miscTagsCancelsAndRefreshes() = runBlocking {
        assertEquals(TagOutcome.Tagged, handler().handle(7, "MISC"))
        assertEquals(listOf(Triple(7L, Category.MISC, TagSource.NOTIFICATION)), writes)
        assertEquals(listOf(7L), cancelled)
        assertEquals(1, dataChanges)
    }

    @Test fun essentialTagsCancelsAndRefreshes() = runBlocking {
        assertEquals(TagOutcome.Tagged, handler().handle(8, "ESSENTIAL"))
        assertEquals(listOf(Triple(8L, Category.ESSENTIAL, TagSource.NOTIFICATION)), writes)
        assertEquals(listOf(8L), cancelled)
        assertEquals(1, dataChanges)
    }

    @Test fun unknownIdDismissesPrompt() = runBlocking {
        assertEquals(TagOutcome.NotFound, handler(setCategory = { _, _, _ -> false }).handle(99, "MISC"))
        assertEquals(listOf(99L), cancelled)
        assertEquals(0, dataChanges)
    }

    @Test fun invalidInputWritesNothing() = runBlocking {
        val cases = listOf(-1L to "MISC", 0L to "MISC", 5L to "UNTAGGED", 5L to "banana", 5L to null, 5L to "misc")
        for ((id, category) in cases) {
            assertEquals("$id/$category", TagOutcome.Invalid, handler().handle(id, category))
        }
        assertTrue(writes.isEmpty())
        assertTrue(cancelled.isEmpty())
        assertEquals(0, dataChanges)
    }

    @Test fun writeThrowsKeepsPrompt() = runBlocking {
        assertEquals(TagOutcome.Failed, handler(setCategory = { _, _, _ -> throw IllegalStateException() }).handle(7, "MISC"))
        assertTrue(cancelled.isEmpty())
        assertEquals(0, dataChanges)
    }

    @Test fun cancelThrowsStillTagged() = runBlocking {
        val handler = TagActionHandler({ _, _, _ -> true }, cancelPrompt = { throw IllegalStateException() })
        assertEquals(TagOutcome.Tagged, handler.handle(7, "MISC"))
    }
}
