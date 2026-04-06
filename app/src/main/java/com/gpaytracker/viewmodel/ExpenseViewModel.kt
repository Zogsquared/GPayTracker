package com.gpaytracker.viewmodel

import androidx.lifecycle.*
import com.gpaytracker.data.CategoryTotal
import com.gpaytracker.data.Expense
import com.gpaytracker.data.ExpenseCategory
import com.gpaytracker.data.ExpenseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExpenseViewModel(private val repository: ExpenseRepository) : ViewModel() {

    /** Weekly budget in INR — user-configurable (default ₹7,000) */
    private val _weeklyBudget = MutableLiveData(7000.0)
    val weeklyBudget: LiveData<Double> = _weeklyBudget

    fun setWeeklyBudget(amount: Double) { _weeklyBudget.value = amount }

    val weeklyExpenses: StateFlow<List<Expense>> = repository
        .getWeeklyExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val weeklyTotal: StateFlow<Double> = repository
        .getWeeklyTotal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)
        .let { flow ->
            // Unwrap nullable Double
            kotlinx.coroutines.flow.combine(flow) { arr -> arr[0] ?: 0.0 }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)
        }

    val categoryTotals: StateFlow<List<CategoryTotal>> = repository
        .getWeeklyCategoryTotals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allExpenses: StateFlow<List<Expense>> = repository
        .getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch { repository.deleteExpense(expense) }
    }

    // --- Derived helpers ---

    fun budgetPercent(total: Double, budget: Double): Int =
        if (budget > 0) ((total / budget) * 100).toInt().coerceIn(0, 100) else 0

    fun dailyAverage(expenses: List<Expense>): Double {
        if (expenses.isEmpty()) return 0.0
        val days = expenses
            .map { java.util.concurrent.TimeUnit.MILLISECONDS.toDays(it.timestamp) }
            .distinct().size
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

    // Factory
    class Factory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ExpenseViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
