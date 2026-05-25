package com.example.campuspal.ui.expense

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campuspal.data.db.dao.CategorySum
import com.example.campuspal.data.db.entity.Expense
import com.example.campuspal.ui.theme.*
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// 分类定义
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
val pieChartColors = expenseCategories.map { it.color }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(viewModel: ExpenseViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val showAddSheet by viewModel.showAddSheet.collectAsState()
    // 成功动画状态
    var showSuccess by remember { mutableStateOf(false) }
    val successAlpha by animateFloatAsState(
        targetValue = if (showSuccess) 1f else 0f,
        animationSpec = tween(400),
        label = "successAlpha",
    )

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddSheet() },
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "记账")
            }
        }
    ) { padding ->
        SwipeRefresh(
            state = rememberSwipeRefreshState(uiState.isRefreshing),
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 汇总卡片
                item {
                    SummaryCards(
                        totalExpense = uiState.totalExpense,
                        totalIncome = uiState.totalIncome,
                        budget = uiState.monthlyBudget,
                        onBudgetClick = { viewModel.setBudget(uiState.monthlyBudget) },
                    )
                }

                // 视图切换
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { viewModel.toggleView() }) {
                            Icon(
                                if (uiState.isMonthView) Icons.Filled.CalendarViewWeek else Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (uiState.isMonthView) "月视图" else "周视图")
                        }
                    }
                }

                // 饼图统计
                if (uiState.categorySums.isNotEmpty()) {
                    item {
                        CategoryPieChart(categorySums = uiState.categorySums)
                    }
                }

                // 收支列表
                if (uiState.expenses.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "暂无收支记录",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    item {
                        Text(
                            "收支明细",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    // 按日期分组
                    val groupedExpenses = uiState.expenses.groupBy {
                        SimpleDateFormat("MM月dd日", Locale.getDefault()).format(Date(it.timestamp))
                    }

                    groupedExpenses.forEach { (dateLabel, dayExpenses) ->
                        item {
                            Text(
                                text = dateLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        items(dayExpenses, key = { it.id }) { expense ->
                            ExpenseItem(
                                expense = expense,
                                onLongPress = { viewModel.deleteExpense(expense) },
                            )
                        }
                    }
                }
            }
        }
    }

    // 快速记账底部面板
    if (showAddSheet) {
        AddExpenseSheet(
            onDismiss = { viewModel.hideAddSheet() },
            onSave = {
                viewModel.addExpense(it)
                showSuccess = true
            },
        )
    }

    // 成功对勾动画
    if (successAlpha > 0f) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SuccessGreen.copy(alpha = successAlpha * 0.9f),
                modifier = Modifier.size(80.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "成功",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }
        }
        LaunchedEffect(showSuccess) {
            if (showSuccess) {
                delay(1000)
                showSuccess = false
            }
        }
    }
}

@Composable
fun SummaryCards(
    totalExpense: Double,
    totalIncome: Double,
    budget: Double,
    onBudgetClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 本月支出
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.1f)),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("本月支出", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "¥%.2f".format(totalExpense),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ExpenseRed,
                )
            }
        }

        // 本月收入
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = IncomeGreen.copy(alpha = 0.1f)),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("本月收入", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "¥%.2f".format(totalIncome),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = IncomeGreen,
                )
            }
        }

        // 预算剩余
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.1f)),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("预算剩余", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val remaining = (budget - totalExpense).coerceAtLeast(0.0)
                Text(
                    text = "¥%.0f".format(remaining),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (remaining > 0) Primary else ErrorRed,
                )
                // 预算进度条
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = (totalExpense / budget).toFloat().coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = if (totalExpense <= budget) Primary else ErrorRed,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

@Composable
fun CategoryPieChart(categorySums: List<CategorySum>) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "支出分类统计",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            val total = categorySums.sumOf { it.total }
            if (total > 0) {
                // 饼图
                Box(
                    modifier = Modifier.size(180.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 36f
                        var startAngle = -90f

                        categorySums.forEachIndexed { index, cat ->
                            val sweep = (cat.total / total).toFloat() * 360f
                            drawArc(
                                color = pieChartColors[index % pieChartColors.size],
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                                style = Stroke(width = strokeWidth),
                            )
                            startAngle += sweep
                        }
                    }

                    // 中心文字
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "¥%.0f".format(total),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "总支出",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 图例
                categorySums.forEachIndexed { index, cat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(pieChartColors[index % pieChartColors.size]),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val catInfo = categoryMap[cat.category]
                        Text(
                            text = catInfo?.name ?: cat.category,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "¥%.2f".format(cat.total),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = " ${(cat.total / total * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Text(
                    "本月暂无支出",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpenseItem(
    expense: Expense,
    onLongPress: () -> Unit,
) {
    val catInfo = categoryMap[expense.category]
    val isExpense = expense.type == "expense"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { onLongPress() })
            },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 分类图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background((catInfo?.color ?: Color.Gray).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = catInfo?.icon ?: Icons.Filled.MoreHoriz,
                    contentDescription = expense.category,
                    modifier = Modifier.size(22.dp),
                    tint = catInfo?.color ?: Color.Gray,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = catInfo?.name ?: expense.category,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                if (expense.note.isNotBlank()) {
                    Text(
                        text = expense.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Text(
                text = "${if (isExpense) "-" else "+"}¥%.2f".format(expense.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isExpense) ExpenseRed else IncomeGreen,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseSheet(
    onDismiss: () -> Unit,
    onSave: (Expense) -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("expense") }
    var selectedCategory by remember { mutableStateOf(expenseCategories[0].name) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("快速记账") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 类型切换
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TabRow(
                        selectedTabIndex = if (selectedType == "expense") 0 else 1,
                        modifier = Modifier.width(200.dp),
                    ) {
                        Tab(selected = selectedType == "expense", onClick = { selectedType = "expense" }) {
                            Text("支出", modifier = Modifier.padding(12.dp))
                        }
                        Tab(selected = selectedType == "income", onClick = { selectedType = "income" }) {
                            Text("收入", modifier = Modifier.padding(12.dp))
                        }
                    }
                }

                // 金额输入
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("金额") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Text("¥", style = MaterialTheme.typography.titleLarge) },
                )

                // 分类选择
                Text("分类", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    expenseCategories.forEach { cat ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedCategory = cat.name }
                                .background(
                                    if (selectedCategory == cat.name) cat.color.copy(alpha = 0.15f)
                                    else Color.Transparent
                                )
                                .padding(8.dp),
                        ) {
                            Icon(
                                imageVector = cat.icon,
                                contentDescription = cat.name,
                                tint = cat.color,
                                modifier = Modifier.size(24.dp),
                            )
                            Text(cat.name, fontSize = 11.sp)
                        }
                    }
                }

                // 备注
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amt = amount.toDoubleOrNull()
                    if (amt != null && amt > 0) {
                        onSave(
                            Expense(
                                amount = amt,
                                category = selectedCategory,
                                note = note,
                                type = selectedType,
                                timestamp = System.currentTimeMillis(),
                            )
                        )
                    }
                },
                enabled = (amount.toDoubleOrNull() ?: 0.0) > 0,
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
