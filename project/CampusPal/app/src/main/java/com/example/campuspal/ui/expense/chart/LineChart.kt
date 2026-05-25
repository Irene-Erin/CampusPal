package com.example.campuspal.ui.expense.chart

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campuspal.ui.theme.ExpenseRed

@Composable
fun LineChartCard(
    title: String,
    subtitle: String = "",
    points: List<LinePoint>,
) {
    val animProgress by animateFloatAsState(1f, tween(700), label = "line")
    val maxVal = points.maxOf { it.value }.coerceAtLeast(1.0)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            if (title.isNotBlank()) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
            }
            Spacer(Modifier.height(8.dp))

            if (points.isEmpty() || maxVal <= 0) {
                Text("暂无数据", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 20.dp))
            } else {
                val themePrimary = MaterialTheme.colorScheme.primary
                val textColor = MaterialTheme.colorScheme.onSurfaceVariant
                val pointGap = 48.dp
                val chartW = points.size * pointGap
                val chartH = 140.dp

                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .width(chartW)
                        .height(chartH + 24.dp),
                ) {
                    val w = size.width; val h = chartH.toPx()
                    val leftPad = 8f; val bottomPad = 20f
                    val drawW = w - leftPad * 2; val drawH = h - bottomPad

                    // 面积填充
                    if (points.size >= 2) {
                        val fillPath = Path().apply {
                            moveTo(leftPad, h - bottomPad)
                            points.forEachIndexed { i, pt ->
                                val px = leftPad + (i.toFloat() / (points.size - 1).coerceAtLeast(1)) * drawW
                                val py = h - bottomPad - ((pt.value / maxVal).toFloat() * drawH * animProgress)
                                lineTo(px, py)
                            }
                            lineTo(leftPad + drawW, h - bottomPad)
                            close()
                        }
                        drawPath(fillPath, Brush.verticalGradient(listOf(themePrimary.copy(alpha = 0.15f), themePrimary.copy(alpha = 0f))))
                    }

                    // 折线
                    if (points.size >= 2) {
                        val linePath = Path()
                        points.forEachIndexed { i, pt ->
                            val px = leftPad + (i.toFloat() / (points.size - 1).coerceAtLeast(1)) * drawW
                            val py = h - bottomPad - ((pt.value / maxVal).toFloat() * drawH * animProgress)
                            if (i == 0) linePath.moveTo(px, py) else linePath.lineTo(px, py)
                        }
                        drawPath(linePath, themePrimary, style = Stroke(2.5f, cap = StrokeCap.Round))
                    }

                    // 今日点
                    points.forEachIndexed { i, pt ->
                        if (pt.isToday) {
                            val px = leftPad + (i.toFloat() / (points.size - 1).coerceAtLeast(1)) * drawW
                            val py = h - bottomPad - ((pt.value / maxVal).toFloat() * drawH * animProgress)
                            // 虚线连接到底部
                            drawLine(
                                themePrimary.copy(alpha = 0.3f),
                                Offset(px, py + 2f),
                                Offset(px, h - bottomPad),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 4f)),
                            )
                            // 高亮圆点
                            drawCircle(themePrimary, 6f, Offset(px, py))
                            drawCircle(Color.White, 3f, Offset(px, py))
                        }
                    }
                }

                // X 轴标签
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).width(chartW),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    points.forEach { pt ->
                        Text(
                            pt.label,
                            fontSize = 9.sp,
                            color = textColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(pointGap),
                        )
                    }
                }
            }
        }
    }
}
