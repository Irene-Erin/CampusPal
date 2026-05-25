package com.example.campuspal.ui.schedule

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campuspal.data.db.entity.Course
import com.example.campuspal.ui.theme.CourseColors
import java.util.*

// 时间段定义
data class TimeSlot(val index: Int, val label: String, val startMinutes: Int)

val timeSlots = (0..13).map { i ->
    val totalMinutes = 8 * 60 + i * 50
    val hour = totalMinutes / 60
    val minute = totalMinutes % 60
    TimeSlot(
        index = i,
        label = String.format("%02d:%02d", hour, minute),
        startMinutes = totalMinutes,
    )
}

val dayLabels = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(viewModel: ScheduleViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部周次选择器
        WeekSelector(
            currentWeek = uiState.currentWeek,
            onPrev = { viewModel.prevWeek() },
            onNext = { viewModel.nextWeek() },
        )

        // 周视图网格
        WeekGrid(
            currentWeek = uiState.currentWeek,
            viewModel = viewModel,
            onCourseClick = { viewModel.showDetail(it) },
        )
    }

    // 悬浮按钮 — 现代化设计
    FloatingActionButton(
        onClick = { viewModel.showAddDialog() },
        modifier = Modifier
            .padding(16.dp)
            .navigationBarsPadding()
            .size(60.dp),
        containerColor = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(18.dp),
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp,
        ),
    ) {
        Icon(Icons.Filled.Add, contentDescription = "添加课程", modifier = Modifier.size(28.dp))
    }

    // 课程详情对话框
    if (uiState.showDetailDialog && uiState.selectedCourse != null) {
        CourseDetailDialog(
            course = uiState.selectedCourse!!,
            onDismiss = { viewModel.hideDetail() },
            onDelete = { viewModel.deleteCourse(it) },
        )
    }

    // 添加课程对话框
    if (uiState.showAddDialog) {
        val allCourses by viewModel.getAllCourses().collectAsState(initial = emptyList())
        AddCourseDialog(
            existingCourses = allCourses,
            onDismiss = { viewModel.hideAddDialog() },
            onSave = { viewModel.addCourse(it) },
        )
    }
}

