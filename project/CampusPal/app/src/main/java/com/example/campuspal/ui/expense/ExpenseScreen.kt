package com.example.campuspal.ui.expense

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campuspal.data.db.dao.CategorySum
import com.example.campuspal.data.db.entity.Expense
import com.example.campuspal.ui.components.AppDimens
import com.example.campuspal.ui.components.EmptyState
import androidx.compose.ui.viewinterop.AndroidView
import com.example.campuspal.ui.expense.chart.PieChartView
import com.example.campuspal.ui.expense.chart.LineChartView
import com.example.campuspal.ui.expense.ExpenseForm
import com.example.campuspal.ui.theme.*
import kotlinx.coroutines.delay
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import java.text.SimpleDateFormat
import java.util.*

data class ExpenseCategory(val name: String, val icon: ImageVector, val color: Color)

val expenseCategories = listOf(
    ExpenseCategory("饮食", Icons.Filled.Restaurant, Color(0xFFFF6B6B)),
    ExpenseCategory("交通", Icons.Filled.DirectionsBus, Color(0xFF4ECDC4)),
    ExpenseCategory("购物", Icons.Filled.ShoppingCart, Color(0xFFFFB347)),
    ExpenseCategory("娱乐", Icons.Filled.Movie, Color(0xFF9B59B6)),
    ExpenseCategory("学习", Icons.Filled.MenuBook, Color(0xFF4A90D9)),
    ExpenseCategory("其他", Icons.Filled.MoreHoriz, Color(0xFF95A5A6)),
)

val categoryMap = expenseCategories.associateBy { it.name }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(viewModel: ExpenseViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val showAddSheet by viewModel.showAddSheet.collectAsState()
    var showSuccess by remember { mutableStateOf(false) }
    val successAlpha by animateFloatAsState(if (showSuccess) 1f else 0f, tween(400), label = "s")

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddSheet() },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(AppDimens.fabCorner),
            ) { Icon(Icons.Filled.Add, contentDescription = "记账", modifier = Modifier.size(28.dp)) }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ViewTabRow(
                currentView = uiState.currentView,
                onViewChange = { viewModel.setView(it) },
            )

            if (uiState.currentView != ExpenseView.SEMESTER) {
                DateNavigator(
                    label = viewModel.selectedDateLabel(),
                    onPrev = { viewModel.goPrev() },
                    onNext = { viewModel.goNext() },
                    onDateClick = { /* DatePicker in DailyContent */ },
                )
            }

            SwipeRefresh(
                state = rememberSwipeRefreshState(uiState.isRefreshing),
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize(),
            ) {
                AnimatedContent(
                    targetState = uiState.currentView,
                    transitionSpec = {
                        (fadeIn(tween(200)) + slideInHorizontally(tween(200)) { it / 4 }) togetherWith
                        (fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { -it / 4 })
                    },
                    label = "viewTransition",
                ) { view ->
                    when (view) {
                        ExpenseView.DAY -> DailyContent(uiState, viewModel)
                        ExpenseView.WEEK -> WeekContent(uiState, viewModel)
                        ExpenseView.MONTH -> MonthContent(uiState, viewModel)
                        ExpenseView.SEMESTER -> SemesterContent(uiState, viewModel)
                        ExpenseView.YEAR -> YearContent(uiState, viewModel)
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        ExpenseForm(
            editingExpense = uiState.editingExpense,
            onDismiss = { viewModel.hideAddSheet() },
            onSave = { viewModel.addExpense(it); showSuccess = true },
        )
    }
    if (successAlpha > 0f) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(shape = RoundedCornerShape(16.dp), color = SuccessGreen.copy(alpha = successAlpha * 0.9f), modifier = Modifier.size(80.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Check, "成功", tint = Color.White, modifier = Modifier.size(48.dp)) }
            }
        }
        LaunchedEffect(showSuccess) { if (showSuccess) { delay(1000); showSuccess = false } }
    }
}

@Composable
fun ViewTabRow(currentView: ExpenseView, onViewChange: (ExpenseView) -> Unit) {
    Surface(shadowElevation = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ExpenseView.entries.forEach { view ->
                FilterChip(
                    selected = currentView == view,
                    onClick = { onViewChange(view) },
                    label = {
                        Text(
                            when (view) {
                                ExpenseView.DAY -> "日"
                                ExpenseView.WEEK -> "周"
                                ExpenseView.MONTH -> "月"
                                ExpenseView.SEMESTER -> "学期"
                                ExpenseView.YEAR -> "年"
                            },
                            fontSize = 13.sp,
                        )
                    },
                )
            }
        }
    }
}

@Composable
fun DateNavigator(label: String, onPrev: () -> Unit, onNext: () -> Unit, onDateClick: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrev) { Icon(Icons.Filled.ChevronLeft, "前") }
            TextButton(onClick = onDateClick) {
                Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            IconButton(onClick = onNext) { Icon(Icons.Filled.ChevronRight, "后") }
        }
    }
}

