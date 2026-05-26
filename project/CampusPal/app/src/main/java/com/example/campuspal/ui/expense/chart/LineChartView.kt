package com.example.campuspal.ui.expense.chart

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import com.example.campuspal.data.db.entity.Expense
import java.util.Calendar

class LineChartView(context: Context) : View(context) {

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFBBBBBB.toInt(); strokeWidth = 1.5f }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x15000000; strokeWidth = 1f; style = Paint.Style.STROKE }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF888888.toInt(); textSize = sp(10f) }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF333333.toInt(); textSize = sp(13f); typeface = Typeface.DEFAULT_BOLD }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF0775A.toInt(); strokeWidth = 3f; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val areaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x20F0775A; style = Paint.Style.FILL }
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF0775A.toInt(); style = Paint.Style.FILL }
    private val pointBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.FILL }
    private val touchBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xDD333333.toInt() }
    private val touchTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textSize = sp(11f); typeface = Typeface.DEFAULT_BOLD }
    private val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF0775A.toInt(); strokeWidth = 1.5f; style = Paint.Style.STROKE; pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f) }

    private var chartTitle = ""
    private var labels: List<String> = emptyList()
    private var values: List<Double> = emptyList()
    private var maxValue = 1.0
    private var pointRects = mutableListOf<RectF>()
    private var selectedIndex = -1
    private var primaryColor = 0xFFF0775A.toInt()

    private val padTop get() = dp(36f)
    private val padBottom get() = dp(40f)
    private val padLeft get() = dp(48f)
    private val padRight get() = dp(16f)

    fun setData(title: String, labels: List<String>, values: List<Double>, color: Int = 0xFFF0775A.toInt()) {
        chartTitle = title
        this.labels = labels
        this.values = values
        maxValue = values.maxOrNull()?.takeIf { it > 0 } ?: 1.0
        selectedIndex = -1
        primaryColor = color
        updateColors(color)
        requestLayout()
        invalidate()
    }

    fun setTimeRange(range: String, expenses: List<Expense>) {
        val cal = Calendar.getInstance()
        when (range) {
            "year" -> {
                labels = (1..12).map { "${it}月" }
                val sums = DoubleArray(12)
                expenses.filter { it.type == "expense" }.forEach {
                    cal.timeInMillis = it.timestamp
                    sums[cal.get(Calendar.MONTH)] += it.amount
                }
                values = sums.toList()
            }
            "month" -> {
                val days = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                labels = (1..days).map { "$it" }
                val sums = DoubleArray(days)
                expenses.filter { it.type == "expense" }.forEach {
                    cal.timeInMillis = it.timestamp
                    sums[cal.get(Calendar.DAY_OF_MONTH) - 1] += it.amount
                }
                values = sums.toList()
            }
            "week" -> {
                labels = listOf("一", "二", "三", "四", "五", "六", "日")
                val sums = DoubleArray(7)
                cal.firstDayOfWeek = Calendar.MONDAY
                expenses.filter { it.type == "expense" }.forEach {
                    cal.timeInMillis = it.timestamp
                    val dayIdx = when (cal.get(Calendar.DAY_OF_WEEK)) {
                        Calendar.MONDAY -> 0; Calendar.TUESDAY -> 1; Calendar.WEDNESDAY -> 2
                        Calendar.THURSDAY -> 3; Calendar.FRIDAY -> 4; Calendar.SATURDAY -> 5
                        Calendar.SUNDAY -> 6; else -> -1
                    }
                    if (dayIdx in 0..6) sums[dayIdx] += it.amount
                }
                values = sums.toList()
            }
            else -> {
                labels = emptyList(); values = emptyList()
            }
        }
        maxValue = values.maxOrNull()?.takeIf { it > 0 } ?: 1.0
        selectedIndex = -1
        requestLayout()
        invalidate()
    }

    private fun updateColors(color: Int) {
        linePaint.color = color
        pointPaint.color = color
        areaPaint.color = color and 0x00FFFFFF or (0x20 shl 24)
        dashPaint.color = color
    }

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val w = if (MeasureSpec.getMode(wSpec) == MeasureSpec.UNSPECIFIED) dp(350f).toInt()
        else MeasureSpec.getSize(wSpec).coerceAtLeast(dp(280f).toInt())
        val h = if (MeasureSpec.getMode(hSpec) == MeasureSpec.UNSPECIFIED) dp(220f).toInt()
        else MeasureSpec.getSize(hSpec)
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (labels.isEmpty()) return
        val w = width.toFloat(); val h = height.toFloat()
        val cTop = padTop; val cBot = h - padBottom; val cLeft = padLeft; val cRight = w - padRight
        val cW = cRight - cLeft; val cH = cBot - cTop

        if (chartTitle.isNotEmpty()) { canvas.drawText(chartTitle, cLeft, dp(18f), titlePaint) }

        val steps = niceScale(maxValue)
        labelPaint.textAlign = Paint.Align.RIGHT
        for (i in 0..steps.size) {
            val y = cBot - i * cH / steps.size
            if (i > 0) { canvas.drawLine(cLeft, y, cRight, y, gridPaint) }
            canvas.drawText(yLabel(steps, i), cLeft - dp(6f), y + labelPaint.textSize / 3f, labelPaint)
        }

        canvas.drawLine(cLeft, cBot, cRight, cBot, axisPaint)
        canvas.drawLine(cLeft, cTop, cLeft, cBot, axisPaint)

        labelPaint.textAlign = Paint.Align.CENTER
        val showEvery = (labels.size / 10 + 1)
        labels.forEachIndexed { i, lb ->
            if (i % showEvery == 0) {
                val x = cLeft + (i.toFloat() / (labels.size - 1).coerceAtLeast(1)) * cW
                canvas.drawText(lb, x, cBot + dp(14f), labelPaint)
            }
        }

        val pts = values.mapIndexed { i, v ->
            val x = cLeft + (i.toFloat() / (labels.size - 1).coerceAtLeast(1)) * cW
            val y = if (maxValue > 0) cBot - (v / maxValue * cH).toFloat() else cBot
            x to y
        }

        val linePath = Path(); var lineStarted = false
        val areaPath = Path(); var areaStarted = false
        var lastValidX = 0f
        pts.forEachIndexed { i, (x, y) ->
            if (values[i] > 0) {
                if (!lineStarted) { linePath.moveTo(x, y); lineStarted = true } else linePath.lineTo(x, y)
                if (!areaStarted) { areaPath.moveTo(x, cBot); areaPath.lineTo(x, y); areaStarted = true } else areaPath.lineTo(x, y)
                lastValidX = x
            }
        }
        if (areaStarted) { areaPath.lineTo(lastValidX, cBot); areaPath.close(); canvas.drawPath(areaPath, areaPaint) }
        if (lineStarted) canvas.drawPath(linePath, linePaint)

        pointRects.clear()
        pts.forEachIndexed { i, (x, y) ->
            pointRects.add(RectF(x - dp(12f), y - dp(12f), x + dp(12f), y + dp(12f)))
            if (values[i] > 0) {
                canvas.drawCircle(x, y, dp(5f), pointPaint)
                canvas.drawCircle(x, y, dp(2.5f), pointBorderPaint)
            }
        }

        if (selectedIndex in pts.indices && values[selectedIndex] > 0) {
            val (sx, sy) = pts[selectedIndex]
            canvas.drawLine(sx, sy + dp(6f), sx, cBot, dashPaint)
            val txt = "¥%.0f".format(values[selectedIndex])
            val tw = touchTextPaint.measureText(txt)
            val th = touchTextPaint.textSize
            val bw = tw + dp(14f); val bh = th + dp(10f)
            var bx = sx - bw / 2
            if (bx < cLeft) bx = cLeft; if (bx + bw > cRight) bx = cRight - bw
            canvas.drawRoundRect(RectF(bx, sy - bh - dp(8f), bx + bw, sy - dp(8f)), dp(6f), dp(6f), touchBgPaint)
            canvas.drawText(txt, bx + bw / 2, sy - bh / 2 - dp(4f), touchTextPaint.apply { textAlign = Paint.Align.CENTER })
            touchTextPaint.textAlign = Paint.Align.LEFT
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> return true
            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {
                val cLeft = padLeft
                val cW = width - padLeft - padRight
                val x = event.x
                if (x < cLeft || x > cLeft + cW) { selectedIndex = -1; invalidate(); return false }
                val ratio = (x - cLeft) / cW
                val idx = (ratio * (labels.size - 1)).toInt().coerceIn(0, labels.size - 1)
                selectedIndex = if (event.action == MotionEvent.ACTION_UP && selectedIndex == idx) -1 else idx
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun niceScale(max: Double): List<Double> {
        if (max <= 0) return listOf(0.0)
        val raw = max / 4
        val mag = Math.pow(10.0, kotlin.math.floor(kotlin.math.ln(raw) / kotlin.math.ln(10.0)))
        val norm = raw / mag
        val step = when { norm <= 1.5 -> 1.0; norm <= 3.5 -> 2.0; norm <= 7.5 -> 5.0; else -> 10.0 } * mag
        return (0..4).map { it * step }
    }

    private fun yLabel(steps: List<Double>, i: Int): String {
        val v = steps.getOrElse(i) { 0.0 }
        return if (v >= 10000) "%.0fk".format(v / 1000) else if (v == 0.0) "0" else "%.0f".format(v)
    }

    private fun dp(v: Float) = v * resources.displayMetrics.density
    private fun sp(v: Float) = v * resources.displayMetrics.scaledDensity
}
