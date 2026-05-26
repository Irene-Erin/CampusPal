package com.example.campuspal.ui.learn

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.campuspal.data.db.entity.Course
import com.example.campuspal.data.db.entity.Exam
import com.example.campuspal.ui.components.FormSheetHeader
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamForm(
    editingExam: Exam? = null,
    courses: List<Course> = emptyList(),
    onSave: (Exam) -> Unit,
    onDismiss: () -> Unit,
) {
    val isEditing = editingExam != null
    var name by remember(editingExam) { mutableStateOf(editingExam?.name ?: "") }
    var selectedCourseId by remember(editingExam) { mutableStateOf(editingExam?.courseId) }
    var examTimestamp by remember(editingExam) { mutableLongStateOf(editingExam?.examDate ?: System.currentTimeMillis()) }
    var nameError by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var courseDropdownExpanded by remember { mutableStateOf(false) }

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
                title = if (isEditing) "编辑考试" else "添加考试",
                onSave = {
                    if (canSave) {
                        onSave(
                            Exam(
                                id = editingExam?.id ?: 0L,
                                name = name,
                                courseId = selectedCourseId,
                                examDate = examTimestamp,
                                isPinned = editingExam?.isPinned ?: false,
                            )
                        )
                    } else {
                        nameError = true
                    }
                },
                onDismiss = onDismiss,
                saveEnabled = canSave,
            )

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = false },
                label = { Text("考试名称 *") },
                isError = nameError,
                supportingText = if (nameError) { { Text("请输入考试名称") } } else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )

            // Course dropdown
            if (courses.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = courseDropdownExpanded,
                    onExpandedChange = { courseDropdownExpanded = it },
                ) {
                    val selectedCourseName = courses.find { it.id == selectedCourseId }?.name ?: ""
                    OutlinedTextField(
                        value = selectedCourseName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("关联课程") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                    )
                    ExposedDropdownMenu(
                        expanded = courseDropdownExpanded,
                        onDismissRequest = { courseDropdownExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("无") },
                            onClick = { selectedCourseId = null; courseDropdownExpanded = false },
                        )
                        courses.forEach { course ->
                            DropdownMenuItem(
                                text = { Text(course.name) },
                                onClick = { selectedCourseId = course.id; courseDropdownExpanded = false },
                            )
                        }
                    }
                }
            }

            // Date/Time
            val cal = Calendar.getInstance().apply { timeInMillis = examTimestamp }
            val dateStr = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(cal.time)
            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(cal.time)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = dateStr,
                    onValueChange = {},
                    label = { Text("考试日期 *") },
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.weight(1f).clickable { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
                OutlinedTextField(
                    value = timeStr,
                    onValueChange = {},
                    label = { Text("考试时间 *") },
                    readOnly = true,
                    enabled = false,
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
    }

    if (showDatePicker) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = examTimestamp)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { dateMs ->
                        val oldCal = Calendar.getInstance().apply { timeInMillis = examTimestamp }
                        val newCal = Calendar.getInstance().apply { timeInMillis = dateMs }
                        newCal.set(Calendar.HOUR_OF_DAY, oldCal.get(Calendar.HOUR_OF_DAY))
                        newCal.set(Calendar.MINUTE, oldCal.get(Calendar.MINUTE))
                        examTimestamp = newCal.timeInMillis
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } },
        ) { DatePicker(state = dpState) }
    }

    if (showTimePicker) {
        val tpState = rememberTimePickerState(
            initialHour = Calendar.getInstance().apply { timeInMillis = examTimestamp }.get(Calendar.HOUR_OF_DAY),
            initialMinute = Calendar.getInstance().apply { timeInMillis = examTimestamp }.get(Calendar.MINUTE),
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("选择时间") },
            text = { TimePicker(state = tpState) },
            confirmButton = {
                TextButton(onClick = {
                    val c = Calendar.getInstance().apply { timeInMillis = examTimestamp }
                    c.set(Calendar.HOUR_OF_DAY, tpState.hour)
                    c.set(Calendar.MINUTE, tpState.minute)
                    examTimestamp = c.timeInMillis
                    showTimePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("取消") } },
        )
    }
}
