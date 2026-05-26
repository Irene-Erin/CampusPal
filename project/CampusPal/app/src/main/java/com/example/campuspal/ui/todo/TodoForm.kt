package com.example.campuspal.ui.todo

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.campuspal.data.db.entity.Course
import com.example.campuspal.data.db.entity.Task
import com.example.campuspal.ui.components.FormSheetHeader
import java.text.SimpleDateFormat
import java.util.*

val PriorityOptions = listOf(
    0 to ("无" to Color(0xFF607D8B)),
    1 to ("低" to Color(0xFF4CAF7C)),
    2 to ("中" to Color(0xFFFFB347)),
    3 to ("高" to Color(0xFFE05550)),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoForm(
    editingTask: Task? = null,
    courses: List<Course> = emptyList(),
    onSave: (Task) -> Unit,
    onDismiss: () -> Unit,
) {
    val isEditing = editingTask != null
    var title by remember(editingTask) { mutableStateOf(editingTask?.title ?: "") }
    var description by remember(editingTask) { mutableStateOf(editingTask?.description ?: "") }
    var priority by remember(editingTask) { mutableIntStateOf(editingTask?.priority ?: 0) }
    var selectedCourseId by remember(editingTask) { mutableStateOf(editingTask?.courseId) }
    var hasDeadline by remember(editingTask) { mutableStateOf(editingTask?.deadline != null) }
    var deadlineTimestamp by remember(editingTask) {
        mutableLongStateOf(editingTask?.deadline?.time ?: System.currentTimeMillis())
    }
    var titleError by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val canSave = title.isNotBlank()

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
                title = if (isEditing) "编辑待办" else "添加待办",
                onSave = {
                    if (canSave) {
                        onSave(Task(
                            id = editingTask?.id ?: 0L,
                            title = title,
                            description = description,
                            priority = priority,
                            isCompleted = editingTask?.isCompleted ?: false,
                            courseId = selectedCourseId,
                            deadline = if (hasDeadline) Date(deadlineTimestamp) else null,
                        ))
                    } else { titleError = true }
                },
                onDismiss = onDismiss,
                saveEnabled = canSave,
            )

            // Title
            OutlinedTextField(
                value = title, onValueChange = { title = it; titleError = false },
                label = { Text("标题 *") }, isError = titleError,
                supportingText = if (titleError) { { Text("请输入标题") } } else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )

            // Description
            OutlinedTextField(
                value = description, onValueChange = { description = it },
                label = { Text("描述") }, minLines = 2, maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )

            // Priority
            Text("优先级", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PriorityOptions.forEach { (value, labelAndColor) ->
                    val (label, color) = labelAndColor
                    FilterChip(
                        selected = priority == value,
                        onClick = { priority = value },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color.copy(alpha = 0.15f),
                            selectedLabelColor = color,
                        ),
                    )
                }
            }

            // Deadline toggle + picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("设置截止时间", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = hasDeadline, onCheckedChange = { hasDeadline = it })
            }

            if (hasDeadline) {
                val cal = Calendar.getInstance().apply { timeInMillis = deadlineTimestamp }
                val dateStr = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(cal.time)
                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(cal.time)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = dateStr, onValueChange = {},
                        label = { Text("截止日期") }, readOnly = true, enabled = false,
                        modifier = Modifier.weight(1f).clickable { showDatePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                    OutlinedTextField(
                        value = timeStr, onValueChange = {},
                        label = { Text("截止时间") }, readOnly = true, enabled = false,
                        modifier = Modifier.weight(1f).clickable { showTimePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }

            // Course association
            if (courses.isNotEmpty()) {
                Text("关联课程", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedCourseId == null,
                        onClick = { selectedCourseId = null },
                        label = { Text("无") },
                    )
                    courses.forEach { course ->
                        FilterChip(
                            selected = selectedCourseId == course.id,
                            onClick = { selectedCourseId = course.id },
                            label = { Text(course.name) },
                        )
                    }
                }
            }
        }
    }

    // Date/Time pickers
    if (showDatePicker) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = deadlineTimestamp)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { dateMs ->
                        val oldCal = Calendar.getInstance().apply { timeInMillis = deadlineTimestamp }
                        val newCal = Calendar.getInstance().apply { timeInMillis = dateMs }
                        newCal.set(Calendar.HOUR_OF_DAY, oldCal.get(Calendar.HOUR_OF_DAY))
                        newCal.set(Calendar.MINUTE, oldCal.get(Calendar.MINUTE))
                        deadlineTimestamp = newCal.timeInMillis
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } },
        ) { DatePicker(state = dpState) }
    }

    if (showTimePicker) {
        val tpState = rememberTimePickerState(
            initialHour = Calendar.getInstance().apply { timeInMillis = deadlineTimestamp }.get(Calendar.HOUR_OF_DAY),
            initialMinute = Calendar.getInstance().apply { timeInMillis = deadlineTimestamp }.get(Calendar.MINUTE),
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("选择时间") },
            text = { TimePicker(state = tpState) },
            confirmButton = {
                TextButton(onClick = {
                    val c = Calendar.getInstance().apply { timeInMillis = deadlineTimestamp }
                    c.set(Calendar.HOUR_OF_DAY, tpState.hour)
                    c.set(Calendar.MINUTE, tpState.minute)
                    deadlineTimestamp = c.timeInMillis
                    showTimePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("取消") } },
        )
    }
}
