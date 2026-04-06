package com.gpaytracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ExpenseCategory {
    FOOD, SHOPPING, TRANSPORT, GROCERIES, ENTERTAINMENT, HEALTH, UTILITIES, OTHER
}

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchant: String,
    val amount: Double,
    val category: ExpenseCategory,
    val timestamp: Long = System.currentTimeMillis(),
    val notificationText: String = "",
    val upiId: String = "",
    val weekStart: Long = 0L  // epoch millis of the Monday this expense belongs to
)

@Entity(tableName = "income")
data class Income(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val source: String,
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val notificationText: String = "",
    val bankName: String = "",
    val weekStart: Long = 0L
)

@Entity(tableName = "weekly_summaries")
data class WeeklySummary(
    @PrimaryKey val weekStart: Long,           // Monday 00:00 epoch — unique per week
    val weekEnd: Long,                          // Sunday 23:59 epoch
    val totalIncome: Double,
    val totalExpenses: Double,
    val netSavings: Double,                     // income - expenses
    val savingsRate: Double,                    // netSavings / income * 100
    val topCategory: String,
    val topCategoryAmount: Double,
    val transactionCount: Int,
    val generatedAt: Long = System.currentTimeMillis(),
    val summaryText: String = ""               // human-readable AI-style insight string
)
