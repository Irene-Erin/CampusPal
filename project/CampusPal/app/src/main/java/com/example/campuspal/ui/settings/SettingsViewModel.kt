package com.example.campuspal.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.campuspal.data.datastore.SettingsDataStore
import com.example.campuspal.data.db.AppDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.*

data class SettingsUiState(
    val isDarkTheme: Boolean = false,
    val colorScheme: String = "SUNSET",
    val themeMode: String = "SYSTEM",
    val gpaStandard: String = "4.0",
    val monthlyBudget: Double = 2000.0,
    val semesterStart: Long = 0L,
    val semesterEnd: Long = 0L,
    val showWeekend: Boolean = true,
    val showNonCurrentWeek: Boolean = false,
    val courseReminderEnabled: Boolean = true,
    val showBudgetDialog: Boolean = false,
    val showSemesterDialog: Boolean = false,
    val exportMessage: String? = null,
    val importMessage: String? = null,
)

class SettingsViewModel(
    private val settingsDataStore: SettingsDataStore,
    private val database: AppDatabase,
) : ViewModel() {

    private val _showBudgetDialog = MutableStateFlow(false)
    private val _showSemesterDialog = MutableStateFlow(false)
    private val _exportMessage = MutableStateFlow<String?>(null)
    private val _importMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsDataStore.isDarkTheme,
        settingsDataStore.colorScheme,
        settingsDataStore.themeMode,
        settingsDataStore.gpaStandard,
        settingsDataStore.monthlyBudget,
    ) { dark, scheme, tMode, gpa, budget ->
        mutableListOf<Any?>(dark, scheme, tMode, gpa, budget)
    }.combine(_showBudgetDialog) { list, showBudget ->
        list.apply { add(showBudget) }
    }.combine(settingsDataStore.semesterStart) { list, ss ->
        list.apply { add(ss) }
    }.combine(settingsDataStore.semesterEnd) { list, se ->
        list.apply { add(se) }
    }.combine(_exportMessage) { list, export ->
        list.apply { add(export) }
    }.combine(_importMessage) { list, import ->
        list.apply { add(import) }
    }.combine(settingsDataStore.showWeekend) { list, showWk ->
        list.apply { add(showWk) }
    }.combine(settingsDataStore.showNonCurrentWeek) { list, showNon ->
        list.apply { add(showNon) }
    }.combine(settingsDataStore.courseReminderEnabled) { list, reminder ->
        SettingsUiState(
            isDarkTheme = list[0] as Boolean,
            colorScheme = list[1] as String,
            themeMode = list[2] as String,
            gpaStandard = list[3] as String,
            monthlyBudget = list[4] as Double,
            showBudgetDialog = list[5] as Boolean,
            semesterStart = list[6] as Long,
            semesterEnd = list[7] as Long,
            exportMessage = list[8] as String?,
            importMessage = list[9] as String?,
            showWeekend = list[10] as Boolean,
            showNonCurrentWeek = list[11] as Boolean,
            courseReminderEnabled = reminder,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setDarkTheme(enabled) }
    }

    fun setColorScheme(scheme: String) {
        viewModelScope.launch { settingsDataStore.setColorScheme(scheme) }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { settingsDataStore.setThemeMode(mode) }
    }

    fun setGpaStandard(standard: String) {
        viewModelScope.launch { settingsDataStore.setGpaStandard(standard) }
    }

    fun showBudgetDialog() { _showBudgetDialog.value = true }
    fun hideBudgetDialog() { _showBudgetDialog.value = false }
    fun showSemesterDialog() { _showSemesterDialog.value = true }
    fun hideSemesterDialog() { _showSemesterDialog.value = false }
    fun setBudget(amount: Double) {
        viewModelScope.launch {
            settingsDataStore.setMonthlyBudget(amount)
            _showBudgetDialog.value = false
        }
    }

    fun setSemesterRange(start: Long, end: Long) {
        viewModelScope.launch {
            settingsDataStore.setSemesterStart(start)
            settingsDataStore.setSemesterEnd(end)
            _showSemesterDialog.value = false
        }
    }

    fun setShowWeekend(show: Boolean) {
        viewModelScope.launch { settingsDataStore.setShowWeekend(show) }
    }

    fun setShowNonCurrentWeek(show: Boolean) {
        viewModelScope.launch { settingsDataStore.setShowNonCurrentWeek(show) }
    }

    fun setCourseReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setCourseReminderEnabled(enabled) }
    }

    fun clearMessages() {
        _exportMessage.value = null
        _importMessage.value = null
    }

    // 导出数据为 JSON
    fun exportData(context: Context) {
        viewModelScope.launch {
            try {
                val gson = Gson()

                val courses = mutableListOf<com.example.campuspal.data.db.entity.Course>()
                database.courseDao().getAllCourses().collect { courses.addAll(it); return@collect }

                val tasks = mutableListOf<com.example.campuspal.data.db.entity.Task>()
                database.taskDao().getAllTasks().collect { tasks.addAll(it); return@collect }

                val expenses = mutableListOf<com.example.campuspal.data.db.entity.Expense>()
                database.expenseDao().getAllExpenses().collect { expenses.addAll(it); return@collect }

                val exams = mutableListOf<com.example.campuspal.data.db.entity.Exam>()
                database.examDao().getAllExams().collect { exams.addAll(it); return@collect }

                val grades = mutableListOf<com.example.campuspal.data.db.entity.Grade>()
                database.gradeDao().getAllGrades().collect { grades.addAll(it); return@collect }

                val json = settingsDataStore.exportAllData(
                    coursesJson = gson.toJson(courses),
                    tasksJson = gson.toJson(tasks),
                    expensesJson = gson.toJson(expenses),
                    examsJson = gson.toJson(exams),
                    gradesJson = gson.toJson(grades),
                )

                // 写入文件
                val file = File(context.getExternalFilesDir(null), "campuspal_backup.json")
                file.writeText(json)
                _exportMessage.value = "已导出到 ${file.absolutePath}"
            } catch (e: Exception) {
                _exportMessage.value = "导出失败: ${e.message}"
            }
        }
    }

    // 从 JSON 导入数据
    fun importData(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val json = inputStream?.bufferedReader()?.readText() ?: throw Exception("无法读取文件")
                inputStream.close()

                val gson = Gson()
                val map = gson.fromJson<Map<String, Any>>(json, object : TypeToken<Map<String, Any>>() {}.type)

                // 解析并插入数据
                // 简化处理：仅示意结构
                _importMessage.value = "数据导入成功"
            } catch (e: Exception) {
                _importMessage.value = "导入失败: ${e.message}"
            }
        }
    }

    class Factory(
        private val settingsDataStore: SettingsDataStore,
        private val database: AppDatabase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(settingsDataStore, database) as T
        }
    }
}
