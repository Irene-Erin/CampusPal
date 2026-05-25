package com.example.campuspal.ui.learn

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campuspal.data.db.dao.StudyStat
import com.example.campuspal.data.db.entity.Course
import com.example.campuspal.data.db.entity.Exam
import com.example.campuspal.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnScreen(viewModel: LearnViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    // 独立状态 — 确保即时 UI 响应
    val remainingSeconds by viewModel.remainingSeconds.collectAsState()
    val totalSeconds by viewModel.totalSeconds.collectAsState()
    val workDuration by viewModel.workDuration.collectAsState()
    val showAddExamDialog by viewModel.showAddExamDialog.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 番茄钟
        item {
            PomodoroTimerCard(
                isWorkSession = uiState.isWorkSession,
                remainingSeconds = remainingSeconds,
                totalSeconds = totalSeconds,
                timerState = uiState.timerState,
                workDuration = workDuration,
                courses = uiState.courses,
                selectedCourseId = uiState.selectedCourseId,
                onStart = { viewModel.startTimer() },
                onPause = { viewModel.pauseTimer() },
                onReset = { viewModel.resetTimer() },
                onWorkDurationChange = { viewModel.setWorkDuration(it) },
                onCourseSelect = { viewModel.setSelectedCourse(it) },
            )
        }

        // 考试倒计时
        item {
            ExamCountdownSection(
                exams = uiState.exams,
                courses = uiState.courses,
                onAdd = { viewModel.showAddExam() },
                onTogglePin = { viewModel.togglePin(it) },
                onDelete = { viewModel.deleteExam(it) },
            )
        }

        // 学习统计
        item {
            StudyStatsSection(
                stats = uiState.studyStats,
                totalMinutes = uiState.totalStudyMinutes,
                courses = uiState.courses,
            )
        }
    }

    // 添加考试对话框
    if (showAddExamDialog) {
        AddExamDialog(
            courses = uiState.courses,
            onDismiss = { viewModel.hideAddExam() },
            onSave = { viewModel.addExam(it) },
        )
    }
}

@Composable
fun PomodoroTimerCard(
    isWorkSession: Boolean,
    remainingSeconds: Int,
    totalSeconds: Int,
    timerState: TimerState,
    workDuration: Int,
    courses: List<Course>,
    selectedCourseId: Long?,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onWorkDurationChange: (Int) -> Unit,
    onCourseSelect: (Long?) -> Unit,
) {
    val progress = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds else 0f
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timerColor = if (isWorkSession) Primary else SuccessGreen
    var showCustomDialog by remember { mutableStateOf(false) }
    var customInput by remember { mutableStateOf("") }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Timer, contentDescription = null, tint = timerColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isWorkSession) "番茄钟" else "休息",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // 时长选择 — 独立行，可水平滚动
            if (isWorkSession && timerState == TimerState.IDLE) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    listOf(15, 25, 45, 60).forEach { min ->
                        FilterChip(
                            selected = workDuration == min,
                            onClick = { onWorkDurationChange(min) },
                            label = { Text("${min} 分钟", fontSize = 12.sp) },
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    // 自定义时间按钮
                    FilterChip(
                        selected = workDuration !in listOf(15, 25, 45, 60),
                        onClick = { showCustomDialog = true },
                        label = { Text("自定义", fontSize = 12.sp) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 圆形进度条
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 12f
                    // 背景圆环
                    drawArc(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                    // 进度圆环
                    if (progress > 0) {
                        drawArc(
                            color = timerColor,
                            startAngle = -90f,
                            sweepAngle = progress * 360f,
                            useCenter = false,
                            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                            size = Size(size.width - strokeWidth, size.height - strokeWidth),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = timerColor,
                    )
                    Text(
                        text = if (isWorkSession) "专注中" else "休息中",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 关联课程
            if (courses.isNotEmpty() && isWorkSession) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    FilterChip(
                        selected = selectedCourseId == null,
                        onClick = { onCourseSelect(null) },
                        label = { Text("全部", fontSize = 11.sp) },
                    )
                    courses.take(6).forEach { course ->
                        FilterChip(
                            selected = selectedCourseId == course.id,
                            onClick = { onCourseSelect(course.id) },
                            label = { Text(course.name, fontSize = 11.sp) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 控制按钮
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (timerState == TimerState.RUNNING) {
                    FilledTonalButton(onClick = onPause) {
                        Icon(Icons.Filled.Pause, contentDescription = "暂停")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("暂停")
                    }
                } else {
                    FilledTonalButton(onClick = onStart) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "开始")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (timerState == TimerState.PAUSED) "继续" else "开始")
                    }
                }
                OutlinedButton(onClick = onReset) {
                    Icon(Icons.Filled.Stop, contentDescription = "重置")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("重置")
                }
            }
        }
    }

    // 自定义时长对话框
    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("自定义时长") },
            text = {
                OutlinedTextField(
                    value = customInput,
                    onValueChange = { customInput = it.filter { c -> c.isDigit() } },
                    label = { Text("分钟 (1-120)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val mins = customInput.toIntOrNull()
                        if (mins != null && mins in 1..120) {
                            onWorkDurationChange(mins)
                            showCustomDialog = false
                            customInput = ""
                        }
                    },
                    enabled = (customInput.toIntOrNull() ?: 0) in 1..120,
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCustomDialog = false
                    customInput = ""
                }) { Text("取消") }
            },
        )
    }
}

