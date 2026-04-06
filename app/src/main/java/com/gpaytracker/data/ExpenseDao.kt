package com.gpaytracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    // ── Expenses ────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("SELECT * FROM expenses WHERE weekStart = :weekStart ORDER BY timestamp DESC")
    fun getExpensesForWeek(weekStart: Long): Flow<List<Expense>>

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT SUM(amount) FROM expenses WHERE weekStart = :weekStart")
    fun getWeeklyExpenseTotal(weekStart: Long): Flow<Double?>

    @Query("""
        SELECT category, SUM(amount) as total 
        FROM expenses WHERE weekStart = :weekStart
        GROUP BY category ORDER BY total DESC
    """)
    fun getWeeklyCategoryTotals(weekStart: Long): Flow<List<CategoryTotal>>

    // ── Income ──────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: Income): Long

    @Delete
    suspend fun deleteIncome(income: Income)

    @Query("SELECT * FROM income WHERE weekStart = :weekStart ORDER BY timestamp DESC")
    fun getIncomeForWeek(weekStart: Long): Flow<List<Income>>

    @Query("SELECT * FROM income ORDER BY timestamp DESC")
    fun getAllIncome(): Flow<List<Income>>

    @Query("SELECT SUM(amount) FROM income WHERE weekStart = :weekStart")
    fun getWeeklyIncomeTotal(weekStart: Long): Flow<Double?>

    // ── Weekly Summaries ────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeklySummary(summary: WeeklySummary)

    @Query("SELECT * FROM weekly_summaries ORDER BY weekStart DESC")
    fun getAllWeeklySummaries(): Flow<List<WeeklySummary>>

    @Query("SELECT * FROM weekly_summaries WHERE weekStart = :weekStart LIMIT 1")
    suspend fun getSummaryForWeek(weekStart: Long): WeeklySummary?

    @Query("SELECT SUM(amount) FROM expenses WHERE weekStart = :weekStart")
    suspend fun getWeeklyExpenseTotalOnce(weekStart: Long): Double?

    @Query("SELECT SUM(amount) FROM income WHERE weekStart = :weekStart")
    suspend fun getWeeklyIncomeTotalOnce(weekStart: Long): Double?

    @Query("SELECT * FROM expenses WHERE weekStart = :weekStart")
    suspend fun getExpensesForWeekOnce(weekStart: Long): List<Expense>

    @Query("SELECT COUNT(*) FROM weekly_summaries WHERE weekStart = :weekStart")
    suspend fun summaryExistsForWeek(weekStart: Long): Int
}

data class CategoryTotal(
    val category: ExpenseCategory,
    val total: Double
)
