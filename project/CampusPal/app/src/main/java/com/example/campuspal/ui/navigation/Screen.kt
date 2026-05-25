package com.example.campuspal.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    data object Home : Screen("home", "今日", Icons.Filled.Home, Icons.Outlined.Home)
    data object Schedule : Screen("schedule", "课程", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
    data object Todo : Screen("todo", "待办", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle)
    data object Expense : Screen("expense", "记账", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet)
    data object Learn : Screen("learn", "学习", Icons.Filled.LocalLibrary, Icons.Outlined.LocalLibrary)
}

// 非底部导航页
object ExtraRoutes {
    const val SETTINGS = "settings"
    const val GRADE = "grade"
}

val bottomNavItems = listOf(Screen.Home, Screen.Schedule, Screen.Todo, Screen.Expense, Screen.Learn)
