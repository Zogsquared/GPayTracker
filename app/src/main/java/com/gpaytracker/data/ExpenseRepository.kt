package com.gpaytracker.data

import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class ExpenseRepository(private val dao: ExpenseDao) {

    // ── Week helpers ─────────────────────────────────────────────────────────

    fun currentWeekStart(): Long {
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun weekStartFor(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun weekEndFor(weekStart: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = weekStart
            add(Calendar.DAY_OF_WEEK, 6)
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }
        return cal.timeInMillis
    }

    // ── Expenses ─────────────────────────────────────────────────────────────

    fun getWeeklyExpenses(): Flow<List<Expense>> =
        dao.getExpensesForWeek(currentWeekStart())

    fun getWeeklyExpenseTotal(): Flow<Double?> =
        dao.getWeeklyExpenseTotal(currentWeekStart())

    fun getWeeklyCategoryTotals(): Flow<List<CategoryTotal>> =
        dao.getWeeklyCategoryTotals(currentWeekStart())

    fun getAllExpenses(): Flow<List<Expense>> = dao.getAllExpenses()

    suspend fun insertExpense(expense: Expense): Long {
        val ws = weekStartFor(expense.timestamp)
        return dao.insertExpense(expense.copy(weekStart = ws))
    }

    suspend fun deleteExpense(expense: Expense) = dao.deleteExpense(expense)

    // ── Income ───────────────────────────────────────────────────────────────

    fun getWeeklyIncome(): Flow<List<Income>> =
        dao.getIncomeForWeek(currentWeekStart())

    fun getWeeklyIncomeTotal(): Flow<Double?> =
        dao.getWeeklyIncomeTotal(currentWeekStart())

    fun getAllIncome(): Flow<List<Income>> = dao.getAllIncome()

    suspend fun insertIncome(income: Income): Long {
        val ws = weekStartFor(income.timestamp)
        return dao.insertIncome(income.copy(weekStart = ws))
    }

    suspend fun deleteIncome(income: Income) = dao.deleteIncome(income)

    // ── Weekly Summaries ─────────────────────────────────────────────────────

    fun getAllWeeklySummaries(): Flow<List<WeeklySummary>> =
        dao.getAllWeeklySummaries()

    suspend fun buildAndSaveSummary(weekStart: Long): WeeklySummary {
        val weekEnd = weekEndFor(weekStart)
        val totalExpenses = dao.getWeeklyExpenseTotalOnce(weekStart) ?: 0.0
        val totalIncome = dao.getWeeklyIncomeTotalOnce(weekStart) ?: 0.0
        val expenses = dao.getExpensesForWeekOnce(weekStart)

        val topCat = expenses
            .groupBy { it.category }
            .mapValues { (_, v) -> v.sumOf { it.amount } }
            .maxByOrNull { it.value }

        val netSavings = totalIncome - totalExpenses
        val savingsRate = if (totalIncome > 0) (netSavings / totalIncome) * 100 else 0.0

        val summaryText = buildSummaryText(
            totalIncome, totalExpenses, netSavings, savingsRate,
            topCat?.key?.name ?: "Other", topCat?.value ?: 0.0,
            expenses.size
        )

        val summary = WeeklySummary(
            weekStart = weekStart,
            weekEnd = weekEnd,
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            netSavings = netSavings,
            savingsRate = savingsRate,
            topCategory = topCat?.key?.name ?: "OTHER",
            topCategoryAmount = topCat?.value ?: 0.0,
            transactionCount = expenses.size,
            summaryText = summaryText
        )
        dao.insertWeeklySummary(summary)
        return summary
    }

    suspend fun summaryExistsForWeek(weekStart: Long): Boolean =
        dao.summaryExistsForWeek(weekStart) > 0

    private fun buildSummaryText(
        income: Double, expenses: Double, savings: Double, savingsRate: Double,
        topCat: String, topCatAmount: Double, txnCount: Int
    ): String {
        val rate = savingsRate.toInt()
        val verdict = when {
            income == 0.0 -> "No income recorded this week."
            rate >= 30    -> "Great week — you saved ${rate}% of your income!"
            rate >= 10    -> "Decent week — you saved ${rate}% of your income."
            rate >= 0     -> "Tight week — only ${rate}% of income was saved."
            else          -> "You spent more than you earned this week."
        }
        val topLine = if (topCatAmount > 0)
            " Your top spend was ${topCat.lowercase().replaceFirstChar(Char::titlecase)} " +
            "(₹${topCatAmount.toLong()})."
        else ""
        return "$verdict$topLine $txnCount transactions tracked."
    }
}
