package com.example.campuspal.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.campuspal.data.db.dao.CategorySum
import com.example.campuspal.data.db.dao.DailySum
import com.example.campuspal.data.db.dao.ExpenseDao
import com.example.campuspal.data.db.dao.MonthlyBreakdown
import com.example.campuspal.data.db.entity.Expense
import com.example.campuspal.data.datastore.SettingsDataStore
import com.example.campuspal.ui.expense.chart.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

enum class ExpenseView { DAY, WEEK, MONTH, SEMESTER, YEAR }

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
    // 图表数据
    val pieSlices: List<PieSlice> = emptyList(),
    val barEntries: List<BarEntry> = emptyList(),
    val linePoints: List<LinePoint> = emptyList(),
    val chartSubtitle: String = "",
    val showSemesterHint: Boolean = false,
    val yearTotalExpense: Double = 0.0,
    val yearTotalIncome: Double = 0.0,
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

    private val semesterRange: StateFlow<Pair<Long, Long>> = combine(
        settingsDataStore.semesterStart, settingsDataStore.semesterEnd,
    ) { start, end -> start to end }.stateIn(viewModelScope, SharingStarted.Eagerly, 0L to 0L)

    private data class TimeRange(val start: Long, val end: Long)

    private val activeRange: StateFlow<TimeRange> = combine(
        _currentView, _selectedDate, semesterRange,
    ) { view, date, (semStart, semEnd) ->
        val today = getTodayMidnight()
        val todayEnd = endOfDay(today)
        when (view) {
            ExpenseView.DAY -> TimeRange(startOfDay(date), endOfDay(date))
            ExpenseView.WEEK -> TimeRange(startOfWeek(date), todayEnd)
            ExpenseView.MONTH -> TimeRange(startOfMonth(date), todayEnd)
            ExpenseView.SEMESTER -> TimeRange(semStart, if (semEnd > today) todayEnd else endOfDay(semEnd))
            ExpenseView.YEAR -> {
                val cal = Calendar.getInstance().apply { timeInMillis = today }
                cal.set(Calendar.MONTH, Calendar.JANUARY); cal.set(Calendar.DAY_OF_MONTH, 1)
                TimeRange(startOfDay(cal.timeInMillis), todayEnd)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, TimeRange(getTodayMidnight(), getTodayMidnight()))

    private val rangeData: StateFlow<Triple<List<Expense>, List<CategorySum>, Double?>> =
        activeRange.flatMapLatest { (start, end) ->
            combine(
                expenseDao.getExpensesByMonth(start, end),
                expenseDao.getCategorySumBetween(start, end),
                expenseDao.getTotalExpenseBetween(start, end),
            ) { exps, cats, total -> Triple(exps, cats, total) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Triple(emptyList(), emptyList(), null))

    private val rangeIncome: StateFlow<Double?> =
        activeRange.flatMapLatest { (start, end) ->
            expenseDao.getTotalIncomeBetween(start, end)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 学期月度柱状图
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
                    if (expTotal > 0 || incTotal > 0) result.add(MonthlyBreakdown(label, expTotal, incTotal))
                    cal.add(Calendar.MONTH, 1)
                }
                emit(result)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val semesterCats: StateFlow<List<CategorySum>> = combine(
        _currentView, semesterRange,
    ) { view, (semStart, semEnd) -> view to (semStart to semEnd) }
        .flatMapLatest { (view, range) ->
            if (view != ExpenseView.SEMESTER) flowOf(emptyList())
            else { val (start, end) = range; expenseDao.getCategorySumBetween(start, end) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 图表数据
    private val chartData: StateFlow<ExpenseChartData> = combine(_currentView, _selectedDate, semesterRange) { v, d, (ss, se) -> Triple(v, d, ss to se) }
        .flatMapLatest { (view, date, semRange) ->
            flow {
                val today = getTodayMidnight()
                val todayEnd = endOfDay(today)
                val cats = expenseCategories.map { it.name }
                val catColors = expenseCategories.map { it.color.value.toLong() }

                when (view) {
                    ExpenseView.DAY -> {
                        val dayStart = startOfDay(date); val dayEnd = endOfDay(date)
                        val sums = withContext(Dispatchers.IO) { expenseDao.getCategorySumByDay(dayStart, dayEnd) }
                        val slices = sums.map { s -> PieSlice(cats.firstOrNull { it == s.category } ?: s.category, s.total, catColors.getOrElse(cats.indexOf(s.category)) { 0xFF95A5A6 }) }
                        emit(ExpenseChartData(pieSlices = slices))
                    }
                    ExpenseView.WEEK -> {
                        val (ws, we) = getWeekRange(today)
                        val dailySums = withContext(Dispatchers.IO) { expenseDao.getDailySumsBetween(ws, we) }
                        val weekStartCal = Calendar.getInstance().apply { timeInMillis = ws }
                        val todayDay = Calendar.getInstance().apply { timeInMillis = today }.get(Calendar.DAY_OF_WEEK)
                        val todayOffset = when (todayDay) { Calendar.MONDAY -> 0; Calendar.TUESDAY -> 1; Calendar.WEDNESDAY -> 2; Calendar.THURSDAY -> 3; Calendar.FRIDAY -> 4; Calendar.SATURDAY -> 5; Calendar.SUNDAY -> 6; else -> -1 }
                        val bars = (0..todayOffset).map { i ->
                            val label = listOf("一","二","三","四","五","六","日")[i]
                            val v = dailySums.getOrElse(i) { 0.0 }
                            BarEntry(label, v, i == todayOffset)
                        }
                        emit(ExpenseChartData(barEntries = bars))
                    }
                    ExpenseView.MONTH -> {
                        val (ms, me) = getMonthRange(today)
                        val dailySums = withContext(Dispatchers.IO) { expenseDao.getDailySumsBetween(ms, me) }
                        val todayDom = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                        val monthExpTotal = dailySums.sum()
                        val points = (1..todayDom).map { d ->
                            val v = dailySums.getOrElse(d - 1) { 0.0 }
                            LinePoint("${d}日", v, d == todayDom)
                        }
                        emit(ExpenseChartData(linePoints = points, chartSubtitle = "本月累计支出 ¥%.2f（统计至${todayDom}日）".format(monthExpTotal)))
                    }
                    ExpenseView.SEMESTER -> {
                        val (semStart, semEnd) = semRange
                        if (semStart <= 0L) {
                            emit(ExpenseChartData(showSemesterHint = true))
                        } else {
                            val bars = monthBreakdowns.value.map { bd -> BarEntry(bd.yearMonth, bd.expenseTotal, false) }
                            val curMonthLabel = monthLabel(today)
                            val barsWithHighlight = bars.map { it.copy(isHighlight = it.label == curMonthLabel) }
                            val total = bars.sumOf { it.value }
                            emit(ExpenseChartData(barEntries = barsWithHighlight, chartSubtitle = "学期累计支出 ¥%.2f".format(total)))
                        }
                    }
                    ExpenseView.YEAR -> {
                        val (ys, ye) = getYearRange(today)
                        val monthlySums = withContext(Dispatchers.IO) { expenseDao.getMonthlySumsBetween(ys, ye) }
                        val curMonth = Calendar.getInstance().get(Calendar.MONTH) // 0-based
                        val curMonthLabel = "${curMonth + 1}月"
                        val bars = (0..curMonth).map { m ->
                            val label = "${m + 1}月"
                            val v = monthlySums.getOrElse(m) { 0.0 }
                            BarEntry(label, v, m == curMonth)
                        }
                        val yearTotal = monthlySums.sum()
                        val yearIncome = withContext(Dispatchers.IO) { expenseDao.getRawTotalIncome(ys, ye) } ?: 0.0
                        emit(ExpenseChartData(
                            barEntries = bars,
                            chartSubtitle = "本年累计支出 ¥%.2f（统计至${curMonth + 1}月）".format(yearTotal),
                            yearTotalExpense = yearTotal,
                            yearTotalIncome = yearIncome,
                        ))
                    }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExpenseChartData())

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<ExpenseUiState> = combine(
        rangeData, rangeIncome, settingsDataStore.monthlyBudget, _currentView, _isRefreshing,
    ) { data, income, budget, view, refreshing ->
        mutableListOf<Any?>(data, income, budget, view, refreshing)
    }.combine(_selectedDate) { acc, date -> acc.apply { add(date) } }
        .combine(settingsDataStore.semesterStart) { acc, ss -> acc.apply { add(ss) } }
        .combine(settingsDataStore.semesterEnd) { acc, se -> acc.apply { add(se) } }
        .combine(monthBreakdowns) { acc, bds -> acc.apply { add(bds) } }
        .combine(semesterCats) { acc, semC -> acc.apply { add(semC) } }
        .combine(_editingExpense) { acc, edit -> acc.apply { add(edit) } }
        .combine(chartData) { acc, cd ->
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
            val editing = acc[10] as Expense?
            val semTotalInc = if (view == ExpenseView.SEMESTER) income ?: 0.0 else 0.0
            ExpenseUiState(
                expenses = data.first, categorySums = data.second,
                totalExpense = data.third ?: 0.0, totalIncome = income ?: 0.0,
                monthlyBudget = budget, currentView = view, isRefreshing = refreshing,
                selectedDate = date,
                semesterStart = semStart, semesterEnd = semEnd,
                monthlyBreakdowns = bds, semesterCategorySums = semCats,
                semesterTotalExpense = semCats.sumOf { it.total },
                semesterTotalIncome = semTotalInc, editingExpense = editing,
                pieSlices = cd.pieSlices, barEntries = cd.barEntries,
                linePoints = cd.linePoints, chartSubtitle = cd.chartSubtitle,
                showSemesterHint = cd.showSemesterHint,
                yearTotalExpense = cd.yearTotalExpense, yearTotalIncome = cd.yearTotalIncome,
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
            ExpenseView.YEAR -> {
                cal.set(Calendar.MONTH, Calendar.JANUARY); cal.set(Calendar.DAY_OF_MONTH, 1)
                SimpleDateFormat("yyyy年", Locale.CHINESE).format(cal.time)
            }
        }
    }

    fun goPrev() {
        val cal = Calendar.getInstance().apply { timeInMillis = _selectedDate.value }
        when (_currentView.value) {
            ExpenseView.DAY -> cal.add(Calendar.DAY_OF_MONTH, -1)
            ExpenseView.WEEK -> cal.add(Calendar.WEEK_OF_YEAR, -1)
            ExpenseView.MONTH -> cal.add(Calendar.MONTH, -1)
            ExpenseView.YEAR -> cal.add(Calendar.YEAR, -1)
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
            ExpenseView.YEAR -> cal.add(Calendar.YEAR, 1)
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
            if (expense.id != 0L) expenseDao.update(expense) else expenseDao.insert(expense)
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

data class ExpenseChartData(
    val pieSlices: List<PieSlice> = emptyList(),
    val barEntries: List<BarEntry> = emptyList(),
    val linePoints: List<LinePoint> = emptyList(),
    val chartSubtitle: String = "",
    val showSemesterHint: Boolean = false,
    val yearTotalExpense: Double = 0.0,
    val yearTotalIncome: Double = 0.0,
)
