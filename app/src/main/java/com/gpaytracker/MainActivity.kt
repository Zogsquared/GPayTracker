package com.gpaytracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.gpaytracker.service.WeeklySummaryReceiver
import com.gpaytracker.ui.MainScreen
import com.gpaytracker.viewmodel.ExpenseViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ExpenseViewModel by viewModels {
        ExpenseViewModel.Factory((application as GPayTrackerApp).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WeeklySummaryReceiver.schedule(this)
        val startTab = intent.getStringExtra("open_tab") ?: "dashboard"
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF0D0D1A))) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0D0D1A)) {
                    MainScreen(viewModel = viewModel, startTab = startTab)
                }
            }
        }
    }
}
