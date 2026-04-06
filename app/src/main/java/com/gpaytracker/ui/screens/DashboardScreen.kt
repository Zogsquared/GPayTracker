package com.gpaytracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gpaytracker.data.Expense
import com.gpaytracker.data.ExpenseCategory
import com.gpaytracker.data.Income
import com.gpaytracker.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(viewModel: ExpenseViewModel) {
    val expenses      by viewModel.weeklyExpenses.collectAsState()
    val expenseTotal  by viewModel.weeklyExpenseTotal.collectAsState()
    val incomeList    by viewModel.weeklyIncome.collectAsState()
    val incomeTotal   by viewModel.weeklyIncomeTotal.collectAsState()
    val budget        by viewModel.weeklyBudget.observeAsState(7000.0)

    val netSavings    = incomeTotal - expenseTotal
    val savingsRate   = if (incomeTotal > 0) (netSavings / incomeTotal * 100).toInt() else 0
    val budgetPct     = viewModel.budgetPercent(expenseTotal, budget)
    val incomePct     = viewModel.incomeSpentPercent(expenseTotal, incomeTotal)
    val daily         = viewModel.dailyAverage(expenses)

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0D0D1A)),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460))),
                        RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
                    .padding(start = 24.dp, end = 24.dp, top = 48.dp, bottom = 24.dp)
            ) {
                Column {
                    // Income / expense ring row
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Column {
                            Text("THIS WEEK", color = Color(0x80FFFFFF), fontSize = 11.sp, letterSpacing = 1.sp)
                            Text("₹${formatAmount(expenseTotal)}", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                            Text("spent", color = Color(0x66FFFFFF), fontSize = 12.sp)
                            Spacer(Modifier.height(6.dp))
                            Text("₹${formatAmount(incomeTotal)}", color = Color(0xFF4ADE80), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("earned", color = Color(0x66FFFFFF), fontSize = 12.sp)
                        }
                        // Ring shows income usage
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = incomePct / 100f,
                                modifier = Modifier.size(90.dp),
                                color = when {
                                    incomePct > 90 -> Color(0xFFFF4444)
                                    incomePct > 70 -> Color(0xFFFFB800)
                                    else -> Color(0xFF4ADE80)
                                },
                                trackColor = Color(0x14FFFFFF),
                                strokeWidth = 8.dp
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$incomePct%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                Text("of income", color = Color(0x66FFFFFF), fontSize = 8.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Savings bar
                    if (incomeTotal > 0) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Saved", color = Color(0x80FFFFFF), fontSize = 12.sp)
                            Text(
                                "₹${formatAmount(netSavings)} ($savingsRate%)",
                                color = if (netSavings >= 0) Color(0xFF4ADE80) else Color(0xFFFF4444),
                                fontSize = 12.sp, fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = (savingsRate.coerceIn(0, 100)) / 100f,
                            modifier = Modifier.fillMaxWidth().height(5.dp),
                            color = if (netSavings >= 0) Color(0xFF4ADE80) else Color(0xFFFF4444),
                            trackColor = Color(0x14FFFFFF)
                        )
                        Spacer(Modifier.height(14.dp))
                    }

                    // Quick stats
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(
                            Triple("Budget left", "₹${formatAmount((budget - expenseTotal).coerceAtLeast(0.0))}", Color(0xFF60A5FA)),
                            Triple("Avg/day", "₹${formatAmount(daily)}", Color(0xFFF472B6)),
                            Triple("Txns", "${expenses.size}", Color(0xFFFFB800))
                        ).forEach { (label, value, color) ->
                            Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0x0FFFFFFF)), shape = RoundedCornerShape(12.dp)) {
                                Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(label, color = Color(0x66FFFFFF), fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent income entries
        if (incomeList.isNotEmpty()) {
            item {
                Text("Income this week", color = Color(0x80FFFFFF), fontSize = 12.sp,
                    modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 8.dp))
            }
            items(incomeList.take(3)) { income -> IncomeRow(income) }
        }

        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Recent expenses", color = Color(0x80FFFFFF), fontSize = 12.sp)
                Text("${expenses.size} this week", color = Color(0x66FFFFFF), fontSize = 11.sp)
            }
        }

        if (expenses.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No expenses yet.\nPay with GPay and they'll appear here automatically.",
                        color = Color(0x66FFFFFF), fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            }
        }
        items(expenses.take(5)) { ExpenseRow(it, viewModel) }
    }
}

@Composable
fun IncomeRow(income: Income) {
    val sdf = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2E1C)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Color(0x334ADE80)), contentAlignment = Alignment.Center) {
                Text("💰", fontSize = 18.sp)
            }
            Column(Modifier.weight(1f)) {
                Text(income.source, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("${income.bankName} · ${sdf.format(Date(income.timestamp))}", color = Color(0x66FFFFFF), fontSize = 11.sp)
            }
            Text("+₹${formatAmount(income.amount)}", color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun ExpenseRow(expense: Expense, viewModel: ExpenseViewModel) {
    val color = Color(viewModel.categoryColor(expense.category))
    val sdf = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C2E)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Text(categoryEmoji(expense.category), fontSize = 18.sp)
            }
            Column(Modifier.weight(1f)) {
                Text(expense.merchant, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(sdf.format(Date(expense.timestamp)), color = Color(0x66FFFFFF), fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("−₹${formatAmount(expense.amount)}", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(99.dp)) {
                    Text(expense.category.name.lowercase().replaceFirstChar(Char::titlecase),
                        color = color, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
        }
    }
}

fun formatAmount(amount: Double): String =
    if (amount == amount.toLong().toDouble()) "%,d".format(amount.toLong())
    else "%,.2f".format(amount)

fun categoryEmoji(category: ExpenseCategory): String = when (category) {
    ExpenseCategory.FOOD          -> "🍔"
    ExpenseCategory.SHOPPING      -> "🛍️"
    ExpenseCategory.TRANSPORT     -> "🚗"
    ExpenseCategory.GROCERIES     -> "🥦"
    ExpenseCategory.ENTERTAINMENT -> "🎬"
    ExpenseCategory.HEALTH        -> "💊"
    ExpenseCategory.UTILITIES     -> "💡"
    ExpenseCategory.OTHER         -> "📦"
}
