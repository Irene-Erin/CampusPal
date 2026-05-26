package com.example.campuspal.ui.schedule

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class CourseReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "course_reminder_channel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val courseName = intent.getStringExtra("course_name") ?: return
        val courseLocation = intent.getStringExtra("course_location") ?: ""
        val courseId = intent.getLongExtra("course_id", 0)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 创建通知渠道
        val channel = NotificationChannel(
            CHANNEL_ID,
            "课程提醒",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "上课前提醒通知"
        }
        notificationManager.createNotificationChannel(channel)

        val text = if (courseLocation.isNotBlank()) {
            "即将上课：$courseName @ $courseLocation"
        } else {
            "即将上课：$courseName"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("课程提醒")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(courseId.toInt(), notification)
    }
}
