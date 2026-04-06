package com.gpaytracker.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.gpaytracker.GPayTrackerApp
import com.gpaytracker.MainActivity
import com.gpaytracker.R
import com.gpaytracker.data.Expense
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GPayListenerService : NotificationListenerService() {

    private val TAG = "GPayListenerService"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val CHANNEL_ID = "gpay_tracker_channel"
        const val NOTIFICATION_ID = 1001
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected")
        createNotificationChannel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Only process Google Pay notifications
        if (sbn.packageName !in GPayNotificationParser.GPAY_PACKAGES) return

        val notification = sbn.notification
        val extras = notification.extras

        val title = extras.getString("android.title") ?: ""
        val body = extras.getCharSequence("android.text")?.toString() ?: ""

        Log.d(TAG, "GPay notification — title: $title | body: $body")

        val result = GPayNotificationParser.parse(title, body) ?: run {
            Log.d(TAG, "Not a payment notification, skipping.")
            return
        }

        val expense = Expense(
            merchant = result.merchant,
            amount = result.amount,
            category = result.category,
            notificationText = "$title $body",
            upiId = result.upiId
        )

        serviceScope.launch {
            val app = application as GPayTrackerApp
            val inserted = app.repository.insertExpense(expense)
            Log.d(TAG, "Saved expense #$inserted: ₹${expense.amount} → ${expense.merchant} (${expense.category})")

            // Show confirmation notification to user
            showConfirmationNotification(expense)
        }
    }

    private fun showConfirmationNotification(expense: Expense) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_rupee)
            .setContentTitle("₹${formatAmount(expense.amount)} tracked ✓")
            .setContentText("${expense.merchant} · ${expense.category.name.lowercase().replaceFirstChar(Char::titlecase)}")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID + expense.id.toInt(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GPay Tracker",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Confirms when a GPay transaction is tracked"
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun formatAmount(amount: Double): String =
        if (amount == amount.toLong().toDouble()) amount.toLong().toString()
        else "%.2f".format(amount)
}
