package com.example.paisapet.notify

import com.example.paisapet.model.Category
import com.example.paisapet.model.TagSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/** The onDataChanged hook (Phase 7): only after a successful tag. */
class TagActionHandlerHookTest {

    private var hookCalls = 0

    private fun handler(
        setCategory: suspend (Long, Category, TagSource) -> Boolean = { _, _, _ -> true },
        onDataChanged: suspend () -> Unit = { hookCalls++ },
    ) = TagActionHandler(setCategory, cancelPrompt = {}, onDataChanged = onDataChanged)

    @Test fun calledAfterMiscAndEssential() = runBlocking {
        assertEquals(TagOutcome.Tagged, handler().handle(7, "MISC"))
        assertEquals(TagOutcome.Tagged, handler().handle(8, "ESSENTIAL"))
        assertEquals(2, hookCalls)
    }

    @Test fun notCalledAfterNotFound() = runBlocking {
        assertEquals(TagOutcome.NotFound, handler(setCategory = { _, _, _ -> false }).handle(7, "MISC"))
        assertEquals(0, hookCalls)
    }

    @Test fun notCalledAfterInvalid() = runBlocking {
        assertEquals(TagOutcome.Invalid, handler().handle(0, "MISC"))
        assertEquals(TagOutcome.Invalid, handler().handle(7, "UNTAGGED"))
        assertEquals(0, hookCalls)
    }

    @Test fun notCalledAfterFailed() = runBlocking {
        assertEquals(TagOutcome.Failed, handler(setCategory = { _, _, _ -> throw IllegalStateException() }).handle(7, "MISC"))
        assertEquals(0, hookCalls)
    }

    @Test fun throwingHookLeavesTagged() = runBlocking {
        assertEquals(TagOutcome.Tagged, handler(onDataChanged = { throw IllegalStateException() }).handle(7, "MISC"))
    }
}
