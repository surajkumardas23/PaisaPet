package com.example.paisapet.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "month_budgets")
data class MonthBudgetEntity(
    /** YYYY-MM. */
    @PrimaryKey val monthKey: String,
    val budgetPaise: Long,
    /** Alert flags (FR-17), used in a later phase. */
    val alerted75: Boolean = false,
    val alerted100: Boolean = false,
)
