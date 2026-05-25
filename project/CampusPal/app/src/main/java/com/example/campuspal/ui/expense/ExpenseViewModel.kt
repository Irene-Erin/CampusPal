package com.example.campuspal.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.campuspal.data.db.dao.CategorySum
import com.example.campuspal.data.db.dao.ExpenseDao
import com.example.campuspal.data.db.entity.Expense
import com.example.campuspal.data.datastore.SettingsDataStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class ExpenseUiState(
    val expenses: List<Expense> = emptyList(),
    val categorySums: List<CategorySum> = emptyList(),
    val totalExpense: Double = 0.0,
    val totalIncome: Double = 0.0,
    val monthlyBudget: Double = 2000.0,
    val isMonthView: Boolean = true,
    val isRefreshing: Boolean = false,
)

class ExpenseViewModel(
    private val expenseDao: ExpenseDao,
    private val settingsDataStore: SettingsDataStore,
) : ViewModel() {

    private val _isMonthView = MutableStateFlow(true)
    // 对话框独立于 combine 链，确保即时响应
    private val _showAddSheet = MutableStateFlow(false)
    val showAddSheet: StateFlow<Boolean> = _showAddSheet.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)

    private val currentMonthStart: Long
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

    private val currentMonthEnd: Long
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            return cal.timeInMillis
        }

    val uiState: StateFlow<ExpenseUiState> = combine(
        expenseDao.getExpensesByMonth(currentMonthStart, currentMonthEnd),
        expenseDao.getCategorySumBetween(currentMonthStart, currentMonthEnd),
        expenseDao.getTotalExpenseBetween(currentMonthStart, currentMonthEnd),
        expenseDao.getTotalIncomeBetween(currentMonthStart, currentMonthEnd),
        settingsDataStore.monthlyBudget,
    ) { expenses, catSums, totalExp, totalInc, budget ->
        mutableListOf<Any?>(expenses, catSums, totalExp, totalInc, budget)
    }.combine(_isMonthView) { list, isMonth ->
        list.apply { add(isMonth) }
    }.combine(_isRefreshing) { list, refreshing ->
        ExpenseUiState(
            expenses = list[0] as List<Expense>,
            categorySums = list[1] as List<CategorySum>,
            totalExpense = (list[2] as Double?) ?: 0.0,
            totalIncome = (list[3] as Double?) ?: 0.0,
            monthlyBudget = list[4] as Double,
            isMonthView = list[5] as Boolean,
            isRefreshing = refreshing,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExpenseUiState())

    fun toggleView() {
        _isMonthView.value = !_isMonthView.value
    }

    fun showAddSheet() { _showAddSheet.value = true }
    fun hideAddSheet() { _showAddSheet.value = false }

    fun refresh() {
        _isRefreshing.value = true
        viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            _isRefreshing.value = false
        }
    }

    fun addExpense(expense: Expense) {
        viewModelScope.launch {
            expenseDao.insert(expense)
            _showAddSheet.value = false
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseDao.delete(expense)
        }
    }

    fun setBudget(amount: Double) {
        viewModelScope.launch {
            settingsDataStore.setMonthlyBudget(amount)
        }
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
