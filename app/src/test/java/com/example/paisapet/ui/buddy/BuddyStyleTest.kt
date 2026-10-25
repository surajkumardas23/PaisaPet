package com.example.paisapet.ui.buddy

import com.example.paisapet.budget.BuddyState
import org.junit.Assert.assertEquals
import org.junit.Test

class BuddyStyleTest {

    @Test fun names() {
        assertEquals(
            listOf("Thriving", "Fine", "Worried", "Sick", "Fainted"),
            BuddyState.entries.map { buddyName(it) },
        )
    }

    @Test fun homeLabelsFromThePrd() {
        assertEquals(
            listOf("Buddy is thriving", "Pacing smoothly", "Buddy is getting nervous", "Buddy is getting sick", "Buddy fainted"),
            BuddyState.entries.map { buddyHomeLabel(it) },
        )
    }
}
