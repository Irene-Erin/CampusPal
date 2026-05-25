package com.example.campuspal.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campuspal.ui.theme.ColorSchemeType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // 文件选择器
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importData(it, context) }
    }

    // Snackbar 消息
    LaunchedEffect(uiState.exportMessage) {
        uiState.exportMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(uiState.importMessage) {
        uiState.importMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 外观设置
            item {
                SettingsGroupTitle("外观")
            }

            // 深色主题
            item {
                SettingsItem(
                    icon = Icons.Filled.DarkMode,
                    title = "深色主题",
                    subtitle = if (uiState.isDarkTheme) "已开启" else "已关闭",
                    trailing = {
                        Switch(
                            checked = uiState.isDarkTheme,
                            onCheckedChange = { viewModel.setDarkTheme(it) },
                        )
                    },
                )
            }

            // 配色方案
            item {
                Text("配色方案", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ColorSchemeType.entries.forEach { scheme ->
                        val colors = when (scheme) {
                            ColorSchemeType.SUNSET -> listOf(0xFFF0775A, 0xFF6B9E8A, 0xFFF0A855)
                            ColorSchemeType.OCEAN -> listOf(0xFF3B6BA5, 0xFF5E9B8C, 0xFFC5896E)
                            ColorSchemeType.CHERRY -> listOf(0xFFD4687C, 0xFFC49BA0, 0xFFD4956A)
                            ColorSchemeType.FOREST -> listOf(0xFF4A8C6F, 0xFF8BAA7C, 0xFFC49B5E)
                        }
                        Card(
                            modifier = Modifier
                                .size(72.dp)
                                .clickable { viewModel.setColorScheme(scheme.name) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (uiState.colorScheme == scheme.name)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                            ),
                            border = if (uiState.colorScheme == scheme.name)
                                CardDefaults.outlinedCardBorder()
                            else null,
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    colors.forEach { c ->
                                        Surface(
                                            modifier = Modifier.size(14.dp),
                                            shape = RoundedCornerShape(7.dp),
                                            color = androidx.compose.ui.graphics.Color(c.toInt()),
                                        ) {}
                                    }
                                }
                                Text(
                                    scheme.label,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }

            // 数据设置
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item { SettingsGroupTitle("数据") }

            // GPA 标准
            item {
                Text("GPA 计算标准", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("4.0", "5.0").forEach { std ->
                        FilterChip(
                            selected = uiState.gpaStandard == std,
                            onClick = { viewModel.setGpaStandard(std) },
                            label = { Text("$std 制") },
                        )
                    }
                }
            }

            // 月度预算
            item {
                SettingsItem(
                    icon = Icons.Filled.AccountBalanceWallet,
                    title = "月度预算",
                    subtitle = "¥%.0f".format(uiState.monthlyBudget),
                    onClick = { viewModel.showBudgetDialog() },
                )
            }

            // 导出数据
            item {
                SettingsItem(
                    icon = Icons.Filled.FileDownload,
                    title = "导出数据",
                    subtitle = "备份为 JSON 文件",
                    onClick = { viewModel.exportData(context) },
                )
            }

            // 导入数据
            item {
                SettingsItem(
                    icon = Icons.Filled.FileUpload,
                    title = "导入数据",
                    subtitle = "从 JSON 文件恢复",
                    onClick = { filePickerLauncher.launch("application/json") },
                )
            }

            // 关于
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item { SettingsGroupTitle("关于") }

            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "CampusPal",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "版本 1.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "校园生活管理助手\n课程表 · 待办 · 记账 · 学习计时 · 成绩管理",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    // 预算设置对话框
    if (uiState.showBudgetDialog) {
        BudgetDialog(
            currentBudget = uiState.monthlyBudget,
            onDismiss = { viewModel.hideBudgetDialog() },
            onSave = { viewModel.setBudget(it) },
        )
    }
}

@Composable
fun SettingsGroupTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onClick ?: {},
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (trailing != null) {
                trailing()
            } else if (onClick != null) {
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun BudgetDialog(
    currentBudget: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
) {
    var amount by remember { mutableStateOf(currentBudget.toInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置月度预算") },
        text = {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("预算金额") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Text("¥") },
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    amount.toDoubleOrNull()?.let { onSave(it) }
                },
                enabled = (amount.toDoubleOrNull() ?: 0.0) > 0,
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
