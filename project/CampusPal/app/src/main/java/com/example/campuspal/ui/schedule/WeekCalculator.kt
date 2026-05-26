package com.example.campuspal.ui.schedule

import com.example.campuspal.data.db.entity.Course
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Calendar

object WeekCalculator {

    // 根据学期开始日期（时间戳）计算当前是第几周
    fun calculateCurrentWeek(semesterStartMs: Long): Int {
        val startDate = LocalDate.ofEpochDay(semesterStartMs / (24 * 60 * 60 * 1000))
        val today = LocalDate.now()
        val daysBetween = ChronoUnit.DAYS.between(startDate, today)
        return ((daysBetween / 7) + 1).toInt().coerceIn(1, ScheduleConstants.DEFAULT_TOTAL_WEEKS)
    }

    // 判断某周是否为单周
    fun isOddWeek(week: Int) = week % 2 == 1

    // 判断课程在指定周是否显示
    fun isCourseVisible(course: Course, week: Int): Boolean {
        if (week < course.startWeek || week > course.endWeek) return false
        if (course.weekType == "odd" && !isOddWeek(week)) return false
        if (course.weekType == "even" && isOddWeek(week)) return false
        return true
    }

    // 获取今天是星期几 (1=周一, 7=周日)
    fun getTodayDayOfWeek(): Int {
        val day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        return when (day) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }

    // 获取指定日期是星期几 (1=周一, 7=周日)
    fun getDayOfWeek(dateMs: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = dateMs }
        val day = cal.get(Calendar.DAY_OF_WEEK)
        return when (day) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }

    // 获取本周一的日期时间戳
    fun getThisMondayMs(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    // 将时间字符串转为分钟数
    fun timeToMinutes(time: String): Int {
        val parts = time.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    // 检查两门课程时间段是否重叠
    fun isTimeOverlap(start1: String, end1: String, start2: String, end2: String): Boolean {
        val s1 = timeToMinutes(start1)
        val e1 = timeToMinutes(end1)
        val s2 = timeToMinutes(start2)
        val e2 = timeToMinutes(end2)
        return s1 < e2 && s2 < e1
    }
}