// ========== 日视图 ==========
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyContent(state: ExpenseUiState, viewModel: ExpenseViewModel) {
    var showDatePicker by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            DateNavigator(
                label = viewModel.selectedDateLabel(),
                onPrev = { viewModel.goPrev() },
                onNext = { viewModel.goNext() },
                onDateClick = { showDatePicker = true },
            )
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.1f))) {
                    Column(Modifier.padding(12.dp)) {
                        Text("今日支出", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("¥%.2f".format(state.totalExpense), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ExpenseRed)
                    }
                }
                Card(Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = IncomeGreen.copy(alpha = 0.1f))) {
                    Column(Modifier.padding(12.dp)) {
                        Text("今日收入", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("¥%.2f".format(state.totalIncome), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = IncomeGreen)
                    }
                }
            }
        }

        // 日视图图表
        if (state.viewChartData.lineData != null || state.viewChartData.pieData != null) {
            item { ChartSection(state.viewChartData) }
        }

        if (state.expenses.isEmpty()) {
            item { EmptyState(icon = Icons.Filled.ReceiptLong, title = "暂无记录", subtitle = "点击右下角 + 添加", actionLabel = "添加记录", onAction = { viewModel.showAddSheet() }) }
        } else {
            item { Text("收支明细", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
            items(state.expenses, key = { it.id }) { expense ->
                ExpenseItem(expense = expense, onLongPress = { viewModel.deleteExpense(expense) }, onClick = { viewModel.editExpense(expense) })
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = state.selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { viewModel.setSelectedDate(it) }; showDatePicker = false }) { Text("确定") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } },
        ) { DatePicker(state = datePickerState) }
    }
}

// ========== 周视图 ==========
@Composable
fun WeekContent(state: ExpenseUiState, viewModel: ExpenseViewModel) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.1f))) {
                    Column(Modifier.padding(12.dp)) {
                        Text("本周支出", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("¥%.2f".format(state.totalExpense), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ExpenseRed)
                    }
                }
                Card(Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = IncomeGreen.copy(alpha = 0.1f))) {
                    Column(Modifier.padding(12.dp)) {
                        Text("本周收入", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("¥%.2f".format(state.totalIncome), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = IncomeGreen)
                    }
                }
            }
        }

        // 周视图图表
        if (state.viewChartData.lineData != null || state.viewChartData.pieData != null) {
            item { ChartSection(state.viewChartData) }
        }

        if (state.expenses.isEmpty()) {
            item { EmptyState(icon = Icons.Filled.ReceiptLong, title = "暂无记录", subtitle = "点击右下角 + 添加") }
        } else {
            item { Text("收支明细", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
            items(state.expenses, key = { it.id }) { expense ->
                ExpenseItem(expense, onLongPress = { viewModel.deleteExpense(expense) }, onClick = { viewModel.editExpense(expense) })
            }
        }
    }
}

// ========== 月视图 ==========
@Composable
fun MonthContent(state: ExpenseUiState, viewModel: ExpenseViewModel) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.1f))) {
                    Column(Modifier.padding(12.dp)) {
                        Text("支出", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("¥%.2f".format(state.totalExpense), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ExpenseRed)
                    }
                }
                Card(Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = IncomeGreen.copy(alpha = 0.1f))) {
                    Column(Modifier.padding(12.dp)) {
                        Text("收入", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("¥%.2f".format(state.totalIncome), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = IncomeGreen)
                    }
                }
                Card(Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("预算剩余", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val rem = (state.monthlyBudget - state.totalExpense).coerceAtLeast(0.0)
                        Text("¥%.0f".format(rem), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (rem > 0) MaterialTheme.colorScheme.primary else ErrorRed)
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(progress = (state.totalExpense / state.monthlyBudget).toFloat().coerceIn(0f, 1f), modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)), color = if (state.totalExpense <= state.monthlyBudget) MaterialTheme.colorScheme.primary else ErrorRed, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }

        // 月视图图表
        if (state.viewChartData.lineData != null || state.viewChartData.pieData != null) {
            item { ChartSection(state.viewChartData) }
        }

        if (state.expenses.isEmpty()) {
            item { EmptyState(icon = Icons.Filled.ReceiptLong, title = "暂无记录", subtitle = "点击右下角 + 添加") }
        } else {
            item { Text("收支明细", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
            val grouped = state.expenses.groupBy { SimpleDateFormat("MM月dd日", Locale.getDefault()).format(Date(it.timestamp)) }
            grouped.forEach { (label, dayExpenses) ->
                item { Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)) }
                items(dayExpenses, key = { it.id }) { expense ->
                    ExpenseItem(expense, onLongPress = { viewModel.deleteExpense(expense) }, onClick = { viewModel.editExpense(expense) })
                }
            }
        }
    }
}

