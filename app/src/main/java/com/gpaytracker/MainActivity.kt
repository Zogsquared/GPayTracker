package com.gpaytracker

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
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

        if (!isNotificationServiceEnabled()) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

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

    private fun isNotificationServiceEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            ?: return false
        return flat.split(":").any { it.contains(packageName) }
    }
}
