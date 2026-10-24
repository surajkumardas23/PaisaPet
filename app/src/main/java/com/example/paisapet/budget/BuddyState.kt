package com.example.paisapet.budget

enum class BuddyState { THRIVING, FINE, WORRIED, SICK, FAINTED }

/**
 * Buddy's state from the exact % used, compared in whole paise so 24.99% and 25% differ.
 * Exactly 100% is SICK; only over 100% is FAINTED.
 */
fun buddyStateOf(miscSpentPaise: Long, budgetPaise: Long): BuddyState {
    require(budgetPaise > 0) { "Budget must be > 0, was $budgetPaise" }
    val spent = Math.multiplyExact(miscSpentPaise, 100L)
    return when {
        spent < Math.multiplyExact(budgetPaise, 25L) -> BuddyState.THRIVING
        spent < Math.multiplyExact(budgetPaise, 50L) -> BuddyState.FINE
        spent < Math.multiplyExact(budgetPaise, 75L) -> BuddyState.WORRIED
        spent <= Math.multiplyExact(budgetPaise, 100L) -> BuddyState.SICK
        else -> BuddyState.FAINTED
    }
}