@Composable
fun WeekSelector(currentWeek: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.clickable { onPrev() },
            ) {
                Icon(
                    Icons.Filled.ChevronLeft,
                    contentDescription = "上一周",
                    modifier = Modifier.padding(8.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "第 ${currentWeek} 周",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "本学期",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.clickable { onNext() },
            ) {
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = "下一周",
                    modifier = Modifier.padding(8.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun WeekGrid(
    currentWeek: Int,
    viewModel: ScheduleViewModel,
    onCourseClick: (Course) -> Unit,
) {
    val slotHeight = 60.dp
    val timeWidth = 52.dp
    val dayWidth = 120.dp

    // 为每一天加载课程
    val dayCoursesMap = remember(currentWeek) {
        mutableStateMapOf<Int, List<Course>>()
    }

    for (day in 1..7) {
        val courses by viewModel.getCoursesForDay(day).collectAsState(initial = emptyList())
        dayCoursesMap[day] = courses
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 表头：星期 — 圆角设计
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(timeWidth))
            val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            val todayIndex = when (today) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                Calendar.SUNDAY -> 7
                else -> -1
            }
            dayLabels.forEachIndexed { index, day ->
                val isToday = (index + 1) == todayIndex
                Surface(
                    modifier = Modifier
                        .width(dayWidth)
                        .height(40.dp),
                    color = if (isToday)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(topStart = if (index == 0) 10.dp else 0.dp, topEnd = if (index == 6) 10.dp else 0.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = day,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isToday)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }

        // 内容区域：时间列 + 课程网格
        Row(modifier = Modifier.fillMaxSize()) {
            // 时间列
            LazyColumn(modifier = Modifier.width(timeWidth)) {
                items(timeSlots) { slot ->
                    Box(
                        modifier = Modifier
                            .width(timeWidth)
                            .height(slotHeight)
                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                        contentAlignment = Alignment.TopEnd,
                    ) {
                        Text(
                            text = slot.label,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 4.dp, top = 2.dp),
                        )
                    }
                }
            }

            // 每天列（可水平滚动）
            val horizontalScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(horizontalScrollState),
            ) {
                for (day in 1..7) {
                    DayColumn(
                        day = day,
                        courses = dayCoursesMap[day] ?: emptyList(),
                        slotHeight = slotHeight,
                        dayWidth = dayWidth,
                        onCourseClick = onCourseClick,
                    )
                }
            }
        }
    }
}

@Composable
fun DayColumn(
    day: Int,
    courses: List<Course>,
    slotHeight: androidx.compose.ui.unit.Dp,
    dayWidth: androidx.compose.ui.unit.Dp,
    onCourseClick: (Course) -> Unit,
) {
    val slotHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { slotHeight.toPx() }
    var pressedCourseId by remember { mutableStateOf<Long?>(null) }

    Box(modifier = Modifier.width(dayWidth)) {
        // 背景网格
        Column {
            timeSlots.forEach { slot ->
                Box(
                    modifier = Modifier
                        .width(dayWidth)
                        .height(slotHeight)
                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }

        // 课程块
        courses.forEach { course ->
            val startMin = timeToMinutes(course.startTime)
            val endMin = timeToMinutes(course.endTime)
            val baseMin = 8 * 60
            val offsetPx = ((startMin - baseMin).toFloat() / 50f) * slotHeightPx
            val heightPx = ((endMin - startMin).toFloat() / 50f) * slotHeightPx
            val scale by animateFloatAsState(
                targetValue = if (pressedCourseId == course.id) 1.05f else 1f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
                label = "courseScale",
            )

            Box(
                modifier = Modifier
                    .offset(y = with(androidx.compose.ui.platform.LocalDensity.current) { (offsetPx / slotHeightPx * slotHeight.toPx()).toDp() })
                    .padding(horizontal = 2.dp)
                    .width(dayWidth - 4.dp)
                    .height(with(androidx.compose.ui.platform.LocalDensity.current) { heightPx.toDp() })
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(course.color))
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clickable {
                        pressedCourseId = course.id
                        onCourseClick(course)
                    }
                    .padding(4.dp),
            ) {
                Column {
                    Text(
                        text = course.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (course.location.isNotBlank()) {
                        Text(
                            text = course.location,
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

fun timeToMinutes(time: String): Int {
    val parts = time.split(":")
    return parts[0].toInt() * 60 + parts[1].toInt()
}

@Composable
fun CourseDetailDialog(
    course: Course,
    onDismiss: () -> Unit,
    onDelete: (Course) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(course.name, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                DetailRow("教师", course.teacher)
                DetailRow("地点", course.location)
                DetailRow("时间", "${dayLabels[course.dayOfWeek - 1]} ${course.startTime}-${course.endTime}")
                DetailRow("周次", "第${course.startWeek}-${course.endWeek}周")
                DetailRow("类型", when (course.weekType) {
                    "odd" -> "单周"
                    "even" -> "双周"
                    else -> "每周"
                })
            }
        },
        confirmButton = {
            TextButton(onClick = { onDelete(course) }) {
                Text("删除", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    if (value.isNotBlank()) {
        Row(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(
                text = "$label：",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCourseDialog(
    existingCourses: List<Course>,
    onDismiss: () -> Unit,
    onSave: (Course) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var teacher by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var dayOfWeek by remember { mutableStateOf(1) }
    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("08:50") }
    var startWeek by remember { mutableStateOf("1") }
    var endWeek by remember { mutableStateOf("16") }
    var weekType by remember { mutableStateOf("every") }
    var selectedColorIndex by remember { mutableStateOf(0) }
    var showOverlapWarning by remember { mutableStateOf(false) }
    var timeError by remember { mutableStateOf<String?>(null) }

    val weekTypeOptions = listOf("every" to "每周", "odd" to "单周", "even" to "双周")

    fun validate(): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        name = trimmed
        teacher = teacher.trim()
        location = location.trim()

        val sMin = timeToMinutes(startTime.trim())
        val eMin = timeToMinutes(endTime.trim())
        if (eMin <= sMin) {
            timeError = "结束时间必须晚于开始时间"
            return false
        }
        timeError = null
        return true
    }

    fun checkOverlap(): Boolean {
        val sMin = timeToMinutes(startTime.trim())
        val eMin = timeToMinutes(endTime.trim())
        val sWeek = startWeek.trim().toIntOrNull() ?: 1
        val eWeek = endWeek.trim().toIntOrNull() ?: 16

        return existingCourses.any { course ->
            course.dayOfWeek == dayOfWeek && course.id != 0L &&
            // 检查周次是否重叠
            course.startWeek <= eWeek && course.endWeek >= sWeek &&
            // 检查周类型是否冲突
            (weekType == "every" || course.weekType == "every" || weekType == course.weekType) &&
            // 检查时间段是否重叠
            timeToMinutes(course.startTime) < eMin &&
            timeToMinutes(course.endTime) > sMin
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加课程") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 450.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("课程名称 *") },
                    singleLine = true,
                    isError = name.trim().isEmpty() && name.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(value = teacher, onValueChange = { teacher = it }, label = { Text("教师") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("地点") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                // 星期选择
                Text("上课日", style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    dayLabels.forEachIndexed { index, label ->
                        FilterChip(selected = dayOfWeek == index + 1, onClick = { dayOfWeek = index + 1 }, label = { Text(label, fontSize = 12.sp) })
                    }
                }

                // 时间
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime, onValueChange = { startTime = it },
                        label = { Text("开始") }, singleLine = true, modifier = Modifier.weight(1f),
                        isError = timeError != null, placeholder = { Text("HH:mm") },
                    )
                    OutlinedTextField(
                        value = endTime, onValueChange = { endTime = it },
                        label = { Text("结束") }, singleLine = true, modifier = Modifier.weight(1f),
                        isError = timeError != null, placeholder = { Text("HH:mm") },
                    )
                }
                if (timeError != null) {
                    Text(timeError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                // 周次
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = startWeek, onValueChange = { startWeek = it }, label = { Text("开始周") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = endWeek, onValueChange = { endWeek = it }, label = { Text("结束周") }, singleLine = true, modifier = Modifier.weight(1f))
                }

                // 周类型
                Text("周类型", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    weekTypeOptions.forEach { (value, label) ->
                        FilterChip(selected = weekType == value, onClick = { weekType = value }, label = { Text(label) })
                    }
                }

                // 颜色选择
                Text("颜色", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CourseColors.forEachIndexed { index, color ->
                        Box(
                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(16.dp)).background(color)
                                .border(if (index == selectedColorIndex) 3.dp else 0.dp, Color.White, RoundedCornerShape(16.dp))
                                .border(if (index == selectedColorIndex) 2.dp else 0.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                                .clickable { selectedColorIndex = index },
                        )
                    }
                }

                // 重叠警告
                if (showOverlapWarning) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("与已有课程时间段重叠，是否继续添加？", color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!validate()) return@TextButton
                    if (!showOverlapWarning && checkOverlap()) {
                        showOverlapWarning = true
                        return@TextButton
                    }
                    onSave(
                        Course(
                            name = name.trim(), teacher = teacher.trim(), location = location.trim(),
                            dayOfWeek = dayOfWeek, startTime = startTime.trim(), endTime = endTime.trim(),
                            startWeek = startWeek.trim().toIntOrNull() ?: 1, endWeek = endWeek.trim().toIntOrNull() ?: 16,
                            weekType = weekType, color = CourseColors[selectedColorIndex].hashCode(),
                        )
                    )
                },
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