// ========== 学期视图 ==========
@Composable
fun SemesterContent(state: ExpenseUiState, viewModel: ExpenseViewModel) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.1f))) {
                    Column(Modifier.padding(12.dp)) {
                        Text("学期支出", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("¥%.2f".format(state.semesterTotalExpense), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ExpenseRed)
                    }
                }
                Card(Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = IncomeGreen.copy(alpha = 0.1f))) {
                    Column(Modifier.padding(12.dp)) {
                        Text("学期收入", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("¥%.2f".format(state.semesterTotalIncome), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = IncomeGreen)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            val balance = state.semesterTotalIncome - state.semesterTotalExpense
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("结余", style = MaterialTheme.typography.titleSmall)
                    Text("¥%.2f".format(balance), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (balance >= 0) SuccessGreen else ErrorRed)
                }
            }
        }

        if (state.showSemesterHint) {
            item {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text("请先在设置中配置学期日期", modifier = Modifier.padding(20.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            // 学期视图图表
            if (state.viewChartData.lineData != null || state.viewChartData.pieData != null) {
                item { ChartSection(state.viewChartData) }
            }

            if (state.semesterCategorySums.isNotEmpty()) {
                item { Text("分类汇总", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
                val total = state.semesterCategorySums.sumOf { it.total }
                items(state.semesterCategorySums, key = { it.category }) { cat ->
                    val catInfo = categoryMap[cat.category]
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(36.dp).clip(CircleShape).background((catInfo?.color ?: Color.Gray).copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                Icon(catInfo?.icon ?: Icons.Filled.MoreHoriz, cat.category, tint = catInfo?.color ?: Color.Gray, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(catInfo?.name ?: cat.category, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                                Text("¥%.2f".format(cat.total), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("${(cat.total / total * 100).toInt()}%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

// ========== 年视图 ==========
@Composable
fun YearContent(state: ExpenseUiState, viewModel: ExpenseViewModel) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.1f))) {
                    Column(Modifier.padding(12.dp)) {
                        Text("本年支出", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("¥%.2f".format(state.yearTotalExpense), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ExpenseRed)
                    }
                }
                Card(Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = IncomeGreen.copy(alpha = 0.1f))) {
                    Column(Modifier.padding(12.dp)) {
                        Text("本年收入", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("¥%.2f".format(state.yearTotalIncome), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = IncomeGreen)
                    }
                }
            }
        }

        // 年视图图表
        if (state.viewChartData.lineData != null || state.viewChartData.pieData != null) {
            item { ChartSection(state.viewChartData) }
        }

        if (state.expenses.isEmpty()) {
            item { EmptyState(icon = Icons.Filled.ReceiptLong, title = "暂无记录", subtitle = "点击右下角 + 添加") }
        } else {
            item { Text("收支明细", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
            items(state.expenses, key = { it.id }) { expense ->
                ExpenseItem(expense, onLongPress = { viewModel.deleteExpense(expense) }, onClick = { viewModel.editExpense(expense) })
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpenseItem(expense: Expense, onLongPress: () -> Unit, onClick: () -> Unit) {
    val catInfo = categoryMap[expense.category]
    val isExpense = expense.type == "expense"
    Card(
        modifier = Modifier.fillMaxWidth().pointerInput(Unit) { detectTapGestures(onLongPress = { onLongPress() }, onTap = { _ -> onClick() }) },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).background((catInfo?.color ?: Color.Gray).copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(catInfo?.icon ?: Icons.Filled.MoreHoriz, expense.category, modifier = Modifier.size(20.dp), tint = catInfo?.color ?: Color.Gray)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(catInfo?.name ?: expense.category, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Row {
                    Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(expense.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (expense.note.isNotBlank()) { Text(" · " + expense.note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }
            Text("${if (isExpense) "-" else "+"}¥%.2f".format(expense.amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = if (isExpense) ExpenseRed else IncomeGreen)
        }
    }
}

// ========== 图表组件 ==========
@Composable
fun ChartSection(data: com.example.campuspal.ui.expense.chart.ViewChartData) {
    val lineColor = MaterialTheme.colorScheme.primary.toArgb()

    // 折线图
    data.lineData?.let { ld ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(ld.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                AndroidView(
                    factory = { LineChartView(it) },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                ) { view -> view.setData(ld.title, ld.labels, ld.values, lineColor) }
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    // 饼图
    data.pieData?.let { pd ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(pd.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                AndroidView(
                    factory = { PieChartView(it) },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                ) { view ->
                    val pieData = pd.slices.map { s ->
                        PieChartView.PieSliceData(s.label, s.value, s.color.toInt())
                    }
                    view.setData(pieData)
                }
            }
        }
    }
}

