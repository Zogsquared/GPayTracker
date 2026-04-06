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
import androidx.compose.ui.Modifier
import com.gpaytracker.ui.MainScreen
import com.gpaytracker.ui.theme.GPayTrackerTheme
import com.gpaytracker.viewmodel.ExpenseViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ExpenseViewModel by viewModels {
        ExpenseViewModel.Factory((application as GPayTrackerApp).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification listener permission if not granted
        if (!isNotificationServiceEnabled()) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        setContent {
            GPayTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":").toTypedArray()
            for (name in names) {
                if (name.contains(packageName)) return true
            }
        }
        return false
    }
}
