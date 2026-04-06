package com.gpaytracker.data

import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class ExpenseRepository(private val dao: ExpenseDao) {

    fun getAllExpenses(): Flow<List<Expense>> = dao.getAllExpenses()

    fun getWeeklyExpenses(): Flow<List<Expense>> {
        val (start, end) = currentWeekRange()
        return dao.getExpensesForWeek(start, end)
    }

    fun getWeeklyTotal(): Flow<Double?> {
        val (start, end) = currentWeekRange()
        return dao.getWeeklyTotal(start, end)
    }

    fun getWeeklyCategoryTotals(): Flow<List<CategoryTotal>> {
        val (start, end) = currentWeekRange()
        return dao.getWeeklyCategoryTotals(start, end)
    }

    suspend fun insertExpense(expense: Expense): Long = dao.insertExpense(expense)

    suspend fun deleteExpense(expense: Expense) = dao.deleteExpense(expense)

    /** Returns (Monday 00:00, Sunday 23:59:59) epoch millis for current week. */
    private fun currentWeekRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val weekStart = cal.timeInMillis
        cal.add(Calendar.DAY_OF_WEEK, 6)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val weekEnd = cal.timeInMillis
        return weekStart to weekEnd
    }
}
