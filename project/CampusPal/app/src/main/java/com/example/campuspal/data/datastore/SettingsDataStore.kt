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

    val gpaStandard: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[GPA_STANDARD_KEY] ?: "4.0"
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

    suspend fun setGpaStandard(standard: String) {
        context.dataStore.edit { prefs ->
            prefs[GPA_STANDARD_KEY] = standard
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
    }
}
