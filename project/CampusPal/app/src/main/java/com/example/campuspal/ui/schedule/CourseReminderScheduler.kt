package com.example.campuspal.ui.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.campuspal.data.db.entity.Course
import java.util.Calendar

object CourseReminderScheduler {

    private const val REMINDER_MINUTES_BEFORE = 10

    fun scheduleCourseReminder(context: Context, course: Course) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, CourseReminderReceiver::class.java).apply {
            putExtra("course_name", course.name)
            putExtra("course_location", course.location)
            putExtra("course_id", course.id)
        }

        // 为每个上课日设置提醒（简化：只处理每周的课程）
        val days = when (course.weekType) {
            "odd" -> getDaysInWeeks(course.startWeek, course.endWeek, oddOnly = true)
            "even" -> getDaysInWeeks(course.startWeek, course.endWeek, evenOnly = true)
            else -> getDaysInWeeks(course.startWeek, course.endWeek)
        }

        days.forEachIndexed { index, dayMs ->
            val (startHour, startMin) = course.startTime.split(":").map { it.toInt() }
            val cal = Calendar.getInstance().apply {
                timeInMillis = dayMs
                set(Calendar.HOUR_OF_DAY, startHour)
                set(Calendar.MINUTE, startMin - REMINDER_MINUTES_BEFORE)
                set(Calendar.SECOND, 0)
            }

            if (cal.timeInMillis > System.currentTimeMillis()) {
                val reqCode = (course.id * 10000 + index).toInt()
                val pi = PendingIntent.getBroadcast(
                    context, reqCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    cal.timeInMillis,
                    pi,
                )
            }
        }
    }

    fun cancelCourseReminder(context: Context, course: Course) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, CourseReminderReceiver::class.java)
        // Cancel up to 20 possible instances
        for (i in 0 until 20) {
            val reqCode = (course.id * 10000 + i).toInt()
            val pi = PendingIntent.getBroadcast(
                context, reqCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            alarmManager.cancel(pi)
        }
    }

    fun scheduleAllCourses(context: Context, courses: List<Course>, enabled: Boolean) {
        if (!enabled) {
            courses.forEach { cancelCourseReminder(context, it) }
            return
        }
        courses.forEach { scheduleCourseReminder(context, it) }
    }

    private fun getDaysInWeeks(startWeek: Int, endWeek: Int, oddOnly: Boolean = false, evenOnly: Boolean = false): List<Long> {
        // 假设学期开始日期从设置读取，这里用当前时间近似
        val semesterStart = Calendar.getInstance()
        semesterStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        semesterStart.set(Calendar.HOUR_OF_DAY, 0)
        semesterStart.set(Calendar.MINUTE, 0)
        semesterStart.add(Calendar.WEEK_OF_YEAR, startWeek - 1)

        val result = mutableListOf<Long>()
        for (week in startWeek..endWeek) {
            if (oddOnly && week % 2 == 0) continue
            if (evenOnly && week % 2 == 1) continue
            val cal = Calendar.getInstance().apply {
                timeInMillis = semesterStart.timeInMillis
                add(Calendar.WEEK_OF_YEAR, week - startWeek)
            }
            result.add(cal.timeInMillis)
        }
        return result
    }
}
