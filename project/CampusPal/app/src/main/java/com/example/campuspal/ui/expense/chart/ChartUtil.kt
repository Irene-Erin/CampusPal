package com.example.campuspal.ui.expense.chart

import java.util.*

fun getWeekRange(today: Long): Pair<Long, Long> {
    val cal = Calendar.getInstance().apply { timeInMillis = today }
    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    val start = startOfDay(cal.timeInMillis)
    val end = endOfDay(today)
    return start to end
}

fun getMonthRange(today: Long): Pair<Long, Long> {
    val cal = Calendar.getInstance().apply { timeInMillis = today }
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val start = startOfDay(cal.timeInMillis)
    val end = endOfDay(today)
    return start to end
}

fun getYearRange(today: Long): Pair<Long, Long> {
    val cal = Calendar.getInstance().apply { timeInMillis = today }
    cal.set(Calendar.MONTH, Calendar.JANUARY)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val start = startOfDay(cal.timeInMillis)
    val end = endOfDay(today)
    return start to end
}

fun getSemesterRange(semesterStart: Long, semesterEnd: Long, today: Long): Pair<Long, Long> {
    val end = if (semesterEnd > today) endOfDay(today) else endOfDay(semesterEnd)
    return startOfDay(semesterStart) to end
}

fun startOfDay(timestamp: Long): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

fun endOfDay(timestamp: Long): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
    cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
    return cal.timeInMillis
}

fun dayOfMonth(timestamp: Long): Int {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    return cal.get(Calendar.DAY_OF_MONTH)
}

fun monthLabel(timestamp: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    return "${cal.get(Calendar.MONTH) + 1}月"
}

fun weekDayLabel(dayOffset: Int): String = when (dayOffset) {
    0 -> "周一"; 1 -> "周二"; 2 -> "周三"; 3 -> "周四"; 4 -> "周五"; 5 -> "周六"; 6 -> "周日"
    else -> ""
}
