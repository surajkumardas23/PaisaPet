package com.example.paisapet.budget

import com.example.paisapet.budget.BuddyState.FAINTED
import com.example.paisapet.budget.BuddyState.FINE
import com.example.paisapet.budget.BuddyState.SICK
import com.example.paisapet.budget.BuddyState.THRIVING
import com.example.paisapet.budget.BuddyState.WORRIED
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Boundaries against a ₹10,000 budget. */
@RunWith(Parameterized::class)
class BuddyStateTest(
    private val label: String,
    private val spentPaise: Long,
    private val state: BuddyState,
    private val floor: Long,
) {

    companion object {
        private const val BUDGET = 1_000_000L

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> = listOf(
            arrayOf("0%", 0L, THRIVING, 0L),
            arrayOf("24.99%", 249_900L, THRIVING, 24L),
            arrayOf("24.9999%", 249_999L, THRIVING, 24L),
            arrayOf("25%", 250_000L, FINE, 25L),
            arrayOf("49.99%", 499_900L, FINE, 49L),
            arrayOf("50%", 500_000L, WORRIED, 50L),
            arrayOf("74.99%", 749_900L, WORRIED, 74L),
            arrayOf("75%", 750_000L, SICK, 75L),
            arrayOf("99.99%", 999_900L, SICK, 99L),
            arrayOf("100%", 1_000_000L, SICK, 100L),
            arrayOf("100.01%", 1_000_100L, FAINTED, 100L),
            arrayOf("300%", 3_000_000L, FAINTED, 300L),
        )
    }

    @Test fun state() = assertEquals(state, buddyStateOf(spentPaise, BUDGET))

    @Test fun pctFloor() = assertEquals(floor, pctUsedFloor(spentPaise, BUDGET))
}
