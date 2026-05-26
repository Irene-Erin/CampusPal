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

data class ChartLinePoint(
    val label: String,
    val value: Double,
    val isHighlight: Boolean = false,
)

data class ChartPieSlice(
    val label: String,
    val value: Double,
    val color: Long,
)

data class LineChartData(
    val title: String,
    val labels: List<String>,
    val values: List<Double>,
    val highlightIndex: Int = -1,
)

data class PieChartData(
    val title: String,
    val centerLabel: String,
    val totalAmount: Double,
    val slices: List<ChartPieSlice>,
)

data class ViewChartData(
    val lineData: LineChartData? = null,
    val pieData: PieChartData? = null,
)
