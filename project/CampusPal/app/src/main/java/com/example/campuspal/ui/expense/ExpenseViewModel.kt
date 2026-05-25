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
    // 日视图
    val selectedDate: Long = System.currentTimeMillis(),
    // 学期视图
    val semesterStart: Long = 0L,
    val semesterEnd: Long = 0L,
    val monthlyBreakdowns: List<MonthlyBreakdown> = emptyList(),
    val semesterCategorySums: List<CategorySum> = emptyList(),
    val semesterTotalExpense: Double = 0.0,
    val semesterTotalIncome: Double = 0.0,
    // 编辑
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

    // 根据当前视图计算时间范围
    private val currentRange: Pair<Long, Long>
        get() {
            val t = _selectedDate.value
            return when (_currentView.value) {
                ExpenseView.DAY -> startOfDay(t) to endOfDay(t)
                ExpenseView.WEEK -> startOfWeek(t) to endOfWeek(t)
                ExpenseView.MONTH -> startOfMonth(t) to endOfMonth(t)
                ExpenseView.SEMESTER -> {
                    // 学期范围从 DataStore 读取，不依赖 selectedDate
                    runBlocking {
                        settingsDataStore.semesterStart.first() to settingsDataStore.semesterEnd.first()
                    }
                }
            }
        }

    // 数据层 combine（月/周/日视图共享此逻辑，仅时间范围不同）
    private val rangeData: StateFlow<Triple<List<Expense>, List<CategorySum>, Double?>> = combine(
        _currentView, _selectedDate,
    ) { view, date -> view to date }.flatMapLatest { (view, date) ->
        val (start, end) = when (view) {
            ExpenseView.DAY -> startOfDay(date) to endOfDay(date)
            ExpenseView.WEEK -> startOfWeek(date) to endOfWeek(date)
            ExpenseView.MONTH -> startOfMonth(date) to endOfMonth(date)
            ExpenseView.SEMESTER -> runBlocking {
                settingsDataStore.semesterStart.first() to settingsDataStore.semesterEnd.first()
            }
        }
        combine(
            expenseDao.getExpensesByMonth(start, end),
            expenseDao.getCategorySumBetween(start, end),
            expenseDao.getTotalExpenseBetween(start, end),
        ) { exps, cats, total -> Triple(exps, cats, total) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Triple(emptyList(), emptyList(), null))

    private val rangeIncome: StateFlow<Double?> = combine(_currentView, _selectedDate) { view, date -> view to date }
        .flatMapLatest { (view, date) ->
            val (start, end) = when (view) {
                ExpenseView.DAY -> startOfDay(date) to endOfDay(date)
                ExpenseView.WEEK -> startOfWeek(date) to endOfWeek(date)
                ExpenseView.MONTH -> startOfMonth(date) to endOfMonth(date)
                ExpenseView.SEMESTER -> runBlocking {
                    settingsDataStore.semesterStart.first() to settingsDataStore.semesterEnd.first()
                }
            }
            expenseDao.getTotalIncomeBetween(start, end)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 学期月度柱状图数据
    private val monthBreakdowns: StateFlow<List<MonthlyBreakdown>> = _currentView
        .flatMapLatest { view ->
            if (view != ExpenseView.SEMESTER) flowOf(emptyList())
            else flow {
                val (start, end) = runBlocking {
                    settingsDataStore.semesterStart.first() to settingsDataStore.semesterEnd.first()
                }
                val result = mutableListOf<MonthlyBreakdown>()
                val cal = Calendar.getInstance().apply { timeInMillis = start }
                while (cal.timeInMillis <= end) {
                    val ms = startOfMonth(cal.timeInMillis)
                    val me = endOfMonth(cal.timeInMillis)
                    val expTotal = expenseDao.getRawTotalExpense(ms, me) ?: 0.0
                    val incTotal = expenseDao.getRawTotalIncome(ms, me) ?: 0.0
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
    private val semesterCats: StateFlow<List<CategorySum>> = _currentView
        .flatMapLatest { view ->
            if (view != ExpenseView.SEMESTER) flowOf(emptyList())
            else {
                val (start, end) = runBlocking {
                    settingsDataStore.semesterStart.first() to settingsDataStore.semesterEnd.first()
                }
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
                semesterTotalIncome = income ?: 0.0,
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
