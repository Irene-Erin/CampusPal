package com.example.campuspal.ui.expense.chart

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campuspal.ui.theme.ExpenseRed

@Composable
fun BarChartCard(
    title: String,
    bars: List<BarEntry>,
    formatPrefix: String = "¥",
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val animProgress by animateFloatAsState(1f, tween(500), label = "bar")
    val maxVal = bars.maxOf { it.value }.coerceAtLeast(1.0)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))

            if (bars.isEmpty()) {
                Text("暂无数据", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 20.dp))
            } else {
                val barH = 140.dp
                val barW = 28.dp
                val gap = 12.dp
                val chartW = (barW + gap) * bars.size
                val themePrimary = MaterialTheme.colorScheme.primary
                val textColor = MaterialTheme.colorScheme.onSurfaceVariant

                Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    Column {
                        Row(
                            Modifier.width(chartW).height(barH),
                            horizontalArrangement = Arrangement.spacedBy(gap),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            bars.forEachIndexed { i, bar ->
                                val barProgress = animProgress * (bar.value / maxVal).toFloat()
                                val barColor = if (bar.isHighlight) themePrimary else textColor.copy(alpha = 0.35f)
                                val isSelected = selectedIndex == i

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        Modifier
                                            .width(barW)
                                            .height(barH * barProgress.coerceAtLeast(0.02f))
                                            .background(
                                                if (isSelected) barColor.copy(alpha = 0.7f) else barColor,
                                                RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                            )
                                            .pointerInput(i) {
                                                detectTapGestures { selectedIndex = if (selectedIndex == i) null else i }
                                            },
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        // X 轴标签
                        Row(Modifier.width(chartW), horizontalArrangement = Arrangement.spacedBy(gap)) {
                            bars.forEach { bar ->
                                Text(
                                    bar.label,
                                    fontSize = 10.sp,
                                    color = textColor,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(barW),
                                )
                            }
                        }
                    }
                }

                // Tooltip
                if (selectedIndex != null) {
                    val sel = bars[selectedIndex!!]
                    Spacer(Modifier.height(8.dp))
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(
                            "${sel.label}: $formatPrefix%.2f".format(sel.value),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}
