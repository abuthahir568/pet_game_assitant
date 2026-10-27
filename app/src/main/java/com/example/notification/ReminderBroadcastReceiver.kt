package com.example.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

private const val CHANNEL_ID = "petpal_smart_reminders"
private const val EXTRA_TITLE = "extra_reminder_title"
private const val EXTRA_BODY = "extra_reminder_body"
private const val EXTRA_ID = "extra_reminder_id"

class ReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "PetPal AI Reminder"
        val body = intent.getStringExtra(EXTRA_BODY) ?: "Your virtual best friend saved this reminder for you!"
        val notificationId = intent.getIntExtra(EXTRA_ID, 1001)

        ensureNotificationChannel(context)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingTap = PendingIntent.getActivity(
            context,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingTap)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }
}

fun ensureNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "PetPal Smart Memories & Wellness Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Scheduled reminders saved by your PetPal AI best friend"
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}

object PetReminderScheduler {
    fun scheduleReminder(
        context: Context,
        reminderKey: String,
        title: String,
        body: String,
        triggerAtEpochMillis: Long
    ): Boolean {
        if (triggerAtEpochMillis <= System.currentTimeMillis()) return false
        return try {
            ensureNotificationChannel(context)
            val requestCode = reminderKey.hashCode()
            val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_BODY, body)
                putExtra(EXTRA_ID, requestCode)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            // Use inexact setAndAllowWhileIdle so no restricted SCHEDULE_EXACT_ALARM permission is needed
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtEpochMillis,
                pendingIntent
            )
            true
        } catch (e: Exception) {
            Log.e("PetReminderScheduler", "Failed to schedule reminder", e)
            false
        }
    }

    fun cancelReminder(context: Context, reminderKey: String) {
        try {
            val requestCode = reminderKey.hashCode()
            val intent = Intent(context, ReminderBroadcastReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.cancel(pendingIntent)
            }
        } catch (e: Exception) {
            Log.e("PetReminderScheduler", "Failed to cancel reminder", e)
        }
    }
}
