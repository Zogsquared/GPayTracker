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
import com.gpaytracker.data.Income
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GPayListenerService : NotificationListenerService() {

    private val TAG = "GPayListenerService"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val CHANNEL_ID = "gpay_tracker_channel"
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        createNotificationChannel()
        Log.d(TAG, "Notification listener connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val body  = extras.getCharSequence("android.text")?.toString() ?: ""

        when {
            // ── GPay expense ─────────────────────────────────────────────────
            pkg in GPayNotificationParser.GPAY_PACKAGES -> {
                val result = GPayNotificationParser.parse(title, body) ?: return
                val expense = Expense(
                    merchant = result.merchant,
                    amount = result.amount,
                    category = result.category,
                    notificationText = "$title $body",
                    upiId = result.upiId
                )
                serviceScope.launch {
                    val repo = (application as GPayTrackerApp).repository
                    repo.insertExpense(expense)
                    showExpenseNotification(expense)
                    Log.d(TAG, "Expense saved: ₹${expense.amount} → ${expense.merchant}")
                }
            }

            // ── Bank deposit / credit ────────────────────────────────────────
            pkg in BankingNotificationParser.BANKING_PACKAGES -> {
                val result = BankingNotificationParser.parse(pkg, title, body) ?: return
                val income = Income(
                    source = result.source,
                    amount = result.amount,
                    notificationText = "$title $body",
                    bankName = result.bankName
                )
                serviceScope.launch {
                    val repo = (application as GPayTrackerApp).repository
                    repo.insertIncome(income)
                    showIncomeNotification(income)
                    Log.d(TAG, "Income saved: ₹${income.amount} from ${income.source}")
                }
            }
        }
    }

    private fun showExpenseNotification(expense: Expense) {
        val pi = mainActivityIntent()
        val note = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_rupee)
            .setContentTitle("₹${expense.amount.toLong()} tracked ✓")
            .setContentText("${expense.merchant} · ${expense.category.name.lowercase().replaceFirstChar(Char::titlecase)}")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        notify(1000 + expense.id.toInt(), note)
    }

    private fun showIncomeNotification(income: Income) {
        val pi = mainActivityIntent()
        val note = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_rupee)
            .setContentTitle("₹${income.amount.toLong()} received 💰")
            .setContentText("${income.source} via ${income.bankName}")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        notify(2000 + income.id.toInt(), note)
    }

    private fun mainActivityIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun notify(id: Int, notification: android.app.Notification) {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(id, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "GPay Tracker", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Transaction tracking confirmations" }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }
}
