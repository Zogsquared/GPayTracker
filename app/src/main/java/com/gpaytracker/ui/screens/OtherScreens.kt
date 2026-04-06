package com.gpaytracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gpaytracker.data.Expense
import com.gpaytracker.data.ExpenseCategory
import com.gpaytracker.viewmodel.ExpenseViewModel

// ─── Transactions ───────────────────────────────────────────────────────────

@Composable
fun TransactionsScreen(viewModel: ExpenseViewModel) {
    val allExpenses by viewModel.allExpenses.collectAsState()
    var selectedCategory by remember { mutableStateOf<ExpenseCategory?>(null) }

    val filtered = if (selectedCategory == null) allExpenses
    else allExpenses.filter { it.category == selectedCategory }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        Text(
            "Transactions",
            color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp,
            modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 14.dp)
        )

        // Category filter chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 20.dp),
            modifier = Modifier.padding(bottom = 14.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFF6B35),
                        selectedLabelColor = Color.White,
                        labelColor = Color(0x80FFFFFF)
                    )
                )
            }
            items(ExpenseCategory.values().toList()) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                    label = { Text(cat.name.lowercase().replaceFirstChar(Char::titlecase)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFF6B35),
                        selectedLabelColor = Color.White,
                        labelColor = Color(0x80FFFFFF)
                    )
                )
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            if (filtered.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No transactions found.", color = Color(0x66FFFFFF), fontSize = 14.sp)
                    }
                }
            }
            items(filtered) { expense ->
                ExpenseRow(expense = expense, viewModel = viewModel)
            }
        }
    }
}

// ─── Analytics ──────────────────────────────────────────────────────────────

@Composable
fun AnalyticsScreen(viewModel: ExpenseViewModel) {
    val categoryTotals by viewModel.categoryTotals.collectAsState()
    val weeklyTotal by viewModel.weeklyTotal.collectAsState()

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A)),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Analytics",
                color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp
            )
        }

        // Category breakdown
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C2E)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        "SPENDING BY CATEGORY",
                        color = Color(0x80FFFFFF), fontSize = 11.sp,
                        letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 14.dp)
                    )

                    if (categoryTotals.isEmpty()) {
                        Text("No data yet.", color = Color(0x66FFFFFF), fontSize = 13.sp)
                    } else {
                        categoryTotals.sortedByDescending { it.total }.forEach { ct ->
                            val pct = if (weeklyTotal > 0) ((ct.total / weeklyTotal) * 100).toInt() else 0
                            val color = Color(viewModel.categoryColor(ct.category))

                            Column(Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        ct.category.name.lowercase().replaceFirstChar(Char::titlecase),
                                        color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("₹${formatAmount(ct.total)}", color = Color(0x80FFFFFF), fontSize = 13.sp)
                                        Text("$pct%", color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = pct / 100f,
                                    modifier = Modifier.fillMaxWidth().height(5.dp).background(Color(0x14FFFFFF), RoundedCornerShape(99.dp)),
                                    color = color,
                                    trackColor = Color(0x14FFFFFF)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Insight card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("💡  WEEKLY INSIGHT", color = Color(0xFF60A5FA), fontSize = 11.sp, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    val topCat = categoryTotals.maxByOrNull { it.total }
                    if (topCat != null) {
                        Text(
                            "Your biggest spend this week is ${topCat.category.name.lowercase()} " +
                            "(₹${formatAmount(topCat.total)}). Review if this is within expectations.",
                            color = Color.White, fontSize = 14.sp, lineHeight = 20.sp
                        )
                    } else {
                        Text("Make a few purchases with GPay to see insights here.", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

// ─── Settings ───────────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(viewModel: ExpenseViewModel) {
    val budget by viewModel.weeklyBudget.observeAsState(7000.0)
    var showBudgetDialog by remember { mutableStateOf(false) }
    var budgetInput by remember { mutableStateOf(budget.toLong().toString()) }
    var autoTrack by remember { mutableStateOf(true) }
    var autoCategorize by remember { mutableStateOf(true) }
    var weeklyReport by remember { mutableStateOf(true) }

    if (showBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text("Set Weekly Budget") },
            text = {
                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it.filter(Char::isDigit) },
                    label = { Text("Amount (₹)") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    budgetInput.toDoubleOrNull()?.let { viewModel.setWeeklyBudget(it) }
                    showBudgetDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) { Text("Cancel") }
            },
            containerColor = Color(0xFF1C1C2E),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A)),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text("Settings", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        }

        // Budget section
        item {
            SettingsSection("BUDGET") {
                SettingsRow(
                    label = "Weekly Budget",
                    subtitle = "Current: ₹${formatAmount(budget)}",
                    onClick = { showBudgetDialog = true }
                )
            }
        }

        // Notifications section
        item {
            SettingsSection("NOTIFICATIONS") {
                ToggleRow("GPay Auto-track", "Read payment notifications", autoTrack) { autoTrack = it }
                Divider(color = Color(0x14FFFFFF))
                ToggleRow("Auto-categorize", "AI merchant categorization", autoCategorize) { autoCategorize = it }
                Divider(color = Color(0x14FFFFFF))
                ToggleRow("Weekly Summary", "Report every Sunday", weeklyReport) { weeklyReport = it }
            }
        }

        // About section
        item {
            SettingsSection("ABOUT") {
                SettingsRow("Version", "GPay Tracker v1.0")
                Divider(color = Color(0x14FFFFFF))
                SettingsRow("Notification Access", "Tap to open system settings")
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, color = Color(0x66FFFFFF), fontSize = 11.sp, letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C2E)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsRow(label: String, subtitle: String, onClick: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, color = Color(0x66FFFFFF), fontSize = 11.sp)
        }
        if (onClick != null) {
            Text("›", color = Color(0x66FFFFFF), fontSize = 22.sp)
        }
    }
}

@Composable
fun ToggleRow(label: String, subtitle: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, color = Color(0x66FFFFFF), fontSize = 11.sp)
        }
        Switch(
            checked = value,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFFF6B35),
                uncheckedTrackColor = Color(0x33FFFFFF)
            )
        )
    }
}
