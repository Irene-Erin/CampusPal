package com.example.campuspal.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.campuspal.data.db.dao.CategorySum
import com.example.campuspal.data.db.dao.ExpenseDao
import com.example.campuspal.data.db.dao.MonthlyBreakdown
import com.example.campuspal.data.db.entity.Expense
import com.example.campuspal.data.datastore.SettingsDataStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

enum class ExpenseView { DAY, WEEK, MONTH, SEMESTER }

data class ExpenseUiState(
    val expenses: List<Expense> = emptyList(),
    val categorySums: List<CategorySum> = emptyList(),
    val totalExpense: Double = 0.0,
    val totalIncome: Double = 0.0,
    val monthlyBudget: Double = 2000.0,
    val currentView: ExpenseView = ExpenseView.MONTH,
    val isRefreshing: Boolean = false,
    val selectedDate: Long = System.currentTimeMillis(),
    val semesterStart: Long = 0L,
    val semesterEnd: Long = 0L,
    val monthlyBreakdowns: List<MonthlyBreakdown> = emptyList(),
    val semesterCategorySums: List<CategorySum> = emptyList(),
    val semesterTotalExpense: Double = 0.0,
    val semesterTotalIncome: Double = 0.0,
    val editingExpense: Expense? = null,
)

class ExpenseViewModel(
    private val expenseDao: ExpenseDao,
    private val settingsDataStore: SettingsDataStore,
) : ViewModel() {

    private val _currentView = MutableStateFlow(ExpenseView.MONTH)
    private val _showAddSheet = MutableStateFlow(false)
    val showAddSheet: StateFlow<Boolean> = _showAddSheet.asStateFlow()
    private val _isRefreshing = MutableStateFlow(false)
    private val _selectedDate = MutableStateFlow(getTodayMidnight())
    private val _editingExpense = MutableStateFlow<Expense?>(null)

    private fun startOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun endOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    private fun startOfWeek(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        return startOfDay(cal.timeInMillis)
    }

    private fun endOfWeek(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        return endOfDay(cal.timeInMillis)
    }

    private fun startOfMonth(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return startOfDay(cal.timeInMillis)
    }

    private fun endOfMonth(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        return endOfDay(cal.timeInMillis)
    }

    private fun getTodayMidnight(): Long = startOfDay(System.currentTimeMillis())

    // 学期范围 (从 DataStore 读取，缓存为 StateFlow)
    private val semesterRange: StateFlow<Pair<Long, Long>> = combine(
        settingsDataStore.semesterStart,
        settingsDataStore.semesterEnd,
    ) { start, end -> start to end }.stateIn(viewModelScope, SharingStarted.Eagerly, 0L to 0L)

    // 根据视图计算时间范围 (不阻塞线程)
    private data class TimeRange(val start: Long, val end: Long)

    private val activeRange: StateFlow<TimeRange> = combine(
        _currentView, _selectedDate, semesterRange,
    ) { view, date, (semStart, semEnd) ->
        when (view) {
            ExpenseView.DAY -> TimeRange(startOfDay(date), endOfDay(date))
            ExpenseView.WEEK -> TimeRange(startOfWeek(date), endOfWeek(date))
            ExpenseView.MONTH -> TimeRange(startOfMonth(date), endOfMonth(date))
            ExpenseView.SEMESTER -> TimeRange(semStart, semEnd)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, TimeRange(getTodayMidnight(), getTodayMidnight()))

    // 支出/分类数据
    private val rangeData: StateFlow<Triple<List<Expense>, List<CategorySum>, Double?>> =
        activeRange.flatMapLatest { (start, end) ->
            combine(
                expenseDao.getExpensesByMonth(start, end),
                expenseDao.getCategorySumBetween(start, end),
                expenseDao.getTotalExpenseBetween(start, end),
            ) { exps, cats, total -> Triple(exps, cats, total) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Triple(emptyList(), emptyList(), null))

    // 收入数据
    private val rangeIncome: StateFlow<Double?> =
        activeRange.flatMapLatest { (start, end) ->
            expenseDao.getTotalIncomeBetween(start, end)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 学期月度柱状图数据
    private val monthBreakdowns: StateFlow<List<MonthlyBreakdown>> = combine(
        _currentView, semesterRange,
    ) { view, (semStart, semEnd) -> view to (semStart to semEnd) }
        .flatMapLatest { (view, range) ->
            if (view != ExpenseView.SEMESTER) flowOf(emptyList())
            else flow {
                val (start, end) = range
                val result = mutableListOf<MonthlyBreakdown>()
                val cal = Calendar.getInstance().apply { timeInMillis = start }
                while (cal.timeInMillis <= end) {
                    val ms = startOfMonth(cal.timeInMillis)
                    val me = endOfMonth(cal.timeInMillis)
                    val expTotal = withContext(Dispatchers.IO) { expenseDao.getRawTotalExpense(ms, me) } ?: 0.0
                    val incTotal = withContext(Dispatchers.IO) { expenseDao.getRawTotalIncome(ms, me) } ?: 0.0
                    val label = SimpleDateFormat("M月", Locale.getDefault()).format(Date(ms))
                    if (expTotal > 0 || incTotal > 0) {
                        result.add(MonthlyBreakdown(label, expTotal, incTotal))
                    }
                    cal.add(Calendar.MONTH, 1)
                }
                emit(result)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 学期分类汇总
    private val semesterCats: StateFlow<List<CategorySum>> = combine(
        _currentView, semesterRange,
    ) { view, (semStart, semEnd) -> view to (semStart to semEnd) }
        .flatMapLatest { (view, range) ->
            if (view != ExpenseView.SEMESTER) flowOf(emptyList())
            else {
                val (start, end) = range
                expenseDao.getCategorySumBetween(start, end)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<ExpenseUiState> = combine(
        rangeData, rangeIncome, settingsDataStore.monthlyBudget, _currentView, _isRefreshing,
    ) { data, income, budget, view, refreshing ->
        mutableListOf<Any?>(data, income, budget, view, refreshing)
    }.combine(_selectedDate) { acc, date -> acc.apply { add(date) } }
        .combine(settingsDataStore.semesterStart) { acc, semStart -> acc.apply { add(semStart) } }
        .combine(settingsDataStore.semesterEnd) { acc, semEnd -> acc.apply { add(semEnd) } }
        .combine(monthBreakdowns) { acc, bds -> acc.apply { add(bds) } }
        .combine(semesterCats) { acc, semCats -> acc.apply { add(semCats) } }
        .combine(_editingExpense) { acc, editing ->
            val data = acc[0] as Triple<List<Expense>, List<CategorySum>, Double?>
            val income = acc[1] as Double?
            val budget = acc[2] as Double
            val view = acc[3] as ExpenseView
            val refreshing = acc[4] as Boolean
            val date = acc[5] as Long
            val semStart = acc[6] as Long
            val semEnd = acc[7] as Long
            val bds = acc[8] as List<MonthlyBreakdown>
            val semCats = acc[9] as List<CategorySum>
            // semesterCats 已包含学期总支出，收入使用学期范围计算
            val semTotalInc = if (view == ExpenseView.SEMESTER) income ?: 0.0 else 0.0
            ExpenseUiState(
                expenses = data.first,
                categorySums = data.second,
                totalExpense = data.third ?: 0.0,
                totalIncome = income ?: 0.0,
                monthlyBudget = budget,
                currentView = view,
                isRefreshing = refreshing,
                selectedDate = date,
                semesterStart = semStart,
                semesterEnd = semEnd,
                monthlyBreakdowns = bds,
                semesterCategorySums = semCats,
                semesterTotalExpense = semCats.sumOf { it.total },
                semesterTotalIncome = semTotalInc,
                editingExpense = editing,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExpenseUiState())

    fun setView(view: ExpenseView) { _currentView.value = view }

    fun selectedDateLabel(): String {
        val cal = Calendar.getInstance().apply { timeInMillis = _selectedDate.value }
        return when (_currentView.value) {
            ExpenseView.DAY -> SimpleDateFormat("M月d日 EEEE", Locale.CHINESE).format(Date(_selectedDate.value))
            ExpenseView.WEEK -> {
                val ms = startOfWeek(_selectedDate.value)
                val me = endOfWeek(_selectedDate.value)
                SimpleDateFormat("M月d日", Locale.getDefault()).format(Date(ms)) + "-" +
                SimpleDateFormat("M月d日", Locale.getDefault()).format(Date(me))
            }
            ExpenseView.MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                SimpleDateFormat("yyyy年M月", Locale.CHINESE).format(cal.time)
            }
            ExpenseView.SEMESTER -> "学期"
        }
    }

    fun goPrev() {
        val cal = Calendar.getInstance().apply { timeInMillis = _selectedDate.value }
        when (_currentView.value) {
            ExpenseView.DAY -> cal.add(Calendar.DAY_OF_MONTH, -1)
            ExpenseView.WEEK -> cal.add(Calendar.WEEK_OF_YEAR, -1)
            ExpenseView.MONTH -> cal.add(Calendar.MONTH, -1)
            ExpenseView.SEMESTER -> return
        }
        _selectedDate.value = cal.timeInMillis
    }

    fun goNext() {
        val cal = Calendar.getInstance().apply { timeInMillis = _selectedDate.value }
        when (_currentView.value) {
            ExpenseView.DAY -> cal.add(Calendar.DAY_OF_MONTH, 1)
            ExpenseView.WEEK -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            ExpenseView.MONTH -> cal.add(Calendar.MONTH, 1)
            ExpenseView.SEMESTER -> return
        }
        _selectedDate.value = cal.timeInMillis
    }

    fun setSelectedDate(timestamp: Long) { _selectedDate.value = startOfDay(timestamp) }

    fun showAddSheet() { _showAddSheet.value = true; _editingExpense.value = null }
    fun hideAddSheet() { _showAddSheet.value = false; _editingExpense.value = null }
    fun editExpense(expense: Expense) { _editingExpense.value = expense; _showAddSheet.value = true }

    fun refresh() {
        _isRefreshing.value = true
        viewModelScope.launch { delay(300); _isRefreshing.value = false }
    }

    fun addExpense(expense: Expense) {
        viewModelScope.launch {
            if (expense.id != 0L) expenseDao.update(expense)
            else expenseDao.insert(expense)
            _showAddSheet.value = false; _editingExpense.value = null
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch { expenseDao.delete(expense) }
    }

    fun setBudget(amount: Double) {
        viewModelScope.launch { settingsDataStore.setMonthlyBudget(amount) }
    }

    class Factory(
        private val expenseDao: ExpenseDao,
        private val settingsDataStore: SettingsDataStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ExpenseViewModel(expenseDao, settingsDataStore) as T
        }
    }
}
