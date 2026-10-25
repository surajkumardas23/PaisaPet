package com.example.paisapet.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthBudgetDao {

    /** Emits null while the month has no row (the default budget applies). */
    @Query("SELECT * FROM month_budgets WHERE monthKey = :monthKey")
    fun observe(monthKey: String): Flow<MonthBudgetEntity?>

    /** Never overwrites an existing row. Returns -1 if the month already had one. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(budget: MonthBudgetEntity): Long

    /** Changes only the budget; keeps alerted75/alerted100. Returns rows changed (0 if the month has no row). */
    @Query("UPDATE month_budgets SET budgetPaise = :budgetPaise WHERE monthKey = :monthKey")
    suspend fun updateBudget(monthKey: String, budgetPaise: Long): Int
}
