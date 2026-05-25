package com.example.campuspal.ui.expense.chart

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campuspal.ui.theme.ExpenseRed
import com.example.campuspal.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.*

val pieColors = listOf(
    Color(0xFFFF6B6B), Color(0xFF4ECDC4), Color(0xFFFFB347),
    Color(0xFF9B59B6), Color(0xFF4A90D9), Color(0xFF95A5A6),
    Color(0xFFF0775A), Color(0xFF5B9ECF), Color(0xFF6B9E8A),
    Color(0xFFC491D0), Color(0xFFF0A855), Color(0xFF60B0A0),
)

@Composable
fun PieChartCard(slices: List<PieSlice>, totalLabel: String = "总支出") {
    val total = slices.sumOf { it.value }
    val animProgress by animateFloatAsState(1f, tween(600), label = "pie")

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("支出分类统计", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))

            if (total <= 0 || slices.isEmpty()) {
                Text("暂无支出数据", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 24.dp))
            } else {
                Box(Modifier.size(170.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val strokeW = 32f
                        val halfSW = strokeW / 2
                        val arcSize = Size(size.width - strokeW, size.height - strokeW)
                        var startAngle = -90f
                        val sweepTotal = 360f * animProgress
                        var remaining = sweepTotal
                        slices.forEachIndexed { i, slice ->
                            val sweep = ((slice.value / total).toFloat() * 360f).coerceAtMost(remaining)
                            if (sweep > 0f) {
                                drawArc(
                                    pieColors[i % pieColors.size],
                                    startAngle, sweep, false,
                                    Offset(halfSW, halfSW), arcSize,
                                    style = Stroke(strokeW),
                                )
                                startAngle += sweep
                                remaining -= sweep
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("¥%.0f".format(total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(totalLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(12.dp))
                slices.forEachIndexed { i, slice ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        val c = pieColors[i % pieColors.size]
                        Box(Modifier.size(10.dp).background(c, RoundedCornerShape(5.dp)))
                        Spacer(Modifier.width(8.dp))
                        Text(slice.label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Text("¥%.2f".format(slice.value), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(6.dp))
                        Text("${(slice.value / total * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
