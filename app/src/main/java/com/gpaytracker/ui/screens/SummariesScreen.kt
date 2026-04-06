package com.gpaytracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gpaytracker.data.WeeklySummary
import com.gpaytracker.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SummariesScreen(viewModel: ExpenseViewModel) {
    val summaries by viewModel.weeklySummaries.collectAsState()

    Column(
        Modifier.fillMaxSize().background(Color(0xFF0D0D1A))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Weekly History", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            // Manual trigger for testing (generates summary for current week)
            OutlinedButton(
                onClick = { viewModel.generateWeeklySummary() },
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FF6B35)),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Generate", color = Color(0xFFFF6B35), fontSize = 11.sp)
            }
        }

        Text(
            "Summaries are auto-generated every Sunday at 9 PM",
            color = Color(0x55FFFFFF), fontSize = 11.sp,
            modifier = Modifier.padding(start = 20.dp, bottom = 14.dp)
        )

        if (summaries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📊", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("No summaries yet", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Your first summary will appear\nhere after Sunday at 9 PM",
                        color = Color(0x66FFFFFF), fontSize = 13.sp, textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(summaries) { summary ->
                    WeeklySummaryCard(summary)
                }
            }
        }
    }
}

@Composable
fun WeeklySummaryCard(summary: WeeklySummary) {
    val sdf = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    val weekLabel = "${sdf.format(Date(summary.weekStart))} – ${sdf.format(Date(summary.weekEnd))}"
    val savings = summary.netSavings
    val savingsColor = if (savings >= 0) Color(0xFF4ADE80) else Color(0xFFFF4444)
    val rate = summary.savingsRate.toInt()

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C2E)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(18.dp)) {

            // Week label + savings rate badge
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(weekLabel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Surface(
                    color = savingsColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(99.dp)
                ) {
                    Text(
                        "${if (rate >= 0) "+" else ""}$rate% saved",
                        color = savingsColor, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Income / Expenses / Saved row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryStatBox("Earned", "₹${formatAmount(summary.totalIncome)}", Color(0xFF4ADE80), Modifier.weight(1f))
                SummaryStatBox("Spent", "₹${formatAmount(summary.totalExpenses)}", Color(0xFFFF6B6B), Modifier.weight(1f))
                SummaryStatBox("Saved", "₹${formatAmount(savings)}", savingsColor, Modifier.weight(1f))
            }

            Spacer(Modifier.height(14.dp))

            // Savings progress bar
            if (summary.totalIncome > 0) {
                val spentPct = ((summary.totalExpenses / summary.totalIncome) * 100).toInt().coerceIn(0, 100)
                LinearProgressIndicator(
                    progress = spentPct / 100f,
                    modifier = Modifier.fillMaxWidth().height(5.dp),
                    color = when {
                        spentPct > 90 -> Color(0xFFFF4444)
                        spentPct > 70 -> Color(0xFFFFB800)
                        else -> Color(0xFF4ADE80)
                    },
                    trackColor = Color(0x14FFFFFF)
                )
                Spacer(Modifier.height(4.dp))
                Text("${spentPct}% of income spent · ${summary.transactionCount} transactions",
                    color = Color(0x66FFFFFF), fontSize = 11.sp)
            }

            // Top category
            if (summary.topCategoryAmount > 0) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Top spend", color = Color(0x66FFFFFF), fontSize = 12.sp)
                    Text(
                        "${summary.topCategory.lowercase().replaceFirstChar(Char::titlecase)} · ₹${formatAmount(summary.topCategoryAmount)}",
                        color = Color(0xFFFFB800), fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Insight text
            if (summary.summaryText.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Divider(color = Color(0x14FFFFFF))
                Spacer(Modifier.height(10.dp))
                Text(summary.summaryText, color = Color(0xCCFFFFFF), fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
fun SummaryStatBox(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0x0AFFFFFF)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
            Text(label, color = Color(0x66FFFFFF), fontSize = 10.sp)
        }
    }
}
