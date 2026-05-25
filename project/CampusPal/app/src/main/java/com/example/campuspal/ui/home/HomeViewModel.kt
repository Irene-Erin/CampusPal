package com.example.campuspal.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.campuspal.data.db.dao.CourseDao
import com.example.campuspal.data.db.dao.ExpenseDao
import com.example.campuspal.data.db.dao.TaskDao
import com.example.campuspal.data.db.entity.Course
import com.example.campuspal.data.db.entity.Expense
import com.example.campuspal.data.db.entity.Task
import com.example.campuspal.data.datastore.SettingsDataStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class HomeUiState(
    val todayCourses: List<Course> = emptyList(),
    val urgentTasks: List<Task> = emptyList(),
    val todayExpenses: List<Expense> = emptyList(),
    val todayTotalExpense: Double = 0.0,
    val monthlyBudget: Double = 2000.0,
    val monthlyTotalExpense: Double = 0.0,
    val currentWeek: Int = 1,
    val greeting: String = "",
    val todayDate: String = "",
    val nextCourseMinutes: Int? = null,
    val isRefreshing: Boolean = false,
)

class HomeViewModel(
    private val courseDao: CourseDao,
    private val taskDao: TaskDao,
    private val expenseDao: ExpenseDao,
    private val settingsDataStore: SettingsDataStore,
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)

    // 计算当前是第几周（简化：从学期开始算）
    private val currentWeek: Int
        get() {
            // 假设学期从2024年2月26日开始，可根据实际情况调整
            val semesterStart = Calendar.getInstance().apply {
                set(2024, Calendar.FEBRUARY, 26)
            }
            val now = Calendar.getInstance()
            val diff = now.get(Calendar.WEEK_OF_YEAR) - semesterStart.get(Calendar.WEEK_OF_YEAR) + 1
            return diff.coerceIn(1, 20)
        }

    private val todayDayOfWeek: Int
        get() {
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

    private val todayStart: Long
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

    private val todayEnd: Long
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            return cal.timeInMillis
        }

    private val monthStart: Long
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

    private val monthEnd: Long
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            return cal.timeInMillis
        }

    private val week = currentWeek
    private val dayOfWeek = todayDayOfWeek

    val uiState: StateFlow<HomeUiState> = combine(
        courseDao.getCoursesForDayAndWeek(dayOfWeek, week),
        taskDao.getUrgentTasks(Date()),
        expenseDao.getExpensesByMonth(todayStart, todayEnd),
        expenseDao.getTotalExpenseBetween(todayStart, todayEnd),
        expenseDao.getTotalExpenseBetween(monthStart, monthEnd),
    ) { courses, tasks, expenses, todayExp, monthExp ->
        mutableListOf<Any?>(courses, tasks, expenses, todayExp, monthExp)
    }.combine(settingsDataStore.monthlyBudget) { list, budget ->
        list.apply { add(budget) }
    }.combine(_isRefreshing) { list, refreshing ->
        val courses = list[0] as List<Course>
        val tasks = list[1] as List<Task>
        val expenses = list[2] as List<Expense>
        val todayExp = list[3] as Double?
        val monthExp = list[4] as Double?
        val budget = list[5] as Double

        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        // 计算下一节课倒计时
        var nextCourseMin: Int? = null
        val sortedCourses = courses.sortedBy { it.startTime }
        for (course in sortedCourses) {
            val startMin = timeToMinutes(course.startTime)
            if (startMin > currentMinutes) {
                nextCourseMin = startMin - currentMinutes
                break
            }
            val endMin = timeToMinutes(course.endTime)
            if (currentMinutes in timeToMinutes(course.startTime)..endMin) {
                nextCourseMin = 0 // 正在上课
                break
            }
        }

        // 问候语
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 6 -> "夜深了，早点休息"
            hour < 9 -> "早上好！新的一天开始了"
            hour < 12 -> "上午好！精力充沛地学习吧"
            hour < 14 -> "中午好！别忘了午休"
            hour < 18 -> "下午好！继续保持专注"
            hour < 22 -> "晚上好！回顾今天的收获"
            else -> "夜深了，早点休息"
        }

        // 日期
        val dateStr = java.text.SimpleDateFormat("M月d日 EEEE", java.util.Locale.CHINESE).format(Date())

        HomeUiState(
            todayCourses = sortedCourses,
            urgentTasks = tasks,
            todayExpenses = expenses,
            todayTotalExpense = todayExp ?: 0.0,
            monthlyBudget = budget,
            monthlyTotalExpense = monthExp ?: 0.0,
            currentWeek = week,
            greeting = greeting,
            todayDate = dateStr,
            nextCourseMinutes = nextCourseMin,
            isRefreshing = refreshing,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun refresh() {
        _isRefreshing.value = true
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            _isRefreshing.value = false
        }
    }

    class Factory(
        private val courseDao: CourseDao,
        private val taskDao: TaskDao,
        private val expenseDao: ExpenseDao,
        private val settingsDataStore: SettingsDataStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(courseDao, taskDao, expenseDao, settingsDataStore) as T
        }
    }
}

private fun timeToMinutes(time: String): Int {
    val parts = time.split(":")
    return parts[0].toInt() * 60 + parts[1].toInt()
}
