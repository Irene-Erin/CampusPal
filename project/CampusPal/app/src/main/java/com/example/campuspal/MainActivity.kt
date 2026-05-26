package com.example.campuspal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.campuspal.ui.expense.ExpenseScreen
import com.example.campuspal.ui.expense.ExpenseViewModel
import com.example.campuspal.ui.grade.GradeScreen
import com.example.campuspal.ui.grade.GradeViewModel
import com.example.campuspal.ui.home.HomeScreen
import com.example.campuspal.ui.home.HomeViewModel
import com.example.campuspal.ui.learn.LearnScreen
import com.example.campuspal.ui.learn.LearnViewModel
import com.example.campuspal.ui.navigation.ExtraRoutes
import com.example.campuspal.ui.navigation.Screen
import com.example.campuspal.ui.navigation.bottomNavItems
import com.example.campuspal.ui.schedule.ScheduleScreen
import com.example.campuspal.ui.schedule.ScheduleViewModel
import com.example.campuspal.ui.settings.SettingsScreen
import com.example.campuspal.ui.settings.SettingsViewModel
import com.example.campuspal.ui.theme.CampusPalTheme
import com.example.campuspal.ui.theme.ColorSchemeType
import com.example.campuspal.ui.theme.ThemeMode
import com.example.campuspal.ui.todo.TodoScreen
import com.example.campuspal.ui.todo.TodoViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as CampusPalApp

        setContent {
            val isDarkTheme by app.settingsDataStore.isDarkTheme.collectAsState(initial = false)
            val colorSchemeName by app.settingsDataStore.colorScheme.collectAsState(initial = "SUNSET")
            val themeModeName by app.settingsDataStore.themeMode.collectAsState(initial = "SYSTEM")
            val colorSchemeType = try { ColorSchemeType.valueOf(colorSchemeName) } catch (_: Exception) { ColorSchemeType.SUNSET }
            val themeMode = try { ThemeMode.valueOf(themeModeName) } catch (_: Exception) { ThemeMode.SYSTEM }

            CampusPalTheme(
                themeMode = themeMode,
                colorSchemeType = colorSchemeType,
            ) {
                MainScreen(app)
            }
        }
    }
}

@Composable
fun MainScreen(app: CampusPalApp) {
    val navController = rememberNavController()

    val scheduleViewModel = remember { ScheduleViewModel(app.database.courseDao(), app.settingsDataStore) }
    val todoViewModel = remember { TodoViewModel(app.database.taskDao()) }
    val expenseViewModel = remember { ExpenseViewModel(app.database.expenseDao(), app.settingsDataStore) }
    val learnViewModel = remember { LearnViewModel(app.database.examDao(), app.database.studySessionDao(), app.database.courseDao()) }
    val gradeViewModel = remember { GradeViewModel(app.database.gradeDao(), app.database.courseDao(), app.settingsDataStore) }
    val settingsViewModel = remember { SettingsViewModel(app.settingsDataStore, app.database) }
    val homeViewModel = remember {
        HomeViewModel(
            app.database.courseDao(),
            app.database.taskDao(),
            app.database.expenseDao(),
            app.settingsDataStore,
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (navController.currentDestination?.hierarchy?.any { it.route == screen.route } == true)
                                    screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title,
                            )
                        },
                        label = { Text(screen.title) },
                        selected = navController.currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(300)) + slideInHorizontally(animationSpec = tween(300)) { it / 4 } },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
        ) {
            composable(Screen.Home.route) {
                HomeScreen(homeViewModel, navController)
            }
            composable(Screen.Schedule.route) {
                ScheduleScreen(scheduleViewModel)
            }
            composable(Screen.Todo.route) {
                TodoScreen(todoViewModel)
            }
            composable(Screen.Expense.route) {
                ExpenseScreen(expenseViewModel)
            }
            composable(Screen.Learn.route) {
                LearnScreen(learnViewModel)
            }
            composable(ExtraRoutes.GRADE) {
                GradeScreen(gradeViewModel)
            }
            composable(ExtraRoutes.SETTINGS) {
                SettingsScreen(settingsViewModel)
            }
        }
    }
}