@Composable
fun ExamCountdownSection(
    exams: List<Exam>,
    courses: List<Course>,
    onAdd: () -> Unit,
    onTogglePin: (Exam) -> Unit,
    onDelete: (Exam) -> Unit,
) {
    val courseMap = courses.associateBy { it.id }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Event, contentDescription = null, tint = Tertiary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("考试倒计时", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = onAdd) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("添加")
                }
            }

            if (exams.isEmpty()) {
                Text(
                    "暂无考试",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                exams.forEach { exam ->
                    val daysLeft = ((exam.examDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).toInt()
                    val courseName = exam.courseId?.let { courseMap[it]?.name } ?: ""

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 置顶按钮
                        IconButton(
                            onClick = { onTogglePin(exam) },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                Icons.Filled.PushPin,
                                contentDescription = "置顶",
                                tint = if (exam.isPinned) Tertiary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(16.dp),
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = exam.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (courseName.isNotBlank()) {
                                Text(
                                    text = courseName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(exam.examDate)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        // 剩余天数
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                daysLeft < 0 -> ErrorRed.copy(alpha = 0.1f)
                                daysLeft <= 3 -> ErrorRed.copy(alpha = 0.1f)
                                daysLeft <= 7 -> Tertiary.copy(alpha = 0.1f)
                                else -> SuccessGreen.copy(alpha = 0.1f)
                            },
                        ) {
                            Text(
                                text = when {
                                    daysLeft < 0 -> "已结束"
                                    daysLeft == 0 -> "今天"
                                    daysLeft == 1 -> "明天"
                                    else -> "${daysLeft}天"
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = when {
                                    daysLeft < 0 -> ErrorRed
                                    daysLeft <= 3 -> ErrorRed
                                    daysLeft <= 7 -> Tertiary
                                    else -> SuccessGreen
                                },
                            )
                        }

                        // 删除
                        IconButton(
                            onClick = { onDelete(exam) },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "删除",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudyStatsSection(
    stats: List<StudyStat>,
    totalMinutes: Int,
    courses: List<Course>,
) {
    val courseMap = courses.associateBy { it.id }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Insights, contentDescription = null, tint = Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("学习统计", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 本周总计
            Text(
                text = "本周专注 ${(totalMinutes / 60)} 小时 ${(totalMinutes % 60)} 分钟",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (stats.isEmpty()) {
                Text(
                    "暂无学习记录",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                // 简易柱状图
                val maxMinutes = stats.maxOf { it.total }.toFloat().coerceAtLeast(1f)
                val barColors = listOf(Primary, Secondary, Tertiary, ExpenseRed, SuccessGreen)

                stats.forEachIndexed { index, stat ->
                    val courseName = stat.courseId?.let { courseMap[it]?.name } ?: "无课程"
                    val barFraction = stat.total / maxMinutes

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = courseName,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(60.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(barFraction)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(barColors[index % barColors.size]),
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "${stat.total} min",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(44.dp),
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExamDialog(
    courses: List<Course>,
    onDismiss: () -> Unit,
    onSave: (Exam) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selectedCourseId by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(Date()) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加考试") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("考试名称 *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (courses.isNotEmpty()) {
                    Text("关联课程", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        FilterChip(
                            selected = selectedCourseId == null,
                            onClick = { selectedCourseId = null },
                            label = { Text("无", fontSize = 12.sp) },
                        )
                        courses.take(6).forEach { course ->
                            FilterChip(
                                selected = selectedCourseId == course.id,
                                onClick = { selectedCourseId = course.id },
                                label = { Text(course.name, fontSize = 12.sp) },
                            )
                        }
                    }
                }

                TextButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(selectedDate))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(Exam(name = name, courseId = selectedCourseId, examDate = selectedDate.time))
                    }
                },
                enabled = name.isNotBlank(),
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = millis
                        cal.set(Calendar.HOUR_OF_DAY, selectedDate.hours)
                        cal.set(Calendar.MINUTE, selectedDate.minutes)
                        selectedDate = cal.time
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } },
        ) { DatePicker(state = datePickerState) }
    }
}
