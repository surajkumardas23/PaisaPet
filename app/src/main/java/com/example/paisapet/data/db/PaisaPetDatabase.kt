package com.example.paisapet.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Schema JSON goes to app/schemas and is committed. Never use a destructive migration (NFR-08):
 * every version bump after testers install needs a real migration.
 */
@Database(
    entities = [TransactionEntity::class, MonthBudgetEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class PaisaPetDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun monthBudgetDao(): MonthBudgetDao

    companion object {
        private const val NAME = "paisapet.db"

        fun create(context: Context): PaisaPetDatabase =
            Room.databaseBuilder(context.applicationContext, PaisaPetDatabase::class.java, NAME).build()
    }
}
