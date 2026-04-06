package com.gpaytracker.viewmodel

import androidx.lifecycle.*
import com.gpaytracker.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ExpenseViewModel(private val repository: ExpenseRepository) : ViewModel() {

    private val _weeklyBudget = MutableLiveData(7000.0)
    val weeklyBudget: LiveData<Double> = _weeklyBudget
    fun setWeeklyBudget(amount: Double) { _weeklyBudget.value = amount }

    // ── Current week ─────────────────────────────────────────────────────────

    val weeklyExpenses: StateFlow<List<Expense>> = repository.getWeeklyExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val weeklyExpenseTotal: StateFlow<Double> = repository.getWeeklyExpenseTotal()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val weeklyIncome: StateFlow<List<Income>> = repository.getWeeklyIncome()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val weeklyIncomeTotal: StateFlow<Double> = repository.getWeeklyIncomeTotal()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val categoryTotals: StateFlow<List<CategoryTotal>> = repository.getWeeklyCategoryTotals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── All-time ─────────────────────────────────────────────────────────────

    val allExpenses: StateFlow<List<Expense>> = repository.getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allIncome: StateFlow<List<Income>> = repository.getAllIncome()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val weeklySummaries: StateFlow<List<WeeklySummary>> = repository.getAllWeeklySummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Actions ──────────────────────────────────────────────────────────────

    fun deleteExpense(expense: Expense) = viewModelScope.launch { repository.deleteExpense(expense) }
    fun deleteIncome(income: Income) = viewModelScope.launch { repository.deleteIncome(income) }

    fun generateWeeklySummary() = viewModelScope.launch {
        repository.buildAndSaveSummary(repository.currentWeekStart())
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    fun budgetPercent(total: Double, budget: Double): Int =
        if (budget > 0) ((total / budget) * 100).toInt().coerceIn(0, 100) else 0

    fun incomeSpentPercent(expenses: Double, income: Double): Int =
        if (income > 0) ((expenses / income) * 100).toInt().coerceIn(0, 100) else 0

    fun dailyAverage(expenses: List<Expense>): Double {
        if (expenses.isEmpty()) return 0.0
        val days = expenses.map { it.timestamp / 86_400_000L }.distinct().size
        return expenses.sumOf { it.amount } / days.coerceAtLeast(1)
    }

    fun categoryColor(category: ExpenseCategory): Long = when (category) {
        ExpenseCategory.FOOD          -> 0xFFFF6B35
        ExpenseCategory.SHOPPING      -> 0xFFFF9900
        ExpenseCategory.TRANSPORT     -> 0xFF8B5CF6
        ExpenseCategory.GROCERIES     -> 0xFF2ECC71
        ExpenseCategory.ENTERTAINMENT -> 0xFFE50914
        ExpenseCategory.HEALTH        -> 0xFF00B4D8
        ExpenseCategory.UTILITIES     -> 0xFF60A5FA
        ExpenseCategory.OTHER         -> 0xFF94A3B8
    }

    class Factory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(repository) as T
        }
    }
}
