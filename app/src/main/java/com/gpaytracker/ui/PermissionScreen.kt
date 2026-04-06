package com.gpaytracker.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PermissionGateScreen(onPermissionGranted: @Composable () -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(isNotificationPermissionGranted(context)) }

    // Re-check whenever composition runs (e.g. user returns from settings)
    LaunchedEffect(Unit) {
        hasPermission = isNotificationPermissionGranted(context)
    }

    if (hasPermission) {
        onPermissionGranted()
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("🔔", fontSize = 64.sp)
            Spacer(Modifier.height(24.dp))
            Text(
                "Notification Access Required",
                color = Color.White, fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "GPay Tracker needs permission to read notifications from Google Pay and your banking apps so it can automatically log transactions.",
                color = Color(0x99FFFFFF), fontSize = 14.sp,
                textAlign = TextAlign.Center, lineHeight = 22.sp
            )
            Spacer(Modifier.height(32.dp))

            // Step-by-step instructions card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C2E)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("How to grant access:", color = Color(0xFFFF6B35),
                        fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    StepRow("1", "Tap the button below")
                    StepRow("2", "Find \"GPay Tracker\" in the list")
                    StepRow("3", "Toggle it ON")
                    StepRow("4", "Come back to the app")
                }
            }

            Spacer(Modifier.height(28.dp))

            // Primary button
            Button(
                onClick = { openNotificationSettings(context) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B35)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Open Notification Settings", color = Color.White,
                    fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(Modifier.height(12.dp))

            // Secondary — re-check manually in case OS didn't return to app
            OutlinedButton(
                onClick = { hasPermission = isNotificationPermissionGranted(context) },
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("I've granted access — continue", color = Color(0x99FFFFFF), fontSize = 14.sp)
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Your data never leaves your device.",
                color = Color(0x55FFFFFF), fontSize = 11.sp
            )
        }
    }
}

@Composable
fun StepRow(number: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            color = Color(0x22FF6B35),
            shape = RoundedCornerShape(99.dp),
            modifier = Modifier.size(26.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(number, color = Color(0xFFFF6B35), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(text, color = Color(0xCCFFFFFF), fontSize = 13.sp)
    }
}

fun isNotificationPermissionGranted(context: Context): Boolean {
    val flat = Settings.Secure.getString(
        context.contentResolver, "enabled_notification_listeners"
    ) ?: return false
    return flat.split(":").any { it.contains(context.packageName) }
}

fun openNotificationSettings(context: Context) {
    val intents = listOf(
        Intent("android.settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS").apply {
            putExtra(
                "android.provider.extra.NOTIFICATION_LISTENER_COMPONENT_NAME",
                ComponentName(context.packageName,
                    "com.gpaytracker.service.GPayListenerService").flattenToString()
            )
        },
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
        Intent(Settings.ACTION_SPECIAL_APP_ACCESS_SETTINGS),
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", context.packageName, null)
        }
    )
    for (intent in intents) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return
        } catch (e: Exception) {
            continue
        }
    }
}
