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
    val upiId: String = ""
)
