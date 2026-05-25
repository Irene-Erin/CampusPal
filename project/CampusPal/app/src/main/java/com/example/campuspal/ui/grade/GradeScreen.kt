package com.example.campuspal.ui.grade

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campuspal.data.db.entity.Course
import com.example.campuspal.data.db.entity.Grade
import com.example.campuspal.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeScreen(viewModel: GradeViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "添加成绩")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // GPA 总览卡片
            item {
                GpaOverviewCard(
                    overallGpa = uiState.overallGpa,
                    gpaStandard = uiState.gpaStandard,
                    semesterGpas = uiState.semesterGpas,
                    totalCredits = uiState.grades.sumOf { it.credits },
                    onStandardChange = { viewModel.setGpaStandard(it) },
                )
            }

            // 学期筛选
            item {
                Text("按学期筛选", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = uiState.selectedSemester == null,
                        onClick = { viewModel.selectSemester(null) },
                        label = { Text("全部") },
                    )
                    uiState.semesters.forEach { semester ->
                        FilterChip(
                            selected = uiState.selectedSemester == semester,
                            onClick = { viewModel.selectSemester(semester) },
                            label = { Text(semester, fontSize = 12.sp) },
                        )
                    }
                }
            }

            // 成绩列表
            if (uiState.grades.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("暂无成绩记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(uiState.grades, key = { it.id }) { grade ->
                    GradeItem(
                        grade = grade,
                        onDelete = { viewModel.deleteGrade(grade) },
                    )
                }
            }
        }
    }

    // 添加成绩对话框
    if (uiState.showAddDialog) {
        AddGradeDialog(
            courses = uiState.courses,
            onDismiss = { viewModel.hideAddDialog() },
            onSave = { viewModel.addGrade(it) },
        )
    }
}

@Composable
fun GpaOverviewCard(
    overallGpa: Double,
    gpaStandard: String,
    semesterGpas: Map<String, Double>,
    totalCredits: Double,
    onStandardChange: (String) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.1f)),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("平均绩点", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                // 标准切换
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("标准", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    listOf("4.0", "5.0").forEach { std ->
                        FilterChip(
                            selected = gpaStandard == std,
                            onClick = { onStandardChange(std) },
                            label = { Text("$std", fontSize = 11.sp) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format("%.2f", overallGpa),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                    )
                    Text("GPA", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "%.0f".format(totalCredits),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = Secondary,
                    )
                    Text("总学分", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // 学期 GPA 变化
            if (semesterGpas.size > 1) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("学期 GPA 变化", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))

                val sortedSemesters = semesterGpas.entries.sortedBy { it.key }
                val maxGpa = sortedSemesters.maxOf { it.value }.coerceAtLeast(0.01)

                sortedSemesters.forEach { (semester, gpa) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = semester,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(80.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        // 进度条
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(16.dp)
                                .padding(end = 8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth((gpa / maxGpa).toFloat().coerceIn(0f, 1f))
                                    .background(
                                        color = when {
                                            gpa >= 3.5 -> SuccessGreen
                                            gpa >= 2.5 -> Tertiary
                                            else -> ErrorRed
                                        },
                                        shape = RoundedCornerShape(4.dp),
                                    ),
                            )
                        }

                        Text(
                            text = String.format("%.2f", gpa),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(36.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GradeItem(grade: Grade, onDelete: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 绩点等级
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when {
                    grade.score >= 90 -> SuccessGreen.copy(alpha = 0.15f)
                    grade.score >= 80 -> Primary.copy(alpha = 0.15f)
                    grade.score >= 70 -> Tertiary.copy(alpha = 0.15f)
                    grade.score >= 60 -> Tertiary.copy(alpha = 0.1f)
                    else -> ErrorRed.copy(alpha = 0.15f)
                },
            ) {
                Text(
                    text = scoreToGradeLevel(grade.score),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = when {
                        grade.score >= 90 -> SuccessGreen
                        grade.score >= 80 -> Primary
                        grade.score >= 70 -> Tertiary
                        grade.score >= 60 -> Tertiary
                        else -> ErrorRed
                    },
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = grade.courseName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row {
                    Text(
                        text = "${grade.score.toInt()}分",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = " · ${grade.credits}学分",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (grade.semester.isNotBlank()) {
                    Text(
                        text = grade.semester,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGradeDialog(
    courses: List<Course>,
    onDismiss: () -> Unit,
    onSave: (Grade) -> Unit,
) {
    var courseName by remember { mutableStateOf("") }
    var score by remember { mutableStateOf("") }
    var credits by remember { mutableStateOf("2.0") }
    var semester by remember { mutableStateOf("") }
    var selectedCourseId by remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加成绩") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // 关联课程
                if (courses.isNotEmpty()) {
                    Text("关联课程", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        courses.take(6).forEach { course ->
                            FilterChip(
                                selected = selectedCourseId == course.id,
                                onClick = {
                                    selectedCourseId = course.id
                                    courseName = course.name
                                },
                                label = { Text(course.name, fontSize = 12.sp) },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = courseName,
                    onValueChange = { courseName = it },
                    label = { Text("课程名称 *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = score,
                        onValueChange = { score = it },
                        label = { Text("分数 *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = credits,
                        onValueChange = { credits = it },
                        label = { Text("学分") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }

                OutlinedTextField(
                    value = semester,
                    onValueChange = { semester = it },
                    label = { Text("学期 (如 2024-2025-1)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val s = score.toDoubleOrNull()
                    if (s != null && s in 0.0..100.0 && courseName.isNotBlank()) {
                        onSave(
                            Grade(
                                courseId = selectedCourseId,
                                courseName = courseName,
                                score = s,
                                gradeLevel = scoreToGradeLevel(s),
                                credits = credits.toDoubleOrNull() ?: 2.0,
                                semester = semester,
                            )
                        )
                    }
                },
                enabled = score.toDoubleOrNull()?.let { it in 0.0..100.0 } == true && courseName.isNotBlank(),
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
