package com.example.paisapet.budget

import com.example.paisapet.model.Category
import java.time.LocalDate

/** One transaction as the budget maths sees it. */
data class Spend(val amountPaise: Long, val category: Category)

data class BudgetSummary(
    val monthKey: String,
    val budgetPaise: Long,
    /** MISC + UNTAGGED. */
    val miscSpentPaise: Long,
    val essentialSpentPaise: Long,
    val totalSpentPaise: Long,
    val untaggedCount: Int,
    /** Can be negative (over budget). */
    val leftPaise: Long,
    /** Whole % for display only; state uses the exact value. */
    val pctUsedFloor: Long,
    val daysLeft: Int,
    val pacePaisePerDay: Long,
    val buddyState: BuddyState,
)

/** MISC and UNTAGGED count against the budget; ESSENTIAL never does. */
fun miscSpent(spends: List<Spend>): Long =
    spends.filter { it.category != Category.ESSENTIAL }.sumOf { it.amountPaise }

fun left(budgetPaise: Long, miscSpentPaise: Long): Long = budgetPaise - miscSpentPaise

fun pctUsedFloor(miscSpentPaise: Long, budgetPaise: Long): Long {
    require(budgetPaise > 0) { "Budget must be > 0, was $budgetPaise" }
    return Math.multiplyExact(miscSpentPaise, 100L) / budgetPaise
}

/** max(left, 0) ÷ daysLeft, rounded down; 0 when no days are left. */
fun pacePerDay(leftPaise: Long, daysLeft: Int): Long =
    if (daysLeft <= 0) 0 else maxOf(leftPaise, 0L) / daysLeft

/** The month's own row wins; with no row the default applies (month reset, FR-16). */
fun effectiveBudget(monthRowBudget: Long?, defaultBudget: Long?): Long? = monthRowBudget ?: defaultBudget

fun summarize(monthKey: String, budgetPaise: Long, spends: List<Spend>, today: LocalDate): BudgetSummary {
    require(budgetPaise > 0) { "Budget must be > 0, was $budgetPaise" }
    val misc = miscSpent(spends)
    val essential = spends.filter { it.category == Category.ESSENTIAL }.sumOf { it.amountPaise }
    val left = left(budgetPaise, misc)
    val days = daysLeft(today, monthKey)
    return BudgetSummary(
        monthKey = monthKey,
        budgetPaise = budgetPaise,
        miscSpentPaise = misc,
        essentialSpentPaise = essential,
        totalSpentPaise = misc + essential,
        untaggedCount = spends.count { it.category == Category.UNTAGGED },
        leftPaise = left,
        pctUsedFloor = pctUsedFloor(misc, budgetPaise),
        daysLeft = days,
        pacePaisePerDay = pacePerDay(left, days),
        buddyState = buddyStateOf(misc, budgetPaise),
    )
}
