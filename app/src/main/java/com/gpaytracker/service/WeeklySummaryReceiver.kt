package com.gpaytracker.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.gpaytracker.GPayTrackerApp
import com.gpaytracker.MainActivity
import com.gpaytracker.R
import com.gpaytracker.data.WeeklySummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Fires every Sunday at 9 PM to generate and deliver the weekly summary notification.
 * Also re-schedules itself for the following Sunday.
 */
class WeeklySummaryReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "weekly_summary_channel"
        const val ACTION_WEEKLY_SUMMARY = "com.gpaytracker.WEEKLY_SUMMARY"
        const val ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED"

        fun schedule(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, WeeklySummaryReceiver::class.java).apply {
                    action = ACTION_WEEKLY_SUMMARY
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Next Sunday at 21:00
            val cal = Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.MONDAY
                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                set(Calendar.HOUR_OF_DAY, 21)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                // If it's already past Sunday 21:00 this week, schedule for next week
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.WEEK_OF_YEAR, 1)
                }
            }

            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                AlarmManager.INTERVAL_DAY * 7,
                intent
            )

            Log.d("WeeklySummaryReceiver", "Scheduled for ${cal.time}")
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_WEEKLY_SUMMARY, ACTION_BOOT_COMPLETED -> {
                if (intent.action == ACTION_BOOT_COMPLETED) {
                    schedule(context) // re-schedule after reboot
                    return
                }
                generateSummary(context)
            }
        }
    }

    private fun generateSummary(context: Context) {
        val app = context.applicationContext as GPayTrackerApp
        CoroutineScope(Dispatchers.IO).launch {
            // Summarise the week that just ended (previous Monday's weekStart)
            val repo = app.repository
            val weekStart = repo.currentWeekStart()

            // Avoid duplicate summaries
            if (repo.summaryExistsForWeek(weekStart)) {
                Log.d("WeeklySummaryReceiver", "Summary already exists for this week, skipping.")
                return@launch
            }

            val summary = repo.buildAndSaveSummary(weekStart)
            showSummaryNotification(context, summary)
        }
    }

    private fun showSummaryNotification(context: Context, summary: WeeklySummary) {
        createChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_tab", "summaries")
        }
        val pi = PendingIntent.getActivity(
            context, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val spent = "₹${summary.totalExpenses.toLong()}"
        val earned = "₹${summary.totalIncome.toLong()}"
        val saved = "₹${summary.netSavings.toLong()}"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_rupee)
            .setContentTitle("📊 Weekly Summary")
            .setContentText(summary.summaryText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Earned: $earned  |  Spent: $spent  |  Saved: $saved\n\n${summary.summaryText}"
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(2000, notification)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Weekly Summary",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "End-of-week financial summary" }
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }
}
