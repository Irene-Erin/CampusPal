package com.example.campuspal.ui.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campuspal.data.db.entity.Expense
import com.example.campuspal.ui.components.FormSheetHeader
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseForm(
    editingExpense: Expense? = null,
    onSave: (Expense) -> Unit,
    onDismiss: () -> Unit,
) {
    val isEditing = editingExpense != null
    var amount by remember(editingExpense) { mutableStateOf(editingExpense?.amount?.toString() ?: "") }
    var selectedType by remember(editingExpense) { mutableStateOf(editingExpense?.type ?: "expense") }
    var selectedCategory by remember(editingExpense) { mutableStateOf(editingExpense?.category ?: expenseCategories[0].name) }
    var note by remember(editingExpense) { mutableStateOf(editingExpense?.note ?: "") }
    var expenseTimestamp by remember(editingExpense) { mutableLongStateOf(editingExpense?.timestamp ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val amountValue = amount.toDoubleOrNull() ?: 0.0
    val canSave = amountValue > 0

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
                title = if (isEditing) "编辑记录" else "快速记账",
                onSave = {
                    if (canSave) {
                        onSave(
                            Expense(
                                id = editingExpense?.id ?: 0L,
                                amount = amountValue,
                                category = selectedCategory,
                                note = note,
                                type = selectedType,
                                timestamp = expenseTimestamp,
                            )
                        )
                    } else {
                        amountError = true
                    }
                },
                onDismiss = onDismiss,
                saveEnabled = canSave,
            )

            // Type selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                SegmentedButtonRow(
                    selected = selectedType,
                    onSelect = { selectedType = it },
                    options = listOf("expense" to "支出", "income" to "收入"),
                )
            }

            // Amount
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it; amountError = false },
                label = { Text("金额 *") },
                leadingIcon = { Text("¥", style = MaterialTheme.typography.titleMedium) },
                isError = amountError,
                supportingText = if (amountError) { { Text("请输入有效金额") } } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )

            // Category grid
            Text("分类", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                expenseCategories.forEach { cat ->
                    val isSelected = selectedCategory == cat.name
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedCategory = cat.name }
                            .background(if (isSelected) cat.color.copy(alpha = 0.15f) else Color.Transparent)
                            .padding(10.dp),
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                                .background(if (isSelected) cat.color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(cat.icon, cat.name, tint = if (isSelected) cat.color else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(cat.name, fontSize = 11.sp, color = if (isSelected) cat.color else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Note
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )

            // Date/Time
            val cal = Calendar.getInstance().apply { timeInMillis = expenseTimestamp }
            val dateStr = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(cal.time)
            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(cal.time)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = dateStr,
                    onValueChange = {},
                    label = { Text("日期") },
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
                    label = { Text("时间") },
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

    // Date picker dialog
    if (showDatePicker) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = expenseTimestamp)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { dateMs ->
                        val oldCal = Calendar.getInstance().apply { timeInMillis = expenseTimestamp }
                        val newCal = Calendar.getInstance().apply { timeInMillis = dateMs }
                        newCal.set(Calendar.HOUR_OF_DAY, oldCal.get(Calendar.HOUR_OF_DAY))
                        newCal.set(Calendar.MINUTE, oldCal.get(Calendar.MINUTE))
                        expenseTimestamp = newCal.timeInMillis
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } },
        ) { DatePicker(state = dpState) }
    }

    // Time picker dialog
    if (showTimePicker) {
        val tpState = rememberTimePickerState(
            initialHour = Calendar.getInstance().apply { timeInMillis = expenseTimestamp }.get(Calendar.HOUR_OF_DAY),
            initialMinute = Calendar.getInstance().apply { timeInMillis = expenseTimestamp }.get(Calendar.MINUTE),
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("选择时间") },
            text = { TimePicker(state = tpState) },
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance().apply { timeInMillis = expenseTimestamp }
                    cal.set(Calendar.HOUR_OF_DAY, tpState.hour)
                    cal.set(Calendar.MINUTE, tpState.minute)
                    expenseTimestamp = cal.timeInMillis
                    showTimePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun SegmentedButtonRow(
    selected: String,
    onSelect: (String) -> Unit,
    options: List<Pair<String, String>>,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (value, label) ->
            val isSelected = selected == value
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelect(value) }
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
