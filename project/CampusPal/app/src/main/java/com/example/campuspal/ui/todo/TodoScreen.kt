package com.example.campuspal.ui.todo

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campuspal.data.db.entity.Task
import com.example.campuspal.ui.todo.TodoForm
import com.example.campuspal.ui.theme.PriorityColors
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(viewModel: TodoViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "添加待办")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // 筛选栏
            FilterBar(
                currentFilter = uiState.filter,
                selectedCourseId = uiState.selectedCourseId,
                onFilterChange = { viewModel.setFilter(it) },
                onCourseFilterChange = { viewModel.setCourseFilter(it) },
            )

            // 待办列表
            SwipeRefresh(
                state = rememberSwipeRefreshState(uiState.isRefreshing),
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize(),
            ) {
                if (uiState.tasks.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "暂无待办事项",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(uiState.tasks, key = { it.id }) { task ->
                            SwipeToDismissTaskItem(
                                task = task,
                                onToggle = { viewModel.toggleTaskCompleted(task) },
                                onDelete = { viewModel.deleteTask(task) },
                            )
                        }
                    }
                }
            }
        }
    }

    // 添加任务对话框
    if (uiState.showAddDialog) {
        TodoForm(
            editingTask = null,
            courses = uiState.courses,
            onDismiss = { viewModel.hideAddDialog() },
            onSave = { viewModel.addTask(it) },
        )
    }
}

@Composable
fun FilterBar(
    currentFilter: TodoFilter,
    selectedCourseId: Long?,
    onFilterChange: (TodoFilter) -> Unit,
    onCourseFilterChange: (Long?) -> Unit,
) {
    val filters = listOf(
        TodoFilter.ALL to "全部",
        TodoFilter.TODAY to "今日",
        TodoFilter.HIGH_PRIORITY to "高优先",
        TodoFilter.UNCOMPLETED to "未完成",
        TodoFilter.COMPLETED to "已完成",
    )

    Surface(shadowElevation = 2.dp) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 主筛选
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                filters.forEach { (filter, label) ->
                    FilterChip(
                        selected = currentFilter == filter,
                        onClick = { onFilterChange(filter) },
                        label = { Text(label, fontSize = 13.sp) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDismissTaskItem(
    task: Task,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    // 划线动画
    val animatedProgress by animateFloatAsState(
        targetValue = if (task.isCompleted) 1f else 0f,
        animationSpec = tween(durationMillis = 400, easing = EaseInOutCubic),
        label = "strikethrough",
    )
    val textColor by animateColorAsState(
        targetValue = if (task.isCompleted)
            MaterialTheme.colorScheme.onSurfaceVariant
        else
            MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(300),
        label = "textColor",
    )

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "删除",
                    modifier = Modifier.padding(end = 20.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
        enableDismissFromStartToEnd = false,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (task.isCompleted)
                    MaterialTheme.colorScheme.surfaceVariant
                else
                    MaterialTheme.colorScheme.surface,
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 完成复选框
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                    ),
                )

                Spacer(modifier = Modifier.width(8.dp))

                // 优先级指示条
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(PriorityColors[task.priority.coerceIn(0, 3)]),
                )

                Spacer(modifier = Modifier.width(12.dp))

                // 内容
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.drawWithContent {
                            drawContent()
                            if (animatedProgress > 0f) {
                                val lineY = size.height / 2f
                                drawLine(
                                    color = textColor,
                                    start = androidx.compose.ui.geometry.Offset(0f, lineY),
                                    end = androidx.compose.ui.geometry.Offset(size.width * animatedProgress, lineY),
                                    strokeWidth = 2f,
                                )
                            }
                        },
                    )
                    if (task.description.isNotBlank()) {
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (task.deadline != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (task.deadline.before(Date()))
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(task.deadline!!),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (task.deadline.before(Date()))
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // 优先级标签
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = PriorityColors[task.priority.coerceIn(0, 3)].copy(alpha = 0.15f),
                ) {
                    Text(
                        text = when (task.priority) {
                            0 -> "无"
                            1 -> "低"
                            2 -> "中"
                            3 -> "高"
                            else -> ""
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = PriorityColors[task.priority.coerceIn(0, 3)],
                    )
                }
            }
        }
    }
}
