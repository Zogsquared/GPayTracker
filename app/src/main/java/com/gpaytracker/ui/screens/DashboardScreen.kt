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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gpaytracker.data.Expense
import com.gpaytracker.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(viewModel: ExpenseViewModel) {
    val expenses by viewModel.weeklyExpenses.collectAsState()
    val total by viewModel.weeklyTotal.collectAsState()
    val budget by viewModel.weeklyBudget.observeAsState(7000.0)
    val percent = viewModel.budgetPercent(total, budget)
    val daily = viewModel.dailyAverage(expenses)
    val saved = (budget - total).coerceAtLeast(0.0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A)),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Header budget card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460))
                        ),
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
                    .padding(start = 24.dp, end = 24.dp, top = 48.dp, bottom = 28.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "THIS WEEK",
                                color = Color(0x80FFFFFF), fontSize = 11.sp,
                                letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "₹${formatAmount(total)}",
                                color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                "of ₹${formatAmount(budget)} budget",
                                color = Color(0x66FFFFFF), fontSize = 13.sp
                            )
                        }
                        // Circular progress indicator
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = percent / 100f,
                                modifier = Modifier.size(90.dp),
                                color = when {
                                    percent > 85 -> Color(0xFFFF4444)
                                    percent > 65 -> Color(0xFFFFB800)
                                    else -> Color(0xFF4ADE80)
                                },
                                trackColor = Color(0x14FFFFFF),
                                strokeWidth = 8.dp
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$percent%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("USED", color = Color(0x66FFFFFF), fontSize = 9.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Quick stats row
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(
                            Triple("Saved", "₹${formatAmount(saved)}", Color(0xFF4ADE80)),
                            Triple("Avg/day", "₹${formatAmount(daily)}", Color(0xFF60A5FA)),
                            Triple("Txns", "${expenses.size}", Color(0xFFF472B6))
                        ).forEach { (label, value, color) ->
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0x0FFFFFFF)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(label, color = Color(0x66FFFFFF), fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent transactions section
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("${expenses.size} this week", color = Color(0x66FFFFFF), fontSize = 12.sp)
            }
        }

        if (expenses.isEmpty()) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No transactions yet.\nPay with GPay and they'll appear here automatically.",
                        color = Color(0x66FFFFFF),
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        items(expenses.take(5)) { expense ->
            ExpenseRow(expense = expense, viewModel = viewModel)
        }
    }
}

@Composable
fun ExpenseRow(expense: Expense, viewModel: ExpenseViewModel) {
    val categoryColor = Color(viewModel.categoryColor(expense.category))
    val sdf = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C2E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category icon box
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(categoryColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(categoryEmoji(expense.category), fontSize = 20.sp)
            }

            Column(Modifier.weight(1f)) {
                Text(expense.merchant, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    sdf.format(Date(expense.timestamp)),
                    color = Color(0x66FFFFFF), fontSize = 11.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "−₹${formatAmount(expense.amount)}",
                    color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold, fontSize = 15.sp
                )
                Surface(
                    color = categoryColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(99.dp)
                ) {
                    Text(
                        expense.category.name.lowercase().replaceFirstChar(Char::titlecase),
                        color = categoryColor,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

fun formatAmount(amount: Double): String =
    if (amount == amount.toLong().toDouble()) "%,d".format(amount.toLong())
    else "%,.2f".format(amount)

fun categoryEmoji(category: com.gpaytracker.data.ExpenseCategory): String = when (category) {
    com.gpaytracker.data.ExpenseCategory.FOOD          -> "🍔"
    com.gpaytracker.data.ExpenseCategory.SHOPPING      -> "🛍️"
    com.gpaytracker.data.ExpenseCategory.TRANSPORT     -> "🚗"
    com.gpaytracker.data.ExpenseCategory.GROCERIES     -> "🥦"
    com.gpaytracker.data.ExpenseCategory.ENTERTAINMENT -> "🎬"
    com.gpaytracker.data.ExpenseCategory.HEALTH        -> "💊"
    com.gpaytracker.data.ExpenseCategory.UTILITIES     -> "💡"
    com.gpaytracker.data.ExpenseCategory.OTHER         -> "📦"
}
