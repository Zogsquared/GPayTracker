package com.gpaytracker.data

import android.content.Context
import androidx.room.*

@Database(
    entities = [Expense::class, Income::class, WeeklySummary::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile private var INSTANCE: ExpenseDatabase? = null

        fun getDatabase(context: Context): ExpenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    "expense_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class Converters {
    @TypeConverter fun fromCategory(v: ExpenseCategory): String = v.name
    @TypeConverter fun toCategory(v: String): ExpenseCategory = ExpenseCategory.valueOf(v)
}
