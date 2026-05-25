package com.example.campuspal.ui.expense.chart

data class PieSlice(
    val label: String,
    val value: Double,
    val color: Long,
)

data class BarEntry(
    val label: String,
    val value: Double,
    val isHighlight: Boolean = false,
)

data class LinePoint(
    val label: String,
    val value: Double,
    val isToday: Boolean = false,
)
