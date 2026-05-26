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

fun getWeekDayLabels(today: Long): List<String> {
    val todayCal = Calendar.getInstance().apply { timeInMillis = today }
    val todayDay = todayCal.get(Calendar.DAY_OF_WEEK)
    val todayOffset = when (todayDay) {
        Calendar.MONDAY -> 0; Calendar.TUESDAY -> 1; Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3; Calendar.FRIDAY -> 4; Calendar.SATURDAY -> 5
        Calendar.SUNDAY -> 6; else -> 0
    }
    return listOf("周一", "周二", "周三", "周四", "周五", "周六", "日").subList(0, todayOffset + 1)
}

fun getDayLabels(startMs: Long, endMs: Long): List<String> {
    val startCal = Calendar.getInstance().apply { timeInMillis = startMs }
    val endCal = Calendar.getInstance().apply { timeInMillis = endMs }
    val startDay = startCal.get(Calendar.DAY_OF_MONTH)
    val endDay = endCal.get(Calendar.DAY_OF_MONTH)
    return (startDay..endDay).map { "$it" }
}

fun getMonthLabelsInRange(startMs: Long, endMs: Long): List<String> {
    val labels = mutableListOf<String>()
    val cal = Calendar.getInstance().apply { timeInMillis = startMs }
    val endCal = Calendar.getInstance().apply { timeInMillis = endMs }
    while (cal.get(Calendar.YEAR) < endCal.get(Calendar.YEAR) ||
        (cal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR) && cal.get(Calendar.MONTH) <= endCal.get(Calendar.MONTH))) {
        labels.add("${cal.get(Calendar.MONTH) + 1}月")
        cal.add(Calendar.MONTH, 1)
    }
    return labels
}
