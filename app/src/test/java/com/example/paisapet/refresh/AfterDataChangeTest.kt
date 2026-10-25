package com.example.paisapet.refresh

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.Collections

class AfterDataChangeTest {

    @Test fun runRefreshesOnce() = runBlocking {
        var refreshes = 0
        AfterDataChange(refreshWidgets = { refreshes++ }).run()
        assertEquals(1, refreshes)
    }

    @Test fun runWithAHeldBackIdStillRefreshes() = runBlocking {
        var refreshes = 0
        AfterDataChange(refreshWidgets = { refreshes++ }).run(heldBackId = 42)
        assertEquals(1, refreshes)
    }

    @Test fun concurrentRunsNeverOverlap() {
        val events = Collections.synchronizedList(mutableListOf<String>())
        val change = AfterDataChange(refreshWidgets = {
            events += "start"
            delay(50)
            events += "end"
        })
        runBlocking(Dispatchers.Default) {
            listOf(launch { change.run() }, launch { change.run() }, launch { change.run(heldBackId = 7) }).joinAll()
        }
        assertEquals(List(3) { listOf("start", "end") }.flatten(), events)
    }

    @Test fun aThrowingRefreshNeverEscapes() = runBlocking {
        AfterDataChange(refreshWidgets = { throw IllegalStateException("widget gone") }).run()
        AfterDataChange(refreshWidgets = { throw StackOverflowError() }).run()
    }

    @Test fun theLockIsReleasedAfterAFailure() = runBlocking {
        var calls = 0
        val change = AfterDataChange(refreshWidgets = {
            calls++
            if (calls == 1) throw IllegalStateException()
        })
        change.run()
        change.run()
        assertEquals(2, calls)
    }

    @Test fun cancellationIsRethrown() {
        assertThrows(CancellationException::class.java) {
            runBlocking { AfterDataChange(refreshWidgets = { throw CancellationException("left") }).run() }
        }
    }
}
