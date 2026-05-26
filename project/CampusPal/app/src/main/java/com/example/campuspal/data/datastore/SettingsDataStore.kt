package com.example.campuspal.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    val monthlyBudget: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[MONTHLY_BUDGET_KEY] ?: 2000.0
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_DARK_THEME_KEY] ?: false
    }

    val colorScheme: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[COLOR_SCHEME_KEY] ?: "SUNSET"
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[THEME_MODE_KEY] ?: "SYSTEM"
    }

    val gpaStandard: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[GPA_STANDARD_KEY] ?: "4.0"
    }

    val semesterStart: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[SEMESTER_START_KEY] ?: getDefaultSemesterStart()
    }

    val semesterEnd: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[SEMESTER_END_KEY] ?: getDefaultSemesterEnd()
    }

    val showWeekend: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SHOW_WEEKEND_KEY] ?: true
    }

    val showNonCurrentWeek: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SHOW_NON_CURRENT_WEEK_KEY] ?: false
    }

    suspend fun setMonthlyBudget(amount: Double) {
        context.dataStore.edit { prefs ->
            prefs[MONTHLY_BUDGET_KEY] = amount
        }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_DARK_THEME_KEY] = enabled
        }
    }

    suspend fun setColorScheme(scheme: String) {
        context.dataStore.edit { prefs ->
            prefs[COLOR_SCHEME_KEY] = scheme
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = mode
        }
    }

    suspend fun setSemesterStart(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[SEMESTER_START_KEY] = timestamp
        }
    }

    suspend fun setSemesterEnd(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[SEMESTER_END_KEY] = timestamp
        }
    }

    suspend fun setGpaStandard(standard: String) {
        context.dataStore.edit { prefs ->
            prefs[GPA_STANDARD_KEY] = standard
        }
    }

    suspend fun setShowWeekend(show: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SHOW_WEEKEND_KEY] = show
        }
    }

    suspend fun setShowNonCurrentWeek(show: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SHOW_NON_CURRENT_WEEK_KEY] = show
        }
    }

    // 导出所有设置和数据的 JSON
    suspend fun exportAllData(
        coursesJson: String,
        tasksJson: String,
        expensesJson: String,
        examsJson: String,
        gradesJson: String,
    ): String {
        return """
        {
            "budget": ${context.dataStore.data.first()[MONTHLY_BUDGET_KEY] ?: 2000.0},
            "gpaStandard": "${context.dataStore.data.first()[GPA_STANDARD_KEY] ?: "4.0"}",
            "courses": $coursesJson,
            "tasks": $tasksJson,
            "expenses": $expensesJson,
            "exams": $examsJson,
            "grades": $gradesJson
        }
        """.trimIndent()
    }

    companion object {
        private val MONTHLY_BUDGET_KEY = doublePreferencesKey("monthly_budget")
        private val IS_DARK_THEME_KEY = booleanPreferencesKey("is_dark_theme")
        private val COLOR_SCHEME_KEY = stringPreferencesKey("color_scheme")
        private val GPA_STANDARD_KEY = stringPreferencesKey("gpa_standard")
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val SEMESTER_START_KEY = longPreferencesKey("semester_start")
        private val SEMESTER_END_KEY = longPreferencesKey("semester_end")
        private val SHOW_WEEKEND_KEY = booleanPreferencesKey("show_weekend")
        private val SHOW_NON_CURRENT_WEEK_KEY = booleanPreferencesKey("show_non_current_week")

        private fun getDefaultSemesterStart(): Long {
            val cal = java.util.Calendar.getInstance()
            cal.set(cal.get(java.util.Calendar.YEAR), java.util.Calendar.SEPTEMBER, 1, 0, 0, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        private fun getDefaultSemesterEnd(): Long {
            val cal = java.util.Calendar.getInstance()
            cal.set(cal.get(java.util.Calendar.YEAR) + 1, java.util.Calendar.JANUARY, 31, 23, 59, 59)
            cal.set(java.util.Calendar.MILLISECOND, 999)
            return cal.timeInMillis
        }
    }
}
