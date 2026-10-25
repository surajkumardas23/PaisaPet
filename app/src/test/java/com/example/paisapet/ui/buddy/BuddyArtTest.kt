package com.example.paisapet.ui.buddy

import com.example.paisapet.R
import com.example.paisapet.budget.BuddyState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class BuddyArtTest {

    @Test fun everyStateHasADrawable() {
        for (state in BuddyState.entries) {
            assertNotEquals("No drawable for $state", 0, buddyDrawable(state))
        }
    }

    @Test fun theFiveDrawablesAreAllDifferent() {
        assertEquals(5, BuddyState.entries.map(::buddyDrawable).toSet().size)
    }

    @Test fun eachStateGetsItsOwnFile() {
        assertEquals(R.drawable.buddy_thriving, buddyDrawable(BuddyState.THRIVING))
        assertEquals(R.drawable.buddy_fine, buddyDrawable(BuddyState.FINE))
        assertEquals(R.drawable.buddy_worried, buddyDrawable(BuddyState.WORRIED))
        assertEquals(R.drawable.buddy_sick, buddyDrawable(BuddyState.SICK))
        assertEquals(R.drawable.buddy_fainted, buddyDrawable(BuddyState.FAINTED))
    }
}
