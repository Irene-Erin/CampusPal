package com.example.campuspal.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.campuspal.data.db.entity.Course
import com.example.campuspal.ui.components.FormSheetHeader
import java.util.Calendar

val CourseColors = listOf(
    0xFF4A90D9.toInt(), 0xFFE05550.toInt(), 0xFF4CAF7C.toInt(), 0xFFFFB347.toInt(),
    0xFF9B59B6.toInt(), 0xFF00BCD4.toInt(), 0xFFFF6B6B.toInt(), 0xFF607D8B.toInt(),
)

val DayLabels = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
val WeekTypeLabels = listOf("every" to "每周", "odd" to "单周", "even" to "双周")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseForm(
    editingCourse: Course? = null,
    onSave: (Course) -> Unit,
    onDismiss: () -> Unit,
) {
    val isEditing = editingCourse != null
    var name by remember(editingCourse) { mutableStateOf(editingCourse?.name ?: "") }
    var teacher by remember(editingCourse) { mutableStateOf(editingCourse?.teacher ?: "") }
    var location by remember(editingCourse) { mutableStateOf(editingCourse?.location ?: "") }
    var selectedDay by remember(editingCourse) { mutableIntStateOf(editingCourse?.dayOfWeek ?: 1) }
    var startTime by remember(editingCourse) { mutableStateOf(editingCourse?.startTime ?: "08:00") }
    var endTime by remember(editingCourse) { mutableStateOf(editingCourse?.endTime ?: "09:40") }
    var startWeek by remember(editingCourse) { mutableIntStateOf(editingCourse?.startWeek ?: 1) }
    var endWeek by remember(editingCourse) { mutableIntStateOf(editingCourse?.endWeek ?: 16) }
    var weekType by remember(editingCourse) { mutableStateOf(editingCourse?.weekType ?: "every") }
    var selectedColor by remember(editingCourse) { mutableIntStateOf(editingCourse?.color ?: CourseColors[0]) }
    var nameError by remember { mutableStateOf(false) }

    val canSave = name.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FormSheetHeader(
                title = if (isEditing) "编辑课程" else "添加课程",
                onSave = {
                    if (canSave) {
                        onSave(Course(
                            id = editingCourse?.id ?: 0L,
                            name = name, teacher = teacher, location = location,
                            dayOfWeek = selectedDay, startTime = startTime, endTime = endTime,
                            startWeek = startWeek, endWeek = endWeek,
                            weekType = weekType, color = selectedColor,
                        ))
                    } else { nameError = true }
                },
                onDismiss = onDismiss,
                saveEnabled = canSave,
            )

            // Name
            OutlinedTextField(
                value = name, onValueChange = { name = it; nameError = false },
                label = { Text("课程名称 *") }, isError = nameError,
                supportingText = if (nameError) { { Text("请输入课程名称") } } else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )

            // Teacher + Location
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = teacher, onValueChange = { teacher = it },
                    label = { Text("教师") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    singleLine = true, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = location, onValueChange = { location = it },
                    label = { Text("教室") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    singleLine = true, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                )
            }

            // Day of week
            Text("上课星期", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DayLabels.forEachIndexed { i, label ->
                    val day = i + 1
                    val isSelected = selectedDay == day
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedDay = day },
                        label = { Text(label) },
                    )
                }
            }

            // Time
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TimeField("开始时间", startTime, { startTime = it }, Modifier.weight(1f))
                TimeField("结束时间", endTime, { endTime = it }, Modifier.weight(1f))
            }

            // Weeks
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WeekField("起始周", startWeek, { startWeek = it }, Modifier.weight(1f))
                WeekField("结束周", endWeek, { endWeek = it }, Modifier.weight(1f))
            }

            // Week type
            Text("周类型", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WeekTypeLabels.forEach { (value, label) ->
                    FilterChip(selected = weekType == value, onClick = { weekType = value }, label = { Text(label) })
                }
            }

            // Color
            Text("课程颜色", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CourseColors.forEach { color ->
                    val isSelected = selectedColor == color
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(color))
                            .clickable { selectedColor = color }
                            .then(if (isSelected) Modifier.background(Color.White.copy(alpha = 0.3f)) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Text("✓", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var showPicker by remember { mutableStateOf(false) }
    val parts = value.split(":")
    val initHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
    val initMin = parts.getOrNull(1)?.toIntOrNull() ?: 0

    OutlinedTextField(
        value = value, onValueChange = {},
        label = { Text(label) }, readOnly = true, enabled = false,
        modifier = modifier.clickable { showPicker = true },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = MaterialTheme.colorScheme.outline,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )

    if (showPicker) {
        val state = rememberTimePickerState(initialHour = initHour, initialMinute = initMin, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text(label) },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = { onChange("%02d:%02d".format(state.hour, state.minute)); showPicker = false }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun WeekField(label: String, value: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = "$value",
        onValueChange = { v -> v.toIntOrNull()?.let { if (it in 1..30) onChange(it) } },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        singleLine = true,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
    )
}
