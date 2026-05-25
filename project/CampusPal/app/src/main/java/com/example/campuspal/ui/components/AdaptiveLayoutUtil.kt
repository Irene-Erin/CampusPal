package com.example.campuspal.ui.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 窗口尺寸枚举
 */
enum class WindowSize { COMPACT, MEDIUM, EXPANDED }

/**
 * 是否为平板级别
 */
fun WindowSize.isTablet(): Boolean = this == WindowSize.MEDIUM || this == WindowSize.EXPANDED

/**
 * 根据 dp 宽度判断窗口尺寸
 */
fun classifyWindowSize(maxWidth: Dp): WindowSize = when {
    maxWidth < 600.dp -> WindowSize.COMPACT
    maxWidth < 840.dp -> WindowSize.MEDIUM
    else -> WindowSize.EXPANDED
}

/**
 * 设计令牌 — 统一管理所有尺寸，避免硬编码 dp
 */
object AppDimens {
    // 间距
    val spacingXs: Dp = 4.dp
    val spacingSm: Dp = 8.dp
    val spacingMd: Dp = 12.dp
    val spacingLg: Dp = 16.dp
    val spacingXl: Dp = 20.dp
    val spacingXxl: Dp = 24.dp

    // 圆角
    val cornerSm: Dp = 6.dp
    val cornerMd: Dp = 10.dp
    val cornerLg: Dp = 16.dp
    val cornerXl: Dp = 20.dp
    val cornerPill: Dp = 999.dp

    // 卡片
    val cardElevation: Dp = 3.dp
    val cardElevationPressed: Dp = 6.dp

    // 图标
    val iconSm: Dp = 16.dp
    val iconMd: Dp = 24.dp
    val iconLg: Dp = 32.dp
    val iconContainer: Dp = 40.dp

    // 组件
    val fabSize: Dp = 60.dp
    val fabCorner: Dp = 18.dp
    val chipHeight: Dp = 32.dp
    val progressHeight: Dp = 8.dp
    val progressHeightSm: Dp = 4.dp
    val colorBar: Dp = 4.dp

    // 课程表
    val scheduleTimeWidth: Dp = 52.dp
    val scheduleDayWidthPhone: Dp = 110.dp
    val scheduleDayWidthTablet: Dp = 140.dp
    val scheduleSlotHeight: Dp = 60.dp

    // 平板双窗格比例
    const val listPaneFraction = 0.4f
    const val detailPaneFraction = 0.6f
}
