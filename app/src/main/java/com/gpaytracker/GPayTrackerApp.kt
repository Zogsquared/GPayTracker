package com.gpaytracker

import android.app.Application
import com.gpaytracker.data.ExpenseDatabase
import com.gpaytracker.data.ExpenseRepository

class GPayTrackerApp : Application() {
    val database by lazy { ExpenseDatabase.getDatabase(this) }
    val repository by lazy { ExpenseRepository(database.expenseDao()) }
}
