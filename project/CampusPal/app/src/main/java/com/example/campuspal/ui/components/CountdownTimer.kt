package com.example.campuspal.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.campuspal.ui.theme.ErrorRed
import com.example.campuspal.ui.theme.SuccessGreen
import kotlinx.coroutines.delay

/**
 * 实时倒计时组件 — 每秒刷新，精确到秒
 *
 * @param targetMillis 目标时间戳（毫秒）
 * @param modifier Modifier
 * @param showLabels 是否显示"天/时/分/秒"标签
 * @param compact 紧凑模式（只显示 HH:mm:ss，不显示天数）
 */
@Composable
fun CountdownTimer(
    targetMillis: Long,
    modifier: Modifier = Modifier,
    showLabels: Boolean = true,
    compact: Boolean = false,
) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(targetMillis) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val remaining = remember(now, targetMillis) { (targetMillis - now).coerceAtLeast(0L) }
    val isExpired = targetMillis <= now
    val isUrgent = !isExpired && remaining < 24 * 60 * 60 * 1000L // < 24 小时

    val textColor by animateColorAsState(
        targetValue = when {
            isExpired -> SuccessGreen
            isUrgent -> ErrorRed
            else -> MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(300),
        label = "countdownColor",
    )

    if (isExpired) {
        Text(
            text = "考试中",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = SuccessGreen,
            modifier = modifier,
        )
        return
    }

    val days = remaining / (24 * 60 * 60 * 1000L)
    val hours = (remaining % (24 * 60 * 60 * 1000L)) / (60 * 60 * 1000L)
    val minutes = (remaining % (60 * 60 * 1000L)) / (60 * 1000L)
    val seconds = (remaining % (60 * 1000L)) / 1000L

    val monoStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = if (compact) 14.sp else 16.sp,
        color = textColor,
        textAlign = TextAlign.Center,
    )
    val labelStyle = TextStyle(
        fontSize = if (compact) 9.sp else 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    val colonSize = (monoStyle.fontSize.value - 2).sp

    Row(modifier = modifier, verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
        if (!compact && days > 0) {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                BasicText(text = String.format("%02d", days), style = monoStyle)
                if (showLabels) BasicText(text = "天", style = labelStyle)
            }
            BasicText(text = ":", style = monoStyle.copy(fontSize = colonSize))
        }
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            BasicText(text = String.format("%02d", hours + (if (compact) days * 24 else 0)), style = monoStyle)
            if (showLabels) BasicText(text = "时", style = labelStyle)
        }
        BasicText(text = ":", style = monoStyle.copy(fontSize = colonSize))
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            BasicText(text = String.format("%02d", minutes), style = monoStyle)
            if (showLabels) BasicText(text = "分", style = labelStyle)
        }
        BasicText(text = ":", style = monoStyle.copy(fontSize = colonSize))
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            BasicText(text = String.format("%02d", seconds), style = monoStyle)
            if (showLabels) BasicText(text = "秒", style = labelStyle)
        }
    }
}
