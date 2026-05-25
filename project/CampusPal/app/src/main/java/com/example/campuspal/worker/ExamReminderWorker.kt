package com.example.campuspal.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.example.campuspal.MainActivity
import com.example.campuspal.data.db.AppDatabase
import java.util.*
import java.util.concurrent.TimeUnit

class ExamReminderWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val examId = inputData.getLong("exam_id", -1)
        if (examId == -1L) return Result.failure()

        val db = AppDatabase.getInstance(applicationContext)
        val exam = db.examDao().getExamById(examId) ?: return Result.failure()

        val remaining = exam.examDate - System.currentTimeMillis()
        // 只在实际剩余时间在 25-35 分钟区间内时发送（容许调度偏差）
        if (remaining !in (25 * 60 * 1000L)..(35 * 60 * 1000L)) return Result.success()

        createNotificationChannel()
        showNotification(exam.name, exam.examDate)
        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "考试提醒", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "考试开始前 30 分钟提醒" }
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(examName: String, examDate: Long) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("考试提醒")
            .setContentText("「$examName」将在 ${sdf.format(java.util.Date(examDate))} 开始，还有约 30 分钟")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(applicationContext).notify(examDate.toInt(), notification)
        }
    }

    companion object {
        private const val CHANNEL_ID = "exam_reminder"

        fun schedule(context: Context, examId: Long, examDate: Long) {
            val remindAt = examDate - 30 * 60 * 1000L // 提前 30 分钟
            if (remindAt <= System.currentTimeMillis()) return // 已经过了提醒时间

            val delay = remindAt - System.currentTimeMillis()
            val request = OneTimeWorkRequestBuilder<ExamReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(Data.Builder().putLong("exam_id", examId).build())
                .addTag("exam_$examId")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "exam_reminder_$examId",
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        fun cancel(context: Context, examId: Long) {
            WorkManager.getInstance(context).cancelUniqueWork("exam_reminder_$examId")
        }
    }
}
