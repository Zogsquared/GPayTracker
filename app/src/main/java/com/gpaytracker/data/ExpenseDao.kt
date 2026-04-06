package com.gpaytracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    /**
     * Get expenses for the current ISO week (Monday–Sunday).
     * :weekStart is epoch millis of Monday 00:00, :weekEnd is Sunday 23:59.
     */
    @Query("SELECT * FROM expenses WHERE timestamp BETWEEN :weekStart AND :weekEnd ORDER BY timestamp DESC")
    fun getExpensesForWeek(weekStart: Long, weekEnd: Long): Flow<List<Expense>>

    @Query("SELECT SUM(amount) FROM expenses WHERE timestamp BETWEEN :weekStart AND :weekEnd")
    fun getWeeklyTotal(weekStart: Long, weekEnd: Long): Flow<Double?>

    @Query("""
        SELECT category, SUM(amount) as total 
        FROM expenses 
        WHERE timestamp BETWEEN :weekStart AND :weekEnd 
        GROUP BY category
    """)
    fun getWeeklyCategoryTotals(weekStart: Long, weekEnd: Long): Flow<List<CategoryTotal>>

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestExpense(): Expense?
}

data class CategoryTotal(
    val category: ExpenseCategory,
    val total: Double
)
